package com.app.backend.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GlobalRateLimiterFilter extends OncePerRequestFilter {

  @Value("${ratelimiter.requests}")
  private int requests;

  @Value("${ratelimiter.durationMinutes}")
  private int durationMinutes;

  private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

  private Bucket createNewBucket() {
    Refill refill = Refill.greedy(requests, Duration.ofMinutes(durationMinutes));
    Bandwidth limit = Bandwidth.classic(requests, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket resolveBucket(String ip) {
    return cache.computeIfAbsent(ip, k -> createNewBucket());
  }

  @Override
  @SuppressWarnings("null")
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String ip = request.getRemoteAddr();
    Bucket bucket = resolveBucket(ip);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response.getWriter().write("Too many requests");
    }
  }
}
