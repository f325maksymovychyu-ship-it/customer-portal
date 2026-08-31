---
artifact_type: test_generation_report
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T11:25:00Z
updated_at: 2026-08-31T11:25:00Z
produced_by: test-writer
inputs:
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
  - path: docs/tests/US-001-test-strategy.md
    version: 1
  - path: docs/tests/US-001-ac-test-matrix.md
    version: 1
supersedes: null
---

# Test-Generation Report — US-001 Customer Registration

## 1. Story

US-001 Customer Registration. Tests authored at `TEST_WRITING`, before
`IMPLEMENTATION`. Red-phase evidence below.

## 2. Environment

| Item | Value |
|---|---|
| Build tool | Gradle 9.7.1 (wrapper) |
| JDK (runtime) | Temurin 25; Gradle toolchain pinned to Java 21 (`build.gradle.kts`) |
| Framework | Spring Boot 4.1.1, Spring Framework 7.0.8 |
| Test libs | JUnit Jupiter 6.0.3, AssertJ 3.27.7, JSONPath 2.10.0, spring-security-test 7.1.1 (all via `spring-boot-starter-*-test`) |
| DB (test) | isolated in-memory H2 `jdbc:h2:mem:us001` (profile `test`) |
| Command | `./gradlew test --console=plain` |

## 3. Files created

### Test sources (`src/test/java/org/example/customerportal/`)

| Path | Level | Tests | Covers |
|---|---|---|---|
| `registration/CustomerRegistrationApiTest.java` | web / contract (`@SpringBootTest` + `MockMvc`) | 22 | AC-001, AC-002, AC-003, AC-005, AC-006, AC-007, unknown/malformed JSON, AC-6 error body, SC-9 hygiene |
| `persistence/CustomerSchemaTest.java` | schema (`@SpringBootTest` + `JdbcTemplate` on `INFORMATION_SCHEMA`) | 10 | AC-002 (unique), AC-003 (email length), AC-004 (`password_hash`, no plaintext column), NFR-4, BR-007, SEC-5 |
| `security/RegistrationSecurityPostureTest.java` | security (`@SpringBootTest` + `MockMvc`) | 4 | SEC-1, SEC-7/OD-002:B, SC-4 (F-3 tripwire), SC-6/SEC-9 |

### Test fixtures / resources

| Path | Purpose |
|---|---|
| `src/test/resources/application-test.yaml` | profile `test`: isolated in-memory H2, `ddl-auto=validate`, `sql.init.mode=embedded`, H2 console off, `fail-on-unknown-properties=true`, `open-in-view=false` (plan C-17) |

### Workflow artifacts

| Path | |
|---|---|
| `docs/tests/US-001-test-strategy.md` | v1 |
| `docs/tests/US-001-ac-test-matrix.md` | v1 (authoritative AC→test map) |
| `docs/evidence/US-001-test-generation-report.md` | this file |

## 4. Files modified

None. `build.gradle.kts` is **not** touched — `spring-boot-starter-validation`
(plan M-1) is `IMPLEMENTATION`'s to add; this suite needs no Bean-Validation API
on the classpath because it asserts at the HTTP and schema boundaries.
`CustomerPortalApplicationTests` is **not** modified — `@ActiveProfiles("test")`
on it is plan edit M-4, owned by `IMPLEMENTATION`.

## 5. Design choice — why boundary-level tests

`test-writer`'s red-phase rule rejects a failing test caused by a compile error
or an invalid import. Before `IMPLEMENTATION` there are no production types to
import, so every test here is written against the running HTTP API (`MockMvc`)
or the live schema (`INFORMATION_SCHEMA`) and imports no
`org.example.customerportal` production class. The tree compiles today and will
keep compiling as production code lands.

Unit-level tests that must import a production type — `PasswordPolicyValidatorTest`
(plan C-T1, the byte-precise policy matrix), `CustomerServiceTest` (C-T3),
`CustomerPersistenceTest` (C-T2) — are specified in full in the AC-test matrix
(§1, §4 `DEFERRED → IMPLEMENTATION` rows) and handed to `IMPLEMENTATION`, whose
plan already gates execution steps 4 and 6 on them. Every mandatory AC still has
executable coverage now.

## 6. Execution evidence

`./gradlew test` — **BUILD FAILED** (expected: red phase).

```
37 tests completed, 33 failed
```

| Test class | tests | passed | failed |
|---|---|---|---|
| `CustomerPortalApplicationTests` | 1 | 1 | 0 |
| `registration.CustomerRegistrationApiTest` | 22 | 0 | 22 |
| `persistence.CustomerSchemaTest` | 10 | 1 | 9 |
| `security.RegistrationSecurityPostureTest` | 4 | 2 | 2 |

### 6.1 Passing existing tests (regression)

| Test | Result | Note |
|---|---|---|
| `CustomerPortalApplicationTests.contextLoads` | PASS | Context boots with Spring Security + Spring Data JPA on the classpath and the `test` profile datasource. Baseline (before this suite) also passed — no regression. |

