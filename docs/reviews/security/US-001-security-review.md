---
artifact_type: security_review
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T12:34:36Z
updated_at: 2026-08-31T12:34:36Z
produced_by: security-reviewer
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/reviews/specifications/US-001-spec-review.md
    version: 1
  - path: docs/impact-analysis/US-001-impact-analysis.md
    version: 1
  - path: docs/plans/US-001-implementation-plan.md
    version: 1
  - path: docs/reviews/plans/US-001-plan-review.md
    version: 1
  - path: docs/evidence/US-001-implementation-report.md
    version: 1
  - path: docs/verification/US-001-implementation-verification.md
    version: 1
  - path: docs/designs/api/US-001-api-design.md
    version: 1
  - path: docs/designs/api/US-001-openapi.yaml
    version: 1
  - path: docs/designs/database/US-001-db-design.md
    version: 1
  - path: docs/designs/database/US-001-entity-model.md
    version: 1
  - path: docs/reviews/designs/US-001-design-review.md
    version: 1
  - path: docs/tests/US-001-test-strategy.md
    version: 1
  - path: docs/tests/US-001-ac-test-matrix.md
    version: 1
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
supersedes: null
critical_findings: 0
major_findings: 0
minor_findings: 4
informational_findings: 2
security_sensitive: true
runtime_checks: PARTIAL
semantic_analysis: TEXT_FALLBACK
---

# Security Review — US-001 Customer Registration

## 1. Executive Summary

**Verdict: PASS.** The US-001 registration implementation introduces no Critical
or Major security risk. Every security requirement in Specification §7 (SEC-1..
SEC-11) is independently verifiable in code, configuration, and tests, and every
security-sensitive Open Decision (OD-002 CSRF, OD-003 enumeration, OD-005
anti-abuse) was resolved by a human at `HUMAN_SPEC_APPROVAL`
(`history.jsonl` 2026-08-31T07:48:48Z: `OD-001:A OD-002:B OD-003:A OD-004:A
OD-005:A OD-006:A`).

Principal verified controls: BCrypt hashing with no no-op encoder; plaintext
password confined to the inbound DTO (never persisted, returned, or logged —
production code contains no logger at all); deny-by-default authorization with a
single exact-match public route; the CSRF exemption scoped to that same single
matcher; H2 console disabled in every profile; `ddl-auto=validate` in every
profile; generic error bodies with no internal leakage; mass-assignment closed
(`fail-on-unknown-properties=true`, only `email`/`password` bindable).

Findings: 0 Critical, 0 Major, 4 Minor, 2 Informational. None blocks
progression; none requires a code change for this Story.

**Recommended next action:** advance to `RECONCILIATION`.

## 2. Reviewed Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Story | docs/stories/US-001-register-customer.md | (unversioned) | active |
| Specification | docs/specifications/US-001-spec.md | 1 | APPROVED |
| Specification Review | docs/reviews/specifications/US-001-spec-review.md | 1 | APPROVED (PASS) |
| API Design | docs/designs/api/US-001-api-design.md | 1 | APPROVED |
| OpenAPI Contract | docs/designs/api/US-001-openapi.yaml | 1 | APPROVED |
| DB Design | docs/designs/database/US-001-db-design.md | 1 | APPROVED |
| Entity Model | docs/designs/database/US-001-entity-model.md | 1 | APPROVED |
| Design Review | docs/reviews/designs/US-001-design-review.md | 1 | APPROVED |
| Impact Analysis | docs/impact-analysis/US-001-impact-analysis.md | 1 | DRAFT (PASS; no review stage follows) |
| Implementation Plan | docs/plans/US-001-implementation-plan.md | 1 | APPROVED (+ HUMAN_PLAN_APPROVAL 2026-08-31T11:20:00Z) |
| Plan Review | docs/reviews/plans/US-001-plan-review.md | 1 | APPROVED |
| Implementation Report | docs/evidence/US-001-implementation-report.md | 1 | DRAFT |
| Implementation Verification | docs/verification/US-001-implementation-verification.md | 1 | DRAFT (verdict PASS) |
| Test Strategy | docs/tests/US-001-test-strategy.md | 1 | DRAFT |
| AC–Test Matrix | docs/tests/US-001-ac-test-matrix.md | 1 | DRAFT |
| Open Decisions | docs/decisions/US-001-open-decisions.md | 1 | DRAFT (stale — see F-4) |

Front matter of the three review artifacts was read directly:
`specification_review` `status: APPROVED`, `design_review` `status: APPROVED`,
`plan_review` `status: APPROVED`. `history.jsonl` records `SPEC_REVIEW` PASS,
`DESIGN_REVIEW` PASS, `PLAN_REVIEW` PASS and both human gates
(`HUMAN_SPEC_APPROVAL` 2026-08-31T07:48:48Z, `HUMAN_PLAN_APPROVAL`
2026-08-31T11:20:00Z). No consumed input is `SUPERSEDED`. No staleness that
blocks: the only stale artifact is `open_decisions` v1 (F-4), whose resolved
values are authoritative in `history.jsonl` and are applied correctly
throughout the implementation.

`implementation_verification` verdict is **PASS** (precondition satisfied).

## 3. Security-Relevant Scope

