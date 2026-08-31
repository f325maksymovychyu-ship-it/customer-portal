---
artifact_type: traceability
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T12:52:00Z
updated_at: 2026-08-31T12:52:00Z
produced_by: reconciliation-reviewer
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/reviews/specifications/US-001-spec-review.md
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
  - path: docs/impact-analysis/US-001-impact-analysis.md
    version: 1
  - path: docs/plans/US-001-implementation-plan.md
    version: 1
  - path: docs/reviews/plans/US-001-plan-review.md
    version: 1
  - path: docs/tests/US-001-test-strategy.md
    version: 1
  - path: docs/tests/US-001-ac-test-matrix.md
    version: 1
  - path: docs/evidence/US-001-test-generation-report.md
    version: 1
  - path: docs/evidence/US-001-implementation-report.md
    version: 1
  - path: docs/verification/US-001-implementation-verification.md
    version: 1
  - path: docs/reviews/security/US-001-security-review.md
    version: 1
supersedes: null
---

# End-to-End Traceability Matrix — US-001 Customer Registration

Authoritative Acceptance-Criteria → artifact / code / test matrix for US-001.
`implementation-verifier`, `security-reviewer`, and `pr-preparer` reference this
matrix; they do not rebuild it. It is owned by `reconciliation-reviewer`.

Base package `org.example.customerportal`. Production base path
`src/main/java/org/example/customerportal/`. Test base path
`src/test/java/org/example/customerportal/`.

Final status vocabulary: `RECONCILED` (intent, spec, design, plan,
implementation, test, verification, and security evidence all agree);
`PARTIALLY_RECONCILED`; `NOT_RECONCILED`; `BLOCKED`.

## 1. Story Acceptance Criteria → full chain

### AC-001 — Successful Registration  (Spec AC-001)

| Dimension | Reference |
|---|---|
| Story | AC-001 Successful Registration |
| Specification | §4 FR-1, FR-5, FR-8, FR-9; §5 AC-001; §6.1 / §6.2 valid path |
| Resolved decisions | OD-001:A (email format/length), OD-004:A (`201` body = `id, email, role, createdAt`), OD-006:A (lowercase-in-service) |
| API design | api-design §4, §4.2, §4.3 `201`; openapi.yaml `paths./customers.post` `201` + `Location` + `CustomerResponse` |
| DB design | db-design §4.2 (`customer` columns), §8.1 DDL; entity-model §2, §4.1 |
| Impact analysis | §6 (controller/service/repository/entity create), §7, §8, §16 |
| Plan step / files | Steps 4, 8, 9; C-13, C-12, C-3, C-2, C-1, C-7, C-16 |
| Production code | `controller/CustomerController.register`; `service/CustomerService.register` (BCrypt encode, `Role.CUSTOMER`, `enabled=true`, `ServletUriComponentsBuilder` `Location`); `repository/CustomerRepository.save`; `model/entity/Customer`; `model/dto/CustomerResponse`; `src/main/resources/schema.sql` |
| Tests | `registration.CustomerRegistrationApiTest.validRegistrationReturns201WithLocationAndCustomerBody`, `.emailIsStoredAndReturnedNormalisedToLowercase`, `.password72CharsMeetingPolicyIsAccepted`; `service.CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash`; `security.RegistrationSecurityPostureTest.registrationEndpointIsReachableWithoutAuthentication` |
| Impl. Verification | §7 AC-001 VERIFIED; §8 contract match; §9 persistence match |
| Security Review | §5 (auth), §7 (BCrypt hash verifies, `role=CUSTOMER`, `enabled=true`) |
| **Final status** | **RECONCILED** |

### AC-002 — Unique Email  (Spec AC-002)

