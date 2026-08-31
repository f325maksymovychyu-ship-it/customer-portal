---
artifact_type: implementation_verification
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T12:20:00Z
updated_at: 2026-08-31T12:20:00Z
produced_by: implementation-verifier
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
supersedes: null
build_status: PASS
tests_status: PASS
acceptance_criteria_verified: 7
acceptance_criteria_total: 7
critical_findings: 0
major_findings: 0
minor_findings: 5
semantic_analysis: TEXT_FALLBACK
---

# Implementation Verification — US-001 Customer Registration

## 1. Executive Summary

**Verdict: PASS.** The implementation of US-001 is functionally correct, complete,
traceable, architecturally compliant, and ready for Security Review.

- **Build:** `./gradlew clean build` independently reproduced — **BUILD SUCCESSFUL**
  (exit 0).
- **Tests:** independently reproduced — **62 tests, 0 failures, 0 errors, 0 skipped**
  across 7 suites.
- **Acceptance Criteria:** 7 of 7 (AC-001..AC-007) **VERIFIED** with executable
  test evidence asserting observable behavior, plus supporting service/persistence
  evidence.
- **Critical risks:** none. **Major findings:** none. **Minor findings:** 5 (one
  implementer-touched test — confirmed benign; illustrative-vs-implemented error
  message strings; a concurrent-duplicate robustness edge; the `RegistrationRequest`
  record `toString()` observation; the carried open-decisions documentation lag).
  The last three are forwarded to Security Review; none requires a code change for
  this Story.
- **Recommended next action:** advance to `SECURITY_REVIEW`.

## 2. Verified Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Story | docs/stories/US-001-register-customer.md | (unversioned) | active |
| Specification | docs/specifications/US-001-spec.md | 1 | APPROVED |
| Specification Review | docs/reviews/specifications/US-001-spec-review.md | 1 | APPROVED (PASS) |
| API Design | docs/designs/api/US-001-api-design.md | 1 | APPROVED |
| OpenAPI Contract | docs/designs/api/US-001-openapi.yaml | 1 | APPROVED |
| DB Design | docs/designs/database/US-001-db-design.md | 1 | APPROVED |
| Entity Model | docs/designs/database/US-001-entity-model.md | 1 | APPROVED |
| Design Review | docs/reviews/designs/US-001-design-review.md | 1 | APPROVED (PASS) |
| Impact Analysis | docs/impact-analysis/US-001-impact-analysis.md | 1 | DRAFT (PASS; no review stage follows) |
| Implementation Plan | docs/plans/US-001-implementation-plan.md | 1 | APPROVED (PASS + HUMAN_PLAN_APPROVAL) |
| Plan Review | docs/reviews/plans/US-001-plan-review.md | 1 | APPROVED (PASS) |
| Test Strategy | docs/tests/US-001-test-strategy.md | 1 | DRAFT |
| AC–Test Matrix | docs/tests/US-001-ac-test-matrix.md | 1 | DRAFT (authoritative AC→test map) |
| Implementation Report | docs/evidence/US-001-implementation-report.md | 1 | DRAFT |

No consumed input is `SUPERSEDED`. No staleness detected: every downstream
artifact references v1 of its upstreams; all design/spec/plan reviews are `PASS`;
both human gates (`HUMAN_SPEC_APPROVAL` 2026-08-31T07:48:48Z, `HUMAN_PLAN_APPROVAL`
2026-08-31T11:20:00Z) are recorded in `history.jsonl`.

## 3. Environment

- JDK: Gradle toolchain Java 21 (build/test); Spring Boot 4.1.1; Hibernate ORM
  7.4.5; H2 2.x; Gradle 9.7.1 (wrapper).
- Build tool: `./gradlew` (Kotlin DSL).
- Active profile under test: `test` (`@ActiveProfiles("test")` on every
  Spring-context test; `src/test/resources/application-test.yaml`).
- Database mode: isolated in-memory H2 for tests
  (`jdbc:h2:mem:us001;DB_CLOSE_DELAY=-1`); file H2 for the default profile
  (`jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`).
- Verification tooling: built-in file inspection, `git`, Gradle. IDEA MCP
  semantic tooling not invoked — architecture checks are **text/inspection
  fallback** (`semantic_analysis: TEXT_FALLBACK`); layering conclusions are based
  on import graphs and method signatures, not PSI call analysis.
- No secrets encountered or recorded.

## 4. Repository State

- Branch: `main` (8 commits ahead of `origin/main`). Implementation committed as
  `fb833b0 "Stage IMPLEMENTATION completed"`; tests as `f6e7eae "Stage
  TEST_WRITING completed"`.
- Working tree: **clean** for all `src/**` and `docs/**` workflow artifacts.
- Untracked (pre-existing, NOT part of this Story, left untouched):
  `docs/evidence/harness-dryrun.py`,
  `docs/evidence/harness-migration-baseline.md`,
  `docs/evidence/harness-migration-report.md`,
  `docs/evidence/harness-post-migration-review.md`,
  `docs/harness-consistency-review.md`.
