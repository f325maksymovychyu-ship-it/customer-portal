---
artifact_type: implementation_plan
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T10:24:32Z
updated_at: 2026-08-31T10:24:32Z
produced_by: implementation-planner
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
supersedes: null
---

# Implementation Plan — US-001 Customer Registration

## Goal

Implement public self-service customer registration exactly as fixed by the
approved artifacts: add one endpoint `POST /api/v1/customers` that validates
`email` + `password` server-side, rejects duplicate emails case-insensitively
with `409`, stores a BCrypt hash, and creates an enabled `CUSTOMER` account with
UTC audit timestamps, returning `201` + `Location` + a credential-free body.

This plan defines what to create/modify and in what order. It introduces no new
business behavior and redesigns nothing upstream. All six Open Decisions are
resolved (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A, OD-006:A; recorded at
`HUMAN_SPEC_APPROVAL` 2026-08-31T07:48:48Z — authoritative over
`open_decisions.md` v1, which still shows the entries as `OPEN`).

## Source Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Story | `docs/stories/US-001-register-customer.md` | (unversioned input) | IN_PROGRESS |
| Open Decisions | `docs/decisions/US-001-open-decisions.md` | 1 | DRAFT (resolutions authoritative, see above) |
| Specification | `docs/specifications/US-001-spec.md` | 1 | APPROVED |
| Specification Review | `docs/reviews/specifications/US-001-spec-review.md` | 1 | APPROVED (PASS) |
| API Design | `docs/designs/api/US-001-api-design.md` | 1 | APPROVED |
| OpenAPI Contract | `docs/designs/api/US-001-openapi.yaml` | 1 | APPROVED |
| DB Design | `docs/designs/database/US-001-db-design.md` | 1 | APPROVED |
| Entity Model | `docs/designs/database/US-001-entity-model.md` | 1 | APPROVED |
| Design Review | `docs/reviews/designs/US-001-design-review.md` | 1 | APPROVED (PASS) |
| Impact Analysis | `docs/impact-analysis/US-001-impact-analysis.md` | 1 | DRAFT (PASS; no review stage follows IMPACT_ANALYSIS) |

Architecture: `architecture.md` (AD-1..AD-8), `package-map.md`,
`api-conventions.md` (AC-1..AC-9), `persistence-conventions.md` (PC-1..PC-9),
`security-conventions.md` (SC-1..SC-9 + policy block). Product:
`business-rules.md` (BR-001..BR-007), `non-functional-requirements.md`
(NFR-001..NFR-008).

## Architectural Changes

No new architecture decision. No new package beyond `package-map.md` (AD-8;
design-review D-9). Greenfield feature slice: every application package is
created for the first time. Layering `controller → service → repository →
model.entity` per AD-2; transactions in the service per AD-3; DTO/entity
boundary per AD-4; validation split per AD-5; single `@RestControllerAdvice` per
AD-6/AC-9; framework config in `config`, security config in `security` per AD-7.

Planning decisions fixed here (previously deferred):

- **PD-1 (resolves R-4 / design-review D-7 / API Q-3) — unknown-JSON-field
  rejection mechanism:** enable Jackson `FAIL_ON_UNKNOWN_PROPERTIES` via
  `spring.jackson.deserialization.fail-on-unknown-properties: true` in
  `application.yaml` (and the test profile). No dedicated `JacksonConfig` class —
  a property is the minimal, convention-compliant mechanism (AD-7 keeps config in
  `application.yml`). Jackson then throws `UnrecognizedPropertyException`, which
  Spring MVC wraps in `HttpMessageNotReadableException` — the same exception it
  raises for malformed JSON. `GlobalExceptionHandler` maps
  `HttpMessageNotReadableException` → `400` once, covering both cases. The
  `RegistrationRequest` DTO does **not** need `@JsonIgnoreProperties`.
- **PD-2 — password-policy length is measured in BYTES (UTF-8), not characters
  (resolves R-3 / design-review D-6 / spec-review F-5):** both the
  `PasswordPolicyValidator` and the service re-check compute
  `password.getBytes(StandardCharsets.UTF_8).length` for the 12..72 bound. The
  four character-class checks operate on the string.
