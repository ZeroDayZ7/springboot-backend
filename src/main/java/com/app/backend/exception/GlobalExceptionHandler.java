package com.app.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

  // Handler dla ResourceNotFoundException
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail problem = buildProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> handleFavicon(NoResourceFoundException ex, HttpServletRequest request) {
    if ("/favicon.ico".equals(request.getRequestURI())) {
      return ResponseEntity.noContent().build(); // 204
    }
    // Jeśli inny resource, zwracamy normalny 404
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  // Handler dla ogólnych wyjątków
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGlobal(Exception ex) {
    // W prod warto logować tylko wiadomość, nie stack trace
    // log.error("Global exception: {}", ex.getMessage());
    ProblemDetail problem = buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  // Handler dla walidacji (np. @Valid)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage));

    ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation Failed",
        "Invalid request parameters");
    problem.setProperty("errors", fieldErrors);

    return ResponseEntity.badRequest().body(problem);
  }

  // Prywatna metoda do budowy ProblemDetail
  private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(title);
    problem.setDetail(detail);
    problem.setProperty("timestamp", OffsetDateTime.now());
    return problem;
  }
}
