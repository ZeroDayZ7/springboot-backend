package com.app.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(4)
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String uri = request.getRequestURI();
    if ("/favicon.ico".equals(uri)) {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      response.setHeader("Content-Length", "0");
      return;
    }

    long start = System.currentTimeMillis();

    // Pobierz lub wygeneruj correlation ID
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    // Ustawienie w MDC
    MDC.put("correlationId", correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;
      int status = response.getStatus();
      String clientIp = extractClientIp(request);
      String method = request.getMethod();
      String query = request.getQueryString();
      if (query != null) {
        uri += "?" + query;
      }
      String userAgent = request.getHeader("User-Agent");

      logger.info("method={} uri={} ip={} status={} timeMs={} ua=\"{}\"",
          method, uri, clientIp, status, duration, userAgent);

      MDC.remove("correlationId");
    }
  }

  private String extractClientIp(HttpServletRequest request) {
    String xfwd = request.getHeader("X-Forwarded-For");
    if (xfwd != null && !xfwd.isBlank()) {
      return xfwd.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp;
    }
    return request.getRemoteAddr();
  }
}