- **PD-3 — entity/DTO mapping stays inside `CustomerService`** as private methods
  (no separate mapper class); AD-4 allows a dedicated mapper but does not require
  one and US-001 has a single trivial mapping each direction. Keeps the class
  count minimal (AD-8).
- **PD-4 — one `SecurityConfig` class** in `security` holds both the
  `SecurityFilterChain` bean and the `PasswordEncoder` (`BCryptPasswordEncoder`)
  bean. `package-map.md` places the encoder bean in `security`; a second class is
  unnecessary.
- **PD-5 — the `201` response `ErrorResponse`/field-error DTO names:** the
  field-error element is named `ApiFieldError` (Java) to avoid a clash with
  `org.springframework.validation.FieldError`; it serializes to the contract's
  `FieldError` schema shape `{ field, message }`. `ErrorResponse` maps to the
  contract `ErrorResponse` (`timestamp, status, error, message, path,
  fieldErrors[]`).
- **PD-6 — H2 timestamp type:** entity fields `createdAt` / `updatedAt` are
  `OffsetDateTime`; columns are `TIMESTAMP WITH TIME ZONE`. If `ddl-auto=validate`
  rejects the exact type token on the project's H2 2.x / Hibernate 7 version,
  IMPLEMENTATION aligns the DDL token (e.g. `TIMESTAMP(6) WITH TIME ZONE`) or the
  Hibernate dialect mapping — the column stays a timezone-aware timestamp (R-1,
  design-review D-5).

## Impact-Analysis Reconciliation

The plan's change surface matches `US-001-impact-analysis.md` §5, §6. Differences
are naming/consolidation choices only, no scope change:

| Impact analysis said | This plan | Why |
|---|---|---|
| `config/JacksonConfig.java` *(only if not handled via DTO/advice)* — MEDIUM confidence | **Not created.** Jackson `FAIL_ON_UNKNOWN_PROPERTIES` set via `application.yaml` property (PD-1). | A property is minimal and convention-compliant; the impact analysis already marked this class conditional. |
| `security/PasswordEncoderConfig.java` *(or a bean in `SecurityConfig`)* | Bean lives in `security/SecurityConfig.java` (PD-4). | One class covers the security wiring; `package-map.md` allows it. |
| Service class "`CustomerService` (or `RegistrationService`)" | `service/CustomerService.java` | Resource-aligned name, matches `CustomerController` / `CustomerResponse`. |
| `model/dto/FieldError.java` | `model/dto/ApiFieldError.java` | Avoids clash with `org.springframework.validation.FieldError` (PD-5); serialized shape unchanged. |
| Possible separate mapper class | Mapping inside `CustomerService` (PD-3) | Single trivial mapping each way; AD-8 reuse-over-duplication. |
| Possible `local` / `test` profile split | One `application-test.yaml`; main `application.yaml` carries the default (file-H2) config. No `local` profile. | Minimal; nothing in the approved artifacts requires a `local` profile. |

All impact-analysis risks R-1..R-8 are carried into this plan's Risks section.

## Files To Create

Base path `src/main/java/org/example/customerportal/`.