| Dimension | Reference |
|---|---|
| Story | AC-002 Unique Email |
| Specification | §4 FR-4; §6.1 uniqueness; §8 duplicate-email row; SEC-8 |
| Resolved decisions | OD-003:A (explicit `409` + message `"An account with this email already exists."`), OD-006:A (case-insensitive via lowercasing + plain `UNIQUE`) |
| API design | api-design §4.3 `409`; openapi.yaml `409` `duplicateEmail` example |
| DB design | db-design §4.3 `uq_customer_email`, §5 case-insensitive mechanism; entity-model §3 |
| Impact analysis | §6, §7, §8, §16 |
| Plan step / files | Step 8; C-12, C-10, C-11, C-3, C-16 |
| Production code | `service/CustomerService.register` (`normalizeEmail` → `existsByEmail` → `DuplicateEmailException`); `exception/GlobalExceptionHandler.handleDuplicateEmail` → `409` + OD-003:A message; `Customer` `@UniqueConstraint(uq_customer_email)`; `schema.sql` `uq_customer_email` |
| Tests | `registration.CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`; `service.CustomerServiceTest.duplicateEmailSameCaseThrowsAndDoesNotSave`, `.duplicateEmailDifferentCaseThrowsAndDoesNotSave`, `.emailIsNormalisedToLowercaseAndTrimmedBeforeCheckAndSave`; `persistence.CustomerSchemaTest.emailHasAUniqueConstraint`, `.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint` |
| Impl. Verification | §7 AC-002 VERIFIED (`save` never called on the second attempt) |
| Security Review | §11 (case-insensitive uniqueness VERIFIED); §16 abuse case "repeated duplicate registrations" PROTECTED (single-request); F-3 for the concurrent race |
| **Final status** | **RECONCILED** (single-request path; concurrent-duplicate race is finding RC-3, out of scope, no requirement covers it) |

### AC-003 — Email Validation  (Spec AC-003)

| Dimension | Reference |
|---|---|
| Story | AC-003 Email Validation |
| Specification | §4 FR-3; §6.1 format + max length |
| Resolved decisions | OD-001:A (`@Email` semantics + max 254) |
| API design | api-design §4.1; openapi.yaml `RegistrationRequest.email` (`minLength 1`, `maxLength 254`, `format: email`), `400` `invalidEmail` example |
| DB design | db-design §4.2 `email VARCHAR(254) NOT NULL` |
| Impact analysis | §7, §16 |
| Plan step / files | Steps 6, 9, 11; C-6, C-11 |
| Production code | `model/request/RegistrationRequest` (`@NotBlank @Email @Size(max = 254)` on `email`); `exception/GlobalExceptionHandler.handleValidation` → `400` + `fieldErrors[]` |
| Tests | `registration.CustomerRegistrationApiTest.malformedEmailReturns400WithEmailFieldError`, `.blankEmailReturns400WithEmailFieldError`, `.emailLongerThan254CharsReturns400WithEmailFieldError`; `persistence.CustomerSchemaTest.emailColumnIsVarchar254NotNull` |
| Impl. Verification | §7 AC-003 VERIFIED; §11 validation active at runtime |
| Security Review | §9 input validation VERIFIED (server-side, explicit constraints) |
| **Final status** | **RECONCILED** |

### AC-004 — Password Storage  (Spec AC-004)

| Dimension | Reference |
|---|---|
| Story | AC-004 Password Storage (not stored in plain text) |
| Specification | §4 FR-5, FR-6, FR-7; §7 SEC-2, SEC-3, SEC-4; PC-9 |
| Resolved decisions | — (no OD; PC-9 `password_hash VARCHAR(60)`) |
| API design | api-design §4.1 (`password` `writeOnly`, dual-layer policy); openapi.yaml `password` `writeOnly` |
| DB design | db-design §4.2 `password_hash VARCHAR(60) NOT NULL`, §7 sensitive-data handling; entity-model §2, §4.1 |
| Impact analysis | §8, §9 |
| Plan step / files | Steps 8, 10; C-12, C-14 (`BCryptPasswordEncoder` bean), C-2 |
| Production code | `service/CustomerService.register` (`passwordEncoder.encode(password)`, plaintext discarded); `security/SecurityConfig.passwordEncoder` = `new BCryptPasswordEncoder()`; `model/entity/Customer.passwordHash` `@Column(length = 60, nullable = false)`; `schema.sql` `password_hash VARCHAR(60) NOT NULL`, no plaintext column; `Customer.toString()` excludes `passwordHash` |
| Tests | `persistence.CustomerSchemaTest.passwordHashColumnIsVarchar60NotNull`, `.noPlaintextPasswordColumnExists`; `service.CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` (hash matches `^\$2[aby]\$\d\d\$.{53}$`, verifies, `!= raw`), `.serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository` |
| Impl. Verification | §7 AC-004 VERIFIED; §9 `password_hash` schema match; §12 no plaintext persisted |
| Security Review | §7 (BCrypt, no no-op encoder; plaintext confined to inbound DTO; never persisted/logged/returned — all VERIFIED); §20 positive controls 1, 2, 3 |
| **Final status** | **RECONCILED** (finding RC-1 — `RegistrationRequest` record `toString()` renders plaintext `password`; no reachable leak path today; accepted-with-recommendation) |

