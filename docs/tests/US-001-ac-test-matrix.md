---
artifact_type: ac_test_matrix
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T11:25:00Z
updated_at: 2026-08-31T11:25:00Z
produced_by: test-writer
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/designs/api/US-001-api-design.md
    version: 1
  - path: docs/designs/api/US-001-openapi.yaml
    version: 1
  - path: docs/designs/database/US-001-db-design.md
    version: 1
  - path: docs/designs/database/US-001-entity-model.md
    version: 1
  - path: docs/impact-analysis/US-001-impact-analysis.md
    version: 1
  - path: docs/plans/US-001-implementation-plan.md
    version: 1
  - path: docs/reviews/plans/US-001-plan-review.md
    version: 1
supersedes: null
---

# Acceptance-Criteria → Test Matrix — US-001

This matrix is **authoritative**. `implementation-verifier`, `security-reviewer`
and `reconciliation-reviewer` read it; they do not rebuild it.

`Status` is the state **at `TEST_WRITING` (pre-implementation)**:

- `RED` — compiles, runs, fails because the feature is not implemented yet
  (expected; see the test-generation report for the exact failure).
- `GREEN (guard)` — already satisfied; kept as a regression guard.
- `DEFERRED → IMPLEMENTATION` — scenario needs a production type that does not
  exist yet; specified here in full, owned by the named plan test skeleton.
  `IMPLEMENTATION` chooses the concrete method names (plan C-T1..C-T6 are
  "indicative"); the **scenario + expected result** columns are the binding
  contract for these rows.

Base test package: `org.example.customerportal` (mirrors production tree,
`package-map.md` test rule).

## 1. Story / Specification Acceptance Criteria