- **Exposed functionality:** one new endpoint, `POST /api/v1/customers`
  (public, unauthenticated, `application/json` only).
- **Protected assets:** customer credentials (plaintext password in transit /
  request binding, BCrypt hash at rest), customer email (PII), account role and
  `enabled` state, the H2 database file.
- **Trust boundaries crossed:** external client → `CustomerController`
  (`@Valid` binding); controller → `CustomerService` (business rules, policy
  re-check, hashing); service → `CustomerRepository` → H2; developer environment
  → repository (secrets / generated DB files).
- **Affected security components:** `security/SecurityConfig` (new),
  `validation/{ValidPassword,PasswordPolicyValidator}` (new),
  `exception/GlobalExceptionHandler` (new), `model/request/RegistrationRequest`
  (new), `model/dto/{CustomerResponse,ErrorResponse,ApiFieldError}` (new),
  `application.yaml` (new persistence + Jackson config), `.gitignore` (`/data/`).

## 4. Environment and Tools

- Spring Boot 4.1.1; Spring Security (session-based stack, form/basic disabled);
  Hibernate ORM 7.4.5; H2 2.x; Java 21 (Gradle toolchain); Gradle 9.7.1.
- Active profile under test: `test` (`application-test.yaml`, isolated in-memory
  H2). Default profile: file-based H2.
- Review tooling: built-in file inspection, `git`, `grep`. IDEA MCP semantic /
  build / runtime tools not invoked → `semantic_analysis: TEXT_FALLBACK`,
  `runtime_checks: PARTIAL`.
- Build/test evidence is **reused** from `implementation_verification` v1
  (`./gradlew clean build` BUILD SUCCESSFUL, 62 tests / 0 failures across 7
  suites, independently reproduced by the verifier). This review did **not**
  re-run the build and did **not** perform dependency vulnerability scanning
  (see §22).
- No secrets encountered or recorded.

## 5. Authentication Review

| Item | Evidence | Result |
|---|---|---|
| Story adds no authentication mechanism | US-001 §10 Out of Scope (login is US-002); no `UserDetailsService`, no `formLogin`, no `httpBasic` in `SecurityConfig` | OK |
| Registration requires no authentication (SEC-1) | `SecurityConfig` `REGISTRATION_ENDPOINT` → `permitAll()`; `RegistrationSecurityPostureTest.registrationEndpointIsReachableWithoutAuthentication` | VERIFIED |
| Created account is a future auth subject (FR-9) | BCrypt hash verifies against submitted password (`CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash`); `role = CUSTOMER`, `enabled = true` | VERIFIED |
| Protected routes deny unauthenticated access | `anyRequest().authenticated()`; `HttpStatusEntryPoint(401)`; `RegistrationSecurityPostureTest.protectedRouteReturns401WhenUnauthenticated` (401, not a 302 redirect) | VERIFIED |

No findings.

## 6. Authorization Review

| Item | Evidence | Result |
|---|---|---|
| Deny-by-default (SC-4, SEC-1) | `authorizeHttpRequests` = one `permitAll` matcher + `anyRequest().authenticated()` | VERIFIED |
| Public scope is minimal | `REGISTRATION_ENDPOINT` matches **exactly** `POST` + URI `/api/v1/customers`; any other method/path on the same URI falls through to `authenticated()` | VERIFIED |
| No role/ownership operation introduced | US-001 adds no read/update/delete endpoint; no `@PreAuthorize`; nothing to bypass | N/A (in scope) |
| Service cannot be reached around authorization | `CustomerService.register` is only called by `CustomerController.register`; controller path is the `permitAll` route by design | VERIFIED (text/import analysis) |

No findings. (Confidence: high from imports/matchers; PSI call-graph not
cross-checked — see §22.)

## 7. Password and Credential Handling

| Requirement | Evidence | Result |
|---|---|---|
| Plaintext only on inbound DTO (SEC-3, SC-1) | `RegistrationRequest.password`; never copied to `Customer`, `CustomerResponse`, or any exception; `CustomerService` passes it only to `passwordEncoder.encode` and `PasswordPolicyValidator.isCompliant` | VERIFIED |
| Never persisted (AC-004, FR-7) | `schema.sql` has no plaintext column; `Customer` has only `passwordHash`; `CustomerSchemaTest.noPlaintextPasswordColumnExists` | VERIFIED |
| Never logged (SEC-3) | `grep -rniE "logger|log\.|System\.out|println|slf4j" src/main` → **no matches**; no `logging.*`/`show-sql` in either properties file; `Customer.toString()` hand-excludes `passwordHash` | VERIFIED (with F-1) |
| Never returned (AC-005, SEC-4) | `CustomerResponse` = `id, email, role, createdAt` only; `CustomerRegistrationApiTest.successResponseNeverExposesCredentialOrInternalState` asserts `$.password`, `$.passwordHash`, `$.password_hash`, `$.enabled`, `$.updatedAt` all absent | VERIFIED |
| BCrypt, no no-op encoder (SEC-2, NFR-1) | `SecurityConfig.passwordEncoder()` = `new BCryptPasswordEncoder()` (default strength), bean in `security` package; `CustomerServiceTest` asserts a real `$2[aby]$`-format hash that verifies and `!= raw` | VERIFIED |
| Policy enforced request-layer **and** service re-check (FR-6, SC-1) | `@ValidPassword` → `PasswordPolicyValidator.isValid` at binding; `CustomerService.register` calls `PasswordPolicyValidator.isCompliant` before `encode`; `CustomerServiceTest.serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository` | VERIFIED |
| Policy matches SC-1 exactly | `MIN_BYTES=12`, `MAX_BYTES=72` (UTF-8 bytes — BCrypt input bound, PD-2/R-3), + one upper/lower/digit/special; `PasswordPolicyValidatorTest` (17 cases incl. 11/12/72/73-byte and multi-byte) | VERIFIED |
| Validation message never echoes the value (SC-9, R-7) | `@ValidPassword` message is the fixed string `"Password does not meet the security policy."`; `InvalidPasswordException` → fixed `"The submitted password does not meet the security policy."`; `PasswordPolicyValidatorTest.defaultMessageNeverEchoesASubmittedValue`, `CustomerRegistrationApiTest.passwordValidationMessageDoesNotEchoTheSubmittedValue` | VERIFIED (R-7 closed) |
| Test fixtures introduce no real credentials | passwords in tests are synthetic policy strings (`Aa1!aaaaaaaa` etc.); dummy BCrypt literals are obvious placeholders | VERIFIED |

