---
name: db-designer
description: Produces database design artifacts and persistence model decisions from an approved specification.
---

# Purpose

Design the persistence model required by the User Story.

Focus on domain entities and database constraints.

---

# Required Context

Read:

- docs/specifications/<StoryId>-spec.md

Read:

- docs/product/business-rules.md
- docs/product/business-glossary.md

Read:

- docs/designs/<StoryId>-openapi.yaml

---

# Responsibilities

Identify:

- entities
- attributes
- relationships
- uniqueness constraints
- nullability rules

Define:

- primary keys
- foreign keys
- indexes

---

# Constraints

Do not rely on JPA defaults.

Explicitly define:

- lengths
- nullable status
- uniqueness
- relationship cardinality

---

# Security

Identify sensitive data.

Examples:

- passwords
- tokens
- personal information

Document storage requirements.

---

# Outputs

Create:

docs/designs/<StoryId>-db-design.md

Create:

docs/designs/<StoryId>-entity-model.md

---

# Completion Criteria

All persistence requirements are documented.

All database constraints are documented.

Entities are mapped to business concepts.