---
artifact_type: design_review
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T09:44:50Z
updated_at: 2026-08-31T09:44:50Z
produced_by: design-reviewer
inputs:
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
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
  - path: docs/architecture/architecture.md
    version: null
  - path: docs/architecture/api-conventions.md
    version: null
  - path: docs/architecture/persistence-conventions.md
    version: null
  - path: docs/architecture/security-conventions.md
    version: null
  - path: docs/architecture/package-map.md
    version: null
  - path: docs/product/business-rules.md
    version: null
  - path: docs/product/business-glossary.md
    version: null
  - path: docs/product/non-functional-requirements.md
    version: null
supersedes: null
---

# Design Review — US-001 Customer Registration

## Summary

**Verdict: PASS.** The API design (OpenAPI contract + API design notes) and the
database design (DB design + entity model) are complete, internally consistent,
cross-consistent with each other, and compliant with the approved Specification,
the architecture conventions, and the security conventions. The six Open
Decisions resolved at `HUMAN_SPEC_APPROVAL` (OD-001:A, OD-002:B, OD-003:A,
OD-004:A, OD-005:A, OD-006:A) are correctly and consistently applied across both
designs.

No `Critical` and no `Major` findings. Nine `Minor` (advisory) findings are
recorded, including confirmation of the three items the DB design explicitly
referred to this stage (entity naming, `role` as an enum column, defensive
CHECK/DEFAULT clauses). None block progression to `IMPACT_ANALYSIS`.

## Reviewed Artifacts

| Artifact | Path | Version | Status |
|---|---|---|---|
| Specification | `docs/specifications/US-001-spec.md` | 1 | APPROVED |
| Specification Review | `docs/reviews/specifications/US-001-spec-review.md` | 1 | APPROVED |
| API Design | `docs/designs/api/US-001-api-design.md` | 1 | DRAFT |
| OpenAPI Contract | `docs/designs/api/US-001-openapi.yaml` | 1 | DRAFT |
| DB Design | `docs/designs/database/US-001-db-design.md` | 1 | DRAFT |
| Entity Model | `docs/designs/database/US-001-entity-model.md` | 1 | DRAFT |
| Open Decisions | `docs/decisions/US-001-open-decisions.md` | 1 | DRAFT |

Precondition check: `specification_review` verdict is `PASS`;
`HUMAN_SPEC_APPROVAL` was recorded 2026-08-31T07:48:48Z (history.jsonl).
Both design areas are applicable — the Specification changes both public API
behavior and persistence behavior, and neither `API_DESIGN` nor `DB_DESIGN`
recorded `NOT_APPLICABLE` (both returned `PASS`). All design artifacts consumed
the current spec v1, spec-review v1, and open_decisions v1; none is
`SUPERSEDED`. Architecture convention documents contain real guidance. Not
stale, not `BLOCKED`.

## API Design Review

