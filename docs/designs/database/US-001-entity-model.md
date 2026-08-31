---
artifact_type: entity_model
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T09:16:56Z
updated_at: 2026-08-31T09:46:30Z
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

# Entity Model — US-001 Customer Registration

Companion to `docs/designs/database/US-001-db-design.md` (physical schema). This
document is the entity/attribute model and its mapping to business concepts and
to the API DTOs.

## 1. Entities

One entity: **`Customer`** → table `customer`. See db-design §3 for the
`Customer` vs glossary `Account` naming reconciliation.

No other entities, no embeddables, no `@ElementCollection`. `Role` is a Java
`enum`, not an entity.

## 2. `Customer`

| Field | Java type | Column | Column type | Nullable | Explicit mapping | Business concept |
|---|---|---|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` identity | no | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Account identifier (surrogate, PC-3) |
| `email` | `String` | `email` | `VARCHAR(254)` | no | `@Column(name = "email", nullable = false, length = 254)`; uniqueness declared once, at table level (§2.1) per PC-4 (either/or, not both) | Customer email (`business-glossary.md` Customer; BR-001, BR-002) |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(60)` | no | `@Column(name = "password_hash", nullable = false, length = 60)` | Hashed access credential (`business-glossary.md` Account; BR-005, PC-9) |
| `role` | `Role` (enum) | `role` | `VARCHAR(20)` | no | `@Enumerated(EnumType.STRING)`, `@Column(name = "role", nullable = false, length = 20)` | Role / permission group (`business-glossary.md` Role; BR-006, SC-2) |
| `enabled` | `boolean` | `enabled` | `BOOLEAN` | no | `@Column(name = "enabled", nullable = false)` | Account enabled state (BR-004) |
| `createdAt` | `OffsetDateTime` | `created_at` | `TIMESTAMP WITH TIME ZONE` | no | `@CreatedDate`, `@Column(name = "created_at", nullable = false, updatable = false)` | Audit — creation instant, UTC (BR-007, PC-6) |
| `updatedAt` | `OffsetDateTime` | `updated_at` | `TIMESTAMP WITH TIME ZONE` | no | `@LastModifiedDate`, `@Column(name = "updated_at", nullable = false)` | Audit — last-modified instant, UTC (BR-007, PC-6) |

`email` uniqueness is declared **once**, via the table-level `@UniqueConstraint`
in §2.1 (named `uq_customer_email`, matching `schema.sql`), not also on
`@Column` — PC-4 is either/or.

### 2.1 Class-level mapping

- `@Entity`, `@Table(name = "customer", uniqueConstraints = @UniqueConstraint(name = "uq_customer_email", columnNames = "email"))`.
- `@EntityListeners(AuditingEntityListener.class)`; `@EnableJpaAuditing` on a
  configuration class (PC-6).
- Physical naming strategy = standard snake_case (PC-5).
- No `@Version` / optimistic-lock column (not required by US-001).
- `equals` / `hashCode` on `id` only (null-safe for transient instances).
- `toString()` MUST exclude `passwordHash` (SC-1, SC-9).

### 2.2 `Role` enum

```
enum Role { CUSTOMER, ADMIN }
```

- Persisted as the constant **name** via `EnumType.STRING` (never ordinal).
- US-001 only ever assigns `CUSTOMER` (SC-2, BR-006). `ADMIN` exists in the
  model for completeness (`business-glossary.md` Role) but is unused by this
  Story.
- The Spring Security authority string (`ROLE_CUSTOMER` / `ROLE_ADMIN`) is
  derived from this enum at authentication time (US-002); it is **not** stored.

## 3. Lifecycle / invariants

| Invariant | Enforced by |
|---|---|
| `email` is unique case-insensitively | Service lowercases before check + insert (OD-006:A); `uq_customer_email` (db-design §5) |
| `email` is stored lowercase | Service normalization (OD-006:A) |
| `passwordHash` is always a BCrypt hash, never plaintext | Service hashes with `BCryptPasswordEncoder` before constructing the entity (SC-1, FR-5) |
| `role` is non-null, defaults to `CUSTOMER` | Service sets `Role.CUSTOMER` explicitly on creation (BR-006) |
| `enabled` is `true` on creation | Service sets `true` explicitly (FR-5, SEC-5) |
| `createdAt` never changes after insert | `updatable = false` + JPA auditing (PC-6) |
| timestamps are UTC | JPA auditing with a UTC `DateTimeProvider` / clock (BR-007, PC-6) |

