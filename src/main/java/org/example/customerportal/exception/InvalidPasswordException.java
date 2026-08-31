package org.example.customerportal.exception;

/**
 * Raised by the service-layer password-policy re-check before hashing (FR-6,
 * SC-1) when a password reaches the service without satisfying the policy — a
 * defense-in-depth guard behind the request-layer {@code @ValidPassword}
 * constraint.
 *
 * <p>Domain exception only: it carries no HTTP concept and never the submitted
 * value (SC-9). Mapped to {@code 400} by {@link GlobalExceptionHandler}.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("The submitted password does not meet the security policy.");
    }
}
