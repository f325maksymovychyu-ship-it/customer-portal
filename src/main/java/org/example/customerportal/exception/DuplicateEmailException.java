package org.example.customerportal.exception;

/**
 * Raised by the service when a registration targets an email that already has an
 * account (compared case-insensitively). Domain exception only — it carries no
 * HTTP concept and no submitted value (architecture.md AD-6, FR-4, OD-003:A).
 * Mapped to {@code 409} by {@link GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("A customer account already exists for the requested email.");
    }
}
