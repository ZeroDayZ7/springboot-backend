package com.app.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

  // ✅ Test query/form param
  @GetMapping("/param")
  public Map<String, String> testParam(
      @RequestParam(required = false, defaultValue = "empty") String param) {
    return Map.of("param", param);
  }

  // ✅ Test JSON body sanitization
  @PostMapping("/json")
  public Map<String, Object> testJson(@RequestBody Map<String, Object> body) {
    // Zwraca body po sanityzacji
    return Map.of("sanitizedBody", body);
  }

  // ✅ Test multiple params
  @GetMapping("/multi")
  public Map<String, String[]> testMulti(@RequestParam Map<String, String[]> params) {
    return params;
  }

  // ✅ Test HTML injection (sprawdzenie sanityzacji)
  @PostMapping("/html")
  public Map<String, String> testHtml(@RequestBody Map<String, String> body) {
    return body; // np. {"content": "<script>alert('XSS')</script>"} powinno być już escaped
  }
}
