package com.tzw.tgframework;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tzw.tgframework.remote.client.RemoteClientConfiguration;
import com.tzw.tgframework.restart.RestartInitializer;
import com.tzw.tgframework.restart.RestartScopeInitializer;
import com.tzw.tgframework.restart.Restarter;
import org.springframework.boot.Banner;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.AnsiOutputApplicationListener;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.logging.ClasspathLoggingApplicationListener;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;

/**
 * Application that can be used to establish a link to remotely running Spring Boot code.
 * @see RemoteClientConfiguration
 */
public final class RemoteSpringApplication {

    private RemoteSpringApplication() {
    }

    private void run(String[] args) {
        Restarter.initialize(args, RestartInitializer.NONE);
        SpringApplication application = new SpringApplication(RemoteClientConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBanner(getBanner());
        application.setInitializers(getInitializers());
        application.setListeners(getListeners());
        application.run(args);
        waitIndefinitely();
    }

    private Collection<ApplicationContextInitializer<?>> getInitializers() {
        List<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
        initializers.add(new RestartScopeInitializer());
        return initializers;
    }

    private Collection<ApplicationListener<?>> getListeners() {
        List<ApplicationListener<?>> listeners = new ArrayList<>();
        listeners.add(new AnsiOutputApplicationListener());
        listeners.add(new ConfigFileApplicationListener());
        listeners.add(new ClasspathLoggingApplicationListener());
        listeners.add(new LoggingApplicationListener());
        listeners.add(new RemoteUrlPropertyExtractor());
        return listeners;
    }

    private Banner getBanner() {
        ClassPathResource banner = new ClassPathResource("remote-banner.txt", RemoteSpringApplication.class);
        return new ResourceBanner(banner);
    }

    private void waitIndefinitely() {
        while (true) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Run the {@link RemoteSpringApplication}.
     * @param args the program arguments (including the remote URL as a non-option
     * argument)
     */
    public static void main(String[] args) {
        new RemoteSpringApplication().run(args);
    }

}
