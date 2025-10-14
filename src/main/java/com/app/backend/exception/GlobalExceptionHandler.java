package com.app.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // =======================
  // ResourceNotFoundException handler
  // =======================
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    // Log warning for missing resource
    log.warn("Resource not found: {}", request.getRequestURI());
    ProblemDetail problem = buildProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  // =======================
  // NoResourceFoundException handler (special case for favicon)
  // =======================
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> handleFavicon(NoResourceFoundException ex, HttpServletRequest request) {
    String uri = request.getRequestURI();
    if ("/favicon.ico".equals(uri)) {
      return ResponseEntity.noContent().build(); // 204 for favicon
    }
    log.warn("Resource not found: {}", uri);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  // =======================
  // Generic Exception handler
  // =======================
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGlobal(Exception ex, HttpServletRequest request) {
    // Log full stack trace for debugging (backend only)
    log.error("Unexpected exception occurred", ex);
    // Return safe message to client without exposing sensitive info
    ProblemDetail problem = buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "Unexpected error occurred",
        request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  // =======================
  // Validation for @Valid in @RequestBody
  // =======================
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage));

    ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST,
        "Validation Failed",
        "Invalid request parameters",
        request);
    problem.setProperty("errors", fieldErrors);

    log.warn("Validation errors: {}", fieldErrors);
    return ResponseEntity.badRequest().body(problem);
  }

  // =======================
  // ConstraintViolationException handler for @RequestParam / @PathVariable
  // =======================
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
      HttpServletRequest request) {
    Map<String, String> errors = ex.getConstraintViolations()
        .stream()
        .collect(Collectors.toMap(
            v -> v.getPropertyPath().toString(),
            v -> v.getMessage()));

    ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST,
        "Validation Failed",
        "Invalid request parameters",
        request);
    problem.setProperty("errors", errors);

    log.warn("Constraint violations: {}", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  // =======================
  // HttpMessageNotReadableException handler for malformed JSON
  // =======================
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleInvalidJson(HttpMessageNotReadableException ex,
      HttpServletRequest request) {
    ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST,
        "Malformed JSON Request",
        ex.getMostSpecificCause().getMessage(),
        request);

    log.warn("Malformed JSON: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(problem);
  }

  // =======================
  // Private helper to build ProblemDetail
  // =======================
  private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(title);
    problem.setDetail(detail);
    problem.setProperty("timestamp", OffsetDateTime.now());
    problem.setProperty("path", request.getRequestURI()); // useful for tracing requests
    problem.setProperty("statusCode", status.value());
    return problem;
  }
}
