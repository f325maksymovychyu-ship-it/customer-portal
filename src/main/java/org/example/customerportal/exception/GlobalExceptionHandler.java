package org.example.customerportal.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.customerportal.model.dto.ApiFieldError;
import org.example.customerportal.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The single place that maps exceptions to HTTP responses (architecture.md AD-6,
 * api-conventions.md AC-9, FR-10). Controllers never build error bodies.
 *
 * <p>Every response is an {@link ErrorResponse} (AC-6 shape). {@code message} is
 * client-safe and never contains stack traces, SQL, class/package names, file
 * paths, database URLs, or the submitted password (SC-9, SEC-6).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean-validation failure on the request body → {@code 400} + {@code fieldErrors[]}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toApiFieldError)
                .toList();
        ErrorResponse body = new ErrorResponse(
                now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields.",
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Malformed JSON <em>and</em> an unknown/extra JSON property both surface as
     * {@link HttpMessageNotReadableException} (PD-1) → one {@code 400} branch.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "The request body is missing, malformed, or contains an unknown field.",
                request);
    }

    /** Missing / non-JSON {@code Content-Type} → {@code 415} (AC-007). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type must be application/json.", request);
    }

    /** Duplicate email → {@code 409} with the OD-003:A message (AC-002). */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "An account with this email already exists.", request);
    }

    /** Service-layer password-policy re-check failure (FR-6) → {@code 400}. */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "The submitted password does not meet the security policy.", request);
    }

    /** Anything unmapped → {@code 500}, no internal detail leaked (SC-9). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private static ApiFieldError toApiFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        return new ApiFieldError(fieldError.getField(), message == null ? "invalid value" : message);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
