---
artifact_type: reconciliation
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
reconciled_acceptance_criteria: 7
total_acceptance_criteria: 7
critical_findings: 0
major_findings: 0
minor_findings: 5
informational_findings: 2
candidate_files: 50
excluded_files: 12
---

# Reconciliation — US-001 Customer Registration

## 1. Executive Summary

**Verdict: PASS.** The delivered result of US-001 is internally consistent,
fully traceable, adequately documented, and ready for `HUMAN_PR_APPROVAL`.

- **Acceptance Criteria:** 7 of 7 `RECONCILED` (see `traceability` matrix).
  Every AC has an approved requirement, an implementation location, executable
  test evidence, a passing result, an independent functional-verification
  record, and an independent security-review record.
- **Specification / designs vs implementation:** aligned. One endpoint
  (`POST /api/v1/customers`), one table (`customer`), the `CustomerResponse`
  field set, the error model, the security posture, and the persistence schema
  all match the approved artifacts.
- **Predicted vs actual impact:** the change surface matches
  `US-001-impact-analysis.md` §5–§6. The two planned divergences (no
  `JacksonConfig` class — property instead; no `local` profile) were disclosed
  and justified in the plan; one supporting class (`InvalidPasswordException`)
  was added for the FR-6 service re-check with the type the plan left
  unspecified.
- **Plan vs implementation:** every plan step C-1..C-16 and M-1..M-4 is
  implemented; execution order followed.
- **Verification / security currency:** `git diff fb833b0..HEAD -- src/` is
  **empty** — no production code, test, build, or application-config file
  changed after `IMPLEMENTATION`. `implementation_verification` (v1, PASS) and
  `security_review` (v1, PASS) both remain current. Nothing triggers a
  `BLOCKED` staleness condition.
- **Findings:** 0 Critical, 0 Major, 5 Minor, 2 Informational (RC-1..RC-7). None
  blocks PR preparation; none requires a code change for this Story. Every one
  is carried from an upstream review and, where relevant, was explicitly
  assigned to RECONCILIATION.
- **PR candidate scope:** 50 files to include (28 code / build / config +
  20 US-001 workflow-doc artifacts including this reconciliation pair +
  2 harness state files). 12 files/paths excluded — all runtime artifacts,
  IDE-local config, or pre-existing harness scaffolding. No secret, no generated
  DB file, no unrelated source change is in the include set.

**Recommended next action:** `HUMAN_PR_APPROVAL` — a human reviews the diff and
records the decision with `/so:approve` or `/so:reject`.

## 2. Artifact Inventory

| Artifact | Path | Type | Ver | Status | Current | Mandatory | Producing stage |
|---|---|---|---|---|---|---|---|
| Story | docs/stories/US-001-register-customer.md | story | (unversioned) | active | yes | yes | BACKLOG_SYNC / human |
| Open Decisions | docs/decisions/US-001-open-decisions.md | open_decisions | 1 | DRAFT | yes (body stale — RC-4) | yes | CLARIFICATION |
| Clarification Report | docs/evidence/US-001-clarification-report.md | clarification_report | 1 | (input) | yes | yes | CLARIFICATION |
| Specification | docs/specifications/US-001-spec.md | specification | 1 | APPROVED | yes | yes | SPECIFICATION |
| Specification Review | docs/reviews/specifications/US-001-spec-review.md | specification_review | 1 | APPROVED (PASS) | yes | yes | SPEC_REVIEW |
| API Design | docs/designs/api/US-001-api-design.md | api_design | 1 | APPROVED | yes | yes | API_DESIGN |
| OpenAPI Contract | docs/designs/api/US-001-openapi.yaml | openapi | 1 | APPROVED | yes | yes | API_DESIGN |
| DB Design | docs/designs/database/US-001-db-design.md | database_design | 1 | APPROVED | yes | yes | DB_DESIGN |
| Entity Model | docs/designs/database/US-001-entity-model.md | entity_model | 1 | APPROVED | yes | yes | DB_DESIGN |
| Design Review | docs/reviews/designs/US-001-design-review.md | design_review | 1 | APPROVED (PASS) | yes | yes | DESIGN_REVIEW |
| Impact Analysis | docs/impact-analysis/US-001-impact-analysis.md | impact_analysis | 1 | DRAFT (PASS; no review stage follows) | yes | yes | IMPACT_ANALYSIS |
| Implementation Plan | docs/plans/US-001-implementation-plan.md | implementation_plan | 1 | APPROVED (PASS + HUMAN_PLAN_APPROVAL) | yes | yes | IMPLEMENTATION_PLANNING |
| Plan Review | docs/reviews/plans/US-001-plan-review.md | plan_review | 1 | APPROVED (PASS) | yes | yes | PLAN_REVIEW |
| Test Strategy | docs/tests/US-001-test-strategy.md | test_strategy | 1 | DRAFT | yes | yes | TEST_WRITING |
| AC–Test Matrix | docs/tests/US-001-ac-test-matrix.md | ac_test_matrix | 1 | DRAFT | yes | yes | TEST_WRITING |
| Test-Generation Report | docs/evidence/US-001-test-generation-report.md | test_generation_report | 1 | DRAFT | yes | yes | TEST_WRITING |
| Implementation Report | docs/evidence/US-001-implementation-report.md | implementation_report | 1 | DRAFT | yes | yes | IMPLEMENTATION |
| Implementation Verification | docs/verification/US-001-implementation-verification.md | implementation_verification | 1 | DRAFT (verdict PASS; envelope `artifact_status: APPROVED`) | yes | yes | IMPLEMENTATION_VERIFICATION |
| Security Review | docs/reviews/security/US-001-security-review.md | security_review | 1 | APPROVED (PASS) | yes | yes | SECURITY_REVIEW |
| Reconciliation | docs/reviews/reconciliation/US-001-reconciliation.md | reconciliation | 1 | this artifact | — | — | RECONCILIATION |
| Traceability | docs/reconciliation/US-001-traceability.md | traceability | 1 | this artifact | — | — | RECONCILIATION |

- **Missing mandatory artifacts:** none.
- **Duplicate current artifacts:** none.
- **Stale artifacts:** none that block. `open_decisions` v1 body still shows
  OD-001..OD-006 as `OPEN` (RC-4) — a documentation lag, not an unresolved
  decision (see §3 and §19).
- **Wrong Story id / wrong path / superseded reference:** none. Every downstream
  artifact's `inputs` front matter records v1 of each upstream; no artifact is
  `SUPERSEDED` or `ARCHIVED`.

### Artifact chain validation

The dependency chain Story → Specification → Spec Review → API/DB Designs →
Design Review → Impact Analysis → Implementation Plan → Plan Review → Test
artifacts → Implementation Report → Implementation Verification → Security
Review is intact. Each artifact consumed the current (v1) version of every
predecessor. `history.jsonl` records `SPEC_REVIEW` PASS, `DESIGN_REVIEW` PASS,
`PLAN_REVIEW` PASS, `IMPACT_ANALYSIS` PASS, `IMPLEMENTATION` PASS,
`IMPLEMENTATION_VERIFICATION` PASS, `SECURITY_REVIEW` PASS, and both human gates
(`HUMAN_SPEC_APPROVAL` 2026-08-31T07:48:48Z, `HUMAN_PLAN_APPROVAL`
2026-08-31T11:20:00Z). No approval was recorded against a version that was later
superseded.

## 3. Source-of-Truth Review

| Item | Finding |
|---|---|
| Story origin | `active-story.yaml` `source.type: local_only` (Story front matter says `github_issue` but `repository` / `issue_number` / `issue_url` are all `null`). No GitHub source is configured or reachable. |
| Remote Issue inspected | No — there is no linked Issue. GitHub MCP not used (correctly — nothing to sync). |
| Local Story | `docs/stories/US-001-register-customer.md` — the documented source of truth for this Story. |
| Source-of-truth policy | `AGENTS.md` / `backlog-sync`: the local Story artifact is authoritative when no GitHub source exists. |
| Synchronization differences | None to reconcile — single local source. |
| Required action | None. No `story_source_conflict`; no `BACKLOG_SYNC` loop-back. |

