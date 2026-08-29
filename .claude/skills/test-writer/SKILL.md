---
name: test-writer
description: >
  Creates story-level automated tests from approved Acceptance Criteria,
  specification, API contract, database design, and implementation plan.
  Use before production implementation.
---

# Test Writer

## Purpose

Create executable evidence proving that the implementation satisfies the
Acceptance Criteria.

## Preconditions

- Specification approved
- API design approved
- Database design approved
- Implementation plan approved
- Human Plan Approval completed

## Inputs

- User Story
- Acceptance Criteria
- Specification
- OpenAPI Design
- Database Design
- Impact Analysis
- Approved Plan

## Workflow

1. Create Acceptance Criteria to Test mapping.
2. Create positive scenarios.
3. Create negative scenarios.
4. Create boundary scenarios.
5. Create validation scenarios.
6. Create security-relevant scenarios.
7. Determine appropriate test levels:
    - contract tests
    - integration tests
    - unit tests
8. Implement automated tests.
9. Generate traceability matrix.

## Output Artifacts

Source files: