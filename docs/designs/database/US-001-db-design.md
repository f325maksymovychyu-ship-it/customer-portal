---
artifact_type: database_design
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T09:16:56Z
updated_at: 2026-08-31T09:16:56Z
produced_by: db-designer
inputs:
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/designs/api/US-001-api-design.md
    version: 1
  - path: docs/designs/api/US-001-openapi.yaml
    version: 1
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
  - path: docs/architecture/persistence-conventions.md
    version: null
  - path: docs/architecture/security-conventions.md
    version: null
  - path: docs/product/business-rules.md
    version: null
  - path: docs/product/business-glossary.md
    version: null
supersedes: null
---

# Database Design — US-001 Customer Registration

Companion to `docs/designs/database/US-001-entity-model.md` (the entity/attribute
model and DTO mapping). This document owns the physical schema: tables, every
explicit constraint, indexes, relationships, schema-initialization approach, and
sensitive-data handling. Authoritative artifact paths:
`docs/workflow/artifact-paths.yaml`.

## 1. Scope

US-001 introduces persistence for exactly one concept: a self-registered
customer account (email + BCrypt password hash + role + enabled flag + audit
timestamps). One new table, `customer`. No foreign keys, no join tables, no
changes to existing schema (the project has no `schema.sql` yet — this Story
creates it).

Persistence behavior **does** change, so `DB_DESIGN` is applicable (not
`NOT_APPLICABLE`).

## 2. Open Decisions applied

Resolutions recorded by the human at `HUMAN_SPEC_APPROVAL` (2026-08-31T07:48:48Z).
The `open_decisions.md` file still shows entries as `OPEN`; the recorded
resolutions are authoritative.

| OD | Resolution | Effect on this design |
|---|---|---|
| OD-001 | A — Jakarta `@Email` + max length 254 | `email VARCHAR(254) NOT NULL` (§4.2, resolves Q-1) |
| OD-002 | B — CSRF-exempt registration path | No persistence impact |
| OD-003 | A — explicit `409` on duplicate | Uniqueness enforced by `uq_customer_email`; duplicate detected before insert (§4.3, §6) |
| OD-004 | A — `201` body = `id, email, role, createdAt` | `enabled` and `updated_at` are persisted but never exposed (§7, entity-model §4) |
| OD-005 | A — anti-abuse out of scope | No persistence impact |
| OD-006 | A — normalize email to lowercase in the service; plain `UNIQUE` | `email` stored already-lowercased; plain `UNIQUE (email)`, no functional index (§4.2, §5) |

## 3. Entity naming reconciliation (resolves spec-review F-6 / API design Q-1)

`business-glossary.md` defines **Customer** ("a person who owns an account") and
**Account** ("technical representation of customer access credentials") as
separate terms. The approved Specification (NFR-4: "The **Customer** entity
declares explicit column lengths…"), `persistence-conventions.md` PC-5 (example
table `customer`), and the API design (`CustomerResponse`, path
`/api/v1/customers`) all name the persisted concept **Customer**.

**Decision (DB_DESIGN, within its entity-naming authority):** the single entity
is named `Customer`, table `customer`. For US-001 it embodies both glossary
concepts — the customer identity and their access credentials — because US-001
persists no personal-information / profile data (the glossary's **Profile** is
out of scope, US-003). If a future Story separates profile data from credentials,
`Customer` can be split then; nothing here forecloses that.

This is a naming reconciliation of an existing artifact inconsistency, not the
resolution of an Open Decision. Flagged as a non-blocking finding for
`DESIGN_REVIEW` to confirm.

## 4. Table `customer`

### 4.1 Purpose

One row per registered customer account. Created by `POST /api/v1/customers`
(FR-1, FR-5). Read later by authentication (US-002, out of scope here).

### 4.2 Columns

