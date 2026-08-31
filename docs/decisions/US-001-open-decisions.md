---
artifact_type: open_decisions
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T00:33:59Z
updated_at: 2026-08-31T00:33:59Z
produced_by: us-clarifier
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
supersedes: null
---

# Open Decisions — US-001 Customer Registration

Each entry is a decision a human must make. Status `OPEN` entries are resolved at
`HUMAN_SPEC_APPROVAL`, not during CLARIFICATION. `recommended` values are
non-binding.

---

## OD-001 — Email length limit and accepted format

- **id:** OD-001
- **question:** What is the maximum stored length of a customer email, and what
  format rule accepts/rejects it (strict RFC 5321/5322, a pragmatic single-`@`
  pattern, or Bean Validation `@Email`)?
- **context:** AC-003 requires invalid email formats to be rejected with a
  validation error, but neither the Story nor the product docs define the rule.
  `persistence-conventions.md` PC-4 and NFR-004 require every `String` column to
  declare an explicit length, so a value must be chosen before DB_DESIGN. BR-002
  already fixes that comparison is case-insensitive.
- **affects:** SPECIFICATION, API_DESIGN, DB_DESIGN, TEST_WRITING
- **status:** OPEN
- **options:**
  - A. `@Email` (Jakarta) + max length 254 (RFC 5321 practical maximum).
  - B. Custom stricter regex (single `@`, non-empty local part, domain with a
    dot) + max length 254.
  - C. `@Email` + a shorter project cap (e.g. 190) to keep index size small.
- **recommended:** A — `@Email` with a 254-character column, matching the RFC
  practical maximum; adequate for a training project.

---

## OD-002 — CSRF classification of the registration endpoint

- **id:** OD-002
- **question:** Is `POST /api/v1/customers` treated as a browser/session
  endpoint (CSRF token required) or as a stateless API endpoint (CSRF disabled
  for this path)?
- **context:** `security-conventions.md` SC-5 keeps CSRF enabled for
  browser/session endpoints (the MVP default) and states that disabling CSRF for
  a stateless API endpoint "requires an approved architecture decision recorded
  for that Story." Registration is unauthenticated and has no prior session, so
  the classification is genuinely undetermined and must be decided here.
- **affects:** SPECIFICATION, API_DESIGN, IMPLEMENTATION, SECURITY_REVIEW
- **status:** OPEN
- **options:**
  - A. Keep CSRF enabled globally; the registration form obtains a CSRF token
    first (consistent with the session-based MVP default).
  - B. Record an architecture decision exempting `POST /api/v1/customers` from
    CSRF as a stateless public endpoint.
- **recommended:** B — registration is a public, pre-session JSON endpoint;
  exempt just this path and keep CSRF on everywhere else.

---

## OD-003 — Duplicate-email response vs. account enumeration

- **id:** OD-003
- **question:** When registration uses an email that already exists, does the
  API return an explicit `409` "an account with this email already exists"
  message, or a response that does not disclose whether the email is registered?
- **context:** AC-002 requires the duplicate registration to be rejected and
  `api-conventions.md` AC-6 shows a `409` duplicate-email example. Separately
  SC-3 forbids account enumeration, but only in the scope of *failed
  authentication*. Whether that principle extends to registration is a policy
  choice the docs do not resolve.
- **affects:** SPECIFICATION, API_DESIGN, SECURITY_REVIEW, TEST_WRITING
- **status:** OPEN
- **options:**
  - A. Return `409 Conflict` with the explicit duplicate-email message from
    AC-6 (best UX, matches the documented example).
  - B. Return a neutral success-shaped response and send no account, hiding
    whether the email exists (strongest anti-enumeration).
  - C. `409` with a generic "registration could not be completed" message
    (compromise).
- **recommended:** A — align with AC-6's documented example; account
  enumeration on a self-service registration form is low-risk for this training
  project and A gives the clearest customer experience.

---

## OD-004 — Fields returned in the successful `201` response body

- **id:** OD-004
- **question:** Which customer fields does the `201 Created` registration
  response include in its body?
- **context:** `api-conventions.md` AC-4 requires `POST /collection` to return
  the "created resource body", and AC-005 states only what must be absent
  (password, password hash). The positive field list is undefined and
  `spec-writer` needs it to define the response DTO.
- **affects:** SPECIFICATION, API_DESIGN
- **status:** OPEN
- **options:**
  - A. `id`, `email`, `role`, `createdAt`.
  - B. `id`, `email` only.
  - C. `id`, `email`, `role`, `enabled`, `createdAt`.
- **recommended:** A — enough for a client to confirm the account and its
  assigned role without exposing internal state.

---

## OD-005 — Anti-abuse controls on self-service registration (scope check)

- **id:** OD-005
- **question:** Does US-001 include any rate limiting / abuse protection on the
  registration endpoint, or is that explicitly deferred?
- **context:** The Story enables self-registration "without administrator
  involvement" but the NFRs and the Out-of-Scope list are both silent on abuse
  protection. Recorded so the decision is explicit for `security-reviewer`.
- **affects:** SPECIFICATION, SECURITY_REVIEW
- **status:** OPEN
- **options:**
  - A. Out of scope for US-001; note as a follow-up.
  - B. In scope; add a basic per-IP rate limit.
- **recommended:** A — out of scope for US-001; no NFR requires it and it is not
  in the Acceptance Criteria.

---

## OD-006 — Email normalization mechanism at persistence

- **id:** OD-006
- **question:** How is case-insensitive uniqueness (BR-001 + BR-002) enforced in
  persistence — store the email lowercased, or store as entered with a
  functional / lower-case unique index?
- **context:** BR-002 already fixes the *requirement* (comparison is
  case-insensitive); only the storage mechanism is open, and it is a DB_DESIGN
  concern. Recorded so DB_DESIGN makes it explicitly rather than by default.
- **affects:** DB_DESIGN
- **status:** OPEN
- **options:**
  - A. Normalize to lowercase in the service before persisting; plain `UNIQUE`
    on the column.
  - B. Store as entered; enforce uniqueness with a lower-case/functional unique
    index.
- **recommended:** A — simplest to implement and reason about for a training
  project.
