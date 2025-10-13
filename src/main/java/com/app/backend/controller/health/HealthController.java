package com.app.backend.controller.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

  private final Instant startTime = Instant.now();

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> status = Map.of(
        "status", "UP",
        "timestamp", Instant.now().toString(),
        "uptimeSeconds", Instant.now().getEpochSecond() - startTime.getEpochSecond());
    return ResponseEntity.ok(status);
  }
}
