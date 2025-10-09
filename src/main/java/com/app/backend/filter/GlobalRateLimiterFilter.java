package com.app.backend.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(2)
public class GlobalRateLimiterFilter extends OncePerRequestFilter {

  @Value("${ratelimiter.requests:100}")
  private int requests;

  @Value("${ratelimiter.durationMinutes:1}")
  private int durationMinutes;

  // Cache z Caffeine: max 10k IP i wygasa po 30 min braku aktywności
  private final Cache<String, Bucket> cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  private Bucket createNewBucket() {
    Refill refill = Refill.greedy(requests, Duration.ofMinutes(durationMinutes));
    Bandwidth limit = Bandwidth.classic(requests, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket resolveBucket(String ip) {
    return cache.get(ip, k -> createNewBucket());
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
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader("Retry-After", String.valueOf(durationMinutes * 60));
      response.getWriter().write("Too many requests");
    }
  }
}