| AC | Scenario | Level | Test class | Test method | Expected result | Status |
|---|---|---|---|---|---|---|
| AC-001 | Valid email + 12-char compliant password | web | `registration.CustomerRegistrationApiTest` | `validRegistrationReturns201WithLocationAndCustomerBody` | `201`; `Location` matches `.*/api/v1/customers/\d+`; body `{id:number, email, role:"CUSTOMER", createdAt}` | RED |
| AC-001 | Mixed-case email normalized to lowercase (OD-006:A) | web | `registration.CustomerRegistrationApiTest` | `emailIsStoredAndReturnedNormalisedToLowercase` | `201`; `$.email` equals the lowercased input | RED |
| AC-001 | 72-byte compliant password accepted (upper boundary) | web | `registration.CustomerRegistrationApiTest` | `password72CharsMeetingPolicyIsAccepted` | `201` | RED |
| AC-001 | Registration reachable without authentication (SEC-1) | security | `security.RegistrationSecurityPostureTest` | `registrationEndpointIsReachableWithoutAuthentication` | status not `401` and not `403` | RED |
| AC-001 | Stored credential is a BCrypt hash of the submitted password; role present (authenticate-later clause, verified indirectly) | service | `service.CustomerServiceTest` (plan C-T3) | _(method named by IMPLEMENTATION)_ | `BCryptPasswordEncoder.matches(raw, storedHash)` is true; `role == CUSTOMER` | DEFERRED → IMPLEMENTATION |
| AC-002 | Duplicate email, second attempt different case → `409`, one resource | web | `registration.CustomerRegistrationApiTest` | `duplicateEmailIsRejectedCaseInsensitivelyWith409` | `409`; `$.status==409`; `$.message=="An account with this email already exists."` | RED |
| AC-002 | `email` has a `UNIQUE` constraint | persistence | `persistence.CustomerSchemaTest` | `emailHasAUniqueConstraint` | a `UNIQUE` `TABLE_CONSTRAINT` covers `EMAIL` | RED |
| AC-002 | Two rows, same normalized email → constraint violation | persistence | `persistence.CustomerSchemaTest` | `caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint` | second `INSERT` throws | RED |
| AC-002 | Service detects the duplicate before insert and throws `DuplicateEmailException` (same + different case) | service | `service.CustomerServiceTest` (plan C-T3) | _(method named by IMPLEMENTATION)_ | exception thrown; repository `save` not called for the second attempt | DEFERRED → IMPLEMENTATION |
| AC-003 | Malformed email format | web | `registration.CustomerRegistrationApiTest` | `malformedEmailReturns400WithEmailFieldError` | `400`; `$.fieldErrors[*].field` has `email` | RED |
| AC-003 | Blank email | web | `registration.CustomerRegistrationApiTest` | `blankEmailReturns400WithEmailFieldError` | `400`; `fieldErrors` has `email` | RED |
| AC-003 | Email longer than 254 chars (OD-001:A) | web | `registration.CustomerRegistrationApiTest` | `emailLongerThan254CharsReturns400WithEmailFieldError` | `400`; `fieldErrors` has `email` | RED |
| AC-003 | `email` column is `VARCHAR(254) NOT NULL` | persistence | `persistence.CustomerSchemaTest` | `emailColumnIsVarchar254NotNull` | type `CHARACTER VARYING`, length `254`, not nullable | RED |
| AC-004 | `password_hash` column is `VARCHAR(60) NOT NULL` (PC-9) | persistence | `persistence.CustomerSchemaTest` | `passwordHashColumnIsVarchar60NotNull` | type `CHARACTER VARYING`, length `60`, not nullable | RED |
| AC-004 | No plaintext-password column exists (BR-005, SEC-3) | persistence | `persistence.CustomerSchemaTest` | `noPlaintextPasswordColumnExists` | zero columns named `PASSWORD` | GREEN (guard) |
| AC-004 | `customer` table exists (hand-written schema) | persistence | `persistence.CustomerSchemaTest` | `customerTableExists` | exactly one `CUSTOMER` table | RED |
| AC-004 | Stored hash is a BCrypt string, not the plaintext; plaintext never assigned to the entity | service | `service.CustomerServiceTest` (plan C-T3) | _(method named by IMPLEMENTATION)_ | `storedHash` matches `^\$2[aby]\$\d\d\$.{53}$` and `!= rawPassword` | DEFERRED → IMPLEMENTATION |
| AC-005 | Success response excludes password, hash, `enabled`, `updatedAt` | web | `registration.CustomerRegistrationApiTest` | `successResponseNeverExposesCredentialOrInternalState` | `$.password`, `$.passwordHash`, `$.password_hash`, `$.enabled`, `$.updatedAt` all absent | RED |
| AC-006 | Password shorter than 12 chars | web | `registration.CustomerRegistrationApiTest` | `passwordShorterThan12CharsReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | Password without uppercase | web | `registration.CustomerRegistrationApiTest` | `passwordWithoutUppercaseReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | Password without lowercase | web | `registration.CustomerRegistrationApiTest` | `passwordWithoutLowercaseReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | Password without digit | web | `registration.CustomerRegistrationApiTest` | `passwordWithoutDigitReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | Password without special char | web | `registration.CustomerRegistrationApiTest` | `passwordWithoutSpecialCharReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | Blank password | web | `registration.CustomerRegistrationApiTest` | `blankPasswordReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` | RED |
| AC-006 | **Byte-length boundary (R-3 / PD-2):** `"Aa1!"+"€"×23` = 27 chars / 73 bytes | web | `registration.CustomerRegistrationApiTest` | `passwordOver72BytesButUnder72CharsReturns400WithPasswordFieldError` | `400`; `fieldErrors` has `password` (fails if length is counted in characters) | RED |
| AC-006 | Exhaustive validator matrix: 11-byte reject, 12-byte accept, 72-byte accept, 73-byte reject; each of upper/lower/digit/special missing → reject; multi-byte `"Aa1!"+"€"×23` (73 bytes) reject; `"Aa1!"+"€"×22`+`"b"` (69 bytes) accept; message contains no submitted value | unit | `validation.PasswordPolicyValidatorTest` (plan C-T1) | _(methods named by IMPLEMENTATION; parameterised per vector)_ | `isValid` returns the expected boolean; violation message is the static generic text | DEFERRED → IMPLEMENTATION |
| AC-006 | Service re-checks the byte-length policy before hashing (FR-6) | service | `service.CustomerServiceTest` (plan C-T3) | _(method named by IMPLEMENTATION)_ | service throws before `passwordEncoder.encode` | DEFERRED → IMPLEMENTATION |
| AC-007 | `Content-Type: text/plain` | web | `registration.CustomerRegistrationApiTest` | `nonJsonContentTypeReturns415` | `415` | RED |
| AC-007 | Missing `Content-Type` | web | `registration.CustomerRegistrationApiTest` | `missingContentTypeReturns415` | `415` | RED |

## 2. Derived rules (Specification §6.3, API design §3)

| Rule | Scenario | Level | Test class | Test method | Expected | Status |
|---|---|---|---|---|---|---|
| Unknown JSON property rejected (`additionalProperties:false`) | body has an extra `role` field | web | `registration.CustomerRegistrationApiTest` | `unknownJsonPropertyReturns400` | `400`; `$.status==400` | RED |
| Malformed JSON rejected | truncated JSON body | web | `registration.CustomerRegistrationApiTest` | `malformedJsonReturns400` | `400`; `$.status==400` | RED |
| AC-6 error-body shape | `400` body has `timestamp, status, error, message, path` | web | `registration.CustomerRegistrationApiTest` | `errorBodyHasTheApiConventionShape` | all five fields present; `path == "/api/v1/customers"` | RED |

## 3. Security posture (Specification §7, OD-002:B)

| Requirement | Scenario | Level | Test class | Test method | Expected | Status |
|---|---|---|---|---|---|---|
| SEC-7 / OD-002:B CSRF exemption scoped to the registration path | tokenless `POST /api/v1/customers` | security | `security.RegistrationSecurityPostureTest` | `registrationPostIsAcceptedWithoutACsrfToken` | status not `403` | RED |
| SEC-1 / SC-4 deny-by-default elsewhere (F-3: `401` entry point) | unauthenticated `GET /api/v1/customers/1` | security | `security.RegistrationSecurityPostureTest` | `protectedRouteReturns401WhenUnauthenticated` | `401` (tripwire: fails on a form-login `302` → `SECURITY_REVIEW` arbitrates R-2) | GREEN (guard) |
| SC-6 / SEC-9 H2 console never exposed | `GET /h2-console` | security | `security.RegistrationSecurityPostureTest` | `h2ConsoleIsNotExposed` | status not `200` | GREEN (guard) |
| SC-9 / SEC-6 no value echo in a validation message | policy-failing password with a distinctive value | web | `registration.CustomerRegistrationApiTest` | `passwordValidationMessageDoesNotEchoTheSubmittedValue` | no `message` contains the submitted string | RED |
| SC-9 / SEC-6 error message leaks no internals | malformed JSON | web | `registration.CustomerRegistrationApiTest` | `errorBodyNeverLeaksInternals` | `message` has no `Exception` / package name / `jdbc:h2` / SQL keyword | RED |
| SEC-5 role + enabled columns non-null | schema | `persistence.CustomerSchemaTest` | `roleColumnIsNotNull`, `enabledColumnIsBooleanNotNull` | both `NOT NULL`; `enabled` is `BOOLEAN` | RED |

## 4. Persistence / NFR-4 / BR-007

| Requirement | Scenario | Level | Test class | Test method | Expected | Status |
|---|---|---|---|---|---|---|
| NFR-4 surrogate `Long id` | `id` column | persistence | `persistence.CustomerSchemaTest` | `idColumnIsBigintNotNull` | `BIGINT`, `NOT NULL` | RED |
| BR-007 / PC-6 UTC audit columns | `created_at` / `updated_at` | persistence | `persistence.CustomerSchemaTest` | `auditTimestampColumnsAreTimeZoneAwareAndNotNull` | both `TIMESTAMP WITH TIME ZONE`, `NOT NULL` | RED |
| PC-6 `created_at` not updatable; auditing writes a UTC offset; `EnumType.STRING` round-trip; entity ↔ `schema.sql` agree under `ddl-auto=validate` (R-1 / PD-6) | `@DataJpaTest` persistence slice | persistence | `persistence.CustomerPersistenceTest` (plan C-T2) | _(methods named by IMPLEMENTATION)_ | `created_at` unchanged after an update; offset is UTC; `role` persists as the name; context starts clean | DEFERRED → IMPLEMENTATION |

## 5. Regression

| Scenario | Test class | Test method | Expected | Status |
|---|---|---|---|---|
| Context still boots with Security + JPA active | `CustomerPortalApplicationTests` | `contextLoads` | passes | GREEN (guard) |

## 6. Coverage check

Every mandatory Acceptance Criterion (AC-001..AC-007) has **at least one
executable test that runs at `TEST_WRITING`** (web or persistence level). No
mandatory AC depends solely on a `DEFERRED` row. The `DEFERRED` rows add
depth (unit-level policy matrix, service internals, JPA-auditing invariants);
each is owned by a named plan skeleton (C-T1 / C-T2 / C-T3) and gated by plan
execution steps 4 and 6.
