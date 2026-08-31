---
artifact_type: plan_review
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T11:05:00Z
updated_at: 2026-08-31T11:05:00Z
produced_by: plan-reviewer
inputs:
  - path: docs/plans/US-001-implementation-plan.md
    version: 1
  - path: docs/stories/US-001-register-customer.md
    version: null
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
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
supersedes: null
critical_findings: 0
major_findings: 0
minor_findings: 5
---

# Plan Review — US-001 Customer Registration

## 1. Review Summary

**Verdict: PASS.** The Implementation Plan (`docs/plans/US-001-implementation-plan.md`
v1) is safe, complete, traceable, and executable. It implements exactly the
approved Specification, the approved API and database designs, and the predicted
impact surface, and introduces no new business behavior. Every Acceptance
Criterion (AC-001..AC-007, plus the derived unknown/malformed-JSON case, the
UTC-audit NFR, and the security posture) maps to at least one planned file and
one planned verification. Layering, DTO/entity separation, the single
`@RestControllerAdvice`, validation split, configuration boundaries, and package
ownership all follow `architecture.md` / `package-map.md`. Security is addressed
explicitly (BCrypt encoder bean, dual-layer password policy, credential
non-exposure, scoped public matcher + CSRF exemption, H2 console off).

- **Plan readiness:** ready for `HUMAN_PLAN_APPROVAL`.
- **Principal risks:** all are known Spring Boot 4 / H2 2.x integration trip
  points already carried from the Impact Analysis (R-1 `ddl-auto=validate` type
  match, R-2 Security filter-chain scope, R-3 72-byte BCrypt bound). Each is
  owned by IMPLEMENTATION / IMPLEMENTATION_VERIFICATION / SECURITY_REVIEW with a
  concrete mitigation in the plan.
- **Recommended next action:** proceed to `HUMAN_PLAN_APPROVAL`. The human should
  confirm the one new dependency (`spring-boot-starter-validation`, mandated by
  AD-5) and note the five Minor findings below, none of which block execution.

## 2. Reviewed Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Implementation Plan | `docs/plans/US-001-implementation-plan.md` | 1 | DRAFT |
| Story | `docs/stories/US-001-register-customer.md` | (unversioned) | IN_PROGRESS |
| Specification | `docs/specifications/US-001-spec.md` | 1 | APPROVED |
| Specification Review | `docs/reviews/specifications/US-001-spec-review.md` | 1 | APPROVED (PASS) |
| API Design | `docs/designs/api/US-001-api-design.md` | 1 | APPROVED |
| OpenAPI Contract | `docs/designs/api/US-001-openapi.yaml` | 1 | APPROVED |
| DB Design | `docs/designs/database/US-001-db-design.md` | 1 | APPROVED |
| Entity Model | `docs/designs/database/US-001-entity-model.md` | 1 | APPROVED |
| Design Review | `docs/reviews/designs/US-001-design-review.md` | 1 | APPROVED (PASS) |
| Impact Analysis | `docs/impact-analysis/US-001-impact-analysis.md` | 1 | DRAFT (PASS; no review stage follows IMPACT_ANALYSIS) |
| Open Decisions | `docs/decisions/US-001-open-decisions.md` | 1 | DRAFT (see F-1) |

Architecture / product references consulted: `architecture.md` (AD-1..AD-8),
`package-map.md`, `api-conventions.md` (AC-1..AC-9), `persistence-conventions.md`
(PC-1..PC-9), `security-conventions.md` (SC-1..SC-9 + policy block),
`business-rules.md` (BR-001..BR-007), `non-functional-requirements.md`
(NFR-001..NFR-008). Repository state inspected: `build.gradle.kts` (Spring Boot
4.1.1, data-jpa + security + webmvc + h2 + lombok present; **no** validation
starter), `src/main/resources/application.yaml` (name only, no datasource),
`CustomerPortalApplicationTests` (`@SpringBootTest` context load, no profile),
`.gitignore` (no `/data/` entry), and the source tree (only
`CustomerPortalApplication` + the smoke test exist). All confirm the plan's
greenfield premise.

### Artifact chain / staleness

The plan's `inputs` front matter and its Source Artifacts table reference spec
v1, spec-review v1, api-design v1, openapi v1, db-design v1, entity-model v1,
design-review v1, impact-analysis v1, and open-decisions v1. Every one is the
current version; none is `SUPERSEDED` or `ARCHIVED`. No downstream artifact
records consuming an older upstream version. The plan is not stale — review is
not `BLOCKED` on that basis.

