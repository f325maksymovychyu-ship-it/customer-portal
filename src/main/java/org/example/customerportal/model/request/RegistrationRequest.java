package org.example.customerportal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.customerportal.validation.ValidPassword;

/**
 * Inbound body of {@code POST /api/v1/customers}
 * ({@code docs/designs/api/US-001-openapi.yaml} {@code RegistrationRequest}).
 *
 * <p>{@code email}: required, well-formed, max 254 characters (OD-001:A).
 * {@code password}: required and policy-compliant ({@link ValidPassword}).
 * Unknown / extra JSON properties are rejected with {@code 400} via
 * {@code spring.jackson.deserialization.fail-on-unknown-properties} (PD-1) — the
 * DTO needs no {@code @JsonIgnoreProperties}.
 */
public record RegistrationRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @ValidPassword
        String password) {
}
