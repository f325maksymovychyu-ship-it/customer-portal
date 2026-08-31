---
artifact_type: specification_review
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T01:19:10Z
updated_at: 2026-08-31T01:22:41Z
produced_by: spec-verifier
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
  - path: docs/evidence/US-001-clarification-report.md
    version: 1
  - path: docs/product/business-rules.md
    version: null
  - path: docs/product/business-glossary.md
    version: null
  - path: docs/product/non-functional-requirements.md
    version: null
  - path: docs/architecture/api-conventions.md
    version: null
  - path: docs/architecture/security-conventions.md
    version: null
  - path: docs/architecture/persistence-conventions.md
    version: null
supersedes: null
---

# Specification Review — US-001 Customer Registration

## Summary

**Verdict: PASS.** The Specification is complete, internally consistent, fully
traceable to the Story and the product/architecture conventions, and expressed
in testable terms. All six Open Decisions are carried forward with their impact
documented and every dependent requirement flagged with a draft assumption.
There are no `Critical` or `Major` findings. Six `Minor` (advisory) findings
are listed below; none block progression to `HUMAN_SPEC_APPROVAL`, where a human
resolves OD-001..OD-006.

## Reviewed Artifacts

| Artifact | Path | Version |
|---|---|---|
| Story | `docs/stories/US-001-register-customer.md` | (unversioned input) |
| Specification | `docs/specifications/US-001-spec.md` | 1 (DRAFT) |
| Open Decisions | `docs/decisions/US-001-open-decisions.md` | 1 |
| Clarification Report | `docs/evidence/US-001-clarification-report.md` | 1 |

Precondition check: the Specification exists, is not `SUPERSEDED`, and its
`inputs` front matter references the current `clarification_report` (v1) and
`open_decisions` (v1). The `story` input is unversioned per the
`artifact-schema.md` Story-artifact exception. Not stale — review is not
`BLOCKED`.

## Completeness

| Area | Present | Location |
|---|---|---|
| Business goal | Yes | §2, cites `product-vision.md` and Story Business Value |
| Business flow | Yes | §3 |
| Functional requirements | Yes | §4, FR-1..FR-11 |
| Acceptance Criteria | Yes | §5, AC-001..AC-007 |
| Validation rules (server-side) | Yes | §6.1 email, §6.2 password, §6.3 request shape |
| Security requirements | Yes | §7, SEC-1..SEC-11, each cited to SC-* or an OD |
| Error handling | Yes | §8, status/`error`/body table via single `@RestControllerAdvice` |
| Non-functional requirements | Yes | §9, NFR-1..NFR-8 mapped to source NFRs |
| Out of scope | Yes | §10 |
| Open Decisions with impact | Yes | §11 |
| Traceability | Yes | §12.1–§12.3 |

No mandatory section is missing. The password policy (12–72, upper/lower/digit/
special) matches `security-conventions.md` SC-1 exactly. The success path
(BCrypt hash, `CUSTOMER` role, `enabled = true`, UTC audit timestamps,
`201` + `Location`, safe response body) is fully specified.

## Consistency

- **Story vs Specification** — consistent. Story AC-001..AC-005 map to Spec
  AC-001..AC-005 (§12.2); AC-006 (password-policy enforcement) and AC-007
  (media-type enforcement) are correctly labelled derived. Story Out-of-Scope
  items all reappear in §10.
- **Business rules vs Specification** — consistent. BR-001/BR-002 (unique,
  case-insensitive email) → §6.1 + FR-4; BR-003 (one account per customer) →
  FR-4; BR-006 (`CUSTOMER` default role) → FR-5/SEC-5; BR-007 (UTC timestamps)
  → FR-5/NFR-4.
- **Architecture conventions vs Specification** — consistent. `api-conventions.md`
  AC-1/AC-3 (`/api/v1/customers`, plural noun) → FR-1; AC-4 (`201` + `Location`
  + body) → FR-8; AC-5 (`400`/`409`/`415`) → §8; AC-6 (error body + `fieldErrors[]`)
  → FR-3/§8; AC-9 (single `@RestControllerAdvice`) → FR-10. `security-conventions.md`
  SC-1 (BCrypt, dual-layer policy check, no plaintext exposure) → FR-6/FR-7/SEC-2/
  SEC-3; SC-4 (deny-by-default, registration public) → SEC-1; SC-5 (CSRF) →
  FR-11/SEC-7 via OD-002; SC-6 (H2 console off) → SEC-9; SC-8 (`ddl-auto`) →
  SEC-10.
