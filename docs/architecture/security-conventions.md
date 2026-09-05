# Security Conventions

Explicit security decisions for this project.

> These are project decisions, not inferred universal Spring Security
> defaults. A Story may deviate only through a resolved Open Decision
> approved by a human.

## Project security policy

```yaml
authentication_model: stateless JWT bearer access tokens + rotating refresh
  tokens with reuse detection (docs/product/product-vision.md goal 1)
password_hashing: OPEN DECISION — see SC-1
password_min_length: 12
password_max_length: 128
password_requirements:
  - at least one uppercase letter
  - at least one lowercase letter
  - at least one digit
  - at least one special character
access_token_lifetime: short-lived (exact value is an Open Decision; ≤ 15 min
  is the working assumption from prior AGENTS.md drafts, unconfirmed)
csrf:
  bearer_token_endpoints: disabled (stateless, no cookie-carried session)
h2_console:
  enabled: false
secrets:
  committed_to_repository: forbidden
ddl_auto:
  allowed:
    - validate
    - none
  forbidden:
    - create
    - create-drop
    - update
```

## SC-1 Passwords — OPEN DECISION

`docs/stories/README.md`'s Gherkin convention note names Argon2id as "a
deliberate constraint carried over from AGENTS.md," but no canonical document
in this project actually specifies a hashing algorithm. Until a human
resolves this, treat it as blocking for any Story that touches password
storage (`US-2.x`, `CP-101`, `US-3.2`, `US-3.3`):

- **Candidates:** BCrypt (Spring Security default, simpler) or Argon2id
  (referenced by existing backlog stories, stronger against GPU attacks).
- **Record the resolution** in `docs/decisions/` and update this file before
  `SPECIFICATION` proceeds on an affected Story.

Regardless of algorithm:

- The encoder bean lives in the `security` package. A no-op / plaintext
  encoder is forbidden.
- Enforce the `password_*` policy above during request validation (a custom
  constraint in `validation`) **and** re-check in the Service before hashing.
- Plaintext passwords: accepted only in the inbound request DTO; never
  persisted, never logged, never returned, never placed on a response DTO.
- The hash is stored in `password_hash` (see `persistence-conventions.md`
  PC-9) and is never returned by any endpoint.

## SC-2 Roles

- `CUSTOMER` — default role on registration.
- `AGENT` — support-ticket handling (`US-4.x`).
- `ADMIN` — user/role administration (`US-3.x`). No administrator can raise
  their own privilege ceiling (`docs/product/product-vision.md` goal 3).
- Authority strings are `ROLE_CUSTOMER` / `ROLE_AGENT` / `ROLE_ADMIN`.
- Default account state on registration: enabled, unless a Story's approved
  design says otherwise.

## SC-3 Authentication

- Spring Security, stateless, JWT bearer tokens issued on login (`US-2.1`).
- A `UserDetailsService` in the `security` package loads the account by
  email.
- Failed authentication returns `401` with the standard error body — it does
  not reveal whether the email exists (no account enumeration) unless a
  Story's approved design explicitly allows it.
- Refresh tokens rotate on use and are checked for reuse (`US-2.4`); a reused
  refresh token revokes the whole token family. Exact mechanics are that
  Story's approved design.

## SC-4 Authorization

- Deny by default: every endpoint requires authentication unless the
  approved API design lists it as public (e.g. registration, login).
- Role checks with `@PreAuthorize` on the Service method — stated per
  endpoint in the API design.
- Ownership checks (a customer may act only on their own resource, e.g. their
  own tickets/sessions) are enforced in the Service layer, not just by role.

## SC-5 CSRF

- Disabled for the bearer-token API (SC-3/AC-7): there is no
  cookie-carried session to forge.
- If a future Story introduces a cookie-based or session-carrying endpoint,
  enabling CSRF for that endpoint is an approved architecture decision.

## SC-6 H2 console

- `spring.h2.console.enabled=false` in **every** profile, including test and
  local. Never exposed through a permissive security rule.

## SC-7 Secrets & repository hygiene

- No credentials, tokens, private keys, or `.env` files committed.
- Config secrets (JWT signing key, etc.) come from environment variables /
  externalized config.
- Generated H2 database files are git-ignored (see
  `persistence-conventions.md` PC-1).
- MCP server configuration (`mcp.json` / `.mcp.json`) references credentials
  via `${ENV_VAR}` only — never inline. Neither file is committed (see
  `.gitignore`).

## SC-8 Schema safety

- `ddl-auto` is restricted to `validate` or `none` (see the policy block and
  `persistence-conventions.md` PC-2). `create`, `create-drop`, `update` are
  forbidden — they can destroy or silently mutate the schema.

## SC-9 Error & log hygiene

- Error responses follow `api-conventions.md` AC-6 and never leak stack
  traces, SQL, entity/class names, filesystem paths, database URLs, tokens,
  or secrets.
- Logs never contain passwords, password hashes, `Authorization` headers,
  raw access/refresh tokens, or full credential-bearing request bodies.
- Tool-usage telemetry (`docs/hooks/tool-usage.jsonl`) records metadata only
  (tool, timestamp, input shape), never full sensitive payloads.

## SC-10 Audit log integrity

- The audit log (`US-3.7`, `US-3.8`) is append-only; no role, including
  `ADMIN`, can edit or delete an entry (`docs/product/product-vision.md`
  standing constraint).
