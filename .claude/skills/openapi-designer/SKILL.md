---
name: openapi-designer
description: Produces OpenAPI contract and API design artifacts from an approved specification.
---

# Purpose

Design API contracts before implementation begins.

This skill is responsible for producing the authoritative API contract.

Implementation should follow the contract.

---

# Required Context

Read:

- docs/specifications/<StoryId>-spec.md

Read:

- docs/product/business-rules.md
- docs/product/business-glossary.md
- docs/product/non-functional-requirements.md

Read:

- docs/reviews/<StoryId>-spec-review.md

---

# Preconditions

Specification review status must be:

APPROVED

or

APPROVED_WITH_COMMENTS

Do not proceed if review status is REJECTED.

---

# Design Responsibilities

Define:

- endpoints
- HTTP methods
- request payloads
- response payloads
- status codes
- validation rules
- error responses

Document:

- authentication requirements
- authorization requirements

---

# Error Handling

Explicitly define:

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict

when applicable.

---

# Validation

All validation constraints from Specification must be reflected in API contracts.

---

# Outputs

Create:

docs/designs/<StoryId>-openapi.yaml

Create:

docs/designs/<StoryId>-api-design.md

---

# Completion Criteria

OpenAPI contract exists.

All acceptance criteria are represented.

Authentication and authorization requirements are documented.

Validation requirements are documented.