package com.app.backend.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConfigurationProperties(prefix = "ratelimiter")
@Validated
class RateLimiterProperties {
  private static final Logger logger = LoggerFactory.getLogger(RateLimiterProperties.class);

  @Min(1)
  private int requests = 100;

  @Min(1)
  private int durationMinutes = 1;

  @Min(1)
  private int logFrequency = 10;

  private List<String> whitelist;

  public int getRequests() {
    return requests;
  }

  public void setRequests(int requests) {
    this.requests = requests;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(int durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public int getLogFrequency() {
    return logFrequency;
  }

  public void setLogFrequency(int logFrequency) {
    this.logFrequency = logFrequency;
  }

  public List<String> getWhitelist() {
    return whitelist;
  }

  public void setWhitelist(List<String> whitelist) {
    this.whitelist = whitelist;
  }

  @PostConstruct
  public void logConfig() {
    logger.info("RateLimiter loaded: requests={}, durationMinutes={}, logFrequency={}, whitelist={}",
        requests, durationMinutes, logFrequency, whitelist);
  }
}

@Component
@Order(3)
public class GlobalRateLimiterFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(GlobalRateLimiterFilter.class);

  private final RateLimiterProperties props;

  public GlobalRateLimiterFilter(RateLimiterProperties props) {
    this.props = props;
  }

  private final Cache<String, Bucket> cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  private final Cache<String, AtomicInteger> exceedCounters = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(30))
      .maximumSize(10_000)
      .build();

  private Bucket createNewBucket() {
    Refill refill = Refill.greedy(props.getRequests(), Duration.ofMinutes(props.getDurationMinutes()));
    Bandwidth limit = Bandwidth.classic(props.getRequests(), refill);
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

    // Skip rate limiting for whitelisted IPs
    if (props.getWhitelist() != null && props.getWhitelist().contains(ip)) {
      filterChain.doFilter(request, response);
      return;
    }

    Bucket bucket = resolveBucket(ip);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      // Continue normally
      filterChain.doFilter(request, response);
    } else {
      int retryAfterSeconds = (int) Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds() + 1;

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json;charset=UTF-8");
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

      String jsonResponse = String.format(
          "{\"error\":\"Too many requests\",\"retryAfterSeconds\":%d}", retryAfterSeconds);

      try (var writer = response.getWriter()) {
        writer.write(jsonResponse);
      }

      // Log only every Nth exceedance
      AtomicInteger counter = resolveCounter(ip);
      int current = counter.incrementAndGet();
      if (current % props.getLogFrequency() == 0) {
        log.warn("Rate limit exceeded for IP: {} ({} times), remaining tokens={}", ip, current,
            probe.getRemainingTokens());
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