| # | Path | Responsibility | Traceability |
|---|---|---|---|
| C-1 | `model/entity/Role.java` | `enum Role { CUSTOMER, ADMIN }` | entity-model §2.2; SC-2, BR-006; design-review D-3 |
| C-2 | `model/entity/Customer.java` | `@Entity` `customer`: `id, email, passwordHash, role, enabled, createdAt, updatedAt`; `@EntityListeners(AuditingEntityListener.class)`; `@Table(uniqueConstraints=@UniqueConstraint(name="uq_customer_email", columnNames="email"))`; explicit `@Column`; `equals`/`hashCode` on `id`; `toString()` excludes `passwordHash` | entity-model §2, §2.1; DB design §4.2; NFR-4, PC-3..PC-6, PC-9; AC-004/AC-005 |
| C-3 | `repository/CustomerRepository.java` | `interface CustomerRepository extends JpaRepository<Customer, Long>` with `boolean existsByEmail(String email)` and `Optional<Customer> findByEmail(String email)` | DB design §5; FR-4; PC-7; AD-2 |
| C-4 | `validation/ValidPassword.java` | Bean Validation constraint annotation (`@Constraint(validatedBy = PasswordPolicyValidator.class)`), default message key (no value echo) | AD-5; Spec §6.2; SC-1; SC-9 |
| C-5 | `validation/PasswordPolicyValidator.java` | `ConstraintValidator<ValidPassword, String>`: null/blank → let `@NotBlank` report; 12..72 **bytes** UTF-8 (PD-2); ≥1 upper / lower / digit / special; static generic message | Spec §6.2; SC-1; design-review D-1/D-6; AC-006 |
| C-6 | `model/request/RegistrationRequest.java` | Inbound DTO `{ email, password }`. `email`: `@NotBlank`, `@Email`, `@Size(max = 254)`. `password`: `@NotBlank`, `@ValidPassword`. Java `record`. No `@JsonIgnoreProperties` (PD-1) | API design §4.1; OpenAPI `RegistrationRequest`; Spec §6.1/§6.2; OD-001:A |
| C-7 | `model/dto/CustomerResponse.java` | `record CustomerResponse(Long id, String email, String role, OffsetDateTime createdAt)` | API design §4.2; OpenAPI `CustomerResponse`; OD-004:A; AC-005 |
| C-8 | `model/dto/ApiFieldError.java` | `record ApiFieldError(String field, String message)` → contract `FieldError` | AC-6; API design §7; FR-3 |
| C-9 | `model/dto/ErrorResponse.java` | `record ErrorResponse(OffsetDateTime timestamp, int status, String error, String message, String path, List<ApiFieldError> fieldErrors)`; `fieldErrors` omitted when null/empty (`@JsonInclude(NON_EMPTY)`) | AC-6; API design §7; FR-3, FR-10 |
| C-10 | `exception/DuplicateEmailException.java` | `RuntimeException` subclass, domain only, no HTTP concept, carries no submitted value | AD-6; FR-4; OD-003:A |
| C-11 | `exception/GlobalExceptionHandler.java` | Single `@RestControllerAdvice`. Maps: `MethodArgumentNotValidException`/`HandlerMethodValidationException` → `400` + `fieldErrors[]`; `HttpMessageNotReadableException` (malformed JSON **and** unknown field, PD-1) → `400`; `HttpMediaTypeNotSupportedException` → `415`; `DuplicateEmailException` → `409` with `"An account with this email already exists."`; fallback `Exception` → `500` with no leak | AD-6; AC-5/AC-6/AC-9; FR-10; API design §4.3/§7; SC-9; AC-002/AC-003/AC-006/AC-007 |
| C-12 | `service/CustomerService.java` | `@Service`. `register(RegistrationRequest)`: normalize email `trim` + `toLowerCase(Locale.ROOT)` (OD-006:A); re-check password policy in **bytes** (PD-2, FR-6) → throw on failure; `existsByEmail(normalized)` → throw `DuplicateEmailException` (OD-003:A); `passwordEncoder.encode(password)`; build `Customer` (`role=CUSTOMER`, `enabled=true`); `save`; map to `CustomerResponse` (private methods, PD-3). `@Transactional` on the write method (AD-3) | FR-3..FR-9; AD-2/AD-3/AD-4; entity-model §3/§4.1; SC-1; AC-001/AC-002/AC-004 |
| C-13 | `controller/CustomerController.java` | `@RestController`, `@RequestMapping("/api/v1/customers")`. `@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)` `register(@Valid @RequestBody RegistrationRequest)` → `201`, `Location` `/api/v1/customers/{id}` via `ServletUriComponentsBuilder`, body `CustomerResponse`. No error handling, no repository access, no entity in signature | FR-1/FR-2/FR-8/FR-10; AC-1..AC-4; AD-2/AD-4; AC-001/AC-007 |
| C-14 | `security/SecurityConfig.java` | `@Configuration`. `SecurityFilterChain`: `authorizeHttpRequests` → `requestMatchers(HttpMethod.POST, "/api/v1/customers").permitAll()`, `anyRequest().authenticated()`; `csrf(c -> c.ignoringRequestMatchers(new AntPathRequestMatcher("/api/v1/customers", "POST")))` (scoped, CSRF stays on elsewhere); disable HTTP Basic/form-login default pages are acceptable (no other endpoint yet). `@Bean PasswordEncoder` → `new BCryptPasswordEncoder()` (PD-4) | SC-1/SC-4/SC-5; OD-002:B; API design §6; SEC-1/SEC-2; R-2 |
| C-15 | `config/JpaAuditingConfig.java` | `@Configuration`, `@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")`; `@Bean DateTimeProvider utcDateTimeProvider` → `() -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC))`. Not on the main application class (AD-7) | PC-6; BR-007; NFR-4; R-6 |
| C-16 | `src/main/resources/schema.sql` | Authoritative `customer` DDL from DB design §8.1 (PK `pk_customer`, `uq_customer_email`, `ck_customer_role` CHECK, `role`/`enabled` DEFAULT). Adjust identity clause ordering / timestamp token for the project's H2 2.x if the build rejects it (PD-6, R-1) | PC-2; SC-8; DB design §8, §8.1 |
| C-17 | `src/test/resources/application-test.yaml` | Profile `test`: isolated in-memory H2 (`jdbc:h2:mem:us001;DB_CLOSE_DELAY=-1`), `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=embedded`, snake_case physical naming, `spring.h2.console.enabled=false`, `spring.jackson.deserialization.fail-on-unknown-properties=true` | PC-1; SC-6; DB design §8; R-5 |

