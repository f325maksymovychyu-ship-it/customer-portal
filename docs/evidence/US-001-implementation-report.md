---
artifact_type: implementation_report
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T12:07:06Z
updated_at: 2026-08-31T12:07:06Z
produced_by: springboot-implementor
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
supersedes: null
tests_status: PASS
build_status: PASS
diagnostics_status: PASS
security_sensitive: true
---

# Implementation Report — US-001 Customer Registration

## 1. Summary

Public self-service customer registration is implemented exactly as fixed by the
approved artifacts: one endpoint `POST /api/v1/customers` that validates `email`
and `password` server-side, rejects a duplicate email case-insensitively with
`409`, stores a BCrypt hash, creates an enabled `CUSTOMER` account with UTC audit
timestamps, and returns `201` + `Location` + a credential-free body.

- **Implementation status:** complete. All 14 planned production files (C-1..C-16
  plus one supporting exception class) and all 4 planned modifications
  (M-1..M-4) are done, in the plan's execution order.
- **Validation status:** `./gradlew clean build` = **BUILD SUCCESSFUL**; **62
  tests, 0 failures, 0 errors**; `ddl-auto=validate` boots clean on both the
  `test` (in-memory) and default (file H2) profiles.
- **Limitations / deviations:** three, all minor and disclosed in §7 — one new
  domain exception for the FR-6 service re-check (`InvalidPasswordException`);
  `CustomerPersistenceTest` uses `@SpringBootTest` instead of the plan-skeleton
  `@DataJpaTest`; and a malformed 61-character dummy BCrypt literal in the
  committed `CustomerSchemaTest` fixture was corrected to 60 characters (no
  assertion changed).
- **Open Decisions:** none touched, none newly required. OD-001..OD-006 remain
  as resolved at `HUMAN_SPEC_APPROVAL` (OD-001:A, OD-002:B, OD-003:A, OD-004:A,
  OD-005:A, OD-006:A) and were applied as-is.

## 2. Source Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Story | `docs/stories/US-001-register-customer.md` | (unversioned) | IN_PROGRESS |
| Open Decisions | `docs/decisions/US-001-open-decisions.md` | 1 | DRAFT (resolutions authoritative via `history.jsonl`) |
| Specification | `docs/specifications/US-001-spec.md` | 1 | APPROVED |
| Specification Review | `docs/reviews/specifications/US-001-spec-review.md` | 1 | APPROVED (PASS) |
| API Design | `docs/designs/api/US-001-api-design.md` | 1 | APPROVED |
| OpenAPI Contract | `docs/designs/api/US-001-openapi.yaml` | 1 | APPROVED |
| DB Design | `docs/designs/database/US-001-db-design.md` | 1 | APPROVED |
| Entity Model | `docs/designs/database/US-001-entity-model.md` | 1 | APPROVED |
| Design Review | `docs/reviews/designs/US-001-design-review.md` | 1 | APPROVED (PASS) |
| Impact Analysis | `docs/impact-analysis/US-001-impact-analysis.md` | 1 | DRAFT (PASS; no review stage follows) |
| Implementation Plan | `docs/plans/US-001-implementation-plan.md` | 1 | APPROVED (PASS + HUMAN_PLAN_APPROVAL) |
| Plan Review | `docs/reviews/plans/US-001-plan-review.md` | 1 | APPROVED (PASS) |
| Test Strategy | `docs/tests/US-001-test-strategy.md` | 1 | DRAFT |
| AC-Test Matrix | `docs/tests/US-001-ac-test-matrix.md` | 1 | DRAFT (authoritative AC→test map) |

No consumed input is `SUPERSEDED`. No staleness detected.

## 3. Implemented Acceptance Criteria