| Column | Type | Null | Default | Constraint(s) | Notes |
|---|---|---|---|---|---|
| `id` | `BIGINT` identity | NOT NULL | generated | `pk_customer` (PK) | Surrogate key. `GenerationType.IDENTITY` (PC-3). Never a natural key. |
| `email` | `VARCHAR(254)` | NOT NULL | — | `uq_customer_email` (UNIQUE) | Stored **already normalized to lowercase** by the service (OD-006:A). 254 = RFC 5321 practical maximum (OD-001:A). Explicit length required by PC-4 / NFR-4. |
| `password_hash` | `VARCHAR(60)` | NOT NULL | — | — | BCrypt output is exactly 60 characters (`$2a$` / `$2b$` / `$2y$`, cost ≤ 31). Fixed by PC-9. **Never** stores plaintext. See §7 and Q-2 resolution below. |
| `role` | `VARCHAR(20)` | NOT NULL | `'CUSTOMER'` | `ck_customer_role` CHECK IN (`'CUSTOMER'`,`'ADMIN'`) | Persisted as the enum **name** (`EnumType.STRING`), not ordinal. US-001 always writes `CUSTOMER` (SC-2, BR-006). Authority string `ROLE_CUSTOMER` is derived at authentication time, not stored. |
| `enabled` | `BOOLEAN` | NOT NULL | `TRUE` | — | Account enabled state (BR-004). US-001 always writes `true` (FR-5, SEC-5). DB default is defensive for manual SQL; the entity always sets it explicitly. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | — | — | UTC (BR-007, PC-6). Set by JPA auditing (`@CreatedDate`). **Not updatable.** |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | — | — | UTC (BR-007, PC-6). Set by JPA auditing (`@LastModifiedDate`); equals `created_at` on insert. |

**Q-2 resolution — final `password_hash` definition:** `VARCHAR(60) NOT NULL`,
column name `password_hash` (PC-9, SEC-4). BCrypt hashes are a fixed 60-character
string, so 60 is exact, not a guess. The `password` **72** limit in the
Specification (§6.2) and OpenAPI (`maxLength: 72`) is the **BCrypt plaintext
input limit** — 72 **bytes** of the pre-hash password (spec-review F-5 / API Q-4)
— enforced in request validation and the service re-check (FR-6), **not** a
column concern. The column only ever holds the 60-char hash.

### 4.3 Constraints

| Name | Kind | Definition | Rationale |
|---|---|---|---|
| `pk_customer` | PRIMARY KEY | `(id)` | PC-3 surrogate key; PC-5 naming. |
| `uq_customer_email` | UNIQUE | `(email)` | BR-001 one account per email; BR-003 one account per customer. Case-insensitivity (BR-002) is satisfied because the service lowercases `email` before both the duplicate check and the insert (OD-006:A). Provides the index for "find by email" (PC-7). |
| `ck_customer_role` | CHECK | `role IN ('CUSTOMER','ADMIN')` | Defensive: keeps `role` within the glossary's fixed value set (`business-glossary.md` Role) even under manual SQL. Optional per conventions; `DESIGN_REVIEW` may drop it. |

### 4.4 Indexes

| Name | Columns | Unique | Purpose |
|---|---|---|---|
| (implied by `uq_customer_email`) | `(email)` | yes | Duplicate-email check (FR-4) and future `findByEmail` (US-002). PC-7: a UNIQUE constraint already provides the index — **no separate `ix_customer_email`**. |

No foreign-key indexes (no foreign keys). No other repository lookup keys in
US-001.

## 5. Case-insensitive uniqueness mechanism (OD-006:A)

- The service normalizes `email` with `toLowerCase(Locale.ROOT)` (or equivalent)
  **before** the existence check and **before** constructing the entity.
- The duplicate check uses the already-lowercased value against a plain
  `Customer findByEmail(String email)` / `boolean existsByEmail(String email)`
  repository method.
- The column stores only lowercase values; `uq_customer_email` is a plain
  `UNIQUE` — no `LOWER(email)` functional index, no database-specific collation.