## 4. Acceptance Criteria Traceability Matrix

See `docs/reconciliation/US-001-traceability.md` (owned by this stage) for the
full AC → Story → Specification → decision → API design → DB design → Impact
Analysis → Plan step → production file/symbol → test → Implementation
Verification → Security Review → final-status matrix.

Summary: **AC-001..AC-007 all `RECONCILED`.** No `PARTIALLY_RECONCILED`, no
`NOT_RECONCILED`, no `BLOCKED`.

## 5. Specification and Design Alignment

| Check | Result |
|---|---|
| Specification → OpenAPI | Consistent. `POST /api/v1/customers`, `application/json` + `415`, `RegistrationRequest {email, password}` with `additionalProperties: false`, `CustomerResponse {id, email, role, createdAt}`, `201` + `Location`, `400/409/415/500` `ErrorResponse` — all present and matching FR-1..FR-11 and §5 / §8. |
| Specification → DB design | Consistent. FR-5 persisted fields ↔ `customer` columns; FR-4 / §6.1 uniqueness ↔ `uq_customer_email` + service lowercasing; PC-9 ↔ `password_hash VARCHAR(60)`; NFR-4 / BR-007 ↔ explicit mapping + UTC audit. |
| Design → implementation (API) | Consistent. Controller path/method/`consumes`/`produces`, DTO records, validation annotations, `Location` construction, single `@RestControllerAdvice` — all as designed. Deviation RC-2 (error `message` strings vs illustrative OpenAPI examples) is documentation-direction only; body shape preserved. |
| Design → implementation (persistence) | Consistent. `Customer` entity `@Column` set, `@UniqueConstraint(uq_customer_email)`, `EnumType.STRING`, `@CreatedDate`/`@LastModifiedDate` + `updatable=false`, `schema.sql` DDL — all match db-design §4.2 / §8.1 and entity-model §2. `ddl-auto=validate` boots clean (R-1 closed). |
| Design does not introduce unsupported scope | Confirmed. The three DB choices (entity name `Customer`, `role` as an enum column, defensive CHECK/DEFAULT) were confirmed by `design_review` D-2/D-3/D-4; the unknown-field rejection was the choice Spec §6.3 delegated to API_DESIGN. Nothing else new. |

## 6. Predicted Versus Actual Impact

### Predicted items (impact-analysis §5, §6)

| Predicted | Confidence | Actual result | Note |
|---|---|---|---|
| All app packages Create (`controller`, `service`, `repository`, `model.*`, `validation`, `security`, `exception`, `config`) | HIGH | **Confirmed** | 16 production classes created in exactly these packages; no feature package (AD-8 / D-9 respected). |
| `controller/CustomerController` | HIGH | Confirmed | C-13 |
| `service/CustomerService` | HIGH | Confirmed | C-12 (name resolved from "CustomerService or RegistrationService") |
| `repository/CustomerRepository` (`existsByEmail` / `findByEmail`) | HIGH | Confirmed | C-3 — both methods present |
| `model/entity/{Customer, Role}` | HIGH | Confirmed | C-2, C-1 |
| `model/request/RegistrationRequest` | HIGH | Confirmed | C-6 (Java `record`) |
| `model/dto/{CustomerResponse, ErrorResponse, FieldError}` | HIGH | Confirmed | C-7, C-9, C-8 — `FieldError` → `ApiFieldError` (PD-5), serialized shape unchanged |
| `validation/{ValidPassword, PasswordPolicyValidator}` | HIGH | Confirmed | C-4, C-5 — byte-measured |
| `security/SecurityConfig` + `PasswordEncoder` bean | HIGH | Confirmed | C-14 — both beans in one class (PD-4) |
| `exception/{DuplicateEmailException, GlobalExceptionHandler}` | HIGH | Confirmed | C-10, C-11 |
| `config/JpaAuditingConfig` | HIGH | Confirmed | C-15 — `@EnableJpaAuditing` off the main class (AD-7) |
| `config/JacksonConfig` (conditional) | MEDIUM | **Not needed — replaced by alternative** | `spring.jackson.deserialization.fail-on-unknown-properties: true` property (PD-1). Impact analysis marked the class conditional; property is minimal and AD-7-compliant. |
| `schema.sql` | HIGH | Confirmed | C-16 |
| `src/test/resources/application-test.yaml` | MEDIUM | Confirmed | C-17 (created by `test-writer`) |
| `build.gradle.kts` + `spring-boot-starter-validation` | HIGH | Confirmed | M-1 — BOM-managed, confirmed at HUMAN_PLAN_APPROVAL |
| `application.yaml` datasource + JPA + naming + console off | HIGH | Confirmed | M-2 |
| `.gitignore` `/data/` | MEDIUM | Confirmed | M-3 |
| `CustomerPortalApplicationTests` `@ActiveProfiles("test")` | MEDIUM | Confirmed | M-4 |
| Possible `local` profile split | LOW | **Not needed** | plan chose a single `application.yaml` default; nothing required a `local` profile. |

### Actual changed items not individually predicted

| File / component | Classification | Justification |
|---|---|---|
| `exception/InvalidPasswordException` | Required Supporting Change | FR-6 / SC-1 require a service-layer password re-check "before hashing"; plan C-12 said "throw on failure" without naming the type. Domain exception in the `exception` package (AD-6), mapped to `400`, not reachable from the HTTP flow (request `@Valid` catches policy failures first). Disclosed as D-1; accepted by `implementation_verification` §15 and `security_review` §18. |
| `src/test/.../persistence/CustomerSchemaTest.java` — 2 dummy-BCrypt-literal chars | Unexpected but Justified (test-fixture defect fix) | See RC-1 (below) / MF-1. `password_hash` is `VARCHAR(60)`; the pre-fix fixture inserted a 61-char literal that failed with "Value too long for column" before the constraint assertion could run (a false RED). `git diff f6e7eae..fb833b0 -- src/test/` confirms this is the **only** change to a `test-writer`-owned test; no assertion, scenario, or expected outcome changed. |
| `.gitignore` `/docs/harness-consistency-review.md`; `docs/evidence/.gitignore` (4 harness entries) | Unrelated (pre-existing harness housekeeping) — committed in `c72ace3` | Not US-001. Adds ignore entries for pre-existing `harness-*` scaffolding. Recorded as RC-8 (Minor). Not security-relevant (`security_review` §17). |
| `docs/workflow/history.jsonl`, `docs/workflow/workflow-state.yaml` | Generated / harness-managed | Owned by `story-orchestrator`; updated at every transition and will change again when this stage's transition is recorded. |

No **Unexpected and Unapproved** change. No unrelated **source** change.

## 7. Plan Versus Implementation

