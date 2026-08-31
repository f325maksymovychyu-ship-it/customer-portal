package org.example.customerportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Request-layer constraint enforcing the training-project password policy
 * (security-conventions.md SC-1, Specification §6.2): 12..72 <em>UTF-8 bytes</em>
 * (PD-2 / risk R-3) and at least one uppercase, lowercase, digit, and special
 * character.
 *
 * <p>The {@code message} is static and generic — it never echoes the submitted
 * value (SC-9 / risk R-7). {@code null} / empty is left to {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidPassword {

    String message() default "Password does not meet the security policy.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