- Consequence for tests (`TEST_WRITING`): `Alice@Example.com` and
  `alice@example.com` collide; the second registration returns `409`.

## 6. Relationships

None. `customer` is a standalone table for US-001.

- `role` is modeled as an **enum column**, not a `customer_role` table. The
  glossary defines Role as a fixed two-value permission group; US-001 assigns
  exactly one role per account at creation. `persistence-conventions.md` PC-5
  mentions `customer_role` only as a *naming* example, not as a required table.
  Flagged for `DESIGN_REVIEW`.

## 7. Sensitive-data handling

| Data | Column | Rule |
|---|---|---|
| Plaintext password | *(none)* | Never persisted. Exists only on the inbound `RegistrationRequest` DTO (SC-1, SEC-3, BR-005). |
| Password hash | `password_hash` | BCrypt hash only. Never logged, never returned by any endpoint, never placed on a response DTO (SC-1, PC-9, SEC-4). Excluded from any entity `toString()`. Absent from `CustomerResponse` (OD-004:A). |
| Email (PII) | `email` | Stored normalized (lowercase). Not a secret, but not emitted in error messages (SC-9) beyond the standard `fieldErrors[].field = "email"`. Returned in `CustomerResponse` (OD-004:A). |
| `enabled` | `enabled` | Internal account state; persisted but not exposed by US-001 responses (OD-004:A). |

No tokens, no other PII, no encryption-at-rest requirement introduced by this
Story.

## 8. Schema initialization

Consistent with `persistence-conventions.md` PC-1, PC-2 and SEC-10:

- Schema is **hand-written** in `src/main/resources/schema.sql` (this Story
  creates the file). The entity mapping and `schema.sql` must agree exactly.
- Applied by Spring SQL init: `spring.sql.init.mode=always` for local/dev,
  `embedded` acceptable for tests.
- Hibernate `spring.jpa.hibernate.ddl-auto` = `validate` (or `none`) in every
  profile. `create` / `create-drop` / `update` are forbidden (SC-8).
- Database: file-based H2 for local runs
  (`jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE`); isolated in-memory H2
  for tests (PC-1). Generated `./data/*` files stay git-ignored.
- `spring.h2.console.enabled=false` in every profile — unchanged by this Story
  (SEC-9, SC-6).

### 8.1 `schema.sql` (authoritative DDL for this Story)

```sql
CREATE TABLE IF NOT EXISTS customer (
    id            BIGINT                   NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    email         VARCHAR(254)             NOT NULL,
    password_hash VARCHAR(60)              NOT NULL,
    role          VARCHAR(20)              NOT NULL DEFAULT 'CUSTOMER',
    enabled       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customer        PRIMARY KEY (id),
    CONSTRAINT uq_customer_email  UNIQUE (email),
    CONSTRAINT ck_customer_role   CHECK (role IN ('CUSTOMER', 'ADMIN'))
);
```

Notes for `IMPLEMENTATION`:

- `GENERATED BY DEFAULT AS IDENTITY` is the H2 form that pairs with
  `GenerationType.IDENTITY` (PC-3). Confirm the exact clause ordering against the
  project's H2 2.x version (H2 2.x expects the nullability clause *after* the
  identity clause, and identity columns are implicitly `NOT NULL`); adjust the
  DDL if the build rejects it.
- Hibernate `validate` checks column names, types, nullability and length against
  the entity. It does **not** verify `DEFAULT` clauses or the `CHECK` constraint,
  so those are safe to keep in DDL only.
- **Implementation-verification step:** `OffsetDateTime` ↔ `TIMESTAMP WITH TIME
  ZONE` under `ddl-auto=validate` is a known strict-match trip point. The build
  (`ddl-auto=validate` startup) must pass against this `schema.sql` before the
  Story is complete (NFR-7); if Hibernate maps `OffsetDateTime` to a different
  SQL type on this H2 version, align the column type or the field mapping then.
