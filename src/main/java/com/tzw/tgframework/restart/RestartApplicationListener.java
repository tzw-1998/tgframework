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

package com.tzw.tgframework.restart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogMessage;
import org.springframework.data.redis.core.RedisTemplate;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * {@link ApplicationListener} to initialize the {@link Restarter}.
 * @see Restarter
 */
public class RestartApplicationListener implements ApplicationListener<ApplicationEvent>, Ordered {

	private static final String ENABLED_PROPERTY = "spring.devtools.restart.enabled";

	private static final Log logger = LogFactory.getLog(RestartApplicationListener.class);

	private int order = HIGHEST_PRECEDENCE;

	@Autowired
	private RedisTemplate<String,Object> tgredisTemplate;

	@Value("${server.port}")
	private String serverPort;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		StringBuffer url = new StringBuffer("http://");
		try {
			InetAddress address = InetAddress.getLocalHost();
			url.append(address.getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		url.append(":");
		url.append(serverPort);
		tgredisTemplate.opsForSet().add("remoteUrl",url.toString());
		if (event instanceof ApplicationStartingEvent) {
			onApplicationStartingEvent((ApplicationStartingEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent((ApplicationPreparedEvent) event);
		}
		if (event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) {
			Restarter.getInstance().finish();
		}
		if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent((ApplicationFailedEvent) event);
		}
	}

	//初始化Restarter
	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		// It's too early to use the Spring environment but we should still allow
		// users to disable restart using a System property.
		String enabled = System.getProperty(ENABLED_PROPERTY);
		if (enabled == null || Boolean.parseBoolean(enabled)) {
			String[] args = event.getArgs();
			DefaultRestartInitializer initializer = new DefaultRestartInitializer();
			boolean restartOnInitialize = !AgentReloader.isActive();
			if (!restartOnInitialize) {
				logger.info("Restart disabled due to an agent-based reloader being active");
			}
			Restarter.initialize(args, false, initializer, restartOnInitialize);
		}
		else {
			logger.info(LogMessage.format("Restart disabled due to System property '%s' being set to false",
					ENABLED_PROPERTY));
			Restarter.disable();
		}
	}

	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		Restarter.getInstance().prepare(event.getApplicationContext());
	}

	private void onApplicationFailedEvent(ApplicationFailedEvent event) {
		Restarter.getInstance().remove(event.getApplicationContext());
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order of the listener.
	 * @param order the order of the listener
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
