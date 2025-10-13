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
@Order(3) // Wyższy numer = później w chain, więc logi i rate limiter będą wcześniej
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // Strict Transport Security (HSTS)
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

    // Zapobiega MIME type sniffing
    response.setHeader("X-Content-Type-Options", "nosniff");

    // Zapobiega osadzaniu w iframe z innych domen
    response.setHeader("X-Frame-Options", "SAMEORIGIN");

    // Polityka referrera
    response.setHeader("Referrer-Policy", "no-referrer");

    // Permissions policy (ograniczenie dostępu do niektórych API przeglądarki)
    response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

    filterChain.doFilter(request, response);
  }
}