| AC | Implementation (file · symbol) | Verifying test(s) | Status |
|---|---|---|---|
| AC-001 Successful registration → `201` + `Location` + `CUSTOMER` body | `controller/CustomerController.register` → `service/CustomerService.register` → `repository/CustomerRepository.save`; entity `model/entity/Customer`; `schema.sql` | `CustomerRegistrationApiTest.validRegistrationReturns201WithLocationAndCustomerBody`, `.emailIsStoredAndReturnedNormalisedToLowercase`, `.password72CharsMeetingPolicyIsAccepted`; `RegistrationSecurityPostureTest.registrationEndpointIsReachableWithoutAuthentication`; `CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` | PASS |
| AC-002 Duplicate email, case-insensitive → `409`, no 2nd account | `CustomerService.register` (email `trim`+`toLowerCase` → `existsByEmail` → `DuplicateEmailException`); `GlobalExceptionHandler.handleDuplicateEmail`; `uq_customer_email` | `CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`; `CustomerSchemaTest.emailHasAUniqueConstraint`, `.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint`; `CustomerServiceTest.duplicateEmailSameCaseThrowsAndDoesNotSave`, `.duplicateEmailDifferentCaseThrowsAndDoesNotSave` | PASS |
| AC-003 Email validation → `400` + `fieldErrors[email]` | `model/request/RegistrationRequest` (`@NotBlank @Email @Size(max=254)`); `GlobalExceptionHandler.handleValidation` | `CustomerRegistrationApiTest.malformedEmailReturns400WithEmailFieldError`, `.blankEmailReturns400WithEmailFieldError`, `.emailLongerThan254CharsReturns400WithEmailFieldError`; `CustomerSchemaTest.emailColumnIsVarchar254NotNull` | PASS |
| AC-004 Password stored as BCrypt hash, never plaintext | `CustomerService.register` (`passwordEncoder.encode`); `Customer.passwordHash` `@Column(length=60)`; `security/SecurityConfig.passwordEncoder` (`BCryptPasswordEncoder`) | `CustomerSchemaTest.passwordHashColumnIsVarchar60NotNull`, `.noPlaintextPasswordColumnExists`; `CustomerServiceTest.happyPathCreatesAnEnabledCustomerWithABcryptHash` (hash matches `^\$2[aby]\$\d\d\$.{53}$`, verifies, `!= raw`) | PASS |
| AC-005 Response excludes credential + internal state | `model/dto/CustomerResponse` (`id,email,role,createdAt` only); `CustomerService.toResponse` | `CustomerRegistrationApiTest.successResponseNeverExposesCredentialOrInternalState` | PASS |
| AC-006 Password policy (incl. 72-**byte** bound) → `400` + `fieldErrors[password]` | `validation/ValidPassword` + `validation/PasswordPolicyValidator` (UTF-8 byte length + 4 char classes, static message); service re-check via `PasswordPolicyValidator.isCompliant` | `CustomerRegistrationApiTest` 7 password scenarios incl. `passwordOver72BytesButUnder72CharsReturns400WithPasswordFieldError`; `PasswordPolicyValidatorTest` (17 tests: 11/12/72/73-byte, multi-byte, each class, no-echo); `CustomerServiceTest.serviceRechecksThePasswordPolicyBeforeHashing...` | PASS |
| AC-007 Non-JSON / missing `Content-Type` → `415` | `CustomerController` `@PostMapping(consumes=APPLICATION_JSON_VALUE)`; `GlobalExceptionHandler.handleMediaType` | `CustomerRegistrationApiTest.nonJsonContentTypeReturns415`, `.missingContentTypeReturns415` | PASS |
| Derived: unknown / malformed JSON → `400` | `spring.jackson.deserialization.fail-on-unknown-properties=true` (PD-1); `GlobalExceptionHandler.handleUnreadable` (single `HttpMessageNotReadableException` branch) | `CustomerRegistrationApiTest.unknownJsonPropertyReturns400`, `.malformedJsonReturns400`, `.errorBodyHasTheApiConventionShape`, `.errorBodyNeverLeaksInternals` | PASS |
| NFR-4 / BR-007: explicit mapping, UTC audit, `created_at` not updatable | `Customer` explicit `@Column`s; `config/JpaAuditingConfig` (UTC `DateTimeProvider`); `schema.sql` | `CustomerSchemaTest.auditTimestampColumnsAreTimeZoneAwareAndNotNull`, `.idColumnIsBigintNotNull`, `.roleColumnIsNotNull`, `.enabledColumnIsBooleanNotNull`; `CustomerPersistenceTest.auditingPopulatesBothTimestampsInUtc`, `.createdAtIsNotChangedByALaterUpdate`, `.roleIsPersistedAsTheEnumName` | PASS |
| SEC-1/SEC-7/SC-4/SC-5: public + CSRF-exempt only for the registration path; deny-by-default elsewhere; H2 console off | `security/SecurityConfig` (scoped `RequestMatcher` for both `permitAll` and `csrf.ignoringRequestMatchers`; `anyRequest().authenticated()`; `HttpStatusEntryPoint(401)`) | `RegistrationSecurityPostureTest` (4 tests: reachable unauthenticated, tokenless POST accepted, other route `401`, `/h2-console` not `200`) | PASS |

