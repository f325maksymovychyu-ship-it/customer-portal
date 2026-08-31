---
artifact_type: test_strategy
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

# Test Strategy — US-001 Customer Registration

## 1. Story

Public self-service customer registration: `POST /api/v1/customers` validates
`email` + `password` server-side, rejects a duplicate email case-insensitively
with `409`, stores a BCrypt hash, creates an enabled `CUSTOMER` account with UTC
audit timestamps, and returns `201` + `Location` + a credential-free body.

Authoritative inputs: Specification `docs/specifications/US-001-spec.md` v1 (§5
Acceptance Criteria, §6 Validation Rules, §7 Security, §8 Error Handling), API
design + `docs/designs/api/US-001-openapi.yaml` v1, DB design + entity model v1,
Implementation Plan v1, Plan Review v1. Open Decisions resolved at
`HUMAN_SPEC_APPROVAL` (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A,
OD-006:A).

## 2. Test scope

**In scope** — every US-001 Acceptance Criterion (spec AC-001..AC-007) plus the
two derived request-shape rules (unknown JSON property → `400`, malformed JSON →
`400`) and the security posture the Story changes (registration path public +
CSRF-exempt; every other route stays deny-by-default; H2 console never exposed).

**Out of scope** — login / authentication (US-002), `GET /api/v1/customers/{id}`
(future Story; only referenced by the `Location` header), password reset, email
verification, MFA, account activation, rate limiting (OD-005:A). No test asserts
behavior for these; `getSingleCustomer` is exercised only as a deny-by-default
guard.

## 3. Pre-implementation state (red phase)

At `TEST_WRITING` no production code exists beyond `CustomerPortalApplication`
and the `contextLoads` smoke test. The executable suite is written now against
the approved contract and is expected to fail until `IMPLEMENTATION` builds the
feature. To keep the test tree **compiling** before the production types exist
(a non-compiling tree is not a valid red phase — `test-writer` red-phase rule),
every test in this suite is written at the **HTTP boundary** (`MockMvc`) or the
**schema boundary** (`INFORMATION_SCHEMA` via `JdbcTemplate`) and references no
`org.example.customerportal` production type.

Consequence: unit-level tests that must import a production class — chiefly
`PasswordPolicyValidatorTest` (plan C-T1), the byte-precise password-policy
matrix — are **specified in full here and in the AC-test matrix** and handed to
`IMPLEMENTATION` (plan execution step 6, which already gates on them). AC-006
still has executable coverage now, at the HTTP layer (7 scenarios).

## 4. Test levels

| Level | Mechanism | Used for |
|---|---|---|
| Contract / web (integration) | `@SpringBootTest` + `@AutoConfigureMockMvc`, `@ActiveProfiles("test")` | status codes, `Location`, response body shape, `fieldErrors[]`, error-body shape, media-type handling, request-shape rules, duplicate-email flow |
| Persistence (schema) | `@SpringBootTest` + `JdbcTemplate` against H2 `INFORMATION_SCHEMA` | column names / types / lengths / nullability, `uq_customer_email`, absence of a plaintext column, timezone-aware audit columns |
| Security posture | `@SpringBootTest` + `MockMvc` | public registration path, CSRF exemption scope, deny-by-default elsewhere, H2 console not exposed |
| Regression | existing `CustomerPortalApplicationTests.contextLoads` | context still boots once Security + JPA are wired |
| Unit — validation (**deferred to IMPLEMENTATION**, plan C-T1) | `ConstraintValidator` unit test | byte-precise 12..72 boundary + character-class matrix + safe message |
| Unit / slice — service (**deferred to IMPLEMENTATION**, plan C-T3) | service test | email normalization, service-layer policy re-check, BCrypt hash verifies, duplicate guard |

Deferred levels are the plan's own skeletons (C-T1, C-T3); `test-writer` records
their exact scenarios here and does not pre-empt the structure `IMPLEMENTATION`
gives them, per plan Open Question 4.

## 5. Scenarios

### 5.1 Positive

- Valid email + 12-char policy-compliant password → `201`, `Location`
  `.../api/v1/customers/{digits}`, body `{ id, email, role=CUSTOMER, createdAt }`.
- Mixed-case email → stored and echoed lowercase (OD-006:A).
- 72-byte ASCII policy-compliant password → `201` (upper boundary accepted).

### 5.2 Negative

- Duplicate email, second attempt in a different letter case → `409`, body
  message exactly `An account with this email already exists.` (OD-003:A), no
  second resource.
- Malformed email, blank email, email > 254 chars → `400` + `fieldErrors[]`
  entry `field = "email"`.
- Password failing each rule individually (too short, no upper, no lower, no
  digit, no special, blank) → `400` + `fieldErrors[]` entry `field = "password"`.
- Unknown / extra JSON property (`additionalProperties: false`) → `400`.
- Malformed JSON body → `400`.
- `Content-Type` not `application/json`, and missing `Content-Type` → `415`.

### 5.3 Boundary

- Password length is measured in **UTF-8 bytes** (R-3 / PD-2, spec-review F-5,
  API Q-4). Vector: `"Aa1!" + "€"×23` = **27 characters / 73 bytes** → `400`
  password field error (would wrongly pass if the limit were counted in
  characters).
- Password `"Aa1!" + "b"×68` = 72 characters / 72 bytes, all classes present →
  `201` (at-limit accepted).
- Email `"a"×250 + "@example.com"` = 262 chars → `400` (over 254).
- Minimum-length compliant password `"Aa1!aaaaaaaa"` (12 chars) → accepted.

### 5.4 Validation

- Server-side only (NFR-002): every rule above is asserted through the HTTP API
  with no client-side assumption.
