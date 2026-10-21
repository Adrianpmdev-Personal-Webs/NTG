package com.adrianpmdev.core.web.exception;

import com.adrianpmdev.core.logging.RequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Global exception handler. Answers with {@link ProblemDetail} and writes ONE log line per error
 * (4xx as WARN without stack trace, 5xx as ERROR with stack trace). Internal details are never sent
 * to the client: 5xx responses only carry the request id so the error can be found in the logs.
 *
 * @author adrianpmdev
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles request body validation errors produced by {@code @Valid}.
     *
     * @param exception validation exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        return validationError(errors, request);
    }

    /**
     * Handles constraint validation errors.
     *
     * @param exception constraint violation exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<String> errors = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return validationError(errors, request);
    }

    /**
     * Handles malformed JSON and wrongly typed path/query values (e.g. an invalid UUID).
     *
     * @param exception source exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", "Malformed or invalid request", exception, request);
    }

    /**
     * Handles missing resources.
     *
     * @param exception missing element exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", "Resource not found", exception, request);
    }

    /**
     * Handles unique/foreign key violations without exposing SQL details.
     *
     * @param exception data integrity exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", "The data conflicts with existing records", exception, request);
    }

    /**
     * Handles authorization failures raised from controllers or method security.
     *
     * @param exception security exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public ResponseEntity<ProblemDetail> handleSecurity(RuntimeException exception, HttpServletRequest request) {
        boolean denied = exception instanceof AccessDeniedException;

        return build(denied ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED,
                denied ? "Forbidden" : "Unauthorized",
                denied ? "Access denied" : "Unauthenticated", exception, request);
    }

    /**
     * Handles every other exception. Spring MVC and {@code ResponseStatusException} errors keep their own
     * status (404, 405, 415, our deliberate 4xx/5xx...); anything else is an internal error.
     *
     * @param exception source exception
     * @param request   current HTTP request
     * @return problem detail response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            boolean deliberate = exception instanceof ResponseStatusException;
            String bodyDetail = errorResponse.getBody().getDetail();
            String detail = status.is5xxServerError() && !deliberate ? "Unexpected error"
                    : bodyDetail != null ? bodyDetail : status.getReasonPhrase();
            ResponseEntity<ProblemDetail> response = build(status, status.getReasonPhrase(), detail, exception, request);

            return ResponseEntity.status(status).headers(errorResponse.getHeaders()).body(response.getBody());
        }

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error", exception, request);
    }

    private ResponseEntity<ProblemDetail> validationError(List<String> errors, HttpServletRequest request) {
        ResponseEntity<ProblemDetail> response = build(
                HttpStatus.BAD_REQUEST, "Validation failed", String.join("; ", errors), null, request);
        response.getBody().setProperty("errors", errors);

        return response;
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail,
                                                Exception exception, HttpServletRequest request) {
        String requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_KEY);
        String clientDetail = status.is5xxServerError() && requestId != null
                ? detail + ". Reference: " + requestId
                : detail;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, clientDetail);
        problem.setTitle(title);
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());

        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }

        logError(status, request, detail, exception);

        return ResponseEntity.status(status).body(problem);
    }

    private void logError(HttpStatus status, HttpServletRequest request, String detail, Exception exception) {
        if (status.is5xxServerError()) {
            log.error("{} {} -> {}", request.getMethod(), request.getRequestURI(), status.value(), exception);
        } else if (status == HttpStatus.NOT_FOUND) {
            log.debug("{} {} -> 404 {}", request.getMethod(), request.getRequestURI(), detail);
        } else {
            log.warn("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), status.value(), detail);
        }
    }
}