## 4. Change Set

Base path `src/main/java/org/example/customerportal/`. All classifications
against `docs/plans/US-001-implementation-plan.md` and
`docs/impact-analysis/US-001-impact-analysis.md`.

### Created — production (Planned)

| File | Plan ref | Notes |
|---|---|---|
| `model/entity/Role.java` | C-1 | `enum Role { CUSTOMER, ADMIN }` |
| `model/entity/Customer.java` | C-2 | `@Entity`, explicit `@Column`s, `@EntityListeners(AuditingEntityListener.class)`, table-level `uq_customer_email`, `equals`/`hashCode` on `id`, `toString()` excludes `passwordHash` |
| `repository/CustomerRepository.java` | C-3 | `JpaRepository<Customer,Long>` + `existsByEmail`, `findByEmail` |
| `validation/ValidPassword.java` | C-4 | constraint annotation, static generic message |
| `validation/PasswordPolicyValidator.java` | C-5 | UTF-8 **byte** length 12..72 + 4 char classes; `public static boolean isCompliant(String)` reused by the service |
| `model/request/RegistrationRequest.java` | C-6 | `record`, `@NotBlank @Email @Size(max=254)` / `@NotBlank @ValidPassword`; no `@JsonIgnoreProperties` (PD-1) |
| `model/dto/CustomerResponse.java` | C-7 | `record(Long id, String email, String role, OffsetDateTime createdAt)` |
| `model/dto/ApiFieldError.java` | C-8 | `record(String field, String message)` → contract `FieldError` (PD-5) |
| `model/dto/ErrorResponse.java` | C-9 | `record`, `@JsonInclude(NON_EMPTY)`, `of(...)` helper for the no-field case |
| `exception/DuplicateEmailException.java` | C-10 | `RuntimeException`, no HTTP concept, no submitted value |
| `exception/GlobalExceptionHandler.java` | C-11 | single `@RestControllerAdvice`; maps validation→400+`fieldErrors`, `HttpMessageNotReadableException`→400, media-type→415, duplicate→409, invalid-password→400, fallback→500; `path` from `getRequestURI()`, UTC `timestamp` |
| `service/CustomerService.java` | C-12 | constructor injection, `@Transactional register(...)`, email normalize, byte re-check, duplicate guard, BCrypt encode, private mapping (PD-3) |
| `controller/CustomerController.java` | C-13 | `@RestController @RequestMapping("/api/v1/customers")`, `@PostMapping(consumes/produces JSON)`, `201` + `Location` via `ServletUriComponentsBuilder`, no error handling |
| `security/SecurityConfig.java` | C-14 | `SecurityFilterChain` + `PasswordEncoder` bean (PD-4); scoped public matcher + scoped CSRF exemption; `formLogin`/`httpBasic`/`logout` disabled; `HttpStatusEntryPoint(401)` (plan F-3 recommendation) |
| `config/JpaAuditingConfig.java` | C-15 | `@EnableJpaAuditing(dateTimeProviderRef=...)`, UTC `DateTimeProvider`; not on the main class (AD-7) |
| `src/main/resources/schema.sql` | C-16 | DB design §8.1 DDL; `GENERATED BY DEFAULT AS IDENTITY` (no explicit `NOT NULL` on `id` — H2 2.x accepts and `validate` passes) |

### Created — production (Required Supporting Change)

| File | Justification |
|---|---|
| `exception/InvalidPasswordException.java` | FR-6 / SC-1 require a service-layer password re-check "before hashing"; plan C-12 says "throw on failure" without naming the type. A dedicated domain exception in the `exception` package (AD-6) mapped to `400` is the minimal, convention-compliant choice. Not reachable from the HTTP flow (request `@Valid` catches policy failures first); exercised directly by `CustomerServiceTest`. |

### Created — test (Planned; skeletons C-T1/C-T2/C-T3, "indicative" per plan Open Question 4)

