package org.example.customerportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * Validates the {@link ValidPassword} constraint and provides the same policy
 * check for the service-layer re-check before hashing (FR-6, SC-1).
 *
 * <p>Length is measured in <strong>UTF-8 bytes</strong> (PD-2 / risk R-3): the
 * 72 bound is BCrypt's plaintext input limit in bytes, not characters. The four
 * character-class checks operate on the string.
 */
public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_BYTES = 12;
    private static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null / empty is reported by @NotBlank, not here.
        if (value == null || value.isEmpty()) {
            return true;
        }
        return isCompliant(value);
    }

    /**
     * @return {@code true} when {@code password} satisfies the full policy
     *     (12..72 UTF-8 bytes, one upper / lower / digit / special). {@code null}
     *     or empty is <em>not</em> compliant for this method (the service uses it
     *     as a hard gate before hashing).
     */
    public static boolean isCompliant(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }
}
