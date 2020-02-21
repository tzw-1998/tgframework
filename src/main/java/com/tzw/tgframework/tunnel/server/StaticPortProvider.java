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

import org.springframework.util.Assert;

/**
 * {@link PortProvider} for a static port that won't change.
 */
public class StaticPortProvider implements PortProvider {

	private final int port;

	public StaticPortProvider(int port) {
		Assert.isTrue(port > 0, "Port must be positive");
		this.port = port;
	}

	@Override
	public int getPort() {
		return this.port;
	}

}
