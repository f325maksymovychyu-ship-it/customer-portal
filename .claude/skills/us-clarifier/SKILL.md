---
name: us-clarifier
description: Clarifies a User Story, identifies ambiguities, missing requirements, and Open Decisions before specification writing.
---

# Purpose

Analyze a User Story and prepare it for Specification creation.

The goal is to eliminate ambiguity and identify missing information before the Specification phase begins.

---

# Required Context

Read:

- docs/product/product-vision.md
- docs/product/personas.md
- docs/product/business-rules.md

Read current story from:

- docs/stories/

Read workflow state from:

- docs/workflow/active-story.yaml

---

# Responsibilities

Analyze:

- business intent
- actor
- business value
- acceptance criteria
- security expectations
- validation expectations
- dependencies
- assumptions

Identify:

- ambiguities
- contradictions
- missing acceptance criteria
- missing validation rules
- missing security requirements
- missing non-functional expectations

---

# Open Decision Detection

When information cannot be reliably inferred from available artifacts:

DO NOT invent requirements.

Instead create an Open Decision.

Examples:

- uniqueness rules
- password policy
- authorization rules
- validation constraints
- duplicate handling
- error handling

---

# Outputs

Create:

docs/decisions/<StoryId>-open-decisions.md

and

docs/evidence/<StoryId>-clarification-report.md

---

# Completion Criteria

Complete only when:

- story scope is understood
- ambiguities are documented
- open decisions are documented
- clarification report is created

Do not write specifications.