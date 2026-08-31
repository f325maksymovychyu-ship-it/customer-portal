# Security Conventions

Explicit security decisions for this project. `security-reviewer` and
`design-reviewer` enforce these; `spec-writer` cites them; `springboot-implementor`
implements to them.

> **These are intentional training-project decisions, not inferred universal
> Spring Security defaults.** A Story may deviate only through a resolved Open
> Decision approved by a human.

## Training-project security policy

```yaml
authentication_model: session-based for initial MVP unless a Story explicitly introduces tokens
password_hashing: BCrypt
password_min_length: 12
password_max_length: 72
password_requirements:
  - at least one uppercase letter
  - at least one lowercase letter
  - at least one digit
  - at least one special character
csrf:
  browser_session_endpoints: enabled
  stateless_api_endpoints: requires explicit architecture decision
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

## SC-1 Passwords

- Hash with `BCryptPasswordEncoder` (Spring Security default strength). The
  encoder bean lives in the `security` package. A no-op / plaintext encoder is
  forbidden.
- Enforce the `password_*` policy above during request validation (a custom
  constraint in `validation`) **and** re-check in the Service before hashing.
- Plaintext passwords: accepted only in the inbound request DTO; never
  persisted, never logged, never returned, never placed on a response DTO.
- The hash is stored in `password_hash` (see `persistence-conventions.md` PC-9)
  and is never returned by any endpoint.

## SC-2 Roles

- `CUSTOMER` — default role on registration.
- `ADMIN` — administrative operations.
- Authority strings are `ROLE_CUSTOMER` / `ROLE_ADMIN`.
- Default account state on registration: enabled (unless a Story's approved
  design says otherwise).

## SC-3 Authentication

- Spring Security, session-based, form/HTTP-Basic as the Story requires.
- A `UserDetailsService` in the `security` package loads the account by email.
- Failed authentication returns `401` with the standard error body — it does not
  reveal whether the email exists (no account enumeration) unless a Story's
  approved design explicitly allows it.

## SC-4 Authorization

- Deny by default: every endpoint requires authentication unless the approved
  API design lists it as public (e.g. registration).
- Role checks with `@PreAuthorize` on the Service method or URL rules in the
  security config — stated per endpoint in the API design.
- Ownership checks (a customer may act only on their own resource) are enforced
  in the Service layer, not just by role.

## SC-5 CSRF

- Enabled for browser/session endpoints (the MVP default).
- Disabling CSRF for a stateless API endpoint requires an approved architecture
  decision recorded for that Story.

## SC-6 H2 console

- `spring.h2.console.enabled=false` in **every** profile, including test and
  local. Never exposed through a permissive security rule.

## SC-7 Secrets & repository hygiene

- No credentials, tokens, private keys, or `.env` files committed.
- Config secrets come from environment variables / externalized config.
- Generated H2 database files are git-ignored (see `persistence-conventions.md`
  PC-1).

## SC-8 Schema safety

- `ddl-auto` is restricted to `validate` or `none` (see the policy block and
  `persistence-conventions.md` PC-2). `create`, `create-drop`, `update` are
  forbidden — they can destroy or silently mutate the schema.

## SC-9 Error & log hygiene

- Error responses follow `api-conventions.md` AC-6 and never leak stack traces,
  SQL, entity/class names, filesystem paths, database URLs, or secrets.
- Logs never contain passwords, password hashes, `Authorization` headers,
  tokens, or full credential-bearing request bodies.
- Tool-usage telemetry (`docs/hooks/tool-usage.jsonl`) records metadata only
  (tool, timestamp, status, sizes), never full sensitive payloads.
