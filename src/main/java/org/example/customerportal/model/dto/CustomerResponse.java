package org.example.customerportal.model.dto;

import java.time.OffsetDateTime;

/**
 * {@code 201} response body for customer registration (OD-004:A;
 * {@code docs/designs/api/US-001-openapi.yaml} {@code CustomerResponse}).
 *
 * <p>Contains exactly {@code id}, {@code email}, {@code role}, {@code createdAt}
 * — never a password, a password hash, {@code enabled}, or {@code updatedAt}
 * (architecture.md AD-4, SC-1, SEC-3/SEC-4).
 */
public record CustomerResponse(
        Long id,
        String email,
        String role,
        OffsetDateTime createdAt) {
}