## 3. Strengths

- **Complete file-level decomposition** (C-1..C-17, M-1..M-4) with a
  Responsibility column and a Traceability column on every row, tied to FR / AC /
  AD / PC / SC / SEC ids and the design sections.
- **Impact-Analysis reconciliation table** — every predicted file from the
  Impact Analysis is either created as-is or its consolidation/rename is
  explained (JacksonConfig folded into a property, PasswordEncoder bean in
  `SecurityConfig`, mapping inside `CustomerService`, `FieldError` →
  `ApiFieldError`, no `local` profile). No silent scope change.
- **Planning decisions PD-1..PD-6** make the six items the design review and
  impact analysis deferred concrete (unknown-field mechanism, 72-**byte**
  password bound, mapping location, encoder bean location, DTO naming, H2
  timestamp token) with a rationale and a convention citation each.
- **Execution Order** — 14 ordered steps, each with an explicit observable
  completion criterion, following the conventional contract → tests → persistence
  → service → API → security → validation → build → verification chain.
- **Validation Strategy** and **Testing Strategy** tables give deterministic
  pass criteria and map test levels to ACs, while correctly deferring the
  definitive suite to `test-writer` at TEST_WRITING.
- **Risks section** carries all eight Impact-Analysis risks forward with a
  per-risk mitigation and an owner stage; none needs a new human decision.
- **Scope discipline** — `GET /api/v1/customers/{id}`, login, and rate limiting
  are explicitly excluded (R-8), and the `Location` header target is noted as a
  future Story.

## 4. Scope Review

| Dimension | Assessment |
|---|---|
| Required scope | Fully covered. One endpoint `POST /api/v1/customers`; server-side email + password validation; case-insensitive duplicate → `409`; BCrypt hash; enabled `CUSTOMER` account; UTC audit timestamps; `201` + `Location` + credential-free body. |
| Missing scope | None identified. All FR-1..FR-11 and AC-001..AC-007 have a home in the file list and the execution order. |
| Scope expansion | None. No speculative abstraction, no mapper library, no extra endpoint, no `local` profile, no runtime OpenAPI serving. `ADMIN` role value exists in the `Role` enum only for model completeness (entity-model §2.2) and is never written. |
| Out-of-Scope compliance | Compliant. Spec §10 items (login, password reset, email verification, MFA, activation, profile, rate limiting, admin account management) are all absent from the plan. R-8 explicitly guards against implementing the `Location` target or a `UserDetailsService`. |

## 5. Requirements Traceability

| AC | Spec section | Design artifact | Impact Analysis | Plan step / files | Planned verification |
|---|---|---|---|---|---|
| AC-001 Successful registration | FR-1/FR-5/FR-8/FR-9, §5 | API §4 + OpenAPI `201`; DB §4, entity-model §4.1 | §7, §8, §16 | Steps 4/8/9; C-13, C-12, C-3, C-2, C-1, C-7, C-16 | web-layer (C-T4) + service (C-T3) + persistence (C-T2) + IT (C-T5) |
| AC-002 Unique email (case-insensitive) | FR-4, §6.1, §8 | API §4.3 `409`; DB §5 `uq_customer_email` | §7, §8 | Step 8; C-12, C-10, C-11, C-3, C-16 | service (C-T3) + IT (C-T5) + persistence (C-T2) |
| AC-003 Email validation | FR-3, §6.1 | OpenAPI `RegistrationRequest.email`; API §4.1 | §7 | Steps 6/9/11; C-6, C-11 | validation/web (C-T4) |
| AC-004 Password storage | FR-5/FR-6/FR-7, §7 | DB §4.2/§7; entity-model §4.1 | §8, §9 | Steps 8/10; C-12, C-14, C-2 | service (C-T3) + persistence (C-T2) |
| AC-005 Secure response | FR-7/FR-8, §7 | API §4.2 `CustomerResponse`; OpenAPI schema | §7, §9 | Steps 7/8; C-7, C-12 | web-layer + contract assertion (C-T4) |
| AC-006 Password policy | FR-3/FR-6, §6.2 | API §4.1; design-review D-1/D-6 | §7, §10 | Steps 6/8; C-4, C-5, C-12 | validation unit (C-T1, incl. multi-byte boundary) + web (C-T4) |
| AC-007 Media type | FR-2, §8 | OpenAPI `415`; API §4.3 | §7 | Steps 9/11; C-13 (`consumes`), C-11 | web-layer (C-T4) |
| Derived: unknown / malformed JSON | §6.3 | API §3, OpenAPI `additionalProperties: false` | §7 (derived), R-4 | Steps 3/11; M-2 (PD-1), C-17, C-11 | web-layer (C-T4) |
| NFR-4 / BR-007 explicit mapping + UTC audit | §9 NFR-4 | DB §4, entity-model §2/§3 | §8 | Steps 3/4; C-2, C-15, C-16 | persistence (C-T2) |
| SEC-1/SEC-2 / SC-4/SC-5 security posture | §7 | API §6 | §9 | Step 10; C-14 | security (C-T6) |