| Check | Result |
|---|---|
| Every externally observable AC maps to an operation / status code | Pass — AC-001→`201`+`Location`+`CustomerResponse`; AC-002→`409`; AC-003→`400`+`fieldErrors[email]`; AC-005→`CustomerResponse` schema (no credential fields); AC-006→`400`+`fieldErrors[password]`; AC-007→`415`. AC-004 is a persistence guarantee, supported negatively by the response schema. |
| Path, method, media type, versioning, error model vs `api-conventions.md` | Pass — `POST /customers` under `servers: /api/v1` (AC-1); `application/json` required, `415` otherwise (AC-2); plural noun, `camelCase` fields (AC-3); `POST /collection` → `201` + `Location` + body (AC-4); `400/409/415/500` (AC-5); `ErrorResponse` matches the AC-6 shape with optional `fieldErrors[]`; single `@RestControllerAdvice` obligation stated (AC-9). |
| Request/response schemas use DTOs, never entities | Pass — `RegistrationRequest`, `CustomerResponse`, `ErrorResponse`, `FieldError`. Consistent with AD-4 / package-map (`model.request`, `model.dto`). |
| No response field exposes a credential or internal-only value | Pass — `CustomerResponse` = `id, email, role, createdAt` (OD-004:A). `password` is `writeOnly`; no `password_hash`, no `enabled`, no `updatedAt`. |
| Specification validation constraints reflected in the contract | Pass with note D-1 — `email`: `minLength 1`, `maxLength 254`, `format: email` (§6.1, OD-001:A). `password`: `minLength 12`, `maxLength 72` (§6.2). The four character-class rules are stated in the schema `description` only, not as a `pattern` (see D-1). |
| Error responses cover the documented failures | Pass — `400` (bean-validation, malformed JSON, unknown field), `409` (duplicate email, OD-003:A message), `415` (media type), `500` (unmapped, no leak). `401`/`403` correctly marked not applicable (public endpoint). |
| Authentication / authorization per operation stated, matches `security-conventions.md` | Pass — `security: []`, `x-public: true`, `x-csrf-exempt: true`, `x-authorization: none`. Registration is the one public endpoint (SC-4); all other routes stay deny-by-default; no other route added. |
| CSRF exemption recorded as an architecture decision | Pass — API design §6 is the recorded decision required by SC-5 for OD-002:B; CSRF stays enabled for every other endpoint. |
| Backward compatibility | Pass — purely additive; no existing contract changes (AC-1). |
| Request-shape policy (spec-review F-2 / Spec §6.3) | Pass — `additionalProperties: false`; unknown/extra fields → `400` (API design §3). Spec §6.3 explicitly delegated this to `API_DESIGN`; recording it here is in scope. See D-7. |

## Database Design Review

| Check | Result |
|---|---|
| Entities trace to business concepts | Pass — single `Customer` entity / `customer` table embodies glossary **Customer** (identity: `email`) and **Account** (credentials: `password_hash`, `role`, `enabled`). Reconciliation in DB design §3. See D-2. |
| Explicit column length, nullability, uniqueness, indexes — no JPA defaults | Pass — every column declares type, nullability, and constraints: `email VARCHAR(254) NOT NULL`, `password_hash VARCHAR(60) NOT NULL`, `role VARCHAR(20) NOT NULL`, `enabled BOOLEAN NOT NULL`, `created_at`/`updated_at TIMESTAMP WITH TIME ZONE NOT NULL`. `uq_customer_email UNIQUE (email)`. Matches PC-4 / NFR-4. |
| Identifier type and generation vs `persistence-conventions.md` | Pass — `id BIGINT` identity, `@GeneratedValue(strategy = IDENTITY)`, `pk_customer` (PC-3). Surrogate key; email is a `UNIQUE` constraint, not a natural PK. |
| Sensitive fields identified with storage rules | Pass — plaintext password never persisted (no column); `password_hash` BCrypt-only, never logged/returned, excluded from `toString()` (PC-9, SC-1, SEC-3, SEC-4); `email` PII stored normalized, not leaked in errors beyond `fieldErrors[].field` (SC-9). |
| Schema-initialization strategy vs `persistence-conventions.md` | Pass — hand-written `src/main/resources/schema.sql` (this Story creates it); `spring.sql.init.mode=always` local/dev, `embedded` for tests; `ddl-auto=validate` (or `none`) in every profile; `create`/`create-drop`/`update` forbidden. Matches PC-1, PC-2, SC-8, SEC-10. No `ddl-auto` shortcut used in place of explicit design. |
| Relationships and cardinality explicit | Pass — none for US-001; `customer` is standalone. `role` modeled as an `EnumType.STRING` column, not a `customer_role` table (see D-3). |
| Indexes | Pass — `uq_customer_email` provides the `email` lookup index (PC-7); no separate `ix_customer_email` (correct — a UNIQUE constraint already indexes). No foreign keys, so no FK indexes. |
| Audit timestamps | Pass — `created_at` (not updatable) / `updated_at`, `TIMESTAMP WITH TIME ZONE`, UTC via JPA auditing `@CreatedDate`/`@LastModifiedDate` with `AuditingEntityListener` + `@EnableJpaAuditing` (PC-6, BR-007). See D-5 for the `OffsetDateTime` ↔ `validate` note the DB design already flags. |