### AC-005 — Secure Response  (Spec AC-005)

| Dimension | Reference |
|---|---|
| Story | AC-005 Secure Response (password and hash not returned) |
| Specification | §4 FR-7, FR-8; §7 SEC-3, SEC-4 |
| Resolved decisions | OD-004:A (`201` body = `id, email, role, createdAt` only) |
| API design | api-design §4.2 `CustomerResponse`; openapi.yaml `CustomerResponse` schema (no credential / internal fields) |
| DB design | db-design §7; entity-model §4.2 (`passwordHash` never mapped; `enabled` / `updatedAt` not exposed) |
| Impact analysis | §7, §9 |
| Plan step / files | Steps 7, 8; C-7, C-12 |
| Production code | `model/dto/CustomerResponse` = `record(Long id, String email, String role, OffsetDateTime createdAt)`; `service/CustomerService.toResponse` |
| Tests | `registration.CustomerRegistrationApiTest.successResponseNeverExposesCredentialOrInternalState` (`$.password`, `$.passwordHash`, `$.password_hash`, `$.enabled`, `$.updatedAt` all absent) |
| Impl. Verification | §7 AC-005 VERIFIED; §8 `CustomerResponse` contract match |
| Security Review | §8 sensitive-data exposure (response DTO OK); §20 positive control 4 |
| **Final status** | **RECONCILED** |

### AC-006 — Password Policy Enforcement  (Spec AC-006, derived from Story AC-001 / AC-003)

| Dimension | Reference |
|---|---|
| Story | derived — password policy (Business Value: self-registration with a valid password) |
| Specification | §4 FR-3, FR-6; §6.2 password policy (12–72, upper/lower/digit/special); SC-1; SC-9 (no value echo) |
| Resolved decisions | — (spec-review F-5 / design-review D-6: 72 is BCrypt input **bytes**; plan PD-2) |
| API design | api-design §4.1; design-review D-1 (policy in schema `description` only — tests source from Spec §6.2), D-6 |
| DB design | db-design §10 note 5 (72-byte bound enforced in validation + service, not the column) |
| Impact analysis | §7, §10; risk R-3 |
| Plan step / files | Steps 6, 8; C-4, C-5, C-12; PD-2 |
| Production code | `validation/ValidPassword` (static generic message `"Password does not meet the security policy."`); `validation/PasswordPolicyValidator` (`MIN_BYTES = 12`, `MAX_BYTES = 72` UTF-8, 4 char-class checks); `service/CustomerService.register` re-check via `PasswordPolicyValidator.isCompliant` → `InvalidPasswordException`; `exception/GlobalExceptionHandler.handleInvalidPassword` / `.handleValidation` → `400` |
| Tests | `registration.CustomerRegistrationApiTest` 8 password scenarios incl. `.passwordOver72BytesButUnder72CharsReturns400WithPasswordFieldError` (73-byte / 27-char vector), `.passwordValidationMessageDoesNotEchoTheSubmittedValue`; `validation.PasswordPolicyValidatorTest` (17 tests: 11/12/72/73-byte, multi-byte, each class, no-echo); `service.CustomerServiceTest.serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository` |
| Impl. Verification | §7 AC-006 VERIFIED (byte boundary, dual-layer, safe message) |
| Security Review | §7 (policy matches SC-1 exactly; enforced twice; byte-measured; static non-echoing message — all VERIFIED); R-7 closed; §20 positive control 11 |
| **Final status** | **RECONCILED** |

### AC-007 — Media Type Enforcement  (Spec AC-007, derived from api-conventions.md AC-2)