Finding: **F-1** (Minor, PASSWORD_HANDLING / DATA_EXPOSURE) — see §19.

## 8. Sensitive Data Exposure

| Surface | Review | Result |
|---|---|---|
| Response DTO | `CustomerResponse` minimal; no hash, no `enabled`, no `updatedAt` | OK |
| Entity serialization | `Customer` never on a controller signature (AD-4); not a Jackson type on any endpoint | OK |
| Exception responses | `ErrorResponse` = `timestamp, status, error, message, path, fieldErrors[]`; `message` is a fixed client-safe string per branch; `fieldErrors[]` carries only `field` + the static constraint message | OK |
| Logs / debug output | none — no logger in `src/main`; no debug logging config | OK |
| Implementation report / telemetry | report contains no secret; `docs/hooks/tool-usage.jsonl` is git-ignored | OK |
| `toString()` of the request record | `RegistrationRequest` is a `record` → generated `toString()` renders `password` | F-1 (Minor) |

## 9. Input Validation

| Item | Evidence | Result |
|---|---|---|
| Server-side, no framework-default reliance (NFR-2) | explicit `@NotBlank @Email @Size(max=254)` on `email`; explicit `@ValidPassword` + custom validator on `password` | VERIFIED |
| Runtime activation | `@Valid @RequestBody` on `CustomerController.register` → `MethodArgumentNotValidException` → `handleValidation` → `400` + `fieldErrors[]`; proven by the real-MVC web tests (`malformedEmailReturns400WithEmailFieldError`, 8 password vectors) | VERIFIED |
| Length bounds explicit | email `@Size(max=254)` + `VARCHAR(254)`; password 12..72 bytes | VERIFIED |
| Required fields enforced | `@NotBlank` on both; null/empty password delegated to `@NotBlank`, and the service `isCompliant` gate treats null/empty as non-compliant | VERIFIED |
| Unknown / extra JSON fields | `spring.jackson.deserialization.fail-on-unknown-properties=true` (both profiles) → `HttpMessageNotReadableException` → `400`; `CustomerRegistrationApiTest.unknownJsonPropertyReturns400` | VERIFIED |
| Mass assignment (role / enabled injection) | only `email` + `password` are record components; any other property → `400` (above). Role/`enabled` are set server-side in `CustomerService` only | VERIFIED |
| Malformed JSON | `400` via the same handler; `malformedJsonReturns400` | VERIFIED |
| Errors reveal no internals | `errorBodyNeverLeaksInternals` | VERIFIED |

No findings.

## 10. API Security

| Item | Evidence | Result |
|---|---|---|
| Only the approved endpoint exists | one `@PostMapping` on `/api/v1/customers`; no other controller | VERIFIED |
| Method + content type constrained | `consumes = APPLICATION_JSON_VALUE`; missing/other `Content-Type` → `415` (`nonJsonContentTypeReturns415`, `missingContentTypeReturns415`) | VERIFIED |
| Request fields restricted | `{email, password}` only, `additionalProperties:false` enforced at runtime | VERIFIED |
| Response fields minimized | `CustomerResponse` = OD-004:A field set | VERIFIED |
| `201` + `Location` | `ResponseEntity.created(location)`; `Location: /api/v1/customers/{id}` | VERIFIED |
| Error responses match AC-6 shape, no leak | `GlobalExceptionHandler` single advice, one branch per status; `errorBodyHasTheApiConventionShape` | VERIFIED |
| Auth declaration matches implementation | OpenAPI `security: []` for this path ↔ `permitAll` matcher | VERIFIED |

Finding: **F-2** (Minor, API_SECURITY / documentation) — implemented `400`/`415`
`message` strings differ verbatim from the OpenAPI *illustrative* `examples`
(the `409` message matches exactly; all tests assert status + body shape). No
contract-behavior mismatch. Carried from IMPL_VERIFICATION MF-2 / disclosed D-4.