| Plan step | State | Evidence |
|---|---|---|
| C-1 `Role` | Completed | `model/entity/Role.java` — `enum { CUSTOMER, ADMIN }` |
| C-2 `Customer` | Completed | explicit `@Column`s, `@EntityListeners`, table-level `uq_customer_email`, `equals`/`hashCode` on `id`, `toString()` excludes `passwordHash` |
| C-3 `CustomerRepository` | Completed | `existsByEmail` + `findByEmail` |
| C-4 `ValidPassword` | Completed | static generic message `"Password does not meet the security policy."` |
| C-5 `PasswordPolicyValidator` | Completed | UTF-8 byte length 12..72 + 4 char classes; `public static boolean isCompliant` reused by the service |
| C-6 `RegistrationRequest` | Completed | `record`, `@NotBlank @Email @Size(max=254)` / `@NotBlank @ValidPassword`; no `@JsonIgnoreProperties` (PD-1) |
| C-7 `CustomerResponse` | Completed | `record(Long id, String email, String role, OffsetDateTime createdAt)` |
| C-8 `ApiFieldError` | Completed | `record(String field, String message)` (PD-5) |
| C-9 `ErrorResponse` | Completed | `record`, `@JsonInclude(NON_EMPTY)`, `of(...)` helper |
| C-10 `DuplicateEmailException` | Completed | `RuntimeException`, no HTTP concept |
| C-11 `GlobalExceptionHandler` | Completed | single `@RestControllerAdvice`; validation→400+`fieldErrors`, `HttpMessageNotReadableException`→400 (one branch, PD-1), media-type→415, duplicate→409, invalid-password→400, fallback→500 |
| C-12 `CustomerService` | Completed | `@Transactional register(...)`, `normalizeEmail` (`trim().toLowerCase(ROOT)`), byte re-check → `InvalidPasswordException`, `existsByEmail` guard → `DuplicateEmailException`, `passwordEncoder.encode`, private mapping (PD-3) |
| C-13 `CustomerController` | Completed | `@PostMapping(consumes/produces JSON)`, `201` + `Location` via `ServletUriComponentsBuilder`, no error handling, no repository access, no entity in a signature |
| C-14 `SecurityConfig` | Completed | scoped `REGISTRATION_ENDPOINT` matcher for both `permitAll` and `csrf.ignoringRequestMatchers`; `anyRequest().authenticated()`; `HttpStatusEntryPoint(401)` (plan-review F-3 recommendation adopted); `formLogin`/`httpBasic`/`logout` disabled; `BCryptPasswordEncoder` bean (PD-4) |
| C-15 `JpaAuditingConfig` | Completed | `@EnableJpaAuditing(dateTimeProviderRef=...)`, UTC `DateTimeProvider`; not on the main class (AD-7) |
| C-16 `schema.sql` | Completed | DB design §8.1 DDL; `GENERATED BY DEFAULT AS IDENTITY` with no explicit `NOT NULL` on `id` (D-5 / RC-7) |
| C-17 `application-test.yaml` | Completed | created by `test-writer`; isolated in-memory H2, `validate`, `sql.init.mode=embedded`, console off, `fail-on-unknown-properties`, `open-in-view=false` |
| M-1 `build.gradle.kts` | Completed | `+ spring-boot-starter-validation` (BOM-managed) |
| M-2 `application.yaml` | Completed | H2 file datasource, `ddl-auto=validate`, snake_case naming, `sql.init.mode=always`, `h2.console.enabled=false`, `fail-on-unknown-properties=true`, `open-in-view=false` |
| M-3 `.gitignore` | Completed | `+ /data/` (present since `fb833b0`) |
| M-4 `CustomerPortalApplicationTests` | Completed | `+ @ActiveProfiles("test")` |

No unimplemented plan step. No undocumented implementation step (the one
supporting class is disclosed). No changed execution strategy beyond the
disclosed PD-1 mechanism choice. No unapproved dependency change (the one added
dependency is the AD-5-mandated first-party starter, confirmed at
`HUMAN_PLAN_APPROVAL`). No unapproved configuration change. No hidden
refactoring — the repository had only `CustomerPortalApplication` + one test
before this Story.

## 8. Test Reconciliation

| | Detail |
|---|---|
| Planned tests | `test-strategy` §4 levels + `ac-test-matrix` (`RED` / `GREEN (guard)` / `DEFERRED → IMPLEMENTATION` rows); plan skeletons C-T1..C-T6 (indicative). |
| Actual tests | 7 suites, 62 tests: `registration.CustomerRegistrationApiTest` (22), `validation.PasswordPolicyValidatorTest` (17), `persistence.CustomerSchemaTest` (10), `service.CustomerServiceTest` (5), `security.RegistrationSecurityPostureTest` (4), `persistence.CustomerPersistenceTest` (3), `CustomerPortalApplicationTests` (1). |
| Executed / result | `./gradlew clean build` — **BUILD SUCCESSFUL**; **62 tests, 0 failures, 0 errors, 0 skipped**. Independently reproduced by `implementation-verifier` v1 (§5, §6) from `build/test-results/test/*.xml`. |
| AC coverage | Every AC-001..AC-007 has ≥1 web- or persistence-level test on the real stack (matrix §6). `DEFERRED` matrix rows (C-T1 policy matrix, C-T2 persistence invariants, C-T3 service internals) were all implemented and pass. |
| Tests validating behaviour outside approved requirements | None found. Security tests use the real `SecurityFilterChain`; service tests use a real `BCryptPasswordEncoder`; persistence tests hit H2 `INFORMATION_SCHEMA`. `test-strategy` §5.5 asserts the plan-review F-3 recommendation (`401` for protected routes) as a deliberate tripwire — the implementation adopted that recommendation, so it passes. |
| Stale evidence | None. No test or production file changed after `IMPLEMENTATION` (`git diff fb833b0..HEAD -- src/` empty), so the verifier's reproduction remains current. |
| Extra / missing test behaviour | The concurrent-duplicate race (RC-3) has no test — accepted: no AC/design/plan covers concurrency and anti-abuse is out of scope (OD-005:A). |

## 9. API Reconciliation

| Contract element (openapi.yaml v1) | Implementation | Match |
|---|---|---|
| `POST /api/v1/customers`, `security: []`, public | `@RequestMapping("/api/v1/customers")` + `@PostMapping`; `SecurityConfig` `permitAll` for `POST /api/v1/customers` | ✅ |
| `application/json` only, else `415` | `consumes = APPLICATION_JSON_VALUE`; `handleMediaType` → `415` | ✅ |
| `RegistrationRequest {email, password}`, `additionalProperties: false` | `record RegistrationRequest(email, password)` + `fail-on-unknown-properties=true` | ✅ |
| `email` `minLength 1` / `maxLength 254` / `format: email` | `@NotBlank @Email @Size(max=254)` | ✅ |
| `password` `writeOnly`, 12–72, policy, service re-check | `@ValidPassword` (byte-measured) + service `isCompliant` re-check | ✅ (contract states a character max; 72 enforced as **bytes** per spec-review F-5 / DB Q-4 — expected, documented) |
| `201` + `Location: /api/v1/customers/{id}` + `CustomerResponse` | `ResponseEntity.created(location).body(created)` | ✅ |
| `CustomerResponse {id, email, role, createdAt}`, `role` enum `[CUSTOMER]` | matching `record`; `role.name()` = `"CUSTOMER"` | ✅ |
| `400 / 409 / 415 / 500` `ErrorResponse` shape; `fieldErrors[]` only for field failures | `GlobalExceptionHandler` one branch per status; `ErrorResponse` `@JsonInclude(NON_EMPTY)` | ✅ |
| `Location` target `GET /api/v1/customers/{id}` | not implemented by US-001 (design-review D-8) — header value correct and stable; future Story | ✅ (accepted) |

**Deviation (RC-2, Minor, documentation-direction):** implemented `message`
strings for `400` (one combined missing/malformed/unknown-field message) and
`415` differ verbatim from the OpenAPI illustrative `examples`. The `409`
message (`"An account with this email already exists."`) matches exactly. The
AC-6 body **shape** — the normative part of the contract — is preserved, and
every test asserts status + shape, not free text. The implemented messages are
*more* generic, so no additional information is disclosed. Authority direction:
**documentation may follow the implemented decision** (optional
`openapi-designer` example alignment in a future revision); the implementation
does not need to change.

## 10. Persistence Reconciliation