- **Glossary terminology** — checked against `business-glossary.md`. "Customer"
  (person who owns an account), "Registration", and "Role" (`CUSTOMER` / `ADMIN`)
  are used consistently. One nuance: the glossary separates *Customer* (the
  person) from *Account* (the technical credential representation), whereas the
  Specification uses "Customer" for the persisted entity (FR-5, "persist a new
  Customer"). This is a common conflation and does not create a requirement
  ambiguity; see F-6.
- No contradictions found, consistent with the Clarification Report §3.

## Traceability

Every Acceptance Criterion maps to at least one functional requirement and the
governing validation/security rule (Spec §12.1):

| AC | FR(s) | Rule(s) | Testable outcome |
|---|---|---|---|
| AC-001 | FR-1, FR-5, FR-8, FR-9 | SEC-5; §6.1/§6.2 valid path | `201`, `Location` header, account with `CUSTOMER` role |
| AC-002 | FR-4 | §6.1 uniqueness; SEC-8; §8 | duplicate rejected (per OD-003), no 2nd account |
| AC-003 | FR-3 | §6.1 format + length | `400` + `fieldErrors[].field="email"`, no account |
| AC-004 | FR-5, FR-6, FR-7 | SEC-2, SEC-4; PC-9 | stored value is a BCrypt hash in `password_hash` |
| AC-005 | FR-7, FR-8 | SEC-3, SEC-4 | response body excludes password and hash |
| AC-006 | FR-3, FR-6 | §6.2 policy | `400` + `fieldErrors[].field="password"`, no account |
| AC-007 | FR-2 | §8 `415` row | `415`, no account |

Requirement → source mapping is provided in §12.3 and checks out against the
cited documents. No orphan requirement (every FR ties to a Story AC or a named
convention); no unmet Story AC.

## Security

All security requirements in §7 cite `security-conventions.md` (SC-1, SC-2,
SC-4, SC-5, SC-6, SC-7, SC-8, SC-9) or an Open Decision (OD-002, OD-003). None
are invented. Authentication posture (endpoint public, all others deny-by-default),
credential handling (BCrypt, plaintext only on inbound DTO, hash never returned),
and error/log hygiene (no stack traces, SQL, class names) are all stated.
Account-enumeration exposure from the duplicate-email response is explicitly
surfaced as a human decision (OD-003 / SEC-8) rather than assumed. CSRF handling
is deferred to OD-002 with the SC-5 requirement (architecture decision must be
recorded if the path is exempted) called out.

## Open Decisions

| OD | Appears in Spec | Impact documented | Dependent requirements flagged |
|---|---|---|---|
| OD-001 email length + format | §6.1, §11 | Yes (§11 table) | FR-3, §6.1, AC-003 |
| OD-002 CSRF classification | FR-11, SEC-7, §11 | Yes | FR-11, SEC-7 |
| OD-003 duplicate-email response | FR-4, §6.1, §8, SEC-8, §11 | Yes | FR-4, AC-002, §8 |
| OD-004 `201` response body fields | FR-8, §11 | Yes | FR-8, AC-005 |
| OD-005 anti-abuse scope | §7, §10, §11 | Yes | §7, §10 |
| OD-006 email normalization | §6.1, §11 | Yes (DB_DESIGN only) | §6.1 note |

All six Open Decisions from `open_decisions` v1 are represented. Each dependent
statement in the Specification is marked with its OD id and a "draft" assumption
matching the recommended option. The Specification correctly states (§11) that
none of these prevent expressing the mandatory requirements, so the verdict is
not `BLOCKED`.

## Findings

| # | Severity | Finding | Suggested handling |
|---|---|---|---|
| F-1 | Minor | AC-007 is annotated "derived (AC-2, `api-conventions.md`)". The token "AC-2" can be misread as Story AC-002; the intended reference is `api-conventions.md` AC-2 (media type) plus Clarification assumption A-2. | Spec-writer may disambiguate the annotation on the next revision; §12.2 already clarifies it. Not blocking. |
| F-2 | Minor | §6.3 defers unknown/extra JSON field handling ("reject as 400" vs "ignore") to API_DESIGN without recording it as an Open Decision. | Acceptable as an explicit design-stage deferral; API_DESIGN should record the choice. Consider a note in §11 for visibility. |
| F-3 | Minor | FR-2 couples two distinct failures — missing `Content-Type: application/json` (`415`) and body shape (`email`/`password` only). The `415` rule is also in §8; the "exactly two fields" rule is only in prose. | Optionally split into request-shape validation in §6.3 for a cleaner TEST_WRITING mapping. |
| F-4 | Minor | SEC-4 and §12.3 cite `persistence-conventions.md` PC-9 (`password_hash VARCHAR(60)`), a DB-design detail surfaced early in the Specification. Verified accurate against PC-9 (`VARCHAR(60)`, non-null). | No change needed; DB_DESIGN owns the final column definition. |
| F-5 | Minor | §6.2 states the BCrypt 72-byte input limit as a `password` max length of 72 "characters". Multi-byte characters make byte-length < 72 characters possible. | DB/impl detail; TEST_WRITING and IMPLEMENTATION should treat the limit as 72 bytes per BCrypt. Not a spec defect. |
| F-6 | Minor | The Specification names the persisted entity "Customer"; `business-glossary.md` reserves "Account" for the technical credential representation and "Customer" for the person. | Entity/table naming is a DB_DESIGN decision; DB_DESIGN should reconcile the entity name with the glossary or record the chosen convention. |

No `Critical` findings. No `Major` findings.

## Verdict

**PASS** — the Specification is implementation-ready and may proceed to
`HUMAN_SPEC_APPROVAL`. The five `Minor` findings are advisory and are carried as
`non_blocking_findings`. A human must resolve OD-001..OD-006 at the approval
gate; a `PASS` here is not human approval.

```yaml
result:
  verdict: PASS
  stage: SPEC_REVIEW
  story: US-001
  artifact_status: APPROVED
  artifacts:
    - docs/reviews/specifications/US-001-spec-review.md
  next_stage: HUMAN_SPEC_APPROVAL
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "F-1: AC-007 annotation \"AC-2\" is ambiguous with Story AC-002; means api-conventions.md AC-2."
    - "F-2: §6.3 defers unknown-JSON-field handling to API_DESIGN without an Open Decision entry."
    - "F-3: FR-2 bundles Content-Type (415) and body-shape rules; body-shape rule only in prose."
    - "F-4: PC-9 password_hash VARCHAR(60) cited early; DB_DESIGN owns the final column definition."
    - "F-5: §6.2 password max 72 'characters' vs BCrypt's 72-byte input limit; treat as bytes downstream."
    - "F-6: entity named 'Customer' vs business-glossary 'Account' for the credential representation; DB_DESIGN to reconcile."
```