## Cross-Model Consistency

| Concern | Result |
|---|---|
| Every API resource maps to a coherent persistence model | Pass — the `customers` resource ↔ `customer` table; the `registerCustomer` create operation ↔ a single `INSERT`. |
| Field names / types / constraints agree between DTO schemas and entities | Pass — `RegistrationRequest.email` `maxLength 254` ↔ `email VARCHAR(254)`; `CustomerResponse.id` `int64` ↔ `id BIGINT`; `CustomerResponse.email` ↔ stored normalized `email`; `CustomerResponse.role` enum `[CUSTOMER]` ↔ `role VARCHAR(20)` `EnumType.STRING` (contract narrows to the only value US-001 emits; entity `Role` enum keeps `ADMIN` for the wider model — not a conflict); `CustomerResponse.createdAt` `date-time` ↔ `created_at TIMESTAMP WITH TIME ZONE`. |
| `password` handling agrees end to end | Pass — `password` `writeOnly` on the inbound DTO only; hashed to `password_hash VARCHAR(60)` (BCrypt output is exactly 60 chars); absent from every response schema and from the entity model's `Customer → CustomerResponse` map. The `maxLength: 72` characters vs 72 bytes point is carried as D-6. |
| Uniqueness / validation enforced consistently | Pass — email format/length at the request layer (Bean Validation), email **uniqueness** as a business rule in the Service (`existsByEmail` on the lowercased value) **and** as `uq_customer_email` at the DB. Case-insensitivity (BR-002) is met by service lowercasing before both the check and the insert, with a plain `UNIQUE` (OD-006:A). Matches AD-5. |
| No design introduces a business decision absent from the Specification or an approved decision | Pass — the three DB choices (entity name, `role` as enum column, CHECK/DEFAULT clauses) are within `db-designer`'s entity/schema authority and are explicitly referred here for confirmation (D-2, D-3, D-4). The unknown-field rejection (D-7) is the design-stage choice Spec §6.3 delegated to `API_DESIGN`. Nothing else new. |

## Security Review of Designs

| Area | Result |
|---|---|
| Authentication posture | Pass — one new public endpoint; everything else deny-by-default (SC-4). |
| CSRF | Pass — exemption for `POST /api/v1/customers` only, recorded as the Story architecture decision in API design §6 (SC-5, OD-002:B). |
| Password hashing & exposure | Pass — `BCryptPasswordEncoder`, encoder bean in `security` package; plaintext only on the inbound DTO; dual-layer policy enforcement (request constraint + service re-check); hash stored only in `password_hash`, never returned (SC-1, PC-9, SEC-2..SEC-4). |
| Error / log hygiene | Pass — single error shape (AC-6); `message` client-safe; examples carry no stack trace, SQL, class name, path, or DB URL; `fieldErrors[].message` never echoes the submitted value (SC-9, SEC-6). |
| Account enumeration | Accepted — the explicit `409` duplicate-email response is a human-approved exposure (OD-003:A / SEC-8), not a default. Documented in API design §7. |
| H2 console / secrets / schema safety | Pass — designs do not change `spring.h2.console.enabled=false` (SC-6), introduce no secrets (SC-7/SEC-11), and keep `ddl-auto` at `validate`/`none` (SC-8/SEC-10). |

## Findings

