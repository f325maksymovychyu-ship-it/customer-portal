# Persistence Conventions

Explicit persistence decisions for this project.

## PC-1 Database & runtime mode

- H2, **file-based**, for local runs. URL form:
  `jdbc:h2:file:./data/customer-portal;AUTO_SERVER=TRUE` (project-relative
  `./data/` directory).
- Generated database files (`./data/*.db`, `*.mv.db`, `*.trace.db`) are
  **not** committed — they are covered by `.gitignore`.
- Automated tests use an **isolated in-memory** H2
  (`jdbc:h2:mem:<random>;DB_CLOSE_DELAY=-1`) configured by the test profile,
  never the file database.
- The H2 console is disabled in every profile (see `security-conventions.md`).
- Moving to PostgreSQL + Flyway is a valid future direction but is out of
  scope until an approved decision changes this file.

## PC-2 Schema initialization

- Schema is **explicit and hand-maintained** in
  `src/main/resources/schema.sql`, applied by Spring SQL init
  (`spring.sql.init.mode=always` for local/dev, `embedded` acceptable for
  tests).
- Hibernate `ddl-auto` is `validate` in local/dev/test (it checks the entity
  mapping against `schema.sql` and fails fast on drift). `none` is also
  acceptable. `create`, `create-drop`, `update` are **forbidden**
  (see `security-conventions.md`).
- Every entity change is accompanied by the matching `schema.sql` change in
  the same Story. `db-designer` (see `docs/workflow/gaps.md` — not yet
  implemented) specifies both; `design-reviewer` checks they agree. Until
  those Skills exist, a human does this check.
- A migration tool (Flyway/Liquibase) is **not** used while the project is on
  H2; adopting one is an approved decision that likely accompanies a move to
  PostgreSQL.

## PC-3 Identifiers

- Surrogate primary key named `id`, type `Long`,
  `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- No business/natural key as a primary key. Natural keys (e.g. email) get a
  `UNIQUE` constraint instead.

## PC-4 Explicit column mapping (no JPA defaults)

Every persistent field declares, explicitly:

- `@Column(nullable = …, length = …)` — length required for every `String`
  column;
- `@Column(unique = true)` or a table-level `@UniqueConstraint` for
  uniqueness;
- `@Column(name = …)` when the column name differs from the field name.

The owning design (once `db-designer` exists) states the exact constraints;
until then, the entity and `schema.sql` must agree and a human review checks
it.

## PC-5 Naming

- Tables: `snake_case`, singular (`customer`, `support_ticket`).
- Columns: `snake_case`.
- Constraints: `uq_<table>_<col>` (unique), `fk_<table>_<ref>` (foreign key),
  `ix_<table>_<col>` (index), `pk_<table>` (primary key).
- Configure Hibernate physical naming to the standard snake_case strategy so
  entity field `emailAddress` maps to column `email_address`.

## PC-6 Audit timestamps

- Every entity has `created_at` and `updated_at` columns,
  `TIMESTAMP WITH TIME ZONE` semantics, stored in **UTC**.
- Populated via JPA auditing (`@CreatedDate` / `@LastModifiedDate` with
  `@EntityListeners(AuditingEntityListener.class)` and `@EnableJpaAuditing`),
  not by hand in services.
- `created_at` is non-null, not updatable; `updated_at` is non-null.

## PC-7 Indexes

- Index every foreign key column.
- Index every column used as a lookup key by a repository query (e.g. `email`
  for "find by email"). A `UNIQUE` constraint already provides an index.

## PC-8 Relationships

- Declare cardinality explicitly (`@ManyToOne`, `@OneToMany(mappedBy = …)`,
  etc.).
- `@ManyToOne` fetch is `LAZY` by default in this project; override to
  `EAGER` only with a stated reason.
- Cascade settings are explicit and minimal — no `CascadeType.ALL` without a
  stated reason.

## PC-9 Sensitive data

- Passwords are stored only as a hash in a column named `password_hash`.
  Plaintext is never persisted or logged. **The hashing algorithm
  (BCrypt vs. Argon2id) is an open decision** — see
  `security-conventions.md` SC-1 and `docs/workflow/gaps.md`.
- Refresh tokens are stored hashed, never in plaintext, with a `revoked_at`
  column supporting reuse detection (per `docs/product/product-vision.md`'s
  "refresh rotation with reuse detection" goal). The exact schema is designed
  by the Story that implements `US-2.4`.
- Other sensitive columns are identified by the owning design with their
  handling rules.

## PC-10 Soft delete / state transitions

- Hard deletes by default. Where a row must persist for audit or ticket
  history (accounts, tickets), deletion is modeled as an explicit state
  transition (e.g. `DEACTIVATED`, `CLOSED`), never a framework-level soft
  delete filter — matching the "deletion is a state transition" constraint in
  `docs/product/product-vision.md`.
