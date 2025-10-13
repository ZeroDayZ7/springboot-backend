package com.app.backend.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Filtr sanitizujący request: query/form params i JSON body przed dotarciem do
 * controllera.
 * - Params: HTML-escape + trim.
 * - JSON: Rekurencyjne escape stringów (nie wpływa na strukturę).
 * - Obsługuje wielokrotne czytanie body via ContentCachingRequestWrapper.
 * - Edge cases: Duże body (limit 1MB), błędy parsowania (fallback do raw).
 * - Wydajność: Streamy dla kolekcji, cache dla mappera.
 */
@Component
@Order(4) // Po logowaniu, rate limiterze i security headers
public class RequestSanitizationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestSanitizationFilter.class);
  private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB limit na body
  private static final String JSON_CONTENT_TYPE = "application/json";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // Wrapper do wielokrotnego czytania body (nie modyfikuje oryginalnego)
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

    // 1️⃣ Sanitizacja query/form params (zawsze)
    HttpServletRequest paramsSanitizedRequest = new HttpServletRequestWrapper(wrappedRequest) {
      @Override
      public String getParameter(String name) {
        String value = super.getParameter(name);
        return sanitize(value);
      }

      @Override
      public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null)
          return null;
        return Arrays.stream(values).map(RequestSanitizationFilter::sanitize).toArray(String[]::new);
      }

      @Override
      public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = super.getParameterMap();
        return map.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Arrays.stream(entry.getValue())
                    .map(RequestSanitizationFilter::sanitize)
                    .toArray(String[]::new)));
      }
    };

    // 2️⃣ Sanitizacja JSON body, jeśli content-type = application/json
    String contentType = request.getContentType();
    if (contentType != null && contentType.toLowerCase().startsWith(JSON_CONTENT_TYPE)) {
      byte[] bodyBytes = wrappedRequest.getContentAsByteArray();
      if (bodyBytes.length > 0 && bodyBytes.length <= MAX_BODY_SIZE) {
        try {
          // Parsuj JSON
          JsonNode root = objectMapper.readTree(bodyBytes);
          sanitizeJsonNode(root);

          // Serializuj z powrotem
          byte[] sanitizedBody = objectMapper.writeValueAsBytes(root);

          // Nowy wrapper z sanitized input stream
          HttpServletRequest bodySanitizedRequest = new HttpServletRequestWrapper(paramsSanitizedRequest) {
            @Override
            public ServletInputStream getInputStream() {
              return new DelegatingServletInputStream(new ByteArrayInputStream(sanitizedBody));
            }

            @Override
            public int getContentLength() {
              return sanitizedBody.length;
            }

            @Override
            public long getContentLengthLong() {
              return sanitizedBody.length;
            }

            @Override
            public String getCharacterEncoding() {
              return StandardCharsets.UTF_8.name(); // Zapewnij UTF-8
            }
          };

          filterChain.doFilter(bodySanitizedRequest, response);
          return; // Wyjdź, bo body jest sanitized
        } catch (Exception e) {
          log.warn("Failed to sanitize JSON body for request URI {}: {}", request.getRequestURI(), e.getMessage());
          // Fallback: Przekaż raw body (bezpieczniejsze niż crash)
        }
      } else if (bodyBytes.length > MAX_BODY_SIZE) {
        log.warn("JSON body too large for sanitization ({} bytes) for URI {}", bodyBytes.length,
            request.getRequestURI());
      }
    }

    // Fallback: Tylko params sanitized
    filterChain.doFilter(paramsSanitizedRequest, response);
  }

  /**
   * Rekurencyjnie sanitizuje stringi w JSON node (obiekty i array'e).
   * Nie zmienia struktury, tylko text values.
   */
  private static void sanitizeJsonNode(JsonNode node) {
    if (node == null || node.isNull())
      return;

    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      for (Map.Entry<String, JsonNode> entry : obj.properties()) {
        JsonNode value = entry.getValue();
        if (value.isTextual()) {
          obj.set(entry.getKey(), TextNode.valueOf(sanitize(value.asText())));
        } else {
          sanitizeJsonNode(value);
        }
      }
    } else if (node.isArray()) {
      ArrayNode array = (ArrayNode) node;
      IntStream.range(0, array.size()).forEach(i -> {
        JsonNode value = array.get(i);
        if (value.isTextual()) {
          array.set(i, TextNode.valueOf(sanitize(value.asText())));
        } else {
          sanitizeJsonNode(value);
        }
      });
    } else if (node.isTextual()) {
      // Dla root text (rzadkie, ale obsługa)
      // To nie zmieni root, bo caller musi to zrobić
    }
  }

  /**
   * Sanitizuje string: HTML-escape + trim.
   * Null-safe.
   */
  private static String sanitize(String input) {
    if (input == null)
      return null;
    return StringEscapeUtils.escapeHtml4(input.trim());
  }

  /**
   * Prosta implementacja ServletInputStream delegująca do InputStream.
   * Obsługuje ReadListener (choć nie async w tym kontekście).
   */
  private static class DelegatingServletInputStream extends ServletInputStream {

    private final InputStream sourceStream;

    public DelegatingServletInputStream(InputStream sourceStream) {
      this.sourceStream = sourceStream;
    }

    @Override
    public boolean isFinished() {
      try {
        return sourceStream.available() == 0;
      } catch (IOException e) {
        return true;
      }
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

    @Override
    public int read() throws IOException {
      return sourceStream.read();
    }

    @Override
    public void close() throws IOException {
      super.close();
      sourceStream.close();
    }
  }
}