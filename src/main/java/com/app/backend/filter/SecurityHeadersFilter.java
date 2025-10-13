package com.app.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  // Stałe nagłówków bezpieczeństwa
  private static final String HSTS = "max-age=31536000; includeSubDomains";
  private static final String X_CONTENT_TYPE_OPTIONS = "nosniff";
  private static final String X_FRAME_OPTIONS = "SAMEORIGIN";
  private static final String REFERRER_POLICY = "no-referrer";
  private static final String PERMISSIONS_POLICY = "geolocation=(), microphone=(), camera=()";
  private static final String X_POWERED_BY = "Spring Boot";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // HSTS
    response.setHeader("Strict-Transport-Security", HSTS);

    // MIME type sniffing
    response.setHeader("X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS);

    // iframe
    response.setHeader("X-Frame-Options", X_FRAME_OPTIONS);

    // Referrer policy
    response.setHeader("Referrer-Policy", REFERRER_POLICY);

    // Permissions policy
    response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);

    // Wyłączenie X-Powered-By
    response.setHeader("X-Powered-By", X_POWERED_BY);

    filterChain.doFilter(request, response);
  }
}