| Dimension | Reference |
|---|---|
| Story | derived — `api-conventions.md` AC-2 + clarification assumption |
| Specification | §4 FR-2; §8 `415` row |
| Resolved decisions | — |
| API design | api-design §4.3 `415`; openapi.yaml `415` `unsupportedMediaType` example |
| DB design | n/a |
| Impact analysis | §7, §16 |
| Plan step / files | Steps 9, 11; C-13 (`consumes`), C-11 |
| Production code | `controller/CustomerController` `@PostMapping(consumes = APPLICATION_JSON_VALUE)`; `exception/GlobalExceptionHandler.handleMediaType` → `415` |
| Tests | `registration.CustomerRegistrationApiTest.nonJsonContentTypeReturns415`, `.missingContentTypeReturns415` |
| Impl. Verification | §7 AC-007 VERIFIED |
| Security Review | §10 API security (method + content type constrained — VERIFIED) |
| **Final status** | **RECONCILED** (finding RC-2 — implemented `415` `message` string differs verbatim from the OpenAPI illustrative example; AC-6 body **shape** preserved; no behaviour mismatch) |

## 2. Derived rules

| Rule | Spec / design | Production code | Test | Status |
|---|---|---|---|---|
| Unknown / extra JSON property → `400` | Spec §6.3; api-design §3; openapi.yaml `additionalProperties: false` | `spring.jackson.deserialization.fail-on-unknown-properties: true` (`application.yaml` + `application-test.yaml`); `GlobalExceptionHandler.handleUnreadable` (single `HttpMessageNotReadableException` branch, PD-1) | `registration.CustomerRegistrationApiTest.unknownJsonPropertyReturns400` | RECONCILED (RC-2 — single combined message vs two OpenAPI examples; shape preserved) |
| Malformed JSON → `400` | Spec §6.3; api-design §3 | same `HttpMessageNotReadableException` branch | `registration.CustomerRegistrationApiTest.malformedJsonReturns400` | RECONCILED |
| AC-6 error body shape (`timestamp, status, error, message, path`, optional `fieldErrors[]`) | api-conventions.md AC-6; api-design §7 | `model/dto/ErrorResponse` (`@JsonInclude(NON_EMPTY)`), `model/dto/ApiFieldError`; single `@RestControllerAdvice` | `registration.CustomerRegistrationApiTest.errorBodyHasTheApiConventionShape`, `.errorBodyNeverLeaksInternals` | RECONCILED |

## 3. Security posture

| Requirement | Spec | Production code | Test | Status |
|---|---|---|---|---|
| SEC-1 / SC-4 — registration public, everything else deny-by-default | §7 SEC-1 | `security/SecurityConfig` `REGISTRATION_ENDPOINT` matcher `permitAll()` + `anyRequest().authenticated()`; `HttpStatusEntryPoint(401)`; `formLogin`/`httpBasic`/`logout` disabled | `security.RegistrationSecurityPostureTest.registrationEndpointIsReachableWithoutAuthentication`, `.protectedRouteReturns401WhenUnauthenticated` | RECONCILED |
| SEC-7 / OD-002:B — CSRF exemption scoped to the registration POST only | §7 SEC-7; api-design §6 (recorded SC-5 decision) | `SecurityConfig` `.csrf(c -> c.ignoringRequestMatchers(REGISTRATION_ENDPOINT))` — the same single matcher | `security.RegistrationSecurityPostureTest.registrationPostIsAcceptedWithoutACsrfToken` | RECONCILED ("CSRF still enforced elsewhere" is unassertable in US-001 — no second writable endpoint; depth verification owned by SECURITY_REVIEW, done) |
| SEC-9 / SC-6 — H2 console disabled every profile | §7 SEC-9 | `spring.h2.console.enabled: false` in `application.yaml` **and** `application-test.yaml` | `security.RegistrationSecurityPostureTest.h2ConsoleIsNotExposed` | RECONCILED |
| SEC-6 — error responses leak no internals | §7 SEC-6 | `GlobalExceptionHandler` — one static client-safe `message` per branch; `500` fallback | `registration.CustomerRegistrationApiTest.errorBodyNeverLeaksInternals` | RECONCILED |
| SEC-5 — new account `ROLE_CUSTOMER`, `enabled = true` | §7 SEC-5 | `CustomerService` sets `Role.CUSTOMER`, `enabled = true`; `schema.sql` `role`/`enabled` `NOT NULL` | `persistence.CustomerSchemaTest.roleColumnIsNotNull`, `.enabledColumnIsBooleanNotNull`; `CustomerServiceTest` happy path | RECONCILED |
| SEC-10 / SC-8 — `ddl-auto` `validate`, hand-written `schema.sql` | §7 SEC-10 | `ddl-auto: validate` both profiles; `src/main/resources/schema.sql` | context boots clean under `validate` (Impl. Verification §5, §9) | RECONCILED |
| SEC-11 / SC-7 — no secrets introduced | §7 SEC-11 | `application.yaml` `password: ""` is the H2 default (not a secret); no tokens/keys | Security Review §17 | RECONCILED (findings RC-5 / RC-6 — Informational: blank H2 credential + `AUTO_SERVER`; approved config, local-dev only) |

