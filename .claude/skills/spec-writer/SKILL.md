---
name: spec-writer
description: Creates a complete Specification artifact from a clarified User Story.
---

# Purpose

Generate a complete implementation-ready Specification.

The Specification must become the primary source of truth for future design, planning, testing and implementation work.

---

# Required Context

Read:

- docs/product/product-vision.md
- docs/product/business-rules.md
- docs/product/business-glossary.md
- docs/product/non-functional-requirements.md

Read:

- active User Story

Read:

- Open Decisions for the story

---

# Preconditions

If unresolved Open Decisions exist:

DO NOT assume answers.

Document their impact inside the Specification.

---

# Specification Structure

Generate sections:

## Overview

## Business Goal

## Business Flow

## Functional Requirements

## Acceptance Criteria

## Validation Rules

## Security Requirements

## Error Handling

## Non-Functional Requirements

## Open Decisions

## Traceability

---

# Security Requirements

Always specify:

- authentication requirements
- authorization requirements
- password handling requirements
- data exposure restrictions

Do not invent security behavior.

---

# Validation Requirements

Explicitly define:

- required fields
- lengths
- uniqueness rules
- allowed values
- invalid cases

Avoid relying on framework defaults.

---

# Output

Create:

docs/specifications/<StoryId>-spec.md

---

# Completion Criteria

Complete only when:

- all acceptance criteria are represented
- validation rules are documented
- security requirements are documented
- Open Decisions are listed
- traceability section is present