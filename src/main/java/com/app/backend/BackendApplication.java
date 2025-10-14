package com.app.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class BackendApplication {
    private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner logStartup(ApplicationContext ctx) {
        return _ -> {
            if (ctx instanceof WebServerApplicationContext webCtx) {
                int port = webCtx.getWebServer().getPort();
                logger.info("Active profile: {}", Arrays.toString(ctx.getEnvironment().getActiveProfiles()));
                logger.info(" Server started successfully on port {}", port);
            } else {
                logger.info(" ApplicationContext is not a WebServerApplicationContext (probably running in tests)");
            }
        };
    }

}