Every AC is covered by at least one file and one verification activity. No
orphan plan step (each C-/M- row cites a requirement). No unmet AC.

## 6. Impact Analysis Coverage

| Impact Analysis item (confidence) | Plan disposition |
|---|---|
| All app packages are **Create** (`controller`, `service`, `repository`, `model.*`, `validation`, `security`, `exception`, `config`) — HIGH | Covered — C-1..C-15 create exactly these packages; no package outside `package-map.md` (AD-8, D-9). |
| `controller/CustomerController` — HIGH | Covered — C-13. |
| `service/CustomerService` — HIGH | Covered — C-12 (name resolved from "`CustomerService` or `RegistrationService`" to `CustomerService`). |
| `repository/CustomerRepository` (`existsByEmail` / `findByEmail`) — HIGH | Covered — C-3 (both methods). |
| `model/entity/Customer` + `Role` — HIGH | Covered — C-2, C-1. |
| `model/request/RegistrationRequest` — HIGH | Covered — C-6. |
| `model/dto/CustomerResponse`, `ErrorResponse`, `FieldError` — HIGH | Covered — C-7, C-9, C-8 (`FieldError` → `ApiFieldError`, PD-5; serialized shape unchanged). |
| `validation/ValidPassword` + `PasswordPolicyValidator` — HIGH | Covered — C-4, C-5 (length in **bytes**, PD-2). |
| `security/SecurityConfig` + `PasswordEncoder` bean — HIGH | Covered — C-14 (both in one class, PD-4; `package-map.md` allows the encoder bean in `security`). |
| `exception/DuplicateEmailException` + `GlobalExceptionHandler` — HIGH | Covered — C-10, C-11. |
| `config/JpaAuditingConfig` — HIGH | Covered — C-15 (`@EnableJpaAuditing` off the main class, AD-7; UTC `DateTimeProvider`). |
| `config/JacksonConfig` *(only if not handled via DTO/advice)* — MEDIUM | **Not created** — replaced by `spring.jackson.deserialization.fail-on-unknown-properties: true` in `application.yaml` + test profile (PD-1). Impact Analysis marked this class conditional; the property is minimal and AD-7-compliant. Justified difference. |
| `schema.sql` — HIGH | Covered — C-16 (DDL from DB design §8.1). |
| `src/test/resources/application-test.yaml` — MEDIUM | Covered — C-17. |
| `build.gradle.kts` add `spring-boot-starter-validation` — HIGH | Covered — M-1 (BOM-managed, no explicit version). |
| `application.yaml` datasource + JPA + naming + console off — HIGH | Covered — M-2. |
| `.gitignore` `/data/` — MEDIUM | Covered — M-3 (repo `.gitignore` currently has no `/data/` entry — confirmed; M-3 adds it). |
| `CustomerPortalApplicationTests` may need `@ActiveProfiles("test")` — MEDIUM | Covered — M-4. |
| Risks R-1..R-8 | All carried into the plan's Risks section with mitigations + owner stages. |

No HIGH- or MEDIUM-confidence affected area is ignored. The two divergences
(no `JacksonConfig`, no `local` profile) are stated with justification and do
not change scope.

## 7. Architecture Review

