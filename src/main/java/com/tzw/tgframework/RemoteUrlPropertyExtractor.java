package com.tzw.tgframework;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.data.redis.core.RedisTemplate;


/**
 * {@link ApplicationListener} to extract the remote URL for the
 * {@link RemoteSpringApplication} to use.

 */
class RemoteUrlPropertyExtractor implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Autowired
    private RedisTemplate<String,Object> tgredisTemplate;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Set<Object> urls = tgredisTemplate.opsForSet().members("remoteUrl");
        StringBuffer stringBuffer = new StringBuffer();
        for (Object object: urls) {
            stringBuffer.append((String)object);
            stringBuffer.append(",");
        }
        String url = stringBuffer.toString();
        Map<String, Object> source = Collections.singletonMap("remoteUrl", (Object) url);
        PropertySource<?> propertySource = new MapPropertySource("remoteUrl", source);
        environment.getPropertySources().addLast(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
