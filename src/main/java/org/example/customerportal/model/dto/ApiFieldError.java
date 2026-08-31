package org.example.customerportal.model.dto;

/**
 * One entry of {@link ErrorResponse#fieldErrors()} — serializes to the API
 * contract's {@code FieldError} shape {@code { field, message }}
 * (api-conventions.md AC-6).
 *
 * <p>Named {@code ApiFieldError} to avoid a clash with
 * {@code org.springframework.validation.FieldError} (PD-5). {@code message} is a
 * safe validation message and never echoes the submitted value (SC-9).
 */
public record ApiFieldError(String field, String message) {
}