Test sources (skeleton set — `test-writer` owns the definitive suite at
TEST_WRITING; listed here for execution-order planning only), under
`src/test/java/org/example/customerportal/`:

| # | Path | Level | ACs |
|---|---|---|---|
| C-T1 | `validation/PasswordPolicyValidatorTest.java` | unit | AC-006 |
| C-T2 | `model/entity/CustomerPersistenceTest.java` (`@DataJpaTest`, `@ActiveProfiles("test")`) | persistence/integration | AC-004, NFR-4, UTC audit |
| C-T3 | `service/CustomerServiceTest.java` | service (unit / slice) | AC-001, AC-002, AC-004 |
| C-T4 | `controller/CustomerControllerTest.java` (`@WebMvcTest` or full slice) | web-layer | AC-001, AC-003, AC-005, AC-006, AC-007, unknown/malformed JSON |
| C-T5 | `registration/CustomerRegistrationIT.java` (`@SpringBootTest`, `@ActiveProfiles("test")`) | end-to-end integration | AC-001, AC-002 |
| C-T6 | `security/SecurityConfigTest.java` | security | SEC-1 (public path + a second arbitrary path still `401`), CSRF scope |

## Files To Modify

| # | Path | Change | Traceability |
|---|---|---|---|
| M-1 | `build.gradle.kts` | Add `implementation("org.springframework.boot:spring-boot-starter-validation")` (version-managed by the existing Spring Boot 4.1.1 BOM — no explicit version) | AD-5; NFR-002; impact analysis §11; **New Dependencies** below |
| M-2 | `src/main/resources/application.yaml` | Add: `spring.datasource` (H2 file `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`, driver, sa/empty per H2 default); `spring.jpa.hibernate.ddl-auto=validate`; `spring.jpa.properties.hibernate.globally_quoted_identifiers=false`; physical naming = `org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy` (snake_case, PC-5); `spring.sql.init.mode=always`; `spring.h2.console.enabled=false`; `spring.jackson.deserialization.fail-on-unknown-properties=true` (PD-1) | PC-1/PC-2/PC-5; SC-6/SC-8; API design §3; design-review D-7 |
| M-3 | `.gitignore` | Add `/data/` (generated H2 files) | PC-1; SC-7; impact analysis §6 |
| M-4 | `src/test/java/org/example/customerportal/CustomerPortalApplicationTests.java` | Add `@ActiveProfiles("test")` so `contextLoads` boots with the in-memory datasource once Security + JPA auto-config are active | NFR-7; R-5; impact analysis §6 "Files To Reuse" |

### Files To Reuse (no change)

- `src/main/java/.../CustomerPortalApplication.java` — component-scan root; new
  packages sit under it. `@EnableJpaAuditing` goes on `JpaAuditingConfig` (C-15),
  not here (AD-7).