- Deleted files: none.
- Generated runtime artifacts: `./data/` (git-ignored via `.gitignore` `+ /data/`);
  not present in the working tree, not committed.

## 5. Build Evidence

| Check | Command | Result |
|---|---|---|
| Full build | `./gradlew clean build` | **BUILD SUCCESSFUL in 13s**, exit 0 |
| Compilation | `:compileJava` / `:compileTestJava` | no errors, no warnings |
| `:test` + `:check` | (part of `build`) | pass |

Only non-functional output: a Gradle 10 deprecation notice (Gradle-internal, not
project code). No `HHH*` Hibernate schema-validation error at context startup on
either profile.

## 6. Test Evidence

Independently reproduced via `./gradlew clean build`; per-suite counts read from
`build/test-results/test/*.xml`.

| Suite | Tests | Fail | Err | Skip |
|---|---|---|---|---|
| `registration.CustomerRegistrationApiTest` | 22 | 0 | 0 | 0 |
| `validation.PasswordPolicyValidatorTest` | 17 | 0 | 0 | 0 |
| `persistence.CustomerSchemaTest` | 10 | 0 | 0 | 0 |
| `service.CustomerServiceTest` | 5 | 0 | 0 | 0 |
| `security.RegistrationSecurityPostureTest` | 4 | 0 | 0 | 0 |
| `persistence.CustomerPersistenceTest` | 3 | 0 | 0 | 0 |
| `CustomerPortalApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **62** | **0** | **0** | **0** |

No test is `@Disabled`; no test is skipped. The implementation report's baseline
(red phase: 37 tests / 33 failed) is consistent with the test-generation report
and the pre-implementation matrix.

## 7. Acceptance Criteria Matrix

| AC | Required behavior | Implementation | Test evidence | Status |
|---|---|---|---|---|
| **AC-001** | Valid email + compliant password → account created (role `CUSTOMER`, verifiable later), `201` + `Location` | `CustomerController.register` → `CustomerService.register` (BCrypt encode, `Role.CUSTOMER`, `enabled=true`) → `CustomerRepository.save`; `Customer` entity; `schema.sql` | `CustomerRegistrationApiTest.validRegistrationReturns201WithLocationAndCustomerBody`, `.emailIsStoredAndReturnedNormalisedToLowercase`, `.password72CharsMeetingPolicyIsAccepted`; `RegistrationSecurityPostureTest.registrationEndpointIsReachableWithoutAuthentication`; `CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` (asserts `BCryptPasswordEncoder.matches(raw, storedHash)`, `role == CUSTOMER`) | VERIFIED |
| **AC-002** | Duplicate email (any case) → rejected `409`, no 2nd account | `CustomerService` normalizes (`trim().toLowerCase(ROOT)`) → `existsByEmail` → `DuplicateEmailException`; `GlobalExceptionHandler.handleDuplicateEmail` → `409`; `uq_customer_email` backstop | `CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409` (registers, then upper-case form → `409` + exact message); `CustomerServiceTest.duplicateEmailSameCaseThrowsAndDoesNotSave`, `.duplicateEmailDifferentCaseThrowsAndDoesNotSave` (`save` never called); `CustomerSchemaTest.emailHasAUniqueConstraint`, `.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint` | VERIFIED |
| **AC-003** | Invalid/blank/over-length email → `400` + `fieldErrors[email]`, no account | `RegistrationRequest` `@NotBlank @Email @Size(max=254)`; `GlobalExceptionHandler.handleValidation` | `CustomerRegistrationApiTest.malformedEmailReturns400WithEmailFieldError`, `.blankEmailReturns400WithEmailFieldError`, `.emailLongerThan254CharsReturns400WithEmailFieldError`; `CustomerSchemaTest.emailColumnIsVarchar254NotNull` | VERIFIED |
| **AC-004** | Password persisted only as BCrypt hash, never plaintext | `CustomerService` `passwordEncoder.encode`; `Customer.passwordHash` `@Column(length=60)`; `SecurityConfig` `BCryptPasswordEncoder`; `schema.sql` `password_hash VARCHAR(60) NOT NULL`, no plaintext column | `CustomerSchemaTest.passwordHashColumnIsVarchar60NotNull`, `.noPlaintextPasswordColumnExists`; `CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` (hash matches `^\$2[aby]\$\d\d\$.{53}$`, verifies, `!= raw`) | VERIFIED |
| **AC-005** | Response contains neither password nor hash (nor `enabled`/`updatedAt`) | `CustomerResponse` record = `id, email, role, createdAt` only; `CustomerService.toResponse` | `CustomerRegistrationApiTest.successResponseNeverExposesCredentialOrInternalState` (`$.password`, `$.passwordHash`, `$.password_hash`, `$.enabled`, `$.updatedAt` all absent) | VERIFIED |
| **AC-006** | Password policy incl. 72-**byte** bound → `400` + `fieldErrors[password]`, no account; service re-check | `ValidPassword` + `PasswordPolicyValidator` (UTF-8 byte length 12..72 + 4 char classes, static generic message); `CustomerService` re-check via `PasswordPolicyValidator.isCompliant` before hashing → `InvalidPasswordException` | `CustomerRegistrationApiTest` 8 password scenarios incl. `passwordOver72BytesButUnder72CharsReturns400WithPasswordFieldError` (73-byte / 27-char vector) and `passwordValidationMessageDoesNotEchoTheSubmittedValue`; `PasswordPolicyValidatorTest` (17: 11/12/72/73-byte, multi-byte, each class, no-echo); `CustomerServiceTest.serviceRechecksThePasswordPolicyBeforeHashingOrTouchingTheRepository` | VERIFIED |
| **AC-007** | Missing / non-JSON `Content-Type` → `415`, no account | `CustomerController` `@PostMapping(consumes=APPLICATION_JSON_VALUE)`; `GlobalExceptionHandler.handleMediaType` → `415` | `CustomerRegistrationApiTest.nonJsonContentTypeReturns415`, `.missingContentTypeReturns415` | VERIFIED |

Derived rules also verified: unknown JSON property → `400`
(`spring.jackson.deserialization.fail-on-unknown-properties=true` +
`GlobalExceptionHandler.handleUnreadable`); malformed JSON → `400`; AC-6 error
body shape (`timestamp, status, error, message, path`, `path == "/api/v1/customers"`);
no internal leakage in error `message`
(`CustomerRegistrationApiTest.unknownJsonPropertyReturns400`, `.malformedJsonReturns400`,
`.errorBodyHasTheApiConventionShape`, `.errorBodyNeverLeaksInternals`).

## 8. API Contract Verification

Compared implementation with `docs/designs/api/US-001-openapi.yaml` v1.

| Contract element | Implementation | Match |
|---|---|---|
| `POST /api/v1/customers`, `security: []`, public | `@RequestMapping("/api/v1/customers")` + `@PostMapping`; `SecurityConfig` `permitAll` for `POST /api/v1/customers` | ✅ |
| Request `application/json` only; `415` otherwise | `consumes=APPLICATION_JSON_VALUE` | ✅ |
| `RegistrationRequest` = `{email, password}`, `additionalProperties:false` | `record RegistrationRequest(email, password)` + `fail-on-unknown-properties=true` | ✅ |
| `email` maxLength 254, `@Email` semantics | `@Size(max=254) @Email` | ✅ |
| `password` writeOnly, 12–72, custom policy, service re-check | `@ValidPassword` (byte-measured) + service `isCompliant` re-check | ✅ (contract states char max; 72 enforced as bytes per spec-review F-5 / DB Q-4 — expected) |
| `201` + `Location: /api/v1/customers/{id}` + `CustomerResponse` | `ResponseEntity.created(location).body(created)`; `Location` via `ServletUriComponentsBuilder` | ✅ |
| `CustomerResponse` = `id, email, role, createdAt`; role enum `[CUSTOMER]` | `record CustomerResponse(Long id, String email, String role, OffsetDateTime createdAt)`; service writes `role.name()` = `"CUSTOMER"` | ✅ |
| `400` / `409` / `415` / `500` `ErrorResponse` shape | `GlobalExceptionHandler` — one branch per status; `ErrorResponse` matches AC-6 | ✅ |
| `fieldErrors[]` present only for field validation | `@JsonInclude(NON_EMPTY)`; `ErrorResponse.of(...)` omits it for non-field errors | ✅ |

**Deviation (Minor, MF-2):** the concrete `message` strings for `409` / `415` /
malformed-JSON differ verbatim from the OpenAPI `examples` (e.g. implemented
`"Content-Type must be application/json."` vs. contract example
`"Content-Type 'text/plain' is not supported."`; implemented single
missing/malformed/unknown-field message vs. two contract examples). The OpenAPI
`examples` are illustrative, not normative; the `409` message
(`"An account with this email already exists."`) does match exactly, and every
test asserts status + body **shape**, not free-text. Consistent with disclosed
deviation D-4 / plan-review F-4. No contract-behavior mismatch.

## 9. Persistence Verification

| Design element (DB design §4.2 / §8.1) | Implementation | Runtime evidence | Match |
|---|---|---|---|
| `customer` table, hand-written `schema.sql`, `ddl-auto=validate` | `src/main/resources/schema.sql`; `application.yaml` + `application-test.yaml` `ddl-auto: validate` | context boots clean on both profiles; `CustomerSchemaTest.customerTableExists` | ✅ |
| `id BIGINT` identity, PK | `@Id @GeneratedValue(IDENTITY) @Column(name="id", nullable=false)`; `GENERATED BY DEFAULT AS IDENTITY` | `CustomerSchemaTest.idColumnIsBigintNotNull` (BIGINT, not nullable) | ✅ |
| `email VARCHAR(254) NOT NULL`, `uq_customer_email` UNIQUE | `@Column(length=254, nullable=false)`; `@Table(uniqueConstraints=@UniqueConstraint(name="uq_customer_email", columnNames="email"))` | `CustomerSchemaTest.emailColumnIsVarchar254NotNull`, `.emailHasAUniqueConstraint` | ✅ |
| `password_hash VARCHAR(60) NOT NULL` (PC-9) | `@Column(name="password_hash", length=60, nullable=false)` | `CustomerSchemaTest.passwordHashColumnIsVarchar60NotNull` | ✅ |
| `role VARCHAR(20) NOT NULL`, `EnumType.STRING`, CHECK | `@Enumerated(STRING) @Column(length=20, nullable=false)`; `ck_customer_role` in DDL | `CustomerSchemaTest.roleColumnIsNotNull`; `CustomerPersistenceTest.roleIsPersistedAsTheEnumName` (stored `"CUSTOMER"`, reload → `Role.CUSTOMER`) | ✅ |
| `enabled BOOLEAN NOT NULL` | `@Column(nullable=false) boolean enabled` | `CustomerSchemaTest.enabledColumnIsBooleanNotNull` | ✅ |
| `created_at` / `updated_at` `TIMESTAMP WITH TIME ZONE NOT NULL`, UTC, `created_at` not updatable | `OffsetDateTime` + `@CreatedDate` / `@LastModifiedDate`; `created_at` `updatable=false`; `JpaAuditingConfig` UTC `DateTimeProvider` | `CustomerSchemaTest.auditTimestampColumnsAreTimeZoneAwareAndNotNull`; `CustomerPersistenceTest.auditingPopulatesBothTimestampsInUtc` (offset `== ZoneOffset.UTC`), `.createdAtIsNotChangedByALaterUpdate` | ✅ |
| Case-insensitive uniqueness via service lowercasing + plain UNIQUE (OD-006:A) | `CustomerService.normalizeEmail` before check and insert; plain `existsByEmail` | `CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`; `CustomerServiceTest.emailIsNormalisedToLowercaseAndTrimmedBeforeCheckAndSave` | ✅ |
| Generated `./data/*` git-ignored | `.gitignore` `+ /data/` | not committed / not in tree | ✅ |
| `OffsetDateTime` ↔ `TIMESTAMP WITH TIME ZONE` under `validate` (R-1 trip point) | maps clean on H2 2.x / Hibernate 7.4.5 | no `HHH` validation error at startup | ✅ R-1 CLOSED |

Runtime schema evidence comes from H2 `INFORMATION_SCHEMA` assertions in
`CustomerSchemaTest` (an actual booted datasource), not from annotations alone.

## 10. Architecture Verification

Method: import-graph + signature inspection (`semantic_analysis: TEXT_FALLBACK`).

| Rule | Evidence | Status |
|---|---|---|
| Controller → Service only | `CustomerController` imports `CustomerService` only; no repository, no entity import | ✅ |
| Service owns business logic | `CustomerService` — normalize, policy re-check, duplicate guard, encode, map | ✅ |
| Repository owns persistence access | `CustomerRepository extends JpaRepository`; query methods only, no logic | ✅ |
| Controller does not touch Repository | no `repository` import/reference in `controller/` | ✅ |
| No entity in an API signature | `register(...)` takes `RegistrationRequest`, returns `ResponseEntity<CustomerResponse>`; `Customer` never on a controller signature | ✅ |
| No HTTP types in Service | `CustomerService` imports no `org.springframework.http` / servlet types | ✅ |
| Entity not exposed as API model | `Customer` used only by service/repository; DTOs are records in `model/dto` / `model/request` | ✅ |
| Single `@RestControllerAdvice` | exactly one — `exception/GlobalExceptionHandler`; controller builds no error body | ✅ |
| Package structure per `package-map.md` | `controller`, `service`, `repository`, `model/{entity,dto,request}`, `exception`, `validation`, `security`, `config` | ✅ |
| No new architectural layer | none introduced | ✅ |
| Auditing config off the main class (AD-7) | `JpaAuditingConfig` separate `@Configuration` | ✅ |

No architecture violation found. (Confidence: high for imports/signatures;
call-path direction not cross-checked with PSI — see Limitations.)

## 11. Validation and Error Handling

- Request validation is active at runtime: `@Valid @RequestBody` on the
  controller method drives `MethodArgumentNotValidException` →
  `handleValidation` → `400` + `fieldErrors[]` (proven by the AC-003 / AC-006
  web tests, which exercise the real MVC stack).
- Custom `@ValidPassword` constraint is wired (`@Constraint(validatedBy=
  PasswordPolicyValidator.class)`) and fires at the request layer;
  `spring-boot-starter-validation` added (M-1).
- `null`/empty password is delegated to `@NotBlank`
  (`PasswordPolicyValidator.isValid` returns `true` for null/empty); the service
  gate treats null/empty as non-compliant (`isCompliant` returns `false`).
- Uniqueness conflict → `409` mapped in the single advice.
- Media-type failure → `415`; malformed/unknown JSON → `400`; unmapped → `500`
  with a generic message.
- Negative-path evidence exists for every rejection class (not only happy path).
- `path` is taken from `HttpServletRequest.getRequestURI()`; `timestamp` is
  `OffsetDateTime.now(ZoneOffset.UTC)`.

## 12. Basic Security Readiness

| Check | Evidence | Result |
|---|---|---|
| No plaintext password persisted | no plaintext column (`schema.sql`, `CustomerSchemaTest.noPlaintextPasswordColumnExists`); entity has only `passwordHash` | PASS |
| Password hash never returned | `CustomerResponse` excludes it; `successResponseNeverExposesCredentialOrInternalState` | PASS |
| Password not logged | `grep -rn "Logger\|log\.\|System\.out\|println" src/main` → **no matches**: production code contains no logger, no `System.out`, no `println` at all. `Customer.toString()` hand-excludes `passwordHash`. `GlobalExceptionHandler` never references the request DTO — only `HttpServletRequest` and `getFieldErrors()`. | PASS (see MF-5) |
| BCrypt encoder, no no-op | `SecurityConfig.passwordEncoder` = `BCryptPasswordEncoder`; `CustomerServiceTest` asserts a genuine `$2a$`-format verifying hash | PASS |
| Public scope minimal | `SecurityConfig` `REGISTRATION_ENDPOINT` matcher = exactly `POST /api/v1/customers`; `anyRequest().authenticated()` | PASS |
| CSRF exemption scoped | same single matcher passed to `csrf.ignoringRequestMatchers(...)`; CSRF otherwise enabled | PASS (depth arbitration is SECURITY_REVIEW's — R-2) |
| Deny-by-default elsewhere | `RegistrationSecurityPostureTest.protectedRouteReturns401WhenUnauthenticated` (401, not a form-login 302); `HttpStatusEntryPoint(401)`; `formLogin`/`httpBasic`/`logout` disabled | PASS |
| H2 console not exposed | `spring.h2.console.enabled=false` in both `application.yaml` and `application-test.yaml`; `RegistrationSecurityPostureTest.h2ConsoleIsNotExposed` | PASS |
| No secrets | datasource password is empty string (local H2 dev only); no tokens/keys added | PASS |
| Validation message hygiene | `@ValidPassword` message is the fixed string `"Password does not meet the security policy."`; `PasswordPolicyValidatorTest.defaultMessageNeverEchoesASubmittedValue`, `CustomerRegistrationApiTest.passwordValidationMessageDoesNotEchoTheSubmittedValue` | PASS |

**Forwarded to SECURITY_REVIEW:** (a) final arbitration of CSRF-scope depth /
posture (risk R-2 — the implementation adopted the plan F-3 recommendation:
`HttpStatusEntryPoint(401)`, scoped matcher for both `permitAll` and the CSRF
exemption); (b) message-hygiene sign-off (R-7); (c) the concurrent-duplicate
edge in MF-3 below (a `DataIntegrityViolationException` from `uq_customer_email`
under a race currently maps to `500`, not `409`); (d) the `RegistrationRequest`
record `toString()` observation in MF-5 (auto-generated `toString()` includes the
plaintext `password` component; no reachable leak path found, but SEC-3 says the
plaintext is "never logged" and the project hand-wrote `Customer.toString()` to
exclude `passwordHash`).

## 13. Configuration Verification

| Setting | Value | Plan alignment |
|---|---|---|
| `spring.datasource.url` (default) | `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE` | M-2 / DB design §8 — file H2, not silently in-memory ✅ |
| `spring.datasource.url` (test) | `jdbc:h2:mem:us001;DB_CLOSE_DELAY=-1` | test-writer C-17 — isolated in-memory ✅ |
| `spring.jpa.hibernate.ddl-auto` | `validate` (both profiles) | SC-8 / SEC-10 / PC-2 ✅ |
| `spring.jpa.open-in-view` | `false` (both) | impact-analysis §11 / test-strategy §6 ✅ |
| `spring.sql.init.mode` | `always` (default) / `embedded` (test) | DB design §8 ✅ |
| `spring.h2.console.enabled` | `false` (both) | SC-6 / SEC-9 ✅ |
| `physical_naming_strategy` | `CamelCaseToUnderscoresNamingStrategy` | DB design §8.1 ✅ |
| `spring.jackson.deserialization.fail-on-unknown-properties` | `true` (both) | PD-1 ✅ |
| `.gitignore` | `+ /data/` | M-3 ✅ |

No undocumented configuration change. No secrets, no new ports, no JVM options.

## 14. Test Quality Review

- Every mandatory AC (AC-001..AC-007) has at least one web- or persistence-level
  test that exercises the real stack; no AC depends solely on a mock.
- Negative scenarios are thorough: 3 email-rejection vectors, 8 password vectors
  (incl. the multi-byte 73-byte boundary), media-type, malformed JSON, unknown
  field, duplicate.
- Persistence assertions hit H2 `INFORMATION_SCHEMA` and real
  round-trips (`CustomerPersistenceTest`), not annotation reflection.
- Sensitive-data non-exposure is asserted positively
  (`successResponseNeverExposesCredentialOrInternalState`,
  `noPlaintextPasswordColumnExists`, message no-echo).
- `CustomerServiceTest` uses a **real** `BCryptPasswordEncoder` (not a mock), so
  "a genuine hash is produced and verifies" is actually checked.
- Tests use randomized (`System.nanoTime()`) emails and do not rely on execution
  order.
- No false-positive assertions spotted; no test mocks away the behavior it
  claims to verify. Security tests use the real `SecurityFilterChain`
  (`@SpringBootTest` + `@AutoConfigureMockMvc`), not `@WebMvcTest` with security
  stubbed.

Minor observations (not findings): (1) `CustomerSchemaTest` /
`CustomerPersistenceTest` share one JVM-lifetime in-memory DB
(`DB_CLOSE_DELAY=-1`); `CustomerSchemaTest.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint`
inserts two non-transactional rows that persist for the session — harmless here
because all other tests use unique emails, but a latent isolation smell.
(2) `CustomerPersistenceTest` uses `@SpringBootTest` rather than the plan's
indicative `@DataJpaTest` (disclosed deviation D-2) — acceptable; all three
C-T2 scenarios are covered.

## 15. Scope Verification

Change set vs. `docs/plans/US-001-implementation-plan.md` +
`docs/impact-analysis/US-001-impact-analysis.md`:

| Class | Files | Status |
|---|---|---|
| Planned production (C-1..C-16) | `model/entity/{Role,Customer}`, `repository/CustomerRepository`, `validation/{ValidPassword,PasswordPolicyValidator}`, `model/request/RegistrationRequest`, `model/dto/{CustomerResponse,ApiFieldError,ErrorResponse}`, `exception/{DuplicateEmailException,GlobalExceptionHandler}`, `service/CustomerService`, `controller/CustomerController`, `security/SecurityConfig`, `config/JpaAuditingConfig`, `resources/schema.sql` | all present, all match plan |
| Required supporting | `exception/InvalidPasswordException` (D-1) | justified — FR-6 re-check needs a domain exception type the plan left unspecified (C-12); in `exception` package (AD-6), mapped to `400`; not reachable from the HTTP flow (request `@Valid` catches policy failures first); exercised by `CustomerServiceTest` |
| Planned modifications | `build.gradle.kts` (+validation starter, M-1), `application.yaml` (M-2), `.gitignore` (M-3), `CustomerPortalApplicationTests` (+`@ActiveProfiles("test")`, M-4) | all match plan |
| Planned test skeletons | `PasswordPolicyValidatorTest` (C-T1), `CustomerServiceTest` (C-T3), `CustomerPersistenceTest` (C-T2) | all present, scenarios match the AC-test matrix |
| Unexpected | `CustomerSchemaTest.java` — 2-line fixture fix (D-3, see MF-1) | benign, confirmed |
| Unrelated | none | — |

`git diff f6e7eae..fb833b0 -- src/test/` confirms the **only** change to a
test-writer-owned test is the D-3 BCrypt literal correction; all other test
changes are the planned new files and M-4.

## 16. Implementation Report Accuracy

The Implementation Report is **materially consistent with observed evidence**.

- Build / test claims (BUILD SUCCESSFUL, 62 tests / 0 failures) — independently
  reproduced, exact.
- All 14+1 production files and M-1..M-4 — confirmed present and as described.
- Deviations D-1..D-5 are all disclosed with accurate detail; D-3 is correctly
  flagged as implementer-modified for this stage to confirm (done — MF-1).
- Layering claim ("verified by inspection") — independently re-confirmed here.
- `front-matter status: DRAFT` on the report is expected at this stage.

No omitted files, no incorrect status claims, no undisclosed security-sensitive
change.

## 17. Findings

All five findings are **Minor** — none blocks progression to Security Review;
none requires a code change for this Story.

### MF-1 — Implementer modified a test-writer-owned test (`CustomerSchemaTest`)

- **Severity:** Minor. **Category:** test ownership / process.
- **Evidence:** `git diff f6e7eae..fb833b0 -- .../persistence/CustomerSchemaTest.java`
  shows a 61-character dummy BCrypt literal shortened to 60 characters
  (`…wxyz012345` → `…wxyz01234`, 2 occurrences) in
  `caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint`.
- **Assessment:** benign and arguably a correctness improvement. `password_hash`
  is `VARCHAR(60)` (PC-9, asserted by the same file's
  `passwordHashColumnIsVarchar60NotNull`); the pre-fix fixture's first `INSERT`
  failed with `Value too long for column` before the intended
  unique-constraint assertion could run, so the test was a false RED. Post-fix it
  genuinely asserts the second insert violates `uq_customer_email`. No assertion,
  scenario, or expected outcome changed. Real `BCryptPasswordEncoder` output is
  exactly 60 chars. Case-insensitive duplicate rejection has independent coverage
  (`CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`,
  `CustomerServiceTest.duplicateEmailDifferentCaseThrowsAndDoesNotSave`).
- **Required correction:** none for correctness. `test-writer` owns the suite and
  may re-issue the fixture for provenance; `RECONCILIATION` to note the
  ownership crossing.
- **Loop-back target:** none.

### MF-2 — Error `message` strings differ from OpenAPI illustrative examples

- **Severity:** Minor. **Category:** contract documentation.
- **Evidence:** implemented `415` message `"Content-Type must be application/json."`
  vs. contract example `"Content-Type 'text/plain' is not supported."`; one
  combined missing/malformed/unknown-field `400` message vs. two contract
  examples (`unknownField`, `malformedJson`).
- **Assessment:** the OpenAPI `examples` are explicitly illustrative; the `409`
  message matches exactly; AC-6 body **shape** is preserved; all tests assert
  status + shape. Consistent with disclosed D-4 / plan-review F-4.
- **Required correction:** none required. Optionally, `openapi-designer` could
  align the `400`/`415` examples with the single implemented messages in a future
  revision.
- **Loop-back target:** none.

### MF-3 — Concurrent duplicate registration maps to `500`, not `409`

- **Severity:** Minor. **Category:** robustness / error mapping (edge).
- **Evidence:** `CustomerService.register` guards duplicates with a
  check-then-act (`existsByEmail` → `save`). Two simultaneous requests for the
  same new email can both pass the check; the second `save` then trips
  `uq_customer_email` and throws `DataIntegrityViolationException`, which is not
  handled in `GlobalExceptionHandler` and falls through to `handleUnexpected` →
  `500`.
- **Assessment:** not a violation of any approved artifact — no AC, test, design,
  or plan step covers concurrency, and anti-abuse/rate-limiting is explicitly out
  of scope (OD-005). The single-request duplicate path (the AC-002 requirement)
  is correct. This is a latent robustness gap, not a defect against requirements.
- **Required correction:** none for this Story. `SECURITY_REVIEW` /
  `RECONCILIATION` to decide whether a follow-up Story should map
  `DataIntegrityViolationException` on `uq_customer_email` to `409`.
- **Loop-back target:** none.

### MF-5 — `RegistrationRequest` record `toString()` exposes the plaintext password

- **Severity:** Minor. **Category:** basic security readiness (SEC-3) — forwarded.
- **Evidence:** `RegistrationRequest` is a `record`, so its compiler-generated
  `toString()` renders every component including `password`. Contrast
  `Customer.toString()`, hand-written specifically to exclude `passwordHash` —
  the project treats credential-in-`toString()` as a real concern.
- **Assessment:** no reachable leak path found. Production code has **no logger
  and no `System.out`/`println`** (`grep` over `src/main` → no matches);
  `GlobalExceptionHandler` reads only `HttpServletRequest` +
  `BindingResult.getFieldErrors()` (field name + default message), never the
  bound DTO; `CustomerController` and `CustomerService` never call
  `request.toString()` or log the request. Spring's default MVC logging does not
  emit request bodies at `INFO`. So SEC-3 ("never logged") holds **today**.
- **Required correction:** none for this Story. `SECURITY_REVIEW` to decide
  whether to require a custom `toString()` (or `@ToString.Exclude`-equivalent) on
  `RegistrationRequest` as a defensive measure consistent with the entity's
  treatment, and to confirm no debug-level logging config could surface it.
- **Loop-back target:** none.

### MF-4 — `open_decisions.md` still shows OD-001..OD-006 as OPEN (carried F-1)

- **Severity:** Minor. **Category:** documentation lag.
- **Evidence:** `docs/decisions/US-001-open-decisions.md` v1 lists all six
  decisions `OPEN`; the authoritative resolutions (OD-001:A, OD-002:B, OD-003:A,
  OD-004:A, OD-005:A, OD-006:A) are recorded in `history.jsonl` at
  `HUMAN_SPEC_APPROVAL` and are correctly applied throughout the implementation.
- **Assessment:** no impact on the implementation, which uses the resolved
  values. Owned by `us-clarifier` (should publish v2).
- **Required correction:** `us-clarifier` to publish `open_decisions.md` v2.
- **Loop-back target:** none.

## 18. Verification Limitations

- **Semantic analysis:** IDEA MCP not invoked; architecture conclusions rest on
  import graphs and method signatures (`TEXT_FALLBACK`), not PSI call-path
  analysis. Layering is unambiguous from imports here, so confidence is high, but
  a call-graph cross-check was not performed.
- **Concurrency:** MF-3 is reasoned from code, not reproduced with a concurrent
  test.
- **Regression scope:** the full project suite is small (62 tests) and was run in
  full; no broader regression surface exists in this training project.
- **Runtime (default profile):** the file-H2 boot / `ddl-auto=validate` result is
  taken from the Implementation Report's `bootRun` evidence plus the test-profile
  context boot; not re-run here (the test profile exercises the same
  `schema.sql` + entity mapping under `validate`).
- **Manual checks still open for humans:** diff review and PR approval
  (`HUMAN_PR_APPROVAL`); adversarial security review (`SECURITY_REVIEW`).

## 19. Verdict Rationale

Build passes (independently reproduced). All 62 tests pass with observed
evidence (independently reproduced). Every Acceptance Criterion AC-001..AC-007 is
`VERIFIED` against observable behavior with executable tests, backed by
service- and persistence-level evidence. The API contract, persistence schema,
architecture layering, validation wiring, configuration, and basic security
posture all match the approved artifacts. The change set is exactly the planned
scope plus one justified supporting exception class and one benign
implementer-made test-fixture correction; no unrelated changes. All five findings
are Minor and none requires a code change for this Story. Upstream artifacts are
correct and current, so no `BLOCKED` condition applies.

Verdict: **PASS** → advance to `SECURITY_REVIEW`.

```yaml
result:
  verdict: PASS
  stage: IMPLEMENTATION_VERIFICATION
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/verification/US-001-implementation-verification.md
  next_stage: SECURITY_REVIEW
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "MF-1: implementer corrected a malformed 61-char dummy BCrypt literal to 60 chars (2 occurrences) in test-writer-owned CustomerSchemaTest.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint (VARCHAR(60) per PC-9). git diff confirms this is the ONLY change to a test-writer test; no assertion/scenario/outcome changed; pre-fix the test was a false RED (first INSERT errored before the constraint assertion). test-writer owns the suite; RECONCILIATION to note the ownership crossing."
    - "MF-2: implemented error message strings for 400 (missing/malformed/unknown-field, one combined message) and 415 differ verbatim from the OpenAPI illustrative examples; the 409 message matches exactly, AC-6 body shape preserved, tests assert status+shape only. Consistent with disclosed D-4 / plan-review F-4. Optional: openapi-designer aligns examples in a future revision."
    - "MF-3 (SECURITY_REVIEW / RECONCILIATION to weigh): CustomerService.register uses check-then-act (existsByEmail then save); a concurrent duplicate would trip uq_customer_email as DataIntegrityViolationException, which is unmapped and falls through to 500 rather than 409. No AC/test/design covers concurrency; anti-abuse is out of scope (OD-005). Latent robustness gap, not a requirements defect; candidate for a follow-up Story."
    - "MF-5 (SECURITY_REVIEW to weigh): RegistrationRequest is a record, so its generated toString() includes the plaintext password component (Customer.toString() was hand-written to exclude passwordHash). No reachable leak path found — production code has NO logger / System.out / println (grep over src/main = no matches); GlobalExceptionHandler never touches the bound DTO. SEC-3 ('never logged') holds today; a defensive custom toString() is a SECURITY_REVIEW call."
    - "MF-4 (carried F-1): docs/decisions/US-001-open-decisions.md v1 still shows OD-001..OD-006 OPEN; authoritative resolutions (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A, OD-006:A) are in history.jsonl at HUMAN_SPEC_APPROVAL and applied correctly. Owned by us-clarifier (publish v2). No implementation impact."
    - "Independently reproduced: ./gradlew clean build = BUILD SUCCESSFUL (exit 0); 62 tests, 0 failures, 0 errors, 0 skipped across 7 suites."
    - "R-1 CLOSED (verified): OffsetDateTime <-> TIMESTAMP WITH TIME ZONE validates under ddl-auto=validate on H2 2.x / Hibernate 7.4.5; context boots clean on the test profile with the hand-written schema.sql."
    - "R-2 (SECURITY_REVIEW owns): SecurityConfig scopes both permitAll and the CSRF exemption to one RequestMatcher for POST /api/v1/customers; anyRequest().authenticated(); HttpStatusEntryPoint(401); formLogin/httpBasic/logout disabled. RegistrationSecurityPostureTest (4/4) green."
    - "R-3 CLOSED (verified): password 12..72 bound measured in UTF-8 bytes in both PasswordPolicyValidator and the service re-check; PasswordPolicyValidatorTest + CustomerRegistrationApiTest cover 11/12/72/73-byte and the multi-byte 73-byte vector."
    - "R-6 CLOSED (verified): JpaAuditingConfig DateTimeProvider returns OffsetDateTime.now(ZoneOffset.UTC); CustomerPersistenceTest asserts the UTC offset on created_at and updated_at and created_at immutability."
    - "R-7 (SECURITY_REVIEW owns): @ValidPassword message is the fixed string 'Password does not meet the security policy.'; no-echo asserted by PasswordPolicyValidatorTest.defaultMessageNeverEchoesASubmittedValue and CustomerRegistrationApiTest.passwordValidationMessageDoesNotEchoTheSubmittedValue."
    - "D-1 accepted: exception/InvalidPasswordException is a justified required supporting change (FR-6 service re-check; plan C-12 left the type unspecified); domain exception in the exception package (AD-6), mapped to 400, not reachable from the HTTP flow."
    - "D-2 accepted: CustomerPersistenceTest uses @SpringBootTest instead of the indicative @DataJpaTest; all three C-T2 scenarios covered and passing."
    - "Test-isolation smell (not a finding): CustomerSchemaTest and CustomerPersistenceTest share one JVM-lifetime in-memory H2 (DB_CLOSE_DELAY=-1); CustomerSchemaTest inserts two non-transactional rows that outlive the test. Harmless because all other tests use randomized emails."
```