| Element | DB design | Implementation | Runtime evidence | Match |
|---|---|---|---|---|
| `customer` table, hand-written `schema.sql`, `ddl-auto=validate` | §8, §8.1 | `src/main/resources/schema.sql`; `validate` both profiles | context boots clean on both profiles (`implementation_verification` §5, §9) | ✅ |
| `id BIGINT` identity, `pk_customer` | §4.2 | `@Id @GeneratedValue(IDENTITY) @Column(nullable=false)`; `GENERATED BY DEFAULT AS IDENTITY` | `CustomerSchemaTest.idColumnIsBigintNotNull` | ✅ (RC-7 — `id` DDL has no explicit `NOT NULL`; H2 2.x implicit; `validate` passes) |
| `email VARCHAR(254) NOT NULL`, `uq_customer_email` | §4.2, §4.3 | `@Column(length=254, nullable=false)`; `@UniqueConstraint(name="uq_customer_email")` | `CustomerSchemaTest.emailColumnIsVarchar254NotNull`, `.emailHasAUniqueConstraint` | ✅ |
| `password_hash VARCHAR(60) NOT NULL` | §4.2 (PC-9) | `@Column(name="password_hash", length=60, nullable=false)` | `CustomerSchemaTest.passwordHashColumnIsVarchar60NotNull` | ✅ |
| `role VARCHAR(20) NOT NULL`, `EnumType.STRING`, `ck_customer_role` | §4.2, §4.3 | `@Enumerated(STRING) @Column(length=20, nullable=false)`; CHECK in DDL | `CustomerSchemaTest.roleColumnIsNotNull`; `CustomerPersistenceTest.roleIsPersistedAsTheEnumName` | ✅ |
| `enabled BOOLEAN NOT NULL` | §4.2 | `@Column(nullable=false) boolean enabled` | `CustomerSchemaTest.enabledColumnIsBooleanNotNull` | ✅ |
| `created_at` / `updated_at` `TIMESTAMP WITH TIME ZONE NOT NULL`, UTC, `created_at` not updatable | §4.2 | `OffsetDateTime` + `@CreatedDate`/`@LastModifiedDate`; `created_at updatable=false`; UTC `DateTimeProvider` | `CustomerSchemaTest.auditTimestampColumnsAreTimeZoneAwareAndNotNull`; `CustomerPersistenceTest.auditingPopulatesBothTimestampsInUtc`, `.createdAtIsNotChangedByALaterUpdate` | ✅ |
| Case-insensitive uniqueness (OD-006:A) | §5 | `CustomerService.normalizeEmail` before check + insert; plain `existsByEmail` | `duplicateEmailIsRejectedCaseInsensitivelyWith409`; `CustomerServiceTest.emailIsNormalisedToLowercaseAndTrimmedBeforeCheckAndSave` | ✅ |
| `OffsetDateTime` ↔ `TIMESTAMP WITH TIME ZONE` under `validate` (R-1) | §8.1 note | maps clean on H2 2.x / Hibernate 7.4.5 | no `HHH` validation error at startup | ✅ R-1 CLOSED |
| Generated `./data/*` files | §8 | `.gitignore` `/data/` | not committed, not in tree | ✅ |
| `role` / `enabled` DDL `DEFAULT`, `ck_customer_role` CHECK | §4.3 / §10 (defensive, DDL-only) | present in `schema.sql`; entity always sets both explicitly | not verified by `validate` (as designed, design-review D-4) | ✅ (accepted) |

Runtime schema evidence comes from H2 `INFORMATION_SCHEMA` assertions in
`CustomerSchemaTest` against a booted datasource, not annotations alone.

## 11. Architecture Reconciliation

| Rule | Evidence | Status |
|---|---|---|
| Layering `controller → service → repository → model.entity` (AD-2) | `CustomerController` imports `CustomerService` only; `CustomerService` → repository / model / exception / validation; `CustomerRepository` → `Customer` only | ✅ |
| Controller: no business logic, no repository access, no entity in a signature (AD-2, AD-4) | `register(...)` takes `RegistrationRequest`, returns `ResponseEntity<CustomerResponse>`; no `repository` import in `controller/` | ✅ |
| Service owns transactions (AD-3) | `@Transactional` on the public `register(...)` write method | ✅ |
| DTO / entity boundary (AD-4) | request/response are `model.request` / `model.dto` records; `Customer` never on a controller signature; mapping in the service (PD-3) | ✅ |
| Validation split (AD-5) | request-shape via Bean Validation + custom `@ValidPassword` in `validation`; uniqueness + password re-check in the service | ✅ |
| Single `@RestControllerAdvice` (AD-6, AC-9) | exactly one — `exception/GlobalExceptionHandler`; controller builds no error body | ✅ |
| Config boundaries (AD-7) | `JpaAuditingConfig` in `config`; `SecurityConfig` in `security`; settings in `application.yaml` / test profile | ✅ |
| Package ownership (`package-map.md`) | `controller`, `service`, `repository`, `model/{entity,dto,request}`, `exception`, `validation`, `security`, `config` — no feature package (AD-8, D-9) | ✅ |
| Reuse over duplication (AD-8) | no mapper class; one `SecurityConfig` for both beans; `PasswordPolicyValidator.isCompliant` reused by the service | ✅ |

Method: import-graph + method-signature inspection
(`semantic_analysis: TEXT_FALLBACK` — IDEA MCP semantic tools not invoked;
consistent with `implementation_verification` §10 and `security_review` §4).
Layering is unambiguous from imports; a PSI call-graph cross-check was not
performed. No architecture drift.

## 12. Security Reconciliation

| Item | Detail |
|---|---|
| Security Review version | `docs/reviews/security/US-001-security-review.md` v1, `status: APPROVED`, verdict PASS (0 Critical, 0 Major, 4 Minor, 2 Informational). |
| Security-sensitive files reviewed | `security/SecurityConfig`, `validation/*`, `exception/GlobalExceptionHandler`, `model/request/RegistrationRequest`, `model/dto/*`, `model/entity/Customer`, `application.yaml`, `application-test.yaml`, `schema.sql`, `.gitignore`, `build.gradle.kts`. |
| Changes after Security Review | `git diff c72ace3..HEAD` = `.gitignore` (this session), the two new reconciliation artifacts, and workflow state — **no `src/**` change, no config change, no dependency change**. `git diff b26c4a9..HEAD -- src/` (after Implementation Verification) is also empty. |
| Current evidence status | **Current.** `security_review` v1 and `implementation_verification` v1 both remain valid — no security-sensitive file changed after either. |
| Security drift | None. SEC-1..SEC-11 all independently verified; security-sensitive Open Decisions (OD-002:B, OD-003:A, OD-005:A) human-resolved and correctly implemented. |
| Carried security findings | RC-1 (`RegistrationRequest` `toString()`), RC-2 (message strings), RC-3 (concurrent duplicate → `500`), RC-4 (`open_decisions.md` stale), RC-5 / RC-6 (Informational: `AUTO_SERVER` + blank H2 credential — approved config). See §16 / §17. |

**No change was made after `security_review` that would make its evidence
stale.** No `SECURITY_REVIEW` loop-back / `BLOCKED` condition applies.

## 13. Configuration and Dependency Reconciliation

| Item | Planned | Actual | Match |
|---|---|---|---|
| `spring-boot-starter-validation` | M-1 (BOM-managed, no explicit version) | present in `build.gradle.kts`, no version pin | ✅ (confirmed at HUMAN_PLAN_APPROVAL) |
| Other dependencies | unchanged | `data-jpa`, `security`, `webmvc`, `h2`, `lombok` + test starters — unchanged | ✅ |
| `application.yaml` | M-2 (datasource, `ddl-auto=validate`, snake_case naming, `sql.init.mode=always`, `h2.console.enabled=false`, `fail-on-unknown-properties=true`) | all present; `open-in-view: false` also added (impact §11 / test-strategy §6) | ✅ |
| `application-test.yaml` | C-17 (isolated in-memory H2, `validate`, `sql.init.mode=embedded`, console off, `fail-on-unknown-properties`) | present, `open-in-view: false` also | ✅ |
| `.gitignore` | M-3 (`/data/`) | `/data/` present since `fb833b0`; `/docs/harness-consistency-review.md` added later (RC-8) | ✅ (M-3) |
| Secrets / env vars / ports / JVM | none | none; `datasource.password: ""` is the H2 default (RC-6, Informational) | ✅ |
| `AUTO_SERVER=TRUE` in the default datasource URL | approved — DB design §8, plan M-2, impact analysis | present in `application.yaml`; test profile uses isolated in-memory H2 | ✅ (RC-5, Informational — future hardening recommendation) |

