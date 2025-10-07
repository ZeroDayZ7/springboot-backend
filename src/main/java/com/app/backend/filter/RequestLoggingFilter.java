package com.app.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  @SuppressWarnings("null")
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    long start = System.currentTimeMillis();

    String clientIp = extractClientIp(request);
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String query = request.getQueryString();
    if (query != null)
      uri += "?" + query;
    String userAgent = request.getHeader("User-Agent");

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;
      int status = response.getStatus();

      logger.info("method={} uri={} ip={} status={} timeMs={} ua=\"{}\"",
          method, uri, clientIp, status, duration, userAgent);
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