US-001 performs only **create**. No update or delete path exists in this Story.

## 4. Mapping to API DTOs

DTOs are defined by `docs/designs/api/US-001-openapi.yaml`. The entity is never
serialized directly.

### 4.1 `RegistrationRequest` → `Customer` (inbound, create)

| DTO field | Entity field | Transformation |
|---|---|---|
| `email` | `email` | `trim` + `toLowerCase(Locale.ROOT)` (OD-006:A) |
| `password` | `passwordHash` | validated (12–72 bytes, char-class policy), then `BCryptPasswordEncoder.encode(password)` (SC-1, FR-6). Plaintext discarded; never stored. |
| *(none)* | `role` | set to `Role.CUSTOMER` (BR-006) |
| *(none)* | `enabled` | set to `true` (SEC-5) |
| *(none)* | `createdAt` / `updatedAt` | set by JPA auditing (PC-6) |
| *(none)* | `id` | assigned by the database on insert (PC-3) |

`additionalProperties: false` on `RegistrationRequest` (API design §3) means any
field other than `email` / `password` is a `400` before this mapping runs.

### 4.2 `Customer` → `CustomerResponse` (outbound, `201`)

| Entity field | DTO field | Notes |
|---|---|---|
| `id` | `id` | int64 |
| `email` | `email` | normalized (lowercase) value as stored |
| `role` | `role` | enum name string, always `CUSTOMER` |
| `createdAt` | `createdAt` | ISO-8601 UTC |
| `passwordHash` | — | **never mapped** (SC-1, SEC-4, OD-004:A) |
| `enabled` | — | not exposed (OD-004:A) |
| `updatedAt` | — | not exposed (OD-004:A) |

`Location: /api/v1/customers/{id}` header is built from `id` (FR-8); the target
endpoint is not implemented by US-001.

## 5. Concept traceability

| Business concept (`business-glossary.md` / BR) | Model element |
|---|---|
| Customer (person who owns an account) | `Customer` entity (identity attributes: `email`) |
| Account (technical representation of access credentials) | `Customer` entity (credential attributes: `password_hash`, `role`, `enabled`) — merged for US-001, see db-design §3 |
| Registration (process of creating a customer account) | `Customer` insert via `POST /api/v1/customers` (FR-1, FR-5) |
| Role (permission group: CUSTOMER, ADMIN) | `Role` enum → `role` column |
| BR-001 unique email | `email` `unique = true` / `uq_customer_email` |
| BR-002 case-insensitive email | lowercase normalization + plain UNIQUE (OD-006:A) |
| BR-003 one account per customer | `uq_customer_email` (email identifies the customer) |
| BR-004 disabled account cannot authenticate | `enabled` column (consumed by US-002) |
| BR-005 no plaintext passwords | only `password_hash` exists; no plaintext field |
| BR-006 default role CUSTOMER | service sets `Role.CUSTOMER` |
| BR-007 UTC timestamps | `created_at` / `updated_at` `TIMESTAMP WITH TIME ZONE`, JPA auditing |

## 6. Notes for downstream stages

- **IMPACT_ANALYSIS / IMPLEMENTATION_PLANNING:** new package `customer` (entity,
  repository, service, controller, DTOs); new `security` package pieces
  (`BCryptPasswordEncoder` bean, security config making `POST /api/v1/customers`
  public + CSRF-exempt); new `src/main/resources/schema.sql`; JPA auditing
  configuration; `application.yaml` datasource + `ddl-auto=validate` +
  `sql.init.mode`.
- **TEST_WRITING:** persistence tests should assert column constraints
  (`email` length 254, `password_hash` length 60, NOT NULL), the case-insensitive
  duplicate collision, that `password_hash` is a BCrypt string and not the
  plaintext, and that `created_at` / `updated_at` are populated and in UTC.
- **DESIGN_REVIEW:** see db-design §10 for the open confirmations (entity name,
  `role` as enum column, CHECK/DEFAULT clauses, 72-byte limit location).