- `build.gradle.kts` Spring Boot BOM / `io.spring.dependency-management` —
  version-manages the new validation starter.

## Execution Order

Each step lists its observable completion criterion. Order follows the
convention chain: contract confirmation → tests → persistence → service → API →
security wiring → validation → build → full verification → doc reconciliation.

1. **Confirm contract & conventions.** Re-read OpenAPI v1, API design v1, DB
   design v1, entity-model v1, design-review v1; confirm OD resolutions and PD-1..PD-6.
   *Evidence:* no open question remains that blocks coding; deviations (if any)
   raised as an Open Decision, not silently coded.
2. **Add the validation dependency (M-1).** *Evidence:* `./gradlew dependencies`
   shows `spring-boot-starter-validation`; project still compiles.
3. **Persistence config + schema (M-2, M-3, C-16, C-17).** Create `schema.sql`,
   wire datasource/JPA/naming in `application.yaml`, add the test profile,
   git-ignore `/data/`. *Evidence:* app context starts with `ddl-auto=validate`
   against `schema.sql` with no entity yet (or defer full validation to step 5);
   `spring.h2.console.enabled=false` confirmed.
4. **Entity + repository + Role enum (C-1, C-2, C-3) and JPA auditing (C-15).**
   *Evidence:* `@DataJpaTest` (C-T2) boots on the `test` profile;
   `ddl-auto=validate` passes — entity mapping agrees with `schema.sql` exactly
   (column names, types, nullability, length, `uq_customer_email`); a persisted
   row has non-null UTC `created_at` / `updated_at`, `created_at` not updatable
   (R-1, R-6 closed here).
5. **Author the test skeletons (C-T1..C-T6).** `test-writer` owns the definitive
   suite; if this plan is executed before TEST_WRITING produces them, create
   failing/red tests mapped to the ACs. *Evidence:* tests compile and fail for
   the right reason (feature not implemented).
6. **Validation constraint (C-4, C-5) + request DTO (C-6).** *Evidence:*
   `PasswordPolicyValidatorTest` (C-T1) passes: 12/72-**byte** boundary, each
   character-class rule, message contains no submitted value (PD-2, SC-9).
7. **Domain exception + DTOs (C-10, C-7, C-8, C-9).** *Evidence:* compiles;
   `CustomerResponse` has no credential field (AC-005).
8. **Service (C-12).** Email normalization, byte-length re-check, `existsByEmail`
   duplicate guard, BCrypt encode, entity build, mapping, `@Transactional`.
   *Evidence:* `CustomerServiceTest` (C-T3) passes: happy path creates a
   `CUSTOMER`, duplicate (any case) throws `DuplicateEmailException`, stored hash
   is BCrypt and verifies against the plaintext, plaintext never assigned to the
   entity.
9. **Controller (C-13).** *Evidence:* `CustomerControllerTest` (C-T4): `201` +
   `Location` + body on valid input; `400` + `fieldErrors[].field="email"` on bad
   email; `400` + `fieldErrors[].field="password"` on policy failure; `415` on
   missing/non-JSON `Content-Type`; unknown field and malformed JSON → `400`.
10. **Security wiring (C-14).** `SecurityFilterChain` + `PasswordEncoder` bean.
    *Evidence:* `SecurityConfigTest` (C-T6): `POST /api/v1/customers` reachable
    unauthenticated; an arbitrary second path (`GET /api/v1/ping` or similar)
    returns `401`; CSRF exemption applies only to the registration path
    (R-2 closed here).
11. **Exception handler (C-11).** Single `@RestControllerAdvice`; controller does
    no error handling. *Evidence:* all error-path assertions from step 9 pass via
    the advice; error bodies match AC-6 and leak nothing (SC-9); one advice
    branch handles malformed JSON + unknown field (PD-1).
12. **Adjust the existing smoke test (M-4).** *Evidence:*
    `CustomerPortalApplicationTests.contextLoads` passes on the `test` profile
    with Security + JPA active (R-5 closed).
13. **Full build & verification.** *Evidence:* `./gradlew clean build` succeeds —
    compilation, all tests green, `ddl-auto=validate` boot clean (NFR-7). No new
    dependency beyond M-1. No file changed outside this plan's scope (Git policy).