## 11. Persistence Security

| Item | Evidence | Result |
|---|---|---|
| No plaintext password column | `schema.sql`; `CustomerSchemaTest.noPlaintextPasswordColumnExists` | VERIFIED |
| `password_hash VARCHAR(60) NOT NULL` (PC-9) | `schema.sql` + `@Column(length=60, nullable=false)`; `passwordHashColumnIsVarchar60NotNull` | VERIFIED |
| `email VARCHAR(254) NOT NULL`, `uq_customer_email` UNIQUE | `schema.sql` + entity `@UniqueConstraint`; `emailColumnIsVarchar254NotNull`, `emailHasAUniqueConstraint` | VERIFIED |
| Case-insensitive uniqueness (BR-001/BR-002, OD-006:A) | `CustomerService.normalizeEmail` (`trim().toLowerCase(ROOT)`) before check and save + plain UNIQUE; `duplicateEmailIsRejectedCaseInsensitivelyWith409` | VERIFIED |
| Explicit nullability / lengths on every column | entity + `schema.sql` agree; `ddl-auto=validate` boots clean | VERIFIED |
| `role` constrained | `EnumType.STRING` + `ck_customer_role CHECK (role IN ('CUSTOMER','ADMIN'))` | VERIFIED |
| Audit timestamps UTC, `created_at` immutable | `JpaAuditingConfig` UTC `DateTimeProvider`; `created_at updatable=false`; `CustomerPersistenceTest` | VERIFIED |
| Generated DB files git-ignored | `.gitignore` `/data/`; not tracked, not in tree | VERIFIED |
| `ddl-auto` = `validate` both profiles (SEC-10, SC-8) | `application.yaml`, `application-test.yaml` | VERIFIED |
| DB path not exposed in responses/logs | no logging; error `message` strings are static | VERIFIED |

Finding: **F-3** (Minor, PERSISTENCE / robustness) — `CustomerService.register`
uses check-then-act (`existsByEmail` → `save`); a concurrent duplicate trips
`uq_customer_email` as an unmapped `DataIntegrityViolationException` → `500`
rather than `409`. The `uq_customer_email` constraint still prevents the second
row, so this is a robustness/response-quality gap, not a data-integrity or
enumeration defect. No AC/design/test covers concurrency; anti-abuse is
explicitly out of scope (OD-005:A). Carried from IMPL_VERIFICATION MF-3.

## 12. H2 and Application Configuration

| Item | Value / Evidence | Result |
|---|---|---|
| H2 console (SC-6, SEC-9) | `spring.h2.console.enabled=false` in `application.yaml` **and** `application-test.yaml`; `RegistrationSecurityPostureTest.h2ConsoleIsNotExposed`; no `/h2-console` permit rule | VERIFIED |
| H2 mode | file-based default (`jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`), isolated in-memory for tests | matches DB design §8 |
| `ddl-auto` | `validate` (both) | VERIFIED |
| `spring.sql.init.mode` | `always` (default) / `embedded` (test) | matches DB design §8 |
| Datasource credentials | `username: sa`, `password: ""` (H2 default, local file DB) | F-6 (Informational) |
| `AUTO_SERVER=TRUE` | present in the **approved** DB design §8 (line 172), plan M-2, and impact analysis — approved configuration | F-5 (Informational) |
| Unsafe defaults becoming runtime defaults | test-only settings live in `application-test.yaml`; no profile leakage | OK |

Findings: **F-5**, **F-6** (both Informational) — see §19.

## 13. Logging and Telemetry

- **Application logging:** none added. `grep` over `src/main` finds no logger,
  `System.out`, or `println`. No `logging.level.*`, `spring.jpa.show-sql`, or
  Hibernate SQL logging in either properties file. Default Spring MVC logging
  does not emit request bodies at `INFO`.
- **Exception logging:** `GlobalExceptionHandler` does not log; it reads only
  `HttpServletRequest.getRequestURI()` and `BindingResult.getFieldErrors()`
  (field name + static message), never the bound DTO.
- **Tool-usage telemetry:** `docs/hooks/tool-usage.jsonl` is git-ignored
  (`.gitignore`); this Story does not modify hook configuration. Not inspected
  for payload content (out of Story scope); AGENTS.md states it is metadata-only
  by policy.
- No password, hash, token, `Authorization` header, or DB credential can reach
  any log via US-001 code paths.

No findings. (F-1 is the latent enabler if logging is later added — tracked
there.)

## 14. Dependencies

| Dependency | Status |
|---|---|
| `spring-boot-starter-validation` | **approved** — plan M-1 (needed for `@Valid` / custom constraint) |
| `spring-boot-starter-security`, `-data-jpa`, `-webmvc`, `h2`, `lombok` | pre-existing, unchanged |
| Test: `-security-test`, `-data-jpa-test`, `-webmvc-test` | test scope only; not on the runtime classpath |

No unapproved dependency. No dependency pinned to an explicit version in
`build.gradle.kts` (all managed by the Spring Boot BOM 4.1.1). **Vulnerability
database scanning was not performed** — no claim is made about known-CVE status
of the resolved dependency tree (§22).

## 15. Security Test Coverage