| id | Severity | Area | Evidence | Required correction |
|---|---|---|---|---|
| D-1 | Minor | API | `RegistrationRequest.password` states the four character-class rules only in the schema `description`; the contract has no `pattern`. OpenAPI 3.0.3 cannot cleanly express "≥1 of each class". | None required. The authoritative enforcement is the custom `validation`-package constraint plus the service re-check (FR-6, SC-1). `TEST_WRITING` must derive password-policy cases from Spec §6.2, not from the OpenAPI schema. Optionally add a documentation `pattern` or `x-password-policy` note in a later revision. |
| D-2 | Minor (confirmation) | Database | DB design §3 / entity-model §1: single entity `Customer` / table `customer` covers glossary **Customer** + **Account**. Referred to this stage (spec-review F-6, API Q-1). | **Confirmed acceptable.** US-001 persists no profile data; merging identity and credentials into one `Customer` matches Spec NFR-4 ("the Customer entity…"), `persistence-conventions.md` PC-5 (`customer` example table), and the API naming (`/customers`, `CustomerResponse`). A future Story may split credential vs profile data; nothing here forecloses that. |
| D-3 | Minor (confirmation) | Database | DB design §6 / entity-model §2.2: `role` is an `EnumType.STRING` `VARCHAR(20)` column, not a `customer_role` lookup table. | **Confirmed acceptable.** PC-5 cites `customer_role` only as a `snake_case` naming example, not a mandated table. A fixed two-value permission group persisted as its enum name is standard and satisfies PC-4 (explicit length, nullability). |
| D-4 | Minor (confirmation) | Database | DB design §4.3 / §10: `ck_customer_role CHECK (role IN ('CUSTOMER','ADMIN'))` and the `role`/`enabled` DDL `DEFAULT` clauses are defensive; `ddl-auto=validate` does not verify either. | **Confirmed acceptable to keep.** They are harmless hardening for manual SQL and do not conflict with the entity, which always sets both fields explicitly. No requirement to drop them; equally no objection if `IMPLEMENTATION` prefers minimal DDL. |
| D-5 | Minor | Database | DB design §8.1: `OffsetDateTime` ↔ `TIMESTAMP WITH TIME ZONE` under `ddl-auto=validate`, and H2 2.x `GENERATED BY DEFAULT AS IDENTITY` clause ordering, are known strict-match trip points. | Carried forward. `IMPLEMENTATION` must make the `schema.sql` and entity mapping agree and the `validate` startup pass (NFR-7); `IMPLEMENTATION_VERIFICATION` checks it. Not a design defect. |
| D-6 | Minor | API / cross-model | `password` `maxLength: 72` in the contract is characters; the real bound is 72 **bytes** of BCrypt input (spec-review F-5, API Q-4, DB design §4.2). | No design change. `TEST_WRITING` / `IMPLEMENTATION` own the 72-byte boundary vector and enforce it in the request constraint + service, not the column. |
| D-7 | Minor | API | API design §3 / OpenAPI `additionalProperties: false`: unknown JSON fields → `400`. Implementation note: Jackson `FAIL_ON_UNKNOWN_PROPERTIES = true` mapped to `400` by the advice (API Q-3). | In scope (Spec §6.3 delegated the choice to `API_DESIGN`) and acceptable. `IMPLEMENTATION` must wire the Jackson setting and the `@RestControllerAdvice` mapping; `IMPLEMENTATION_VERIFICATION` confirms the `400` path. |
| D-8 | Minor (informational) | API | `Location: /api/v1/customers/{id}` targets `GET /api/v1/customers/{id}`, which US-001 does not implement. | Acceptable. The header value is correct and stable; the read endpoint is a future Story. AC-4 requires the header, not a live target within this Story. |
| D-9 | Minor | Database (downstream note) | Entity-model §6 "notes for downstream stages" mentions a "new package `customer`". The authoritative package layout (`package-map.md`) is `model.entity` / `model.request` / `model.dto` / `controller` / `service` / `repository` / `validation` / `security` / `exception`. | Advisory only — the note is not the design. `IMPACT_ANALYSIS` / `IMPLEMENTATION_PLANNING` must place classes in the `package-map.md` packages; adding a feature package needs an approved decision (AD-8). |

No `Critical` findings. No `Major` findings.

## Open Decisions

All six are resolved (human, 2026-08-31T07:48:48Z) and applied consistently in
both designs. The `open_decisions.md` file still shows entries as `OPEN`; the
recorded resolutions are authoritative (both designs note this explicitly).