No undocumented dependency. No undocumented configuration. Local-environment
assumptions: file-based H2 under `./data/` for local runs (git-ignored),
isolated in-memory H2 for tests — both as designed.

## 14. Documentation Reconciliation

| Document | State | Authority direction |
|---|---|---|
| Specification v1 | Current — matches the implementation. | — |
| OpenAPI contract v1 | Current for shapes / status codes / auth. Illustrative `400` / `415` `examples` differ from the implemented `message` strings (RC-2). | **Documentation may follow the implementation** — optional `openapi-designer` example alignment; not required. |
| API design v1 / DB design v1 / entity model v1 | Current — match the implementation. | — |
| `open_decisions.md` v1 | **Stale body** — OD-001..OD-006 still `status: OPEN` (RC-4). Authoritative resolutions (`OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A OD-006:A`) are in `history.jsonl` at `HUMAN_SPEC_APPROVAL` and applied consistently everywhere. | **Documentation must follow the recorded decision** — `us-clarifier` should publish v2 marking each `RESOLVED`. Already ruled non-blocking by the human at `HUMAN_PLAN_APPROVAL` ("Non-blocking: sync docs/decisions/US-001-open-decisions.md … owned by us-clarifier"). |
| Architecture / convention docs | No edit required (impact-analysis §12); none was made. The CSRF exemption is recorded in API design §6 per SC-5, not in `architecture.md`. | — |
| Implementation Report v1 | Materially consistent with observed evidence (`implementation_verification` §16). | — |
| `README` / `AGENTS.md` | No change required; none made. | — |

No document requires the implementation to change. Two documentation items
(RC-2, RC-4) are follow-ups owned by `openapi-designer` and `us-clarifier`
respectively.

## 15. Pull Request Candidate Scope

Classification convention: one row per path in `git diff --name-status
origin/main..HEAD` plus the two reconciliation artifacts created by this stage.
Counts are **as of this artifact's write time** — `docs/workflow/history.jsonl`
and `docs/workflow/workflow-state.yaml` will change again when the RECONCILIATION
→ HUMAN_PR_APPROVAL transition is recorded (still INCLUDE). `pr-preparer`
consumes this classification and must not re-derive it.

### Include (50)

**Production / build / configuration (28)**

- `src/main/java/org/example/customerportal/config/JpaAuditingConfig.java`
- `src/main/java/org/example/customerportal/controller/CustomerController.java`
- `src/main/java/org/example/customerportal/exception/DuplicateEmailException.java`
- `src/main/java/org/example/customerportal/exception/GlobalExceptionHandler.java`
- `src/main/java/org/example/customerportal/exception/InvalidPasswordException.java`
- `src/main/java/org/example/customerportal/model/dto/ApiFieldError.java`
- `src/main/java/org/example/customerportal/model/dto/CustomerResponse.java`
- `src/main/java/org/example/customerportal/model/dto/ErrorResponse.java`
- `src/main/java/org/example/customerportal/model/entity/Customer.java`
- `src/main/java/org/example/customerportal/model/entity/Role.java`
- `src/main/java/org/example/customerportal/model/request/RegistrationRequest.java`
- `src/main/java/org/example/customerportal/repository/CustomerRepository.java`
- `src/main/java/org/example/customerportal/security/SecurityConfig.java`
- `src/main/java/org/example/customerportal/service/CustomerService.java`
- `src/main/java/org/example/customerportal/validation/PasswordPolicyValidator.java`
- `src/main/java/org/example/customerportal/validation/ValidPassword.java`
- `src/main/resources/schema.sql`
- `src/main/resources/application.yaml` (M)
- `src/test/java/org/example/customerportal/CustomerPortalApplicationTests.java` (M — M-4)
- `src/test/java/org/example/customerportal/persistence/CustomerPersistenceTest.java`
- `src/test/java/org/example/customerportal/persistence/CustomerSchemaTest.java`
- `src/test/java/org/example/customerportal/registration/CustomerRegistrationApiTest.java`
- `src/test/java/org/example/customerportal/security/RegistrationSecurityPostureTest.java`
- `src/test/java/org/example/customerportal/service/CustomerServiceTest.java`
- `src/test/java/org/example/customerportal/validation/PasswordPolicyValidatorTest.java`
- `src/test/resources/application-test.yaml`
- `build.gradle.kts` (M — M-1)
- `.gitignore` (M — carries M-3 `/data/`; note: also contains one pre-existing harness line `/docs/harness-consistency-review.md` added in `c72ace3`, RC-8)