| Security requirement / abuse case | Test(s) | Status |
|---|---|---|
| SEC-2 BCrypt hash before persistence | `CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` (real encoder, `$2[aby]$` regex, verifies, `!= raw`) | COVERED |
| AC-004 no plaintext column / stored | `CustomerSchemaTest.noPlaintextPasswordColumnExists`, `.passwordHashColumnIsVarchar60NotNull` | COVERED |
| AC-005 / SEC-3/SEC-4 hash + internal state absent from response | `CustomerRegistrationApiTest.successResponseNeverExposesCredentialOrInternalState` | COVERED |
| AC-006 invalid password rejected pre-persistence | `CustomerRegistrationApiTest` (8 vectors) + `CustomerServiceTest.serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository` (`save` never called) | COVERED |
| AC-003 invalid email rejected | `CustomerRegistrationApiTest.malformedEmailReturns400WithEmailFieldError`, `.blankEmailReturns400WithEmailFieldError`, `.emailLongerThan254CharsReturns400WithEmailFieldError` | COVERED |
| AC-002 / SEC-8 duplicate email (any case) → OD-003:A `409`, no 2nd account | `CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`; `CustomerServiceTest` (both cases, `save` never called) | COVERED |
| Mass assignment (unknown field) | `CustomerRegistrationApiTest.unknownJsonPropertyReturns400` | COVERED |
| SEC-9 validation message never echoes value | `PasswordPolicyValidatorTest.defaultMessageNeverEchoesASubmittedValue`, `CustomerRegistrationApiTest.passwordValidationMessageDoesNotEchoTheSubmittedValue` | COVERED |
| SEC-1 registration public / other routes deny-by-default | `RegistrationSecurityPostureTest` (reachable w/o auth; protected route → 401) | COVERED |
| SEC-7 CSRF exemption scoped to the registration POST | `RegistrationSecurityPostureTest.registrationPostIsAcceptedWithoutACsrfToken` | COVERED |
| SEC-6/SEC-9 H2 console not exposed | `RegistrationSecurityPostureTest.h2ConsoleIsNotExposed` | COVERED |
| SEC-6 error body leaks no internals | `CustomerRegistrationApiTest.errorBodyNeverLeaksInternals` | COVERED |
| Concurrent duplicate (F-3) | none | GAP (accepted — out of scope, follow-up) |
| `RegistrationRequest.toString()` (F-1) | none | GAP (accepted — no leak path today) |

Test quality: security tests use the real `SecurityFilterChain`
(`@SpringBootTest` + `@AutoConfigureMockMvc`), not a stubbed `@WebMvcTest`;
persistence tests hit H2 `INFORMATION_SCHEMA`; service tests use a real
`BCryptPasswordEncoder`. No test mocks away the property it asserts.

## 16. Abuse Case Review

| Scenario | Expected protection | Evidence | Status |
|---|---|---|---|
| Submit `role` / `enabled` in the JSON to self-escalate | field ignored/rejected; server sets role | `fail-on-unknown-properties=true` → `400`; `unknownJsonPropertyReturns400` | PROTECTED |
| Repeated duplicate registrations | `409`, no second account | AC-002 tests | PROTECTED (single-request); F-3 for the concurrent race |
| Oversized email / password | `400` (`@Size(max=254)`; password > 72 bytes) | `emailLongerThan254CharsReturns400...`, `passwordOver72BytesButUnder72CharsReturns400...` | PROTECTED |
| Malformed / non-JSON body | `400` / `415` | `malformedJsonReturns400`, `nonJsonContentTypeReturns415` | PROTECTED |
| Weak / non-compliant password | `400`, no account, no hash computed | 8 password vectors + service re-check | PROTECTED |
| Inspect success response for credentials | none present | `successResponseNeverExposesCredentialOrInternalState` | PROTECTED |
| Account enumeration via `409` message | accepted risk (OD-003:A / SEC-8) | human-approved decision | ACCEPTED RISK (not a finding) |
| High-volume automated signups (DoS / spam) | out of scope (OD-005:A) | no NFR/AC requires rate limiting | OUT OF SCOPE (follow-up recommended) |
| Reach the H2 TCP server started by `AUTO_SERVER` | blank `sa` password on a local file DB | F-5 | INFORMATIONAL (approved config; local dev only) |

## 17. Repository Hygiene

- **Secret-like files:** none. No `.env`, token, key, or credential file tracked
  or untracked in scope. `application.yaml` `password: ""` is the H2 default for
  a local file DB, not a real secret (F-6).
- **Generated H2 files:** `/data/` git-ignored; not present in the working tree.
- **Working tree:** `M .gitignore`, `M docs/evidence/.gitignore` — both are
  pre-existing harness housekeeping (adding ignore entries for
  `harness-*` scaffolding and `harness-consistency-review.md`), **not** part of
  US-001 and not security-relevant. No `src/**` change is uncommitted.
- **Untracked:** `docs/evidence/harness-*.{py,md}`,
  `docs/harness-consistency-review.md` — pre-existing, outside this Story, left
  untouched.
- No secret value is reproduced in this report.

## 18. Deviations

