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

package com.tzw.tgframework.tunnel.server;

import com.tzw.tgframework.remote.server.Handler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * Adapts a {@link HttpTunnelServer} to a {@link Handler}.
 */
public class HttpTunnelServerHandler implements Handler {

	private HttpTunnelServer server;

	/**
	 * Create a new {@link HttpTunnelServerHandler} instance.
	 * @param server the server to adapt
	 */
	public HttpTunnelServerHandler(HttpTunnelServer server) {
		Assert.notNull(server, "Server must not be null");
		this.server = server;
	}

	@Override
	public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		this.server.handle(request, response);
	}

}