| Check | Result |
|---|---|
| Layering `controller → service → repository → model.entity` (AD-2) | Pass — C-13 delegates to C-12; C-12 uses C-3; C-3 depends only on `Customer`. |
| Controller has no business logic, no repository access, no entity in a signature (AD-2, AD-4) | Pass — C-13 explicitly: "No error handling, no repository access, no entity in signature". |
| Service owns transactions (AD-3) | Pass — C-12 `@Transactional` on the public `register(...)` write method (not private / self-invoked). |
| DTO / entity boundary (AD-4) | Pass — request binds to `model.request.RegistrationRequest`; responses are `model.dto` records; `Customer` never in a controller signature or a body; mapping in the service (PD-3, permitted by AD-4). |
| Validation split (AD-5) | Pass — request-shape via Bean Validation on C-6 + custom `@ValidPassword` (C-4/C-5 in `validation`); business-rule (uniqueness) + password re-check in C-12 before persistence. |
| Single `@RestControllerAdvice` (AD-6, AC-9) | Pass — C-11 is the only advice; C-13 builds no error bodies; `DuplicateEmailException` (C-10) carries no HTTP concept. |
| Configuration boundaries (AD-7) | Pass — `JpaAuditingConfig` (C-15) in `config`; `SecurityConfig` (C-14) in `security`; all settings in `application.yaml` / test profile, none hard-coded. |
| Package ownership (`package-map.md`) | Pass — every new class lands in a mapped package; no feature package (`customer`) is introduced (D-9, AD-8). |
| Reuse over duplication (AD-8) | Pass — no mapper class, one `SecurityConfig` for both beans, `application.yaml` reused; new dependency is the AD-5-mandated first-party starter only. |

No direct controller→repository access, no business logic in a controller, no
persistence logic outside the repository, no entity used as an API type, no
unjustified package. No architecture finding.

## 8. API Review

| Check | Result |
|---|---|
| Contract alignment | Pass — plan targets the approved OpenAPI v1 operation `registerCustomer` verbatim; no invented behavior. Step 1 re-confirms the contract before coding. |
| Status codes | Pass — `201` (C-13), `400` (C-11: `MethodArgumentNotValidException` / `HandlerMethodValidationException` / `HttpMessageNotReadableException`), `409` (C-11: `DuplicateEmailException`), `415` (C-11: `HttpMediaTypeNotSupportedException`; C-13 `consumes = application/json`), `500` (C-11 fallback, no leak). |
| Request / response models | Pass — `RegistrationRequest { email, password }` as a `record` with no unknown-field annotation (PD-1); `CustomerResponse { id, email, role, createdAt }` matches OD-004:A and the OpenAPI schema exactly. |
| Validation behavior | Pass — `@NotBlank` + `@Email` + `@Size(max = 254)` on `email`; `@NotBlank` + `@ValidPassword` on `password`; policy re-checked in the service (FR-6). |
| Error mapping | Pass — all mapping in C-11; `ErrorResponse` maps to the AC-6 body; `ApiFieldError` serializes to the contract `FieldError` shape; messages never echo the submitted value (SC-9). |
| Auth / authorization | Pass — public, no authorization; CSRF exemption and the public matcher are both scoped to `POST /api/v1/customers` (C-14, OD-002:B, SC-5). |
| Compatibility | Pass — purely additive; no existing contract exists to break (AC-1). |
| Planned contract tests | Pass — C-T4 (web-layer) covers `201` + `Location` + body, `400` for email and password, `415`, unknown field, malformed JSON; C-T5 end-to-end IT. |

See F-3 (unauthenticated response for protected routes) and F-4 (single advice
branch, two OpenAPI example messages) — both Minor.

## 9. Persistence Review

| Check | Result |
|---|---|
| Entity changes | Pass — one entity `Customer` (C-2) / table `customer`; `Role` enum (C-1); matches entity-model §2 field-by-field. |
| Explicit constraints / nullability / length | Pass — C-2 mandates explicit `@Column` on every field; `email` length 254, `password_hash` length 60, `role` length 20; all `nullable = false`; `createdAt` `updatable = false`. |
| Uniqueness | Pass — table-level `@UniqueConstraint(name = "uq_customer_email", columnNames = "email")` (declared once, PC-4 either/or) + plain `UNIQUE` in `schema.sql`; case-insensitivity via service `toLowerCase(Locale.ROOT)` before check and insert (OD-006:A). |
| Indexes | Pass — `uq_customer_email` serves the `email` lookup (PC-7); no redundant `ix_customer_email`; no FKs. |
| Schema initialization | Pass — hand-written `schema.sql` (C-16) from DB design §8.1; `spring.sql.init.mode=always` (main) / `embedded` (test); `ddl-auto=validate` in both profiles (M-2, C-17). No `create` / `update` shortcut (SC-8, PC-2). |
| Migration implications | None — new table, no data, no migration tool (PC-2). |
| Auditing | Pass — `@EntityListeners(AuditingEntityListener.class)` on C-2; `@EnableJpaAuditing` on C-15 with a UTC `DateTimeProvider` (BR-007, PC-6). |
| Persistence tests | Pass — C-T2 (`@DataJpaTest`, `test` profile) asserts column constraints, `uq_customer_email` collision, `EnumType.STRING`, UTC audit values, `created_at` not updatable, and `ddl-auto=validate` agreement. |