| Deviation | Security assessment |
|---|---|
| `exception/InvalidPasswordException` added (D-1, plan C-12 left the type unspecified) | Benign. Domain exception, no HTTP concept, no submitted value in the message, mapped to `400`. Improves defense-in-depth (FR-6). |
| Implemented error `message` strings differ from OpenAPI examples (D-4 / F-2) | Illustrative examples only; AC-6 shape preserved; no information leak. |
| `open_decisions.md` v1 still `OPEN` (F-4) | Documentation lag only. Resolutions are authoritative in `history.jsonl` and applied correctly. |
| `CustomerSchemaTest` 2-char fixture fix by the implementer (IMPL_VERIFICATION MF-1) | Not security-relevant; no assertion changed. Ownership crossing noted for RECONCILIATION. |

No undisclosed security-relevant deviation found.

## 19. Findings

### F-1 — `RegistrationRequest` record `toString()` renders the plaintext password

- **Severity:** Minor. **Category:** PASSWORD_HANDLING / DATA_EXPOSURE.
- **Affected:** `src/main/java/org/example/customerportal/model/request/RegistrationRequest.java`
- **Observed:** `RegistrationRequest` is a `record`, so the compiler-generated
  `toString()` includes every component, `password` among them. By contrast
  `Customer.toString()` was hand-written specifically to exclude `passwordHash`
  — the project treats credential-in-`toString()` as a real concern.
- **Expected:** SEC-3 / SC-1 — the plaintext password is "never logged, never
  placed on a response DTO." A `toString()` that emits it is a latent violation
  of that intent.
- **Risk:** Low **today**. Independently confirmed: production code has no
  logger, no `System.out`/`println` (`grep` over `src/main`), no SQL/debug
  logging config; `GlobalExceptionHandler` never touches the bound DTO;
  `CustomerController` / `CustomerService` never call `request.toString()` or
  log the request. No reachable leak path exists. The risk is realized only if
  future code logs the request object or a framework is configured to.
- **Required correction:** none for US-001. **Recommendation:** a follow-up
  change should give `RegistrationRequest` a custom `toString()` that masks
  `password` (consistent with `Customer`), and RECONCILIATION should record this
  as accepted-with-recommendation.
- **Loop-back target:** none.

### F-2 — Error `message` strings differ from OpenAPI illustrative examples

- **Severity:** Minor. **Category:** API_SECURITY / documentation.
- **Affected:** `exception/GlobalExceptionHandler.java`, `docs/designs/api/US-001-openapi.yaml`
- **Observed:** implemented `415` message `"Content-Type must be
  application/json."` vs. contract example `"Content-Type 'text/plain' is not
  supported."`; one combined `400` message for
  missing/malformed/unknown-field vs. two contract examples. The `409` message
  matches exactly.
- **Expected:** OpenAPI `examples` are explicitly illustrative, not normative;
  AC-6 body **shape** is what the contract fixes, and it is preserved.
- **Risk:** none — the implemented messages are *more* generic than the
  examples, so no additional information is disclosed.
- **Required correction:** none. Optionally `openapi-designer` aligns the
  examples in a future revision.
- **Loop-back target:** none.

### F-3 — Concurrent duplicate registration maps to `500`, not `409`

- **Severity:** Minor. **Category:** PERSISTENCE / ERROR_HANDLING (robustness).
- **Affected:** `service/CustomerService.java`, `exception/GlobalExceptionHandler.java`
- **Observed:** check-then-act (`existsByEmail` → `save`). Two simultaneous
  requests for the same new email can both pass the check; the second `save`
  throws `DataIntegrityViolationException` (from `uq_customer_email`), which is
  unmapped and falls through to `handleUnexpected` → `500`.
- **Expected:** no approved artifact covers concurrency. AC-002 (the
  single-request duplicate path) is correctly `409`. The unique constraint
  still prevents the duplicate row, so data integrity holds.
- **Risk:** low — a `500` on a rare race; no data corruption, and no
  information disclosure beyond what the accepted OD-003:A `409` already
  reveals. Not a DoS vector on its own.
- **Required correction:** none for US-001. **Recommendation:** a follow-up
  Story maps `DataIntegrityViolationException` on `uq_customer_email` to `409`
  (catch-and-translate, or rely on the constraint alone with a handler).
- **Loop-back target:** none.

### F-4 — `open_decisions.md` v1 still shows OD-001..OD-006 as `OPEN`

- **Severity:** Minor. **Category:** REPOSITORY_HYGIENE / documentation.
- **Affected:** `docs/decisions/US-001-open-decisions.md`
- **Observed:** the artifact is `status: DRAFT` with all six decisions `OPEN`.
  The authoritative resolutions (`OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A
  OD-006:A`) are recorded in `history.jsonl` at `HUMAN_SPEC_APPROVAL`
  (2026-08-31T07:48:48Z) and are applied correctly throughout the code and the
  downstream artifacts.
- **Risk:** none to the implementation; a reader consulting only the file could
  be misled.
- **Required correction:** `us-clarifier` publishes `open_decisions.md` v2 with
  the resolved statuses. Carried from spec-review F-1 / IMPL_VERIFICATION MF-4.
- **Loop-back target:** none (owned by `us-clarifier`, not a SECURITY_REVIEW
  loop-back target).