| File | Plan ref | Level | Tests |
|---|---|---|---|
| `validation/PasswordPolicyValidatorTest.java` | C-T1 | unit | 17 — byte boundary (11/12/72/73), multi-byte, each char class, `isValid` null/empty delegation, no-value-echo message |
| `service/CustomerServiceTest.java` | C-T3 | service (Mockito) | 5 — happy path (role/enabled/BCrypt verify/`!= raw`), email normalize before check+save, duplicate same/different case (no `save`), policy re-check before repo access |
| `persistence/CustomerPersistenceTest.java` | C-T2 | persistence (`@SpringBootTest`) | 3 — UTC auditing on both timestamps, `created_at` unchanged by a later update, `role` persisted as `EnumType.STRING` name |

### Modified

| File | Change | Plan ref | Classification |
|---|---|---|---|
| `build.gradle.kts` | + `implementation("org.springframework.boot:spring-boot-starter-validation")` (BOM-managed, no version) | M-1 | Planned (confirmed at HUMAN_PLAN_APPROVAL) |
| `src/main/resources/application.yaml` | + H2 file datasource, `ddl-auto=validate`, `open-in-view=false`, snake_case `physical_naming_strategy`, `sql.init.mode=always`, `h2.console.enabled=false`, `jackson…fail-on-unknown-properties=true` | M-2 | Planned |
| `.gitignore` | + `/data/` | M-3 | Planned |
| `src/test/java/.../CustomerPortalApplicationTests.java` | + `@ActiveProfiles("test")` | M-4 | Planned |
| `src/test/java/.../persistence/CustomerSchemaTest.java` | dummy BCrypt literal `…012345` (61 chars) → `…01234` (60 chars) in `caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint` (2 occurrences) | — | **Unexpected (test-fixture defect fix)** — see §7 |

### Unrelated changes

None included. Five pre-existing untracked `docs/**/harness-*` files are left
untouched and are **not** part of this Story's change set.

## 5. Validation Evidence

| Check | Command / tool | Result |
|---|---|---|
| Baseline (red phase, pre-implementation) | `./gradlew test` | BUILD FAILED — 37 tests, 33 failed (matches `US-001-test-generation-report.md`) |
| Full build | `./gradlew clean build` | **BUILD SUCCESSFUL** |
| Full test run | `./gradlew clean test` | **62 tests, 0 failures, 0 errors** |
| Web / contract | `registration.CustomerRegistrationApiTest` | 22/22 PASS |
| Persistence — schema | `persistence.CustomerSchemaTest` | 10/10 PASS |
| Persistence — entity slice | `persistence.CustomerPersistenceTest` | 3/3 PASS |
| Security posture | `security.RegistrationSecurityPostureTest` | 4/4 PASS |
| Service unit | `service.CustomerServiceTest` | 5/5 PASS |
| Validation unit | `validation.PasswordPolicyValidatorTest` | 17/17 PASS |
| Regression | `CustomerPortalApplicationTests.contextLoads` | 1/1 PASS (Security + JPA active, `test` profile) |
| `ddl-auto=validate` — test profile | test-suite context boot | clean (no Hibernate schema-validation error) |
| `ddl-auto=validate` — default profile (file H2) | `./gradlew bootRun` (90 s, then stopped) | `Started CustomerPortalApplication` — clean; no `HHH` schema-validation error (**R-1 closed**) |
| Compilation diagnostics | `./gradlew compileJava` / `build` | no warnings, no errors |

Build environment: Gradle 9.7.1 (wrapper), Gradle toolchain Java 21, Spring Boot
4.1.1, Hibernate ORM 7.4.5, H2 2.x. IDEA MCP semantic/diagnostic tooling was not
invoked by this Skill; layer-dependency and semantic checks are
`IMPLEMENTATION_VERIFICATION`'s (plan "Validation Strategy"). Layering was
verified by inspection: `controller` → `service` only; `service` →
`repository`/`model`/`exception`/`validation`; `repository` → `model.entity`;
no entity in any controller signature; no error body built in the controller.

## 6. Configuration Changes