- `fieldErrors[]` uses the JSON field name (`email` / `password`) per
  `api-conventions.md` AC-6 and API design §7.
- The error body carries `timestamp, status, error, message, path` (AC-6 shape).

### 5.5 Security

- `POST /api/v1/customers` is reachable unauthenticated (SEC-1, SC-4) — not
  `401`, not `403`.
- The same POST is accepted **without a CSRF token** (OD-002:B, SC-5) — the
  exemption is scoped to this path.
- An arbitrary other route (`GET /api/v1/customers/1`) returns `401` when
  unauthenticated (deny-by-default; encodes the plan's recommended
  `HttpStatusEntryPoint(401)` — see §8 F-3).
- `GET /h2-console` is not served (`200` never returned) — SC-6 / SEC-9.
- Response body never contains `password`, `passwordHash`, `password_hash`,
  `enabled`, or `updatedAt` (SEC-3, SEC-4, OD-004:A).
- No validation `message` echoes the submitted password (SC-9).
- Error `message` never contains `Exception`, a package name, a `jdbc:h2` URL,
  or SQL keywords (SC-9, SEC-6).

### 5.6 Persistence

- Table `customer` exists (hand-written `schema.sql`, PC-2 / SC-8).
- `email VARCHAR(254) NOT NULL`; `password_hash VARCHAR(60) NOT NULL`;
  `role NOT NULL`; `enabled BOOLEAN NOT NULL`; `id BIGINT NOT NULL`.
- `created_at` / `updated_at` are `TIMESTAMP WITH TIME ZONE`, `NOT NULL`
  (BR-007, PC-6).
- A `UNIQUE` constraint covers `email` (`uq_customer_email`).
- No column named `password` (no plaintext at rest — BR-005, SEC-3).
- Two rows with the same normalized email violate the unique constraint.
- **Deferred to IMPLEMENTATION C-T2** (needs the entity): `created_at` not
  updatable, auditing writes a UTC offset, `EnumType.STRING` round-trip, and
  `ddl-auto=validate` agreement between entity and `schema.sql` (R-1 / PD-6).

## 6. Required fixtures

- `src/test/resources/application-test.yaml` (profile `test`): isolated
  in-memory H2 `jdbc:h2:mem:us001;DB_CLOSE_DELAY=-1`,
  `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=embedded`,
  `spring.h2.console.enabled=false`,
  `spring.jackson.deserialization.fail-on-unknown-properties=true`,
  `open-in-view=false`. Created by `test-writer` (plan C-17). `IMPLEMENTATION`
  may extend it (e.g. an explicit snake_case physical-naming strategy) but must
  not weaken the isolation, the `validate` mode, or the H2-console setting.
- No shared mutable state between tests: every HTTP test generates a unique
  email (`nanoTime`), so ordering is irrelevant and the in-memory DB is not
  assumed empty.
- No external services, no sleeps, no time-dependent assertions.

## 7. Excluded scenarios (with justification)

| Excluded | Why |
|---|---|
| "Customer can authenticate later" (Story AC-001 clause) | Authentication is US-002. Verified indirectly: the stored value is a BCrypt hash of the submitted password (persistence + service tests) and `role` is present. |
| `GET /api/v1/customers/{id}` response contract | Not implemented by US-001 (API design §1). Only a deny-by-default guard is kept. |
| Rate limiting / lockout | Out of scope, OD-005:A. |
| CSRF enforcement on *other* writable endpoints | US-001 adds no second writable endpoint; R-2 assigns full CSRF-scope verification to `SECURITY_REVIEW`. |
| DB `DEFAULT` / `CHECK` clause behavior | Hibernate `validate` does not check them (db-design §8.1); asserting them adds no requirement coverage. |

## 8. Known limitations & carried findings

- **F-3 (plan review):** the plan does not fix the unauthenticated response for
  protected non-registration routes (`401` vs form-login `302`).
  `RegistrationSecurityPostureTest.protectedRouteReturns401WhenUnauthenticated`
  asserts `401`, encoding the plan's own recommendation
  (`HttpStatusEntryPoint(401)`). If `IMPLEMENTATION` chooses form login, that
  test fails and `SECURITY_REVIEW` arbitrates per risk R-2 — this is a
  deliberate tripwire, not a defect in the test.
- **R-3 / PD-2:** the byte-vs-character boundary is covered at the HTTP layer
  now; the exhaustive validator matrix (11/12/72/73-byte vectors × each class)
  is specified in the AC-test matrix and owned by `IMPLEMENTATION` C-T1.
- **R-2:** the CSRF exemption is asserted as "tokenless POST to the registration
  path is not `403`"; that it stays enabled elsewhere is `SECURITY_REVIEW`'s.
- **R-5:** the existing `contextLoads` smoke test is **not modified** by
  `test-writer` (M-4 is `IMPLEMENTATION`'s edit). It currently passes; new tests
  carry `@ActiveProfiles("test")` themselves.
- Pre-implementation, all new behavior tests fail with HTTP `403` (Spring
  Security default CSRF rejects the tokenless POST before routing) or a
  missing-table SQL error. Both are "feature not implemented" failures, not test
  defects — see the test-generation report for the per-test classification.
- `docs/decisions/US-001-open-decisions.md` v1 still lists OD-001..OD-006 as
  `OPEN`; the resolutions applied here are the authoritative ones recorded in
  `history.jsonl` at `HUMAN_SPEC_APPROVAL`. Documentation lag owned by
  `us-clarifier`; non-blocking.

## 9. Open Decisions affecting testing

None open. All six are resolved and applied consistently across the Story
artifacts and this suite.
