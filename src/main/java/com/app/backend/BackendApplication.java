package com.app.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class BackendApplication {
    private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BackendApplication.class, args);

        int port = ((WebServerApplicationContext) context).getWebServer().getPort();

        logger.info("✅ Server started successfully on port {}", port);
    }
}
