/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzw.tgframework.remote.client;

import com.tzw.tgframework.classpath.ClassPathChangedEvent;
import com.tzw.tgframework.filewatch.ChangedFile;
import com.tzw.tgframework.filewatch.ChangedFiles;
import com.tzw.tgframework.restart.classloader.ClassLoaderFile;
import com.tzw.tgframework.restart.classloader.ClassLoaderFiles;
import com.tzw.tgframework.restart.classloader.ClassLoaderFile.Kind;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Listens and pushes any classpath updates to a remote endpoint.
 */
public class ClassPathChangeUploader implements ApplicationListener<ClassPathChangedEvent> {

	private static final Map<ChangedFile.Type, ClassLoaderFile.Kind> TYPE_MAPPINGS;

	static {
		Map<ChangedFile.Type, ClassLoaderFile.Kind> map = new EnumMap<>(ChangedFile.Type.class);
		map.put(ChangedFile.Type.ADD, ClassLoaderFile.Kind.ADDED);
		map.put(ChangedFile.Type.DELETE, ClassLoaderFile.Kind.DELETED);
		map.put(ChangedFile.Type.MODIFY, ClassLoaderFile.Kind.MODIFIED);
		TYPE_MAPPINGS = Collections.unmodifiableMap(map);
	}

	private static final Log logger = LogFactory.getLog(ClassPathChangeUploader.class);

	private final URI[] uris;

	private final ClientHttpRequestFactory requestFactory;

	public ClassPathChangeUploader(String[] remoteUrls, ClientHttpRequestFactory requestFactory) {
		try {
			URI[] uris = new URI[remoteUrls.length];
			for (int i = 0;i<remoteUrls.length;i++){
				uris[i] = new URL(remoteUrls[i]).toURI();
			}
			this.uris = uris;
		}
		catch (URISyntaxException | MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL '" + remoteUrls + "'");
		}
		this.requestFactory = requestFactory;
	}
	//响应ClassPathChangeEvent
	@Override
	public void onApplicationEvent(ClassPathChangedEvent event) {
		try {
			ClassLoaderFiles classLoaderFiles = getClassLoaderFiles(event);
			byte[] bytes = serialize(classLoaderFiles);
			performUpload(classLoaderFiles, bytes);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	//http请求传输字节码
	private void performUpload(ClassLoaderFiles classLoaderFiles, byte[] bytes) throws IOException {
		try {
			while (true) {
				try {
					for (URI uri: this.uris) {
						ClientHttpRequest request = this.requestFactory.createRequest(uri, HttpMethod.POST);
						HttpHeaders headers = request.getHeaders();
						headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
						headers.setContentLength(bytes.length);
						FileCopyUtils.copy(bytes, request.getBody());
						ClientHttpResponse response = request.execute();
						HttpStatus statusCode = response.getStatusCode();
						Assert.state(statusCode == HttpStatus.OK,
								() -> "Unexpected " + statusCode + " response uploading class files");
						logUpload(classLoaderFiles);
					}
					return;
				}
				catch (SocketException ex) {
					logger.debug("Upload failure", ex);
					Thread.sleep(2000);
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	private void logUpload(ClassLoaderFiles classLoaderFiles) {
		int size = classLoaderFiles.size();
		logger.info(LogMessage.format("Uploaded %s class %s", size, (size != 1) ? "resources" : "resource"));
	}
	//序列化
	private byte[] serialize(ClassLoaderFiles classLoaderFiles) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(classLoaderFiles);
		objectOutputStream.close();
		return outputStream.toByteArray();
	}
	//返回更改过的文件集合
	private ClassLoaderFiles getClassLoaderFiles(ClassPathChangedEvent event) throws IOException {
		ClassLoaderFiles files = new ClassLoaderFiles();
		for (ChangedFiles changedFiles : event.getChangeSet()) {
			String sourceFolder = changedFiles.getSourceFolder().getAbsolutePath();
			for (ChangedFile changedFile : changedFiles) {
				files.addFile(sourceFolder, changedFile.getRelativeName(), asClassLoaderFile(changedFile));
			}
		}
		return files;
	}
	//如果文件状态是删除 则为null 并把时间设置为当前时间并返回
	private ClassLoaderFile asClassLoaderFile(ChangedFile changedFile) throws IOException {
		ClassLoaderFile.Kind kind = TYPE_MAPPINGS.get(changedFile.getType());
		byte[] bytes = (kind != Kind.DELETED) ? FileCopyUtils.copyToByteArray(changedFile.getFile()) : null;
		long lastModified = (kind != Kind.DELETED) ? changedFile.getFile().lastModified() : System.currentTimeMillis();
		return new ClassLoaderFile(kind, lastModified, bytes);
	}

}