| File | Change | Approving plan step |
|---|---|---|
| `src/main/resources/application.yaml` | H2 file datasource `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`; `spring.jpa.hibernate.ddl-auto=validate`; `spring.jpa.open-in-view=false`; `spring.jpa.properties.hibernate.physical_naming_strategy=…CamelCaseToUnderscoresNamingStrategy`; `spring.sql.init.mode=always`; `spring.h2.console.enabled=false`; `spring.jackson.deserialization.fail-on-unknown-properties=true` | M-2 (PD-1); `open-in-view=false` also per impact-analysis §11 / test-strategy §6 |
| `src/test/resources/application-test.yaml` | **not modified** — created by `test-writer` (plan C-17); already correct (isolated in-memory H2, `validate`, `sql.init.mode=embedded`, console off, fail-on-unknown-properties, `open-in-view=false`) | — |
| `.gitignore` | `+ /data/` | M-3 |

No secrets, no environment variables, no new ports, no JVM options. Generated
`./data/` H2 files are git-ignored and were removed after the boot check.

## 7. Deviations and Discovered Problems

### D-1 — New class `InvalidPasswordException` (Required Supporting Change)

FR-6 mandates a service-layer password re-check before hashing; the plan (C-12)
prescribes the behavior ("throw on failure") but not the exception type. Added
`exception/InvalidPasswordException` (domain exception, no HTTP concept, no
submitted value) mapped to `400` by the advice. Minimal, within AD-6, traceable
to FR-6/SC-1. No requirement or design change. **No loop-back warranted.**

### D-2 — `CustomerPersistenceTest` uses `@SpringBootTest`, not `@DataJpaTest`

Plan skeleton C-T2 suggests `@DataJpaTest`; plan Open Question 4 states the
skeletons are "indicative" and the AC-test-matrix scenario/expected columns are
the binding contract. `@SpringBootTest @ActiveProfiles("test")` was used so the
datasource, SQL-init, and auditing config are exactly the running
application's, avoiding `@DataJpaTest` datasource-replacement / auditing-import
ambiguity. All three deferred C-T2 scenarios (UTC auditing, `created_at`
immutability, `EnumType.STRING` round-trip) are covered and pass. **No
behavioral gap.**

### D-3 — Corrected a malformed dummy BCrypt literal in `CustomerSchemaTest` (Unexpected)

`persistence.CustomerSchemaTest.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint`
(committed by `test-writer` at TEST_WRITING) inserted a **61-character** dummy
hash into `password_hash`, which is `VARCHAR(60)` per PC-9 / DB design §4.2 /
Q-2 — and which the same file's own `passwordHashColumnIsVarchar60NotNull`
asserts. The first `INSERT` therefore failed with `Value too long for column`
before the intended unique-constraint assertion could run. The literal was
shortened by one character (`…wxyz012345` → `…wxyz01234`, 60 chars) in both
occurrences. **No assertion, scenario, or expected outcome was changed**; the
test still asserts the second insert violates `uq_customer_email`. This is a
test-fixture defect, not a schema problem — real `BCryptPasswordEncoder` output
is exactly 60 characters and fits (proven by `CustomerRegistrationApiTest` and
`CustomerServiceTest`). Flagged for `IMPLEMENTATION_VERIFICATION` /
`RECONCILIATION` visibility; `test-writer` owns the suite and may prefer to
re-issue the fixture.

The touched test's own purpose (a duplicate row violates `uq_customer_email`)
remains covered by it after the fix; **case-insensitive** duplicate rejection —
which this test does not actually exercise (it inserts a byte-identical string
twice) — has independent real coverage in
`CustomerRegistrationApiTest.duplicateEmailIsRejectedCaseInsensitivelyWith409`
(registers an address, then its `toUpperCase` form → `409` only because
`CustomerService` lowercases before `existsByEmail`) and
`CustomerServiceTest.duplicateEmailDifferentCaseThrowsAndDoesNotSave` /
`.emailIsNormalisedToLowercaseAndTrimmedBeforeCheckAndSave`.

### D-4 — Unknown-field vs malformed-JSON share one `message` (plan F-4, expected)