14. **Documentation reconciliation prep.** *Evidence:* the implementation report
    (`springboot-implementor`) lists every file created/modified against this
    plan; no architecture/convention doc needed editing (impact analysis §12); if
    a genuinely new architectural need surfaced, it was raised as an Open
    Decision, not coded around (AD-8).

## Validation Strategy

| Step | Validation activity | Pass criterion |
|---|---|---|
| Build | `./gradlew clean build` | BUILD SUCCESSFUL; all tests pass (NFR-7) |
| Schema/entity agreement | `ddl-auto=validate` startup on `test` and default profiles | context starts, no Hibernate schema-validation error (R-1) |
| Password policy | `PasswordPolicyValidatorTest` | byte-length + char-class matrix from Spec §6.2 all correct; no value echo |
| Duplicate email | service + integration tests | `Alice@Example.com` then `alice@example.com` → second is `409`, one row |
| Credential safety | web-layer + persistence tests | no `password`/hash in any response; only `password_hash` column holds a BCrypt string; plaintext never persisted/logged |
| Security posture | `SecurityConfigTest` | only `POST /api/v1/customers` public; CSRF exemption scoped; every other route `401` |
| Error contract | web-layer tests | `400/409/415/500` bodies match AC-6; `fieldErrors[]` for field failures; no internal leak |
| Audit timestamps | `@DataJpaTest` | `created_at`/`updated_at` non-null, UTC offset, `created_at` not updated on a subsequent modify |

MCP: `idea-validation` (Gradle build + diagnostics) and `idea-semantic-analysis`
(layer-dependency check: no `controller → repository`, no entity in a controller
signature) at IMPLEMENTATION_VERIFICATION.

## Testing Strategy (by level, mapped to Acceptance Criteria)

`test-writer` authors the executable tests at TEST_WRITING from Spec §5/§6, the
API design, and the DB design (not from the OpenAPI schema for password rules —
design-review D-1). Predicted coverage:

| Level | Scope | Acceptance Criteria |
|---|---|---|
| Unit — validation | `PasswordPolicyValidator`: length in bytes (11/12/72/73-byte vectors, incl. a multi-byte string that is ≤72 chars but >72 bytes), each missing character class, safe message | AC-006 |
| Unit / slice — service | `CustomerService`: happy path (role `CUSTOMER`, `enabled=true`), email normalization, duplicate detection (same + different case), BCrypt hash verifies, service-layer policy re-check | AC-001, AC-002, AC-004 |
| Persistence — `@DataJpaTest` | column constraints (`email` 254, `password_hash` 60, NOT NULL), `uq_customer_email` collision, `EnumType.STRING` `role`, UTC audit timestamps, `created_at` not updatable, `ddl-auto=validate` agreement | AC-004, NFR-4, BR-007 |
| Web-layer — `@WebMvcTest` / full slice | `201` + `Location` + `CustomerResponse`; `400` + `fieldErrors[email]`; `400` + `fieldErrors[password]`; `415` non-JSON `Content-Type`; unknown JSON field → `400`; malformed JSON → `400`; response body has no credential field | AC-001, AC-003, AC-005, AC-006, AC-007 + derived |
| Security | registration path public; a second arbitrary path `401`; CSRF exemption limited to `POST /api/v1/customers`; BCrypt encoder bean present, no no-op encoder | SEC-1, SEC-2, SC-4, SC-5 |
| End-to-end integration — `@SpringBootTest` | real HTTP → persisted row → hash verifies; duplicate second call → `409`, no second row | AC-001, AC-002 |
| Regression | `CustomerPortalApplicationTests.contextLoads` on the `test` profile with Security + JPA enabled | — |

## Risks

Carried from impact analysis §13; all owned by later stages, none needs a new
human decision.