### F-5 — `AUTO_SERVER=TRUE` starts an H2 TCP listener guarded by a blank `sa` password

- **Severity:** Informational. **Category:** CONFIGURATION.
- **Affected:** `src/main/resources/application.yaml`
- **Observed:** the default-profile datasource URL is
  `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE` with `username: sa`,
  `password: ""`. `AUTO_SERVER` starts an H2 automatic mixed-mode server so
  other local processes can attach to the same file DB; the account guarding it
  has no password.
- **Expected / provenance:** this exact URL is specified in the **approved** DB
  design §8 (line 172), the **approved** plan M-2, and the impact analysis — it
  is approved configuration, not an implementer addition. The test profile uses
  isolated in-memory H2 with no server.
- **Risk:** for a local training project, low and accepted by the approved
  design. The listener is local-oriented (mixed mode for concurrent local JVMs)
  and the DB holds only training data. Not exposed to the public API. It is
  nonetheless a blank-credential network listener.
- **Required correction:** none. **Recommendation:** a future hardening Story
  could drop `AUTO_SERVER` (single-process access is sufficient) or bind it
  explicitly to loopback, and set an H2 password from externalized config.
- **Loop-back target:** none.

### F-6 — Empty datasource password committed in `application.yaml`

- **Severity:** Informational. **Category:** SECRET_MANAGEMENT.
- **Affected:** `src/main/resources/application.yaml`
- **Observed:** `spring.datasource.password: ""` and `username: sa` are checked
  in.
- **Expected:** SC-7 — "config secrets come from environment variables /
  externalized config." An empty string is the H2 default and not a secret, so
  no policy is breached, but the pattern (inline credential fields) is worth
  noting before a real database is introduced.
- **Risk:** none for local file H2 with an empty password.
- **Required correction:** none for US-001. When US-00x introduces a real
  datasource, the credentials must be externalized.
- **Loop-back target:** none.

## 20. Positive Controls (independently observed)

1. **BCrypt, no no-op encoder** — `new BCryptPasswordEncoder()` in the
   `security` package; a real `$2[aby]$` verifying hash is asserted by a test
   using the real encoder.
2. **Plaintext password confined to the request DTO** — traced through
   `CustomerService`: used only for `isCompliant` and `encode`, never stored on
   the entity, DTO, or exception.
3. **No logging anywhere in production code** — `grep` over `src/main` returns
   nothing; no SQL/debug logging config. Credentials cannot reach a log via
   US-001 paths.
4. **Response DTO isolation** — `CustomerResponse` carries exactly
   `id, email, role, createdAt`; positively asserted absence of
   `password`/`passwordHash`/`enabled`/`updatedAt`.
5. **Deny-by-default authorization** — `anyRequest().authenticated()` with a
   single exact `POST /api/v1/customers` `permitAll` matcher; protected route
   returns `401` (verified, not a redirect).
6. **CSRF exemption minimally scoped** — the *same* single `RequestMatcher`
   drives both `permitAll` and `csrf.ignoringRequestMatchers`; CSRF stays on
   globally (OD-002:B, the recorded SC-5 architecture decision).
7. **H2 console disabled in every profile** — `enabled: false` in both
   properties files; `GET /h2-console` asserted non-200; no permit rule.
8. **`ddl-auto=validate` in every profile** — schema is hand-written
   `schema.sql`; entity mapping validated against it at boot (SC-8).
9. **Mass-assignment closed** — `fail-on-unknown-properties=true`; only
   `email`/`password` bindable; `role`/`enabled` set server-side only.
10. **Error hygiene** — one `@RestControllerAdvice`, one static client-safe
    message per status, `500` fallback leaks nothing; asserted by
    `errorBodyNeverLeaksInternals`.
11. **Password policy = SC-1 exactly**, enforced twice (request constraint +
    service re-check), byte-measured for the BCrypt bound; static non-echoing
    message.

## 21. Open Decisions

All six US-001 Open Decisions were resolved by a human at `HUMAN_SPEC_APPROVAL`
(`history.jsonl` 2026-08-31T07:48:48Z): `OD-001:A OD-002:B OD-003:A OD-004:A
OD-005:A OD-006:A`. The security-sensitive ones — OD-002 (CSRF: exempt the
registration POST only), OD-003 (duplicate-email: explicit `409`, enumeration
exposure accepted per SEC-8), OD-005 (anti-abuse: out of scope, follow-up) —
are all resolved and correctly implemented.

**No blocking security Open Decisions were identified.** The `open_decisions.md`
file being stale (F-4) is a documentation issue, not an unresolved decision.

## 22. Review Limitations

- **Build/test not re-run in this stage.** Build and test evidence
  (`./gradlew clean build`, 62/62) is reused from `implementation_verification`
  v1, which independently reproduced it. This review did not re-execute it.
- **`runtime_checks: PARTIAL`** — no application boot, no live HTTP probing, no
  live H2/TCP reachability test was performed by this stage. Security posture
  conclusions rest on the existing security tests (which run the real filter
  chain) plus code/config inspection.
- **`semantic_analysis: TEXT_FALLBACK`** — IDEA MCP semantic tools not invoked.
  Authorization / call-path conclusions rest on import graphs and method
  signatures, not PSI call analysis. Layering is unambiguous from imports here.