R-1 (`OffsetDateTime` ↔ `TIMESTAMP WITH TIME ZONE` strict match; H2 2.x IDENTITY
clause ordering) is correctly gated at execution step 4 (clean `validate` boot
before proceeding) and owned by IMPLEMENTATION / IMPLEMENTATION_VERIFICATION
(PD-6). Acceptable — this is an execution-time integration detail, not a plan
defect.

## 10. Security Review

| Area | Result |
|---|---|
| Authentication boundary | Addressed — C-14 `SecurityFilterChain`: `requestMatchers(POST, "/api/v1/customers").permitAll()` + `anyRequest().authenticated()`. See F-3: the plan does not state the response for an unauthenticated call to a protected route (401 vs form-login redirect), which C-T6 asserts. |
| Authorization boundary | Addressed — none required for the operation (`x-authorization: none`); no other route added. |
| Password hashing | Addressed — `BCryptPasswordEncoder` bean in `security` (C-14, PD-4); no-op / plaintext encoder forbidden (SC-1, SEC-2); C-T6 asserts the encoder bean is present and not a no-op. |
| Password exposure | Addressed — `password` only on the inbound record; `toString()` on `Customer` excludes `passwordHash` (C-2); `CustomerResponse` has no credential field (C-7); service discards plaintext after `encode` (C-12). |
| Dual-layer policy | Addressed — `@ValidPassword` (C-4/C-5) at the request layer **and** a byte-length + char-class re-check in `CustomerService` before hashing (C-12, FR-6, PD-2). |
| Input validation | Addressed — server-side only; no reliance on framework default handling (NFR-002); unknown fields rejected explicitly (PD-1). |
| Sensitive-data logging | Addressed — C-4/C-5 static generic message (no value echo, SC-9); C-11 fallback `500` leaks nothing; R-7 tracked. |
| H2 console | Addressed — `spring.h2.console.enabled=false` in `application.yaml` (M-2) and the test profile (C-17), every profile (SC-6, SEC-9). |
| Dev-only / insecure config | None — file H2 for local, isolated in-memory for tests; `ddl-auto=validate`; no permissive security rule. |
| Secret management | Addressed — no secret introduced or committed; `/data/` git-ignored (M-3); H2 `sa` / empty is the H2 default, not a secret (SEC-11). |
| Security tests | Addressed — C-T6: public path reachable unauthenticated, a second path still denied, CSRF exemption scoped to the one path. |

The Story handles passwords, credentials, and account state, and the plan
contains explicit security steps (execution step 10 + C-14 + C-4/C-5 + the
Validation Strategy "Credential safety" and "Security posture" rows). SEC review
requirement satisfied — no blocking security finding. Final verification of the
filter chain and the CSRF scope is owned by SECURITY_REVIEW (R-2).

## 11. Testing and Validation Review

- **AC coverage:** every AC (§5 table) maps to at least one planned test level.
  The multi-byte password boundary vector (>72 bytes, ≤72 chars) is explicitly
  called out for C-T1 (R-3 / PD-2 / design-review D-6).
- **Test categories:** unit (validation), service slice, `@DataJpaTest`
  persistence, `@WebMvcTest` / full-slice web-layer, `@SpringBootTest` end-to-end
  IT, security, plus the `contextLoads` regression — matches NFR-005
  (happy-path + validation + security) and the Impact Analysis §10 prediction.
- **Negative scenarios:** invalid email, each missing password character class,
  length boundaries, duplicate email (same and different case), non-JSON
  `Content-Type`, unknown JSON field, malformed JSON, credential absent from the
  response — all listed.
- **Deterministic validation:** the Validation Strategy table gives an
  observable pass criterion per concern; execution step 13 requires
  `./gradlew clean build` green (NFR-7) and a clean `ddl-auto=validate` boot;
  MCP `idea-validation` / `idea-semantic-analysis` are named for
  IMPLEMENTATION_VERIFICATION.
- **Structure vs behavior:** the planned tests assert observable outcomes
  (status codes, headers, persisted values, hash verification), not
  implementation shape.