| OD | Resolution | API design | DB design | Consistent |
|---|---|---|---|---|
| OD-001 | A — `@Email` + max length 254 | `email` `format: email`, `maxLength 254`, `minLength 1` | `email VARCHAR(254) NOT NULL` | Yes |
| OD-002 | B — CSRF-exempt registration path | `security: []`, `x-csrf-exempt`, decision recorded §6 | no persistence impact | Yes |
| OD-003 | A — explicit `409` | `409` + `"An account with this email already exists."` | `uq_customer_email`; duplicate detected before insert | Yes |
| OD-004 | A — `id, email, role, createdAt` | `CustomerResponse` field list | `enabled` / `updated_at` persisted, never exposed | Yes |
| OD-005 | A — anti-abuse out of scope | no rate-limit headers, no `429` | no persistence impact | Yes |
| OD-006 | A — lowercase in service + plain `UNIQUE` | no contract impact; response `email` is the normalized value | `email` stored lowercased; plain `UNIQUE (email)`, no functional index | Yes |

No unresolved Open Decision affects API or persistence design.

## Limitations

- Review is document-level: it does not execute the build or validate the
  `schema.sql` against a live H2 instance. The `ddl-auto=validate` startup and
  the `OffsetDateTime` / identity-column type matches (D-5) are verified at
  `IMPLEMENTATION` / `IMPLEMENTATION_VERIFICATION`.
- OpenAPI was reviewed as a document; it was not run through a schema linter.
- Package placement (D-9) and the Jackson unknown-property wiring (D-7) are
  planning/implementation concerns flagged here, not resolved here.

## Verdict

**PASS** — both designs are sound, mutually consistent, and compliant with the
approved Specification and the architecture/security conventions. The nine
`Minor` findings are advisory and carried as `non_blocking_findings`; D-2, D-3,
and D-4 are explicit confirmations of items the DB design referred to this
stage. The Story may proceed to `IMPACT_ANALYSIS`.

```yaml
result:
  verdict: PASS
  stage: DESIGN_REVIEW
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/reviews/designs/US-001-design-review.md
  next_stage: IMPACT_ANALYSIS
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "D-1: password character-class policy is only in the OpenAPI description, not a pattern; TEST_WRITING must source password cases from Spec §6.2, not the schema."
    - "D-2 (confirmed): single Customer entity/table covers glossary Customer + Account for US-001 (no profile data); matches Spec NFR-4, PC-5, API naming. Future split not foreclosed."
    - "D-3 (confirmed): role modeled as an EnumType.STRING VARCHAR(20) column, not a customer_role table; PC-5 cites customer_role only as a naming example."
    - "D-4 (confirmed): ck_customer_role CHECK and role/enabled DEFAULT clauses are defensive DDL-only; acceptable to keep, not verified by ddl-auto=validate."
    - "D-5: OffsetDateTime <-> TIMESTAMP WITH TIME ZONE under ddl-auto=validate and H2 2.x IDENTITY clause ordering are known trip points; IMPLEMENTATION/IMPLEMENTATION_VERIFICATION own it."
    - "D-6: password maxLength 72 in the contract is characters; the real limit is 72 bytes (BCrypt input). TEST_WRITING/IMPLEMENTATION own the byte-boundary vector."
    - "D-7: unknown-JSON-field rejection (additionalProperties: false -> 400) is the design-stage choice Spec §6.3 delegated to API_DESIGN; IMPLEMENTATION must wire Jackson FAIL_ON_UNKNOWN_PROPERTIES + advice mapping (API Q-3)."
    - "D-8: Location header targets GET /api/v1/customers/{id}, not implemented by US-001; header value is stable and correct."
    - "D-9: entity-model §6 mentions a 'customer' package; IMPACT_ANALYSIS/PLANNING must use the package-map.md packages (model.entity, model.request, model.dto, ...), not a feature package (AD-8)."
```