- Physical naming strategy = standard snake_case (PC-5) so entity field
  `passwordHash` → column `password_hash`, `createdAt` → `created_at`, etc.

## 9. Traceability

| Requirement / rule | Design element |
|---|---|
| FR-5 persist email, BCrypt hash, role `CUSTOMER`, `enabled=true`, UTC audit | `customer` columns `email`, `password_hash`, `role`, `enabled`, `created_at`, `updated_at` (§4.2) |
| FR-4 / AC-002 duplicate email rejected, case-insensitive | `uq_customer_email` + service lowercasing (§4.3, §5) |
| FR-7 / AC-004 / SEC-3 / BR-005 no plaintext password | No plaintext column (§7) |
| SEC-4 / PC-9 hash only in `password_hash`, never returned | `password_hash VARCHAR(60) NOT NULL`; absent from `CustomerResponse` (§4.2, §7) |
| OD-001:A email length 254 | `email VARCHAR(254)` (§4.2) |
| OD-006:A normalization mechanism | Service lowercases; plain `UNIQUE` (§5) |
| NFR-4 explicit column lengths, nullability, unique email, surrogate `Long id`, UTC timestamps | §4.2, §4.3 |
| BR-007 / PC-6 UTC audit timestamps via JPA auditing | `created_at` / `updated_at` `TIMESTAMP WITH TIME ZONE`, `@CreatedDate` / `@LastModifiedDate` (§4.2) |
| SC-8 / SEC-10 / PC-2 hand-written schema, `ddl-auto` validate | §8, §8.1 |
| SC-2 / BR-006 role `CUSTOMER`, enabled | `role`, `enabled` columns (§4.2) |

## 10. Non-blocking findings for DESIGN_REVIEW

1. **Entity name** `Customer` / table `customer` chosen over glossary term
   `Account` for the credential representation; rationale in §3. Confirm.
2. **`role` as an enum column** (not a `customer_role` table); rationale in §6.
   Confirm this is acceptable for the project's role model.
3. **`ck_customer_role` CHECK constraint** is a defensive addition not mandated
   by `persistence-conventions.md`; drop if the project prefers minimal DDL.
4. **`role` / `enabled` DB `DEFAULT` clauses** are defensive for manual SQL only;
   the entity always sets both explicitly.
5. Password **72-byte** input limit (spec-review F-5 / API Q-4) is enforced in
   validation + service, not in the schema — `password_hash` is `VARCHAR(60)`.
   `TEST_WRITING` / `IMPLEMENTATION` own the 72-byte boundary vector.

## 11. Result

```yaml
result:
  verdict: PASS
  stage: DB_DESIGN
  story: US-001
  artifact_status: DRAFT
  artifacts:
    - docs/designs/database/US-001-db-design.md
    - docs/designs/database/US-001-entity-model.md
  next_stage: DESIGN_REVIEW
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "Q-1 resolved: email column is VARCHAR(254) NOT NULL (OD-001:A)."
    - "Q-1 resolved: entity named Customer / table customer; embodies glossary Customer + Account for US-001 (no profile data). DESIGN_REVIEW to confirm (spec-review F-6)."
    - "Q-2 resolved: password_hash VARCHAR(60) NOT NULL (PC-9); BCrypt output is exactly 60 chars."
    - "Q-4 (TEST_WRITING/IMPLEMENTATION): password 72 limit is BCrypt plaintext input BYTES, enforced in validation/service, not a column constraint; write the 72-byte boundary vector (spec-review F-5)."
    - "role modeled as an EnumType.STRING column (VARCHAR(20)) with a CHECK, not a customer_role table; PC-5 cites customer_role only as a naming example. DESIGN_REVIEW to confirm."
    - "ck_customer_role CHECK and role/enabled DB DEFAULT clauses are defensive additions; DESIGN_REVIEW may drop them."
```
