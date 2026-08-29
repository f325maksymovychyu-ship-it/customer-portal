---
name: spec-verifier
description: Reviews a Specification artifact for completeness, consistency, and implementation readiness.
---

# Purpose

Verify whether a Specification is ready for architectural design and planning.

The skill acts as a quality gate.

---

# Required Context

Read:

- docs/product/business-rules.md
- docs/product/business-glossary.md
- docs/product/non-functional-requirements.md

Read:

- Specification

Read:

- User Story

Read:

- Open Decisions

---

# Verification Checklist

Validate:

## Completeness

- business goal exists
- acceptance criteria exist
- security requirements exist
- validation rules exist
- error handling exists

---

## Consistency

Check:

- story vs specification
- business rules vs specification
- glossary terminology consistency

---

## Traceability

Each acceptance criterion should map to:

- functional requirement

or

- validation rule

---

## Security

Verify:

- authentication requirements
- authorization requirements
- password handling expectations

---

## Open Decisions

Verify:

- all Open Decisions are listed
- impact is documented

---

# Findings

Classify findings:

## Critical

Blocks progression.

## Major

Requires correction.

## Minor

Should be improved.

---

# Outputs

Create:

docs/reviews/<StoryId>-spec-review.md

Set review result:

- APPROVED
- APPROVED_WITH_COMMENTS
- REJECTED

---

# Completion Criteria

A review artifact exists.

All findings are documented.

Approval status is explicitly stated.