- **Ownership:** the plan correctly states `test-writer` owns the definitive
  AC→test matrix at TEST_WRITING and that C-T1..C-T6 are indicative skeletons
  for execution-order planning only — it does not pre-empt that stage.
- **Missing evidence:** none blocking. See F-2 — some per-step completion
  criteria in steps 6–10 name assertions that only pass once the advice (step
  11) and security wiring (step 10) exist; the step-13 full build is the real
  gate.

## 12. Execution Order Review

The 14-step order is dependency-safe:

1–2. Contract confirmation, then the build dependency (M-1) — nothing depends on
code yet.
3. Persistence config + `schema.sql` + test profile + `.gitignore` — before any
entity.
4. Entity + repository + `Role` + JPA auditing — gated on a clean
`ddl-auto=validate` boot (closes R-1, R-6 here).
5. Test skeletons (or consume `test-writer`'s suite) — red before implementation.
6–7. Validation constraint + request DTO, then domain exception + DTOs.
8. Service — needs the entity, repository, encoder, DTOs.
9. Controller — needs the service and DTOs.
10. Security wiring — needs the encoder bean location fixed (it is, C-14).
11. Exception handler — consolidates all error mapping; closes the error-path
assertions.
12. Adjust the smoke test (M-4).
13. Full `./gradlew clean build` + `validate` boot (NFR-7).
14. Documentation reconciliation prep for the implementation report.

This differs from the canonical guidance only by placing the exception handler
(step 11) after the controller (step 9) and security (step 10); the plan states
the reason (consolidate every error branch once the producing paths exist) and
the step-13 build is the gate. Acceptable per the "approve a different order when
the plan explains why" rule. See F-2 for the per-step criteria wording.

## 13. Reviewability

The change is one reviewable Pull Request: a single Gradle module, ~17 new files
+ 4 small modifications, one first-party dependency, one new table, one endpoint.
No unrelated module, no broad refactor, no multiple independent capabilities, no
discretionary third-party library. No decomposition needed.

## 14. Findings

### F-1 — `open_decisions.md` v1 still marks OD-001..OD-006 as `status: OPEN`

- **Severity:** Minor (advisory; non-blocking)
- **Location:** `docs/decisions/US-001-open-decisions.md` v1; plan Goal
  paragraph and Source Artifacts table acknowledge it.
- **Problem:** All six Open Decisions were resolved by the human at
  `HUMAN_SPEC_APPROVAL` (`history.jsonl` 2026-08-31T07:48:48Z:
  `OD-001:A OD-002:B OD-003:A OD-004:A OD-005:A OD-006:A`) and are applied
  consistently across the Specification, both designs, the design review, the
  impact analysis, and this plan. The `open_decisions.md` body was never
  re-published, so it still shows `OPEN` and the recommended options as
  "non-binding".
- **Why it matters:** `AGENTS.md` treats a literal `OPEN` / "Open Decision"
  marker in an input artifact as a blocker. Every stage so far has correctly
  read through the lag, but it is a standing trip hazard for a fresh reviewer or
  a future stage, and it makes `open_decisions.md` disagree with the
  authoritative record.
- **Required correction:** none by the Planner. `us-clarifier` (the owner) should
  publish `open_decisions.md` v2 with each entry `status: RESOLVED` and the
  chosen option recorded. Tracked as a carried non-blocking finding.
- **Loop-back target:** none. Not `IMPLEMENTATION_PLANNING` (does not own the
  file) and not a genuine blocker — the decisions are made and consistently
  applied, so this does not warrant `BLOCKED`.

### F-2 — Per-step completion criteria in steps 6–10 reference tests that pass only after steps 10–11

- **Severity:** Minor
- **Location:** plan "Execution Order" steps 6, 8, 9 (and the step-9 error-path
  assertions).
- **Problem:** e.g. step 9 lists "`415` … unknown field and malformed JSON →
  `400`" as its evidence, but those paths are only wired in step 11 (advice) and
  step 10 (security context for `@WebMvcTest`).
- **Why it matters:** read literally, a step cannot be "done" by its own
  criterion until later steps land; this can confuse progress tracking during
  IMPLEMENTATION.
- **Required correction:** the Planner may add one sentence that the per-step
  criteria are cumulative and the authoritative gate is step 13
  (`./gradlew clean build` green). No structural change needed.
- **Loop-back target:** `IMPLEMENTATION_PLANNING` (cosmetic; not required for
  approval).

### F-3 — C-14 does not specify the unauthenticated response for protected (non-registration) routes

- **Severity:** Minor
- **Location:** plan C-14; execution step 10; C-T6.
- **Problem:** With `spring-boot-starter-security` on the classpath and a custom
  `SecurityFilterChain` that only sets `permitAll()` for the registration path
  and `anyRequest().authenticated()`, the default behavior for an unauthenticated
  request to another path is a `302` redirect to a generated login page, not a
  `401`. C-T6's evidence line ("an arbitrary second path … returns `401`") is
  only well-defined if the chain also configures an entry point (e.g.
  `httpBasic`, or `exceptionHandling` with an `HttpStatusEntryPoint(UNAUTHORIZED)`,
  or disables form login).
- **Why it matters:** ambiguous acceptance evidence for the security test;
  Spec §8 says `401` is "not applicable" only for the public endpoint, and SEC-1
  requires the rest to stay deny-by-default (a `302` is still a denial, so this
  is not a scope defect — just an under-specified assertion).
- **Required correction:** the Planner should state the intended unauthenticated
  response for protected routes in C-14 (recommend `401` via an
  `HttpStatusEntryPoint`, consistent with a JSON API), so C-T6 and
  SECURITY_REVIEW have a fixed target. Alternatively, defer the exact assertion
  to `test-writer` / SECURITY_REVIEW and soften C-T6's wording.
- **Loop-back target:** `IMPLEMENTATION_PLANNING` (small clarification; not
  required for approval — SECURITY_REVIEW (R-2) owns final verification).

### F-4 — One `HttpMessageNotReadableException` advice branch vs two distinct OpenAPI example messages

- **Severity:** Minor
- **Location:** plan C-11 / PD-1; `US-001-openapi.yaml` `400` examples
  `unknownField` ("Request body contains an unknown field.") and `malformedJson`
  ("Malformed JSON request body.").
- **Problem:** PD-1 routes both malformed JSON and an unknown property through a
  single `HttpMessageNotReadableException` mapping, implying one `message`, while
  the contract illustrates two different messages.
- **Why it matters:** the OpenAPI `examples` are illustrative, not normative, and
  the AC-6 body **shape** is preserved either way, so this is not a contract
  break — but IMPLEMENTATION_VERIFICATION should know the messages may be
  unified.
- **Required correction:** none required. If distinct messages are wanted, the
  advice can branch on `ex.getCause() instanceof UnrecognizedPropertyException`.
  Note it in the plan or leave to IMPLEMENTATION.
- **Loop-back target:** none (advisory).

### F-5 — New dependency confirmation

- **Severity:** Minor (advisory; already flagged by the plan)
- **Location:** M-1; plan "New Dependencies" and "Open Questions" §1.
- **Problem / why it matters:** `spring-boot-starter-validation` is not currently
  on the classpath (`build.gradle.kts` confirmed). It is mandated by AD-5, is a
  first-party Spring Boot starter, and is version-managed by the existing
  4.1.1 BOM — not discretionary — but adding any dependency is a
  `HUMAN_PLAN_APPROVAL` confirmation point.
- **Required correction:** none. The human confirms at the gate.
- **Loop-back target:** none.

No `Critical` findings. No `Major` findings.

## 15. Open Decisions

**No blocking Open Decisions were identified.** OD-001..OD-006 were all resolved
by the human at `HUMAN_SPEC_APPROVAL` (OD-001:A, OD-002:B, OD-003:A, OD-004:A,
OD-005:A, OD-006:A) and are applied consistently across every upstream artifact
and this plan. The `open_decisions.md` file body still shows `OPEN` — this is a
documentation lag owned by `us-clarifier` (F-1), not an unresolved decision, and
it does not block execution.

No unresolved `TODO` / `TBD` / `FIXME` / `???` / "to be decided" marker was found
in any `APPROVED` input artifact that would affect implementation.

## 16. Required Plan Changes

None are required before `HUMAN_PLAN_APPROVAL`. The following are optional
clarity improvements the Planner may fold into a revision (none blocks
execution):

1. Add one sentence to the Execution Order noting that per-step completion
   criteria are cumulative and step 13 (`./gradlew clean build`) is the
   authoritative gate (F-2).
2. State the intended unauthenticated response for protected routes in C-14
   (recommend `401` via `HttpStatusEntryPoint`) so C-T6 has a fixed target, or
   soften C-T6's assertion and defer it to SECURITY_REVIEW (F-3).
3. Optionally note in C-11 whether the malformed-JSON and unknown-field `400`
   responses share one `message` or branch on the cause (F-4).

Separately (not a Planner action): `us-clarifier` should publish
`open_decisions.md` v2 marking OD-001..OD-006 `RESOLVED` (F-1).

## 17. Verdict Rationale

The plan has zero `Critical` and zero `Major` findings. It covers every
Acceptance Criterion with a planned file and a planned verification; it is
consistent with the approved Specification, the approved API and database
designs, the design review, and the predicted impact surface; it adds no
business behavior and no out-of-scope work; it complies with AD-2..AD-8,
`package-map.md`, `api-conventions.md`, `persistence-conventions.md`, and
`security-conventions.md`; its security handling is explicit and sufficient for
a Story that processes credentials; its execution order is dependency-safe with
observable evidence per step and a deterministic full-build gate; and the change
is reviewable as a single Pull Request. The five Minor findings are advisory and
are carried as `non_blocking_findings`.

Per `stage-map.yaml`, `PLAN_REVIEW` with verdict `PASS` advances to
`HUMAN_PLAN_APPROVAL`. A `PASS` here is not human approval — a human records the
decision with `/so:approve` (or `/so:reject`) and confirms the new dependency at
that gate.

```yaml
result:
  verdict: PASS
  stage: PLAN_REVIEW
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/reviews/plans/US-001-plan-review.md
  next_stage: HUMAN_PLAN_APPROVAL
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "F-1: docs/decisions/US-001-open-decisions.md v1 still shows OD-001..OD-006 as status: OPEN; resolutions (OD-001:A, OD-002:B, OD-003:A, OD-004:A, OD-005:A, OD-006:A) live in history.jsonl (HUMAN_SPEC_APPROVAL) and are applied consistently. us-clarifier should publish v2 marking them RESOLVED. Non-blocking."
    - "F-2: Execution-order per-step completion criteria for steps 6-10 reference assertions (415, unknown-field/malformed-JSON 400) that only pass after steps 10-11; step 13 (./gradlew clean build) is the real gate. Add a note that criteria are cumulative."
    - "F-3: C-14 does not specify the unauthenticated response for protected non-registration routes (401 vs form-login 302); C-T6 asserts 401. Planner should fix an entry point (recommend HttpStatusEntryPoint 401) or defer the assertion to SECURITY_REVIEW (R-2)."
    - "F-4: PD-1 maps malformed JSON and unknown-field both via one HttpMessageNotReadableException branch (one message); OpenAPI shows two distinct example messages. Examples are illustrative; AC-6 shape preserved. Branch on the cause if distinct messages are wanted."
    - "F-5: New dependency spring-boot-starter-validation (M-1) is absent from build.gradle.kts; mandated by AD-5, first-party, BOM-version-managed. Confirm at HUMAN_PLAN_APPROVAL."
    - "Carried from IMPACT_ANALYSIS / IMPLEMENTATION_PLANNING: R-1 ddl-auto=validate vs OffsetDateTime<->TIMESTAMP WITH TIME ZONE + H2 2.x IDENTITY clause ordering (PD-6; execution step 4 gate; IMPLEMENTATION/IMPLEMENTATION_VERIFICATION own it)."
    - "Carried: R-2 Spring Security 6 filter chain must scope the public matcher AND the CSRF exemption to POST /api/v1/customers and keep anyRequest().authenticated(); SECURITY_REVIEW verifies (C-T6)."
    - "Carried: R-3/PD-2 password 12..72 bound is UTF-8 BYTES in both the validator and the service re-check; C-T1 needs a multi-byte (>72 bytes, <=72 chars) boundary vector."
    - "Carried: R-5 M-4 adds @ActiveProfiles(\"test\") to CustomerPortalApplicationTests so contextLoads still boots with Security + JPA active."
    - "Carried: R-6 JPA auditing DateTimeProvider pinned to OffsetDateTime.now(ZoneOffset.UTC) in config (C-15), not the main application class (AD-7)."
    - "Carried: R-7 password constraint message is static/generic, never echoes the submitted value (SC-9); SECURITY_REVIEW checks."
    - "Carried: R-8 scope guard - GET /api/v1/customers/{id}, login, UserDetailsService, rate limiting all explicitly out of scope."
    - "Test skeletons C-T1..C-T6 are indicative; test-writer owns the definitive AC->test matrix at TEST_WRITING and may restructure them."
```
