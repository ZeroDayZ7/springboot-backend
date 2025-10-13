package com.app.backend.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe; // Dodaj ten import!
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(3)
public class GlobalRateLimiterFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(GlobalRateLimiterFilter.class);

  @Value("${ratelimiter.requests:100}")
  private int requests;

  @Value("${ratelimiter.durationMinutes:1}")
  private int durationMinutes;

  @Value("${ratelimiter.logFrequency:10}")
  private int logFrequency; // loguj co N-te przekroczenie

  // Cache z Caffeine: max 10k IP, wygasa po 30 min braku aktywności
  private final Cache<String, Bucket> cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  // Liczniki przekroczeń na IP do logowania
  private final Cache<String, AtomicInteger> exceedCounters = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  private Bucket createNewBucket() {
    Refill refill = Refill.greedy(requests, Duration.ofMinutes(durationMinutes));
    Bandwidth limit = Bandwidth.classic(requests, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket resolveBucket(String ip) {
    return cache.get(ip, _ -> createNewBucket());
  }

  private AtomicInteger resolveCounter(String ip) {
    return exceedCounters.get(ip, _ -> new AtomicInteger(0));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String ip = resolveClientIp(request);
    Bucket bucket = resolveBucket(ip);

    // Poprawka: Użyj ConsumptionProbe
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      // Sukces – kontynuuj (możesz użyć probe.getRemainingTokens() do logów, jeśli
      // chcesz)
      filterChain.doFilter(request, response);
    } else {

      int retryAfterSeconds = (int) Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds() + 1;

      // JSON response
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json;charset=UTF-8");
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

      String jsonResponse = String.format(
          "{\"error\":\"Too many requests\",\"retryAfterSeconds\":%d}", retryAfterSeconds);

      try (var writer = response.getWriter()) {
        writer.write(jsonResponse);
      }

      // Logowanie co N-te zdarzenie
      AtomicInteger counter = resolveCounter(ip);
      int current = counter.incrementAndGet();
      if (current % logFrequency == 0) {
        log.warn("Rate limit exceeded for IP: {} ({} times)", ip, current);
      }
    }
  }

  private String resolveClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty()) {
      ip = ip.split(",")[0].trim();
    } else {
      ip = request.getHeader("X-Real-IP");
      if (ip == null || ip.isEmpty()) {
        ip = request.getRemoteAddr();
      }
    }
    return ip;
  }
}