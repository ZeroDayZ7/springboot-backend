package com.app.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(5)
public class ErrorLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    try {
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      // Logujemy błąd globalnie z podstawowymi info o request
      logger.error("Exception caught in request [{} {}]: {}",
          request.getMethod(),
          request.getRequestURI(),
          ex.getMessage(), ex);

      // Opcjonalnie można ustawić własny status lub body w response
      if (!response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Internal server error");
      }

      // Rethrow, aby global handler (np. @ControllerAdvice) też miał szansę obsłużyć
      throw ex;
    }
  }
}