| # | Severity | Risk | Mitigation in this plan | Owner stage |
|---|---|---|---|---|
| R-1 | Major | `ddl-auto=validate` fails: `OffsetDateTime` ↔ `TIMESTAMP WITH TIME ZONE` strict match, or H2 2.x `GENERATED BY DEFAULT AS IDENTITY` clause ordering | PD-6; step 4 gates on a clean `validate` boot before proceeding; align DDL token / mapping then | IMPLEMENTATION, IMPLEMENTATION_VERIFICATION |
| R-2 | Major | Spring Security 6 filter chain opens more than intended, or CSRF disabled globally | C-14 uses method+path `requestMatchers` + `anyRequest().authenticated()` + scoped `csrf.ignoringRequestMatchers`; C-T6 asserts a second path is still `401` | SECURITY_REVIEW |
| R-3 | Major | 72-byte BCrypt bound implemented as 72 characters | PD-2: both validator and service measure UTF-8 bytes; C-T1 includes a multi-byte boundary vector | TEST_WRITING, IMPLEMENTATION_VERIFICATION |
| R-4 | Minor | Unknown-field vs malformed-JSON `400` wired inconsistently | PD-1: property-driven `FAIL_ON_UNKNOWN_PROPERTIES`; one `HttpMessageNotReadableException` branch in C-11 | IMPLEMENTATION_VERIFICATION |
| R-5 | Minor | `contextLoads` breaks once Security + JPA auto-config is added | M-4 adds `@ActiveProfiles("test")`; C-17 provides the datasource | IMPLEMENTATION |
| R-6 | Minor | JPA auditing writes non-UTC timestamps | C-15 `DateTimeProvider` returns `OffsetDateTime.now(ZoneOffset.UTC)`; persistence test asserts the offset | IMPLEMENTATION |
| R-7 | Minor | Password constraint message echoes the submitted value | C-4/C-5 use a static generic message; validator never includes the value | SECURITY_REVIEW |
| R-8 | Minor | Scope creep — implementing `GET /customers/{id}` or login | Explicitly out of scope (§ Out of Scope of the Spec, design-review D-8); `Location` header target is a future Story | PLAN_REVIEW, RECONCILIATION |

## New Dependencies

| Dependency | Scope | Justification | Approval |
|---|---|---|---|
| `org.springframework.boot:spring-boot-starter-validation` | `implementation` | AD-5 states it is a **required** dependency for `@Valid` request validation; Bean Validation is not currently on the classpath. First-party Spring Boot starter, version-managed by the existing Spring Boot 4.1.1 BOM — not a discretionary third-party library. | Confirm at **HUMAN_PLAN_APPROVAL** |

No other dependency. `spring-boot-starter-data-jpa`, `-security`, `-webmvc`, `h2`,
`lombok`, and the matching test starters are already present.

## Configuration Changes

| File | Change | Profile | Source |
|---|---|---|---|
| `src/main/resources/application.yaml` | H2 file datasource; `spring.jpa.hibernate.ddl-auto=validate`; snake_case physical naming; `spring.sql.init.mode=always`; `spring.h2.console.enabled=false`; `spring.jackson.deserialization.fail-on-unknown-properties=true` | default | PC-1, PC-2, PC-5, SC-6, SC-8, PD-1 |
| `src/test/resources/application-test.yaml` *(new)* | isolated in-memory H2; `ddl-auto=validate`; `spring.sql.init.mode=embedded`; snake_case naming; H2 console off; Jackson fail-on-unknown-properties | `test` | PC-1, SC-6, DB design §8 |
| `.gitignore` | add `/data/` | — | PC-1, SC-7 |

No environment variables, no secrets, no external services, no ports, no JVM
changes (impact analysis §11).

## Open Questions

None blocking. Items for the plan reviewer / approver to confirm:

1. **Dependency `spring-boot-starter-validation`** — confirm at HUMAN_PLAN_APPROVAL (mandated by AD-5; recorded above).
2. **PD-1** — property-based `FAIL_ON_UNKNOWN_PROPERTIES` rather than a `JacksonConfig` class or `@JsonIgnoreProperties(ignoreUnknown = false)`. Acceptable minimal choice; IMPLEMENTATION_VERIFICATION confirms both `400` paths.
3. **PD-3 / PD-4** — no separate mapper class, one `SecurityConfig` holding both beans. Consistent with `package-map.md` and AD-8; flag if the project prefers a class per bean.
4. **Test skeletons (C-T1..C-T6)** are indicative; TEST_WRITING owns the definitive AC→test matrix and may restructure them. This plan does not pre-empt that.