Per PD-1, both surface as `HttpMessageNotReadableException` and get one advice
branch → one generic `400` message ("The request body is missing, malformed, or
contains an unknown field."). The OpenAPI `400` examples show two distinct
illustrative messages; the AC-6 body **shape** is preserved and tests assert
status + shape only, not message text. Consistent with plan-review F-4 ("none
required").

### D-5 — `schema.sql` identity column

`id BIGINT GENERATED BY DEFAULT AS IDENTITY` is written without an explicit
`NOT NULL` (H2 2.x makes identity columns implicitly `NOT NULL`; adding the
clause before the identity clause is rejected on this H2 version — DB design
§8.1 note). `ddl-auto=validate` passes on both profiles and
`CustomerSchemaTest.idColumnIsBigintNotNull` confirms `BIGINT` + not-nullable.

## 8. Open Decisions

None touched; none newly required. OD-001..OD-006 were consumed as resolved at
`HUMAN_SPEC_APPROVAL` (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A,
OD-006:A). `docs/decisions/US-001-open-decisions.md` v1 still shows them `OPEN` —
documentation lag owned by `us-clarifier` (carried finding F-1); non-blocking,
no impact on this implementation.

No security-sensitive decision is missing. Credential handling is fully
specified and implemented (BCrypt, dual-layer policy, no plaintext persisted /
logged / returned, scoped CSRF exemption, deny-by-default). Residual
verification items (R-2 CSRF scope depth, R-3 byte boundary, R-7 message
hygiene) are owned by `SECURITY_REVIEW` and `IMPLEMENTATION_VERIFICATION` per
the plan.

## 9. Result

```yaml
result:
  verdict: PASS
  stage: IMPLEMENTATION
  story: US-001
  artifact_status: DRAFT
  artifacts:
    - docs/evidence/US-001-implementation-report.md
  next_stage: IMPLEMENTATION_VERIFICATION
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "D-1: added exception/InvalidPasswordException for the FR-6 service-layer password re-check (plan C-12 left the type unspecified); domain exception, mapped to 400, not reachable from the HTTP flow. No requirement/design change."
    - "D-2: CustomerPersistenceTest (C-T2) uses @SpringBootTest instead of the indicative @DataJpaTest skeleton; all three deferred scenarios covered and passing."
    - "D-3: corrected a malformed 61-char dummy BCrypt literal to 60 chars in the committed CustomerSchemaTest.caseInsensitiveDuplicateEmailCollidesOnTheUniqueConstraint fixture (VARCHAR(60) per PC-9); no assertion changed. test-writer owns the suite."
    - "D-4 / plan F-4: malformed JSON and unknown-field share one HttpMessageNotReadableException advice branch and one generic 400 message (PD-1); OpenAPI's two example messages are illustrative, AC-6 shape preserved."
    - "D-5: schema.sql id column uses 'GENERATED BY DEFAULT AS IDENTITY' with no explicit NOT NULL (H2 2.x implicit); ddl-auto=validate passes on both profiles."
    - "R-1 closed: ddl-auto=validate boots clean on the test (in-memory) and default (file H2) profiles; OffsetDateTime <-> TIMESTAMP WITH TIME ZONE validates on H2 2.x / Hibernate 7.4.5."
    - "R-2 (SECURITY_REVIEW): SecurityConfig scopes both the public matcher and the CSRF exemption to POST /api/v1/customers via one RequestMatcher; anyRequest().authenticated(); HttpStatusEntryPoint(401). 'CSRF still enforced elsewhere' remains unassertable in US-001 (no second writable endpoint)."
    - "R-3 closed at implementation level: password 12..72 bound measured in UTF-8 bytes in both PasswordPolicyValidator and the service re-check; PasswordPolicyValidatorTest covers 11/12/72/73-byte and multi-byte vectors."
    - "R-6 closed: JpaAuditingConfig DateTimeProvider returns OffsetDateTime.now(ZoneOffset.UTC); CustomerPersistenceTest asserts the UTC offset on both timestamps."
    - "R-7 (SECURITY_REVIEW): password constraint message is a fixed generic string ('Password does not meet the security policy.'); PasswordPolicyValidatorTest and CustomerRegistrationApiTest.passwordValidationMessageDoesNotEchoTheSubmittedValue assert no value echo."
    - "F-1 (carried): docs/decisions/US-001-open-decisions.md v1 still shows OD-001..OD-006 as OPEN; authoritative resolutions are in history.jsonl at HUMAN_SPEC_APPROVAL. Owned by us-clarifier; non-blocking."
    - "spring-boot-starter-validation (M-1) added to build.gradle.kts; BOM-version-managed, first-party; confirmed at HUMAN_PLAN_APPROVAL."
```