## 4. Persistence / NFR-4 / BR-007

| Requirement | Design | Production code | Test | Status |
|---|---|---|---|---|
| Surrogate `Long id`, `BIGINT` identity | db-design §4.2; entity-model §2 | `Customer.id` `@GeneratedValue(IDENTITY)`; `schema.sql` `id BIGINT GENERATED BY DEFAULT AS IDENTITY`, `pk_customer` | `persistence.CustomerSchemaTest.idColumnIsBigintNotNull` | RECONCILED (finding RC-7 — Informational: `id` DDL has no explicit `NOT NULL`; H2 2.x implicit; `validate` passes) |
| Explicit column length / nullability on every column | db-design §4.2; NFR-4; PC-4 | `Customer` explicit `@Column(...)` per field | `persistence.CustomerSchemaTest` (email 254, password_hash 60, role/enabled/id NOT NULL) | RECONCILED |
| UTC audit timestamps, `created_at` immutable | db-design §4.2; entity-model §3; BR-007; PC-6 | `Customer.createdAt` `@CreatedDate @Column(updatable = false)`, `updatedAt` `@LastModifiedDate`; `config/JpaAuditingConfig` UTC `DateTimeProvider`; `schema.sql` `TIMESTAMP WITH TIME ZONE NOT NULL` | `persistence.CustomerSchemaTest.auditTimestampColumnsAreTimeZoneAwareAndNotNull`; `persistence.CustomerPersistenceTest.auditingPopulatesBothTimestampsInUtc`, `.createdAtIsNotChangedByALaterUpdate` | RECONCILED (R-1 / R-6 closed) |
| `role` persisted as enum name (`EnumType.STRING`) | db-design §4.2; entity-model §2.2 | `Customer.role` `@Enumerated(EnumType.STRING) @Column(length = 20)`; `ck_customer_role` CHECK in DDL | `persistence.CustomerPersistenceTest.roleIsPersistedAsTheEnumName` | RECONCILED |
| Generated `./data/*` git-ignored | db-design §8; impact §6 M-3 | `.gitignore` `/data/` | not tracked, not in tree (Impl. Verification §4) | RECONCILED |

## 5. Regression

| Scenario | Test | Status |
|---|---|---|
| Context still boots with Security + JPA active | `CustomerPortalApplicationTests.contextLoads` (+`@ActiveProfiles("test")`, M-4) | RECONCILED — 1/1 PASS |

## 6. Coverage summary

| | Count |
|---|---|
| Story / Specification Acceptance Criteria (AC-001..AC-007) | 7 |
| RECONCILED | 7 |
| PARTIALLY_RECONCILED / NOT_RECONCILED / BLOCKED | 0 |
| Executable tests (independently reproduced by `implementation-verifier` v1) | 62 across 7 suites, 0 failures / 0 errors / 0 skipped |

Every mandatory Acceptance Criterion has an approved requirement, an
implementation location, executable test evidence, a successful result, an
independent functional-verification record, and an independent security-review
record. No AC depends solely on a mocked test. Findings RC-1..RC-7 are Minor or
Informational and change no AC status.