### 6.2 Expected failing new tests (red — feature not implemented)

**`registration.CustomerRegistrationApiTest` — all 22 fail identically:**

```
java.lang.AssertionError: Status expected:<201|400|409|415> but was:<403>
```

Cause: with no `SecurityConfig`, Spring Security's default filter chain enforces
CSRF and rejects every tokenless `POST` with `403` before it reaches a handler —
and there is no controller, DTO, validator or exception advice yet. After
`IMPLEMENTATION` (registration path `permitAll` + CSRF-exempt per OD-002:B, plus
the controller / validation / `@RestControllerAdvice`) these resolve to the
asserted `201` / `400` / `409` / `415`. Failure is uniform and consistent with
"not implemented".

**`persistence.CustomerSchemaTest` — 9 fail:**

```
AssertionError: Expecting actual not to be null        (column lookups → no `customer` table)
AssertionFailedError: expected: 1 but was: 0           (customerTableExists)
AssertionError: 0 to be greater than or equal to 1     (emailHasAUniqueConstraint)
BadSqlGrammarException: ... Table "CUSTOMER" not found  (duplicate-email INSERT)
```

Cause: `src/main/resources/schema.sql` and the `customer` table do not exist
yet. After `IMPLEMENTATION` creates the hand-written schema (db-design §8.1) and
`ddl-auto=validate` confirms the entity agrees with it, these pass.

**`security.RegistrationSecurityPostureTest` — 2 fail:**

| Test | Failure | Resolves when |
|---|---|---|
| `registrationEndpointIsReachableWithoutAuthentication` | `Response status Expected: not <403> but: was <403>` | registration path is made `permitAll` |
| `registrationPostIsAcceptedWithoutACsrfToken` | `Response status Expected: not <403> but: was <403>` | the scoped CSRF exemption (OD-002:B) is added |

### 6.3 New tests already passing at TEST_WRITING (guards / already-satisfied)

| Test | Why it passes now | Still valid after IMPLEMENTATION? |
|---|---|---|
| `persistence.CustomerSchemaTest.noPlaintextPasswordColumnExists` | No `customer` table ⇒ no `PASSWORD` column | Yes — the implemented schema has only `password_hash` (BR-005) |
| `security.RegistrationSecurityPostureTest.protectedRouteReturns401WhenUnauthenticated` | Default security returns `401` for all routes | Yes — `anyRequest().authenticated()` + `HttpStatusEntryPoint(401)` keeps it `401`. **Tripwire:** if `IMPLEMENTATION` uses form login this becomes `302` and fails → `SECURITY_REVIEW` arbitrates (plan-review F-3 / risk R-2) |
| `security.RegistrationSecurityPostureTest.h2ConsoleIsNotExposed` | No H2-console servlet registered | Yes — `spring.h2.console.enabled=false` in every profile (SC-6) |

None of these three is a weak assertion masking a missing mandatory behavior:
each states a property that must remain true, and every AC they touch also has a
`RED` row.

### 6.4 Unexpected failures

None. Every failure is a status/assertion mismatch from missing production
behavior or a missing schema object. No compile error, no invalid import, no
context-load failure, no bad fixture, no missing dependency.

## 7. Untested / deferred Acceptance Criteria

None untested. Deferred-to-`IMPLEMENTATION` depth (owned by plan skeletons,
fully specified in the AC-test matrix):

| Deferred test | AC | Owner |
|---|---|---|
| `validation.PasswordPolicyValidatorTest` — byte-length boundary matrix + class rules + safe message | AC-006 | plan C-T1 (execution step 6) |
| `service.CustomerServiceTest` — normalization, service-layer re-check (FR-6), BCrypt verify, duplicate guard | AC-001, AC-002, AC-004 | plan C-T3 (execution step 8) |
| `persistence.CustomerPersistenceTest` — `created_at` not updatable, UTC offset, `EnumType.STRING`, `ddl-auto=validate` agreement | AC-004, NFR-4, BR-007 | plan C-T2 (execution step 4) |

## 8. Open Decisions

None open. OD-001..OD-006 are resolved (OD-001:A, OD-002:B, OD-003:A, OD-004:A,
OD-005:A, OD-006:A — `history.jsonl` at `HUMAN_SPEC_APPROVAL`). Non-blocking
documentation lag: `docs/decisions/US-001-open-decisions.md` v1 still marks them
`OPEN`; owned by `us-clarifier`.

## 9. Overall result

`verdict: PASS`. The executable suite covers every mandatory Acceptance
Criterion, compiles, runs, and fails **only** for missing production behavior
(red phase verified). The regression smoke test still passes. Three new tests
pass now as deliberate guards. `test-strategy` and `ac-test-matrix` exist with
valid front matter. No blocking Open Decisions.

Next stage: `IMPLEMENTATION` (`springboot-implementor`).
