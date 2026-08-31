package org.example.customerportal.model.entity;

/**
 * Permission group assigned to a {@link Customer} account.
 *
 * <p>Persisted as the constant name via {@code EnumType.STRING} (never ordinal).
 * US-001 only ever assigns {@link #CUSTOMER} (security-conventions.md SC-2,
 * business-rules.md BR-006); {@link #ADMIN} exists for model completeness
 * (business-glossary.md Role) and is unused by this Story. The Spring Security
 * authority string ({@code ROLE_CUSTOMER} / {@code ROLE_ADMIN}) is derived at
 * authentication time (US-002), not stored.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
