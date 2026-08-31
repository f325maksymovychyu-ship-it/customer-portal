package org.example.customerportal.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard error body (api-conventions.md AC-6), produced only by
 * {@link org.example.customerportal.exception.GlobalExceptionHandler}
 * (architecture.md AD-6 / AC-9).
 *
 * <p>{@code fieldErrors} is omitted from the JSON when {@code null} or empty.
 * {@code message} is client-safe and never leaks internals (SC-9).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ApiFieldError> fieldErrors) {

    public static ErrorResponse of(OffsetDateTime timestamp, int status, String error,
                                   String message, String path) {
        return new ErrorResponse(timestamp, status, error, message, path, null);
    }
}