## Traceability

| AC | Spec | Design | Files | Test level |
|---|---|---|---|---|
| AC-001 Successful registration | FR-1/FR-5/FR-8/FR-9, §5 | API §4, OpenAPI `201`; DB §4, entity-model §4.1 | C-13, C-12, C-3, C-2, C-1, C-7, C-16 | web + service + persistence + IT |
| AC-002 Unique email (case-insensitive) | FR-4, §6.1, §8 | API §4.3 `409`; DB §5, `uq_customer_email` | C-12, C-10, C-11, C-3, C-16 | service + IT + persistence |
| AC-003 Email validation | FR-3, §6.1 | OpenAPI `RegistrationRequest.email`; API §4.1 | C-6, C-11 | validation unit + web |
| AC-004 Password storage | FR-5/FR-6/FR-7, §7 | DB §4.2/§7; entity-model §4.1 | C-12, C-14 (encoder), C-2 | service + persistence |
| AC-005 Secure response | FR-7/FR-8, §7 | API §4.2 `CustomerResponse`; OpenAPI schema | C-7, C-12 | web + contract assertion |
| AC-006 Password policy | FR-3/FR-6, §6.2 | API §4.1; design-review D-1/D-6 | C-4, C-5, C-12 | validation unit + web |
| AC-007 Media type | FR-2, §8 | OpenAPI `415`; API §4.3 | C-13 (`consumes`), C-11 | web |
| (derived) unknown/malformed JSON | §6.3 | API §3, OpenAPI `additionalProperties:false` | M-2 (PD-1), C-11 | web |
| NFR-4 / BR-007 explicit mapping, UTC audit | §9 NFR-4 | DB §4, entity-model §2/§3 | C-2, C-15, C-16 | persistence |
| SEC-1/SEC-2 security posture | §7 | API §6 | C-14 | security |

## Result

```yaml
result:
  verdict: PASS
  stage: IMPLEMENTATION_PLANNING
  story: US-001
  artifact_status: DRAFT
  artifacts:
    - docs/plans/US-001-implementation-plan.md
  next_stage: PLAN_REVIEW
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "PD-1 (R-4/D-7/API Q-3): unknown-JSON-field rejection is property-driven (spring.jackson.deserialization.fail-on-unknown-properties=true); no JacksonConfig class; one HttpMessageNotReadableException branch in the advice covers malformed JSON + unknown field."
    - "PD-2 (R-3/D-6/spec-review F-5): password 12..72 bound is measured in UTF-8 BYTES in both the validator and the service re-check; C-T1 needs a multi-byte boundary vector."
    - "PD-6 (R-1/D-5): createdAt/updatedAt are OffsetDateTime / TIMESTAMP WITH TIME ZONE; if ddl-auto=validate rejects the type token on the project's H2 2.x, IMPLEMENTATION aligns the DDL token or dialect mapping at execution-order step 4."
    - "New dependency spring-boot-starter-validation (M-1) must be confirmed at HUMAN_PLAN_APPROVAL; mandated by AD-5, BOM-version-managed, first-party."
    - "Impact-analysis reconciliation: JacksonConfig not created (PD-1); PasswordEncoder bean in SecurityConfig (PD-4); mapping inside CustomerService (PD-3); model.dto FieldError renamed ApiFieldError to avoid the org.springframework.validation.FieldError clash (PD-5). No scope change."
    - "R-2: C-14 scopes both the public matcher and the CSRF exemption to POST /api/v1/customers; C-T6 asserts a second arbitrary path is still 401. SECURITY_REVIEW verifies."
    - "R-5: M-4 adds @ActiveProfiles(\"test\") to CustomerPortalApplicationTests so contextLoads still boots with Security + JPA active."
    - "Test skeletons C-T1..C-T6 are indicative; test-writer owns the definitive AC->test matrix at TEST_WRITING."
    - "open_decisions.md v1 still shows OD-001..OD-006 as OPEN; authoritative resolutions (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A, OD-006:A) are from history.jsonl HUMAN_SPEC_APPROVAL. Documentation lag owned by us-clarifier."
```
