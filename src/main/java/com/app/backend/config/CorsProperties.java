package com.app.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Configuration
@Validated
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

  private static final Logger logger = LoggerFactory.getLogger(CorsProperties.class);

  @NotEmpty(message = "CORS allowedOrigins cannot be empty")
  private List<String> allowedOrigins;

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @PostConstruct
  public void logCorsOrigins() {
    logger.info("Loaded CORS allowed origins: {}", allowedOrigins);
  }
}