- **No dependency vulnerability scanning.** No CVE database was consulted; no
  claim is made about known vulnerabilities in the resolved dependency tree. An
  approved scanner (e.g. OWASP Dependency-Check) should run if organizational
  policy requires it.
- **Concurrency (F-3)** is reasoned from code, not reproduced with a concurrent
  test.
- **Tool-usage telemetry payload content** was not audited (out of Story
  scope); policy (AGENTS.md, SC-9) states it is metadata-only.
- Human diff review and `HUMAN_PR_APPROVAL` remain outstanding by design.

## 23. Verdict Rationale

`implementation_verification` verdict is PASS (precondition met).
`specification_review`, `design_review`, and `plan_review` are all APPROVED
(front matter read directly); both human gates are recorded; no consumed input
is `SUPERSEDED`. Every security requirement SEC-1..SEC-11 is independently
verifiable in code, configuration, and executable tests that exercise the real
security filter chain. All security-sensitive Open Decisions were resolved by a
human and are correctly implemented. Mass assignment, plaintext exposure,
console exposure, schema-destruction, and authorization-bypass vectors are all
closed. The four Minor findings are latent/robustness/documentation items with
no reachable exploit path under the current code; the two Informational items
concern approved local-dev configuration. There is no Critical or Major finding,
so no `CHANGES_REQUIRED` loop-back is warranted; no mandatory security
requirement is undefined and no human security decision is pending, so no
`BLOCKED` condition applies.

**Verdict: PASS** → advance to `RECONCILIATION`.

```yaml
result:
  verdict: PASS
  stage: SECURITY_REVIEW
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/reviews/security/US-001-security-review.md
  next_stage: RECONCILIATION
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "F-1 (Minor, PASSWORD_HANDLING): RegistrationRequest is a record — its generated toString() renders the plaintext password. No reachable leak path today (no logger/System.out/println anywhere in src/main; GlobalExceptionHandler never touches the bound DTO; no debug/SQL logging config). SEC-3 holds. RECONCILIATION to record accepted-with-recommendation; follow-up: custom masking toString() consistent with Customer.toString()."
    - "F-2 (Minor, API_SECURITY/doc): implemented 400/415 error message strings differ verbatim from the OpenAPI illustrative examples (409 matches exactly; AC-6 shape preserved; tests assert status+shape). Implemented messages are more generic — no extra disclosure. Optional: openapi-designer aligns examples in a future revision. Carried from IMPL_VERIFICATION MF-2 / D-4."
    - "F-3 (Minor, PERSISTENCE/robustness): CustomerService.register is check-then-act (existsByEmail then save); a concurrent duplicate trips uq_customer_email as an unmapped DataIntegrityViolationException -> 500 instead of 409. Unique constraint still prevents the duplicate row; no data-integrity or enumeration impact beyond accepted OD-003:A. No AC/design/test covers concurrency; anti-abuse out of scope (OD-005:A). Follow-up Story to map the exception to 409. Carried from IMPL_VERIFICATION MF-3."
    - "F-4 (Minor, doc): docs/decisions/US-001-open-decisions.md v1 still shows OD-001..OD-006 OPEN; authoritative resolutions (OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A OD-006:A) are in history.jsonl at HUMAN_SPEC_APPROVAL 2026-08-31T07:48:48Z and applied correctly. Owned by us-clarifier (publish v2). No implementation impact."
    - "F-5 (Informational, CONFIGURATION): default-profile datasource URL uses AUTO_SERVER=TRUE with username sa / blank password — an H2 mixed-mode TCP listener with no credential. This is APPROVED configuration (DB design section 8 line 172, plan M-2, impact analysis), local-dev only, training data only, test profile uses isolated in-memory H2. Recommendation: a future hardening Story drops AUTO_SERVER or binds it to loopback and externalizes an H2 password."
    - "F-6 (Informational, SECRET_MANAGEMENT): spring.datasource.password: \"\" and username sa are committed in application.yaml. Empty string is the H2 default, not a secret, so SC-7 is not breached — but when a real datasource is introduced its credentials must be externalized."
    - "PASS basis: SEC-1..SEC-11 all independently verified in code/config/tests; security tests run the real SecurityFilterChain. Security-sensitive Open Decisions (OD-002 CSRF, OD-003 enumeration, OD-005 anti-abuse) resolved by human at HUMAN_SPEC_APPROVAL and correctly implemented. Account enumeration via the 409 message is human-accepted (SEC-8 / OD-003:A) — a Positive Control with noted accepted risk, not a finding."
    - "Positive controls verified: BCrypt (no no-op encoder); plaintext password confined to the request DTO; zero logging in src/main; CustomerResponse credential/internal-state isolation (positively asserted); deny-by-default authz with a single exact permitAll matcher; CSRF exemption on the same single matcher only; H2 console disabled every profile; ddl-auto=validate every profile; mass-assignment closed (fail-on-unknown-properties)."
    - "Limitations: build/tests reused from implementation_verification v1 (not re-run); runtime_checks PARTIAL (no app boot / live probing this stage); semantic_analysis TEXT_FALLBACK (no IDEA MCP); NO dependency vulnerability scanning performed — no CVE claim about the dependency tree."
```
