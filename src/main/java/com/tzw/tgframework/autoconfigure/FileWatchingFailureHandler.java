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

package com.tzw.tgframework.autoconfigure;



import com.tzw.tgframework.classpath.ClassPathFolders;
import com.tzw.tgframework.filewatch.ChangedFiles;
import com.tzw.tgframework.filewatch.FileChangeListener;
import com.tzw.tgframework.filewatch.FileSystemWatcher;
import com.tzw.tgframework.filewatch.FileSystemWatcherFactory;
import com.tzw.tgframework.restart.FailureHandler;
import com.tzw.tgframework.restart.Restarter;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * {@link FailureHandler} that waits for filesystem changes before retrying.
 *
 */
class FileWatchingFailureHandler implements FailureHandler {

	private final FileSystemWatcherFactory fileSystemWatcherFactory;

	FileWatchingFailureHandler(FileSystemWatcherFactory fileSystemWatcherFactory) {
		this.fileSystemWatcherFactory = fileSystemWatcherFactory;
	}

	@Override
	public Outcome handle(Throwable failure) {
		CountDownLatch latch = new CountDownLatch(1);
		FileSystemWatcher watcher = this.fileSystemWatcherFactory.getFileSystemWatcher();
		watcher.addSourceFolders(new ClassPathFolders(Restarter.getInstance().getInitialUrls()));
		watcher.addListener(new Listener(latch));
		watcher.start();
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return Outcome.RETRY;
	}

	private static class Listener implements FileChangeListener {

		private final CountDownLatch latch;

		Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onChange(Set<ChangedFiles> changeSet) {
			this.latch.countDown();
		}

	}

}