**US-001 workflow-doc artifacts (20)** — `docs/decisions/US-001-open-decisions.md`,
`docs/evidence/US-001-clarification-report.md`,
`docs/evidence/US-001-implementation-report.md`,
`docs/evidence/US-001-test-generation-report.md`,
`docs/specifications/US-001-spec.md`,
`docs/reviews/specifications/US-001-spec-review.md`,
`docs/designs/api/US-001-api-design.md`, `docs/designs/api/US-001-openapi.yaml`,
`docs/designs/database/US-001-db-design.md`,
`docs/designs/database/US-001-entity-model.md`,
`docs/reviews/designs/US-001-design-review.md`,
`docs/impact-analysis/US-001-impact-analysis.md`,
`docs/plans/US-001-implementation-plan.md`,
`docs/reviews/plans/US-001-plan-review.md`,
`docs/tests/US-001-test-strategy.md`, `docs/tests/US-001-ac-test-matrix.md`,
`docs/verification/US-001-implementation-verification.md`,
`docs/reviews/security/US-001-security-review.md`,
`docs/reviews/reconciliation/US-001-reconciliation.md` (this file),
`docs/reconciliation/US-001-traceability.md` (this file's pair).

**Harness state (2)** — `docs/workflow/history.jsonl` (M),
`docs/workflow/workflow-state.yaml` (M). Harness-managed; part of the branch.

### Exclude — Runtime Artifacts (3)

- `/data/` (generated H2 files — git-ignored, never in the tree)
- `build/`, `.gradle/` (git-ignored)

### Exclude — Local Configuration (3)

- `.idea/`, `customer-portal.iml` (git-ignored IDE files)
- `docs/hooks/` (git-ignored — `tool-usage.jsonl` telemetry)

### Exclude — Sensitive Files (0)

None. No `.env`, token, key, or credential file — tracked or untracked — is in
scope.

### Exclude — Unrelated Changes (6)

- `docs/evidence/.gitignore` (M) — **already committed on the branch** in
  `c72ace3`; pure harness scaffolding (ignore entries for pre-existing
  `harness-*` files), no US-001 content. Recorded as RC-8 (Minor). Reconciliation
  cannot "remove" an already-committed change; noted for human awareness.
- `docs/evidence/harness-dryrun.py`, `docs/evidence/harness-migration-baseline.md`,
  `docs/evidence/harness-migration-report.md`,
  `docs/evidence/harness-post-migration-review.md`,
  `docs/harness-consistency-review.md` — pre-existing, untracked (now git-ignored),
  outside this Story, left untouched (`implementation_verification` §4,
  `security_review` §17).

### Human Decision Required (0)

None. Scope is unambiguous.

**Totals:** `candidate_files: 50`, `excluded_files: 12` (3 runtime + 3
local-config + 6 unrelated; the 0 sensitive is included in the count as a
category check, not a file).

## 16. Drift Register

| id | Type | Severity | Affected | Expected | Actual | Risk | Correction | Loop-back |
|---|---|---|---|---|---|---|---|---|
| RC-1 | Documentation / Security (latent) | Minor | `model/request/RegistrationRequest.java` | Plaintext password never emitted anywhere (SEC-3); `Customer.toString()` was hand-written to exclude `passwordHash` | `RegistrationRequest` is a `record` → generated `toString()` renders `password` | Low — no reachable leak path today (no logger / `System.out` / `println` in `src/main`; `GlobalExceptionHandler` never touches the bound DTO; no debug/SQL logging config) | Follow-up: custom masking `toString()` on `RegistrationRequest`, consistent with `Customer`. **Accepted-with-recommendation** for US-001. | none |
| RC-2 | Documentation (API) | Minor | `exception/GlobalExceptionHandler.java`, `docs/designs/api/US-001-openapi.yaml` | Implemented `message` strings could match the OpenAPI examples | Implemented `400` (one combined message) and `415` messages differ verbatim from the illustrative examples; `409` matches exactly | None — examples are illustrative; AC-6 body shape preserved; implemented messages are more generic (less disclosure) | Optional: `openapi-designer` aligns the `400`/`415` examples in a future revision. Documentation-direction. | none |
| RC-3 | Requirement-adjacent (robustness) | Minor | `service/CustomerService.java`, `exception/GlobalExceptionHandler.java` | (no approved artifact covers concurrency) | Check-then-act (`existsByEmail` → `save`); a concurrent duplicate trips `uq_customer_email` as an unmapped `DataIntegrityViolationException` → `500`, not `409` | Low — `uq_customer_email` still prevents the duplicate row; no data-integrity or enumeration impact beyond accepted OD-003:A; not a DoS vector | Follow-up Story: map `DataIntegrityViolationException` on `uq_customer_email` to `409`. Out of scope for US-001 (no AC / design / plan covers it; anti-abuse is OD-005:A out of scope). | none |
| RC-4 | Artifact / Documentation | Minor | `docs/decisions/US-001-open-decisions.md` | Body reflects the recorded resolutions | All six still `status: OPEN`; recommended options "non-binding" | None to the implementation (uses the resolved values); a reader consulting only the file could be misled | `us-clarifier` publishes v2 marking OD-001..OD-006 `RESOLVED`. Human already ruled this non-blocking at `HUMAN_PLAN_APPROVAL`. | none (owner `us-clarifier` / CLARIFICATION; no `RECONCILIATION.loop_back` key reaches it, and it is not a genuine blocker) |
| RC-5 | Configuration | Informational | `src/main/resources/application.yaml` | — | `AUTO_SERVER=TRUE` starts an H2 mixed-mode TCP listener guarded by a blank `sa` password | Low — **approved** config (DB design §8, plan M-2, impact analysis); local-dev only; training data only; test profile isolated in-memory | Future hardening Story: drop `AUTO_SERVER` or bind loopback + externalize an H2 password | none |
| RC-6 | Secret management | Informational | `src/main/resources/application.yaml` | Config secrets externalized (SC-7) | `spring.datasource.password: ""` + `username: sa` committed | None — empty string is the H2 default, not a secret; SC-7 not breached | When a real datasource is introduced, externalize its credentials | none |
| RC-7 | Persistence (DDL style) | Informational | `src/main/resources/schema.sql` | — | `id BIGINT GENERATED BY DEFAULT AS IDENTITY` with no explicit `NOT NULL` (H2 2.x rejects the clause before the identity clause) | None — H2 2.x makes identity columns implicitly `NOT NULL`; `ddl-auto=validate` passes; `CustomerSchemaTest.idColumnIsBigintNotNull` confirms | None. Documented in DB design §8.1 note / disclosed D-5. | none |
| RC-8 | Scope (repository hygiene) | Minor | `.gitignore`, `docs/evidence/.gitignore` | Story change set contains only US-001 files | Two `.gitignore` files also carry pre-existing harness-scaffolding ignore entries, committed in `c72ace3` | None — no code, no secret, no runtime artifact; `security_review` §17 characterized these as pre-existing non-US-001 hygiene | None actionable by reconciliation (already committed). Noted for human diff review. | none |

No Requirement Drift, no Design Drift (implementation follows the approved API /
DB / architecture designs), no Plan Drift (every step implemented), no Test
Drift (tests assert approved behaviour), no Security Drift (no security-sensitive
change after `security_review`). Documentation Drift = RC-2, RC-4 (both
follow-ups, neither requires an implementation change). Scope Drift = RC-8 (two
harness-hygiene lines already on the branch; no source impact).

## 17. Findings

### RC-1 — `RegistrationRequest` record `toString()` renders the plaintext password  (= SEC F-1 / IMPL_VERIFICATION MF-5)

- **Severity:** Minor. **Category:** documentation / data-exposure (latent).
- **Evidence:** `model/request/RegistrationRequest.java` is a `record`; the
  compiler-generated `toString()` includes every component, `password` among
  them. `model/entity/Customer.toString()` was hand-written specifically to
  exclude `passwordHash`.
- **Impact:** SEC-3 / SC-1 intent is that the plaintext password is never
  logged or placed on a response. A `toString()` that emits it is a latent
  contradiction of that intent. **No reachable leak path exists today** —
  independently confirmed by `implementation_verification` §12 and
  `security_review` §7/§13: no logger, `System.out`, or `println` anywhere in
  `src/main`; `GlobalExceptionHandler` reads only `HttpServletRequest` +
  `BindingResult.getFieldErrors()`; controller and service never call
  `request.toString()`; no debug/SQL logging config.
- **Required correction:** none for US-001. **Recommendation:** a follow-up
  change gives `RegistrationRequest` a custom masking `toString()` consistent
  with `Customer`. Recorded here as **accepted-with-recommendation** per the
  explicit hand-off from `security_review` F-1 and `implementation_verification`
  MF-5.
- **Responsible stage / loop-back:** none. Follow-up work item, not a US-001
  defect.

### RC-2 — Implemented error `message` strings differ from the OpenAPI illustrative examples  (= SEC F-2 / IMPL_VERIFICATION MF-2 / IMPL D-4 / plan-review F-4)

- **Severity:** Minor. **Category:** documentation (API).
- **Evidence:** implemented `415` message `"Content-Type must be
  application/json."` vs contract example `"Content-Type 'text/plain' is not
  supported."`; one combined `400` message
  (`"The request body is missing, malformed, or contains an unknown field."`)
  vs two contract examples. `409` matches exactly.
- **Impact:** none. OpenAPI `examples` are explicitly illustrative, not
  normative. The AC-6 body **shape** is preserved; all tests assert status +
  shape. Implemented messages are more generic — no extra disclosure.
- **Required correction:** none. Optional `openapi-designer` example alignment
  in a future revision (documentation-direction).
- **Responsible stage / loop-back:** none.

### RC-3 — Concurrent duplicate registration maps to `500`, not `409`  (= SEC F-3 / IMPL_VERIFICATION MF-3)

- **Severity:** Minor. **Category:** robustness / error mapping (edge).
- **Evidence:** `CustomerService.register` guards duplicates with check-then-act
  (`existsByEmail` → `save`). Two simultaneous requests for the same new email
  can both pass the check; the second `save` throws
  `DataIntegrityViolationException` (from `uq_customer_email`), which
  `GlobalExceptionHandler` does not map → falls through to `handleUnexpected` →
  `500`.
- **Impact:** low. The unique constraint still prevents the duplicate row (data
  integrity holds). The single-request duplicate path — the AC-002 requirement —
  is correctly `409`. No approved artifact covers concurrency; anti-abuse /
  rate-limiting is explicitly out of scope (OD-005:A).
- **Required correction:** none for US-001. **Recommendation:** a follow-up
  Story catches-and-translates `DataIntegrityViolationException` on
  `uq_customer_email` to `409`.
- **Responsible stage / loop-back:** none (not a requirements defect; not
  `implementation_drift`).

### RC-4 — `open_decisions.md` v1 still shows OD-001..OD-006 as `OPEN`  (= spec-review F-1 / IMPL_VERIFICATION MF-4 / SEC F-4 / plan-review F-1)

- **Severity:** Minor. **Category:** artifact / documentation lag.
- **Evidence:** `docs/decisions/US-001-open-decisions.md` is `status: DRAFT`
  with all six decisions `status: OPEN` and options "non-binding". The
  authoritative resolutions (`OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A
  OD-006:A`) are recorded in `history.jsonl` at `HUMAN_SPEC_APPROVAL`
  (2026-08-31T07:48:48Z) and applied consistently across the Specification, both
  designs, the design review, the impact analysis, the plan, the plan review,
  the tests, the implementation, the implementation verification, and the
  security review.
- **Impact:** none on the implementation, which uses the resolved values. A
  reader consulting only the file could be misled. `AGENTS.md` treats a literal
  `OPEN` marker as a blocker — but the decisions are *resolved*, authoritatively,
  just not reflected in this one file, and the human explicitly ruled this
  non-blocking at `HUMAN_PLAN_APPROVAL` ("Non-blocking: sync
  docs/decisions/US-001-open-decisions.md with the approved resolutions …
  owned by us-clarifier").
- **Required correction:** `us-clarifier` publishes `open_decisions.md` v2
  marking each decision `RESOLVED` with the chosen option. Not a
  RECONCILIATION action (this stage must not resolve or edit Open Decisions).
- **Responsible stage / loop-back:** `us-clarifier` (CLARIFICATION owns the
  file). No `RECONCILIATION.loop_back` key targets CLARIFICATION, and this is
  not a genuine blocker — so `loop_back_stage: null`, verdict stays PASS.

### RC-5 — `AUTO_SERVER=TRUE` H2 listener with a blank `sa` password  (= SEC F-5)

- **Severity:** Informational. **Category:** configuration.
- **Evidence:** default-profile datasource URL
  `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`, `username: sa`,
  `password: ""`.
- **Impact:** low — **approved** configuration (DB design §8 line 172, plan
  M-2, impact analysis §11); local-dev only; training data only; the test
  profile uses isolated in-memory H2 with no server. Not exposed via the public
  API.
- **Required correction:** none. **Recommendation:** a future hardening Story
  drops `AUTO_SERVER` (single-process access suffices) or binds it to loopback
  and externalizes an H2 password.
- **Responsible stage / loop-back:** none.

### RC-6 — Empty datasource password committed in `application.yaml`  (= SEC F-6)

- **Severity:** Informational. **Category:** secret management.
- **Evidence:** `spring.datasource.password: ""` + `username: sa` are checked
  in.
- **Impact:** none — the empty string is the H2 default, not a secret; SC-7 is
  not breached.
- **Required correction:** none for US-001. When a real datasource is
  introduced, its credentials must be externalized (SC-7).
- **Responsible stage / loop-back:** none.

### RC-7 — `schema.sql` `id` column has no explicit `NOT NULL`  (= IMPL D-5)

- **Severity:** Informational. **Category:** persistence (DDL style).
- **Evidence:** `id BIGINT GENERATED BY DEFAULT AS IDENTITY` — no explicit
  `NOT NULL` clause (H2 2.x rejects it before the identity clause; DB design
  §8.1 note).
- **Impact:** none — H2 2.x makes identity columns implicitly `NOT NULL`;
  `ddl-auto=validate` passes on both profiles; `CustomerSchemaTest.idColumnIsBigintNotNull`
  confirms `BIGINT` + not-nullable.
- **Required correction:** none.
- **Responsible stage / loop-back:** none.

### RC-8 — Two `.gitignore` files carry pre-existing harness-scaffolding entries  (from IMPL_VERIFICATION §4 / SEC §17)

- **Severity:** Minor. **Category:** scope / repository hygiene.
- **Evidence:** `.gitignore` gained `/docs/harness-consistency-review.md` and
  `docs/evidence/.gitignore` gained four `harness-*` entries, committed in
  `c72ace3` ("Stage security_review completed"). These ignore pre-existing
  harness scaffolding, not US-001 output.
- **Impact:** none — no code, no secret, no runtime artifact; the entries are
  harmless and `security_review` §17 already characterized them as pre-existing
  non-US-001 hygiene. `.gitignore` also legitimately carries M-3 (`/data/`), so
  it stays in the PR include set.
- **Required correction:** none actionable by reconciliation (already
  committed). Flagged for the human diff reviewer at `HUMAN_PR_APPROVAL` so the
  two extra ignore lines are not mistaken for US-001 scope.
- **Responsible stage / loop-back:** none.

**No Critical findings. No Major findings.**

## 18. Positive Alignment

1. **Every Acceptance Criterion is `RECONCILED`** with an approved requirement,
   an implementation location, executable test evidence on the real stack, a
   passing result, an independent functional-verification record, and an
   independent security-review record.
2. **Change set == planned scope.** C-1..C-16 + M-1..M-4 exactly, plus one
   disclosed supporting class (`InvalidPasswordException`) and one benign
   2-character test-fixture fix. No unrelated source change.
3. **Build + tests reproduced independently** by `implementation-verifier`:
   `./gradlew clean build` BUILD SUCCESSFUL, 62 tests / 0 failures / 0 errors /
   0 skipped.
4. **Verification and security evidence are current** — `git diff
   fb833b0..HEAD -- src/` is empty; nothing changed after `IMPLEMENTATION`.
5. **Security posture verified end to end** — BCrypt (no no-op encoder);
   plaintext password confined to the request DTO; zero logging in `src/main`;
   `CustomerResponse` credential/internal-state isolation positively asserted;
   deny-by-default authorization with one exact `permitAll` matcher; CSRF
   exemption on the same single matcher; H2 console disabled every profile;
   `ddl-auto=validate` every profile; mass assignment closed.
6. **All six Open Decisions resolved by a human** and applied consistently
   across every downstream artifact (design-review, plan-review, impl,
   verification, security-review all confirm).
7. **Risks R-1..R-8 all closed or owned** — R-1 (`OffsetDateTime` ↔ TZ type)
   and R-6 (UTC auditing) verified closed; R-2 (CSRF scope), R-3 (byte bound),
   R-7 (message hygiene) verified by SECURITY_REVIEW; R-5 (context boot) closed;
   R-8 (scope) closed here.
8. **Architecture clean** — layering, single advice, DTO/entity boundary,
   config placement, package ownership all per `architecture.md` /
   `package-map.md`.

## 19. Open Decisions

**No blocking Open Decisions were identified.** OD-001..OD-006 were all
resolved by the human at `HUMAN_SPEC_APPROVAL` (`history.jsonl`
2026-08-31T07:48:48Z: `OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A OD-006:A`)
and are applied consistently across every Story artifact and the implementation.
The `open_decisions.md` file body still showing `OPEN` (RC-4) is a documentation
lag owned by `us-clarifier`, explicitly ruled non-blocking by the human at
`HUMAN_PLAN_APPROVAL` — not an unresolved decision.

No unresolved `TODO` / `TBD` / `FIXME` / `???` / "to be decided" marker was
found in any `APPROVED` input artifact that affects an Acceptance Criterion,
observable behaviour, the API contract, persistence, security, validation,
architecture, testing, configuration, dependency selection, or PR scope.

## 20. Reconciliation Limitations

- **Semantic analysis:** IDEA MCP semantic tools not invoked
  (`semantic_analysis: TEXT_FALLBACK`). Architecture / call-path conclusions
  rest on import graphs and method signatures, consistent with
  `implementation_verification` §10 and `security_review` §4. Layering is
  unambiguous from imports; a PSI call-graph cross-check was not performed.
- **Build / tests not re-run in this stage.** Build and test evidence
  (`./gradlew clean build`, 62/62) is reused from `implementation_verification`
  v1, which independently reproduced it. This stage confirmed via `git` that no
  `src/**` file changed after `IMPLEMENTATION`, so that evidence is current.
- **Runtime checks:** no application boot, no live HTTP probing, no live H2/TCP
  reachability test performed by this stage (`runtime_checks` inherited PARTIAL
  from `security_review`).
- **Concurrency (RC-3)** is reasoned from code, not reproduced with a
  concurrent test.
- **No dependency vulnerability scanning** was performed at any stage — no CVE
  claim is made about the resolved dependency tree.
- **Remote source:** no GitHub Issue exists for US-001; remote/local Story
  reconciliation is n/a.
- **Human checks still required:** diff review and PR approval at
  `HUMAN_PR_APPROVAL`.

## 21. Verdict Rationale

Every Acceptance Criterion (AC-001..AC-007) is `RECONCILED` in the
`traceability` matrix. The Specification and both designs align with the
implementation; the differences from the impact analysis are the two disclosed,
justified plan divergences plus one justified supporting class. The approved
plan is materially implemented step-for-step. `implementation_verification` v1
and `security_review` v1 are both current (verified: no `src/**` change after
`IMPLEMENTATION`) and both `PASS`. The artifact chain is intact — every
downstream artifact consumed the current v1 upstreams, no artifact is
`SUPERSEDED`, both human gates are recorded. There are no Critical and no Major
findings; the five Minor and two Informational findings (RC-1..RC-8) are latent,
robustness, documentation, or approved-config items with no reachable exploit
path and no requirement impact, and none maps to a `RECONCILIATION.loop_back`
key. The PR candidate scope is identified (50 include / 12 exclude) with no
secret, no generated database file, and no unrelated **source** change in the
include set; the two harness-hygiene lines already on the branch (RC-8) are
flagged for the human diff reviewer.

No `implementation_drift`, `test_gap`, `plan_gap`, `design_gap`,
`specification_gap`, or `story_source_conflict` condition exists, so no
`CHANGES_REQUIRED` loop-back is warranted. No missing mandatory artifact, no
stale mandatory input, no unresolved blocking Open Decision, no post-review
change to verified code, and no pending human security decision, so no `BLOCKED`
condition applies.

**Verdict: PASS** → advance to `HUMAN_PR_APPROVAL`.

```yaml
result:
  verdict: PASS
  stage: RECONCILIATION
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/reviews/reconciliation/US-001-reconciliation.md
    - docs/reconciliation/US-001-traceability.md
  next_stage: HUMAN_PR_APPROVAL
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "RC-1 (Minor, = SEC F-1 / IMPL_VERIFICATION MF-5): RegistrationRequest is a record - its generated toString() renders the plaintext password. No reachable leak path today (zero logger/System.out/println in src/main; GlobalExceptionHandler never touches the bound DTO; no debug/SQL logging config). Recorded ACCEPTED-WITH-RECOMMENDATION; follow-up: custom masking toString() consistent with Customer.toString()."
    - "RC-2 (Minor, = SEC F-2 / IMPL_VERIFICATION MF-2 / IMPL D-4 / plan-review F-4): implemented 400 (one combined message) and 415 error message strings differ verbatim from the OpenAPI illustrative examples; 409 matches exactly, AC-6 body shape preserved, tests assert status+shape. Implemented messages are more generic - no extra disclosure. Optional openapi-designer example alignment (documentation-direction)."
    - "RC-3 (Minor, = SEC F-3 / IMPL_VERIFICATION MF-3): CustomerService.register is check-then-act (existsByEmail then save); a concurrent duplicate trips uq_customer_email as an unmapped DataIntegrityViolationException -> 500 not 409. Constraint still prevents the duplicate row; no data-integrity/enumeration impact beyond accepted OD-003:A. No AC/design/test covers concurrency; anti-abuse out of scope (OD-005:A). Follow-up Story to map to 409."
    - "RC-4 (Minor, = spec-review F-1 / IMPL_VERIFICATION MF-4 / SEC F-4 / plan-review F-1): docs/decisions/US-001-open-decisions.md v1 still shows OD-001..OD-006 as status: OPEN. Authoritative resolutions (OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A OD-006:A) are in history.jsonl at HUMAN_SPEC_APPROVAL and applied consistently everywhere; human explicitly ruled it non-blocking at HUMAN_PLAN_APPROVAL. us-clarifier to publish v2. Not a RECONCILIATION loop-back target."
    - "RC-5 (Informational, = SEC F-5): default-profile datasource uses AUTO_SERVER=TRUE with sa / blank password - an H2 mixed-mode TCP listener with no credential. APPROVED config (DB design section 8 line 172, plan M-2, impact analysis); local-dev / training data only; test profile isolated in-memory. Future hardening Story: drop AUTO_SERVER or bind loopback + externalize an H2 password."
    - "RC-6 (Informational, = SEC F-6): spring.datasource.password: \"\" + username sa committed in application.yaml. Empty string is the H2 default (not a secret), SC-7 not breached; externalize when a real datasource is introduced."
    - "RC-7 (Informational, = IMPL D-5): schema.sql id column uses 'GENERATED BY DEFAULT AS IDENTITY' with no explicit NOT NULL (H2 2.x implicit; rejects the clause before the identity clause). ddl-auto=validate passes on both profiles; CustomerSchemaTest.idColumnIsBigintNotNull confirms BIGINT + not-nullable."
    - "RC-8 (Minor, scope/hygiene): .gitignore (+ /docs/harness-consistency-review.md) and docs/evidence/.gitignore (+4 harness-* entries) were committed in c72ace3 and ignore pre-existing harness scaffolding, not US-001 output. No code/secret/runtime-artifact impact (security_review section 17). .gitignore also carries M-3 (/data/) so it stays in the PR. Flagged for the human diff reviewer at HUMAN_PR_APPROVAL."
    - "Currency verified: git diff fb833b0..HEAD -- src/ is EMPTY - no production code, test, build, or application-config file changed after IMPLEMENTATION. implementation_verification v1 (PASS) and security_review v1 (PASS) both remain current; no BLOCKED staleness condition."
    - "Traceability: AC-001..AC-007 all RECONCILED in docs/reconciliation/US-001-traceability.md (this stage owns it). 62 tests / 0 failures across 7 suites, independently reproduced by implementation-verifier v1."
    - "PR candidate scope: 50 include (28 code/build/config + 20 US-001 workflow-doc artifacts incl. this reconciliation pair + 2 harness state files) / 12 exclude (3 runtime artifacts + 3 IDE-local + 6 pre-existing harness scaffolding). No secret, no generated DB file, no unrelated source change in the include set. Counts as-of-artifact-write; docs/workflow/{history.jsonl,workflow-state.yaml} change again when this transition is recorded."
    - "Accepted deviations (from IMPLEMENTATION, re-confirmed): D-1 exception/InvalidPasswordException (FR-6 service re-check; plan C-12 left the type unspecified; domain exception in exception package, mapped to 400, not reachable from the HTTP flow); D-2 CustomerPersistenceTest uses @SpringBootTest not the indicative @DataJpaTest (all three C-T2 scenarios covered); MF-1 the only change to a test-writer-owned test is a benign 2-char BCrypt-literal fix in CustomerSchemaTest (git diff f6e7eae..fb833b0 -- src/test confirms), no assertion/scenario/outcome changed."
```
