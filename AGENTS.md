# Customer Portal

## Purpose

Customer Portal is a training project used to demonstrate Agentic Software Development workflows.

The project intentionally starts from a minimal Spring Boot application and evolves through User Stories managed as repository artifacts.

The repository serves as:

- example backend system,
- agentic development sandbox,
- artifact-driven SDLC demonstration.

All changes must be traceable to documented requirements and workflow artifacts.

---

# Technology Stack

## Runtime

- Java 21

## Framework

- Spring Boot 4.x

## Build

- Gradle Kotlin DSL

## Persistence

- Spring Data JPA
- H2 Database

## Security

- Spring Security

## Testing

- JUnit 5
- Spring Boot Test

## Dependency Injection

- Spring Native DI

## Boilerplate Reduction

- Lombok

---

# Architecture Principles

The project follows a layered architecture.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Forbidden:

```text
Controller -> Repository
```

```text
Controller containing business logic
```

```text
Repository containing business logic
```

```text
Service accessing HTTP layer classes
```

---

# Package Structure

```text
org.example.customerportal

├── controller
├── service
├── repository
├── model
│   ├── entity
│   ├── dto
│   └── request
├── config
├── security
├── validation
└── exception
```

Packages should remain consistent with this structure.

---

# Source of Truth

The following artifacts define requirements.

Order of authority:

1. Story
2. Specification
3. Design Changes
4. Open Decisions
5. Implementation Plan

Implementation must never override documented requirements.

---

# Artifact Driven Development

Code generation must always be driven by artifacts.

Required flow:

```text
Story
    ↓
Specification
    ↓
Review
    ↓
Design
    ↓
Plan
    ↓
Tests
    ↓
Implementation
```

Do not start implementation directly from a User Story unless explicitly instructed.

---

# Workflow Artifacts

## Stories

Location:

```text
docs/stories
```

## Specifications

Location:

```text
docs/specifications
```

## Reviews

Location:

```text
docs/reviews
```

## Designs

Location:

```text
docs/designs
```

## Plans

Location:

```text
docs/plans
```

## Tests

Location:

```text
docs/tests
```

## Verification

Location:

```text
docs/verification
```

## Security Reviews

Location:

```text
docs/security
```

---

# Active Scope

Current workflow state is defined by:

```text
docs/workflow/active-story.yaml
```

and

```text
docs/workflow/workflow-state.yaml
```

Work only on the currently active story unless explicitly instructed otherwise.

---

# Open Decisions Policy

Open Decisions are blockers.

If an artifact references:

```text
TODO
```

```text
TBD
```

```text
???
```

or an unresolved Open Decision,

implementation should not proceed.

Instead:

1. Document missing information.
2. Request clarification.
3. Update the specification.

---

# Security Policy

Security-first defaults are mandatory.

Do not introduce insecure configurations without explicit approval.

Forbidden defaults:

```text
ddl-auto=create
ddl-auto=create-drop
ddl-auto=update
```

unless required by a documented decision.

---

```text
spring.h2.console.enabled=true
```

must never be enabled for production-like configurations.

---

Passwords must never be stored or transported in plain text.

Use:

```text
BCryptPasswordEncoder
```

unless requirements explicitly state another mechanism.

---

Authentication and authorization requirements must be documented in specifications.

Do not invent security behavior.

---

# Database Policy

Schema changes must be documented.

Entities must:

- define explicit constraints,
- define explicit lengths,
- define uniqueness rules,
- define nullability.

Avoid relying on JPA defaults.

---

# API Policy

Public API contracts should be defined before implementation.

Preferred flow:

```text
Specification
    ↓
OpenAPI Design
    ↓
Implementation
```

Endpoints should not be invented during coding if contracts are missing.

---

# Testing Policy

Implementation is incomplete without tests.

At minimum provide:

- happy path tests,
- validation tests,
- security tests.

Prefer:

```text
Specification
    ↓
Tests
    ↓
Implementation
```

---

# Build Requirements

Generated code must compile successfully.

Implementation is not complete if:

```text
Build fails
```

or

```text
IDE inspections report errors
```

---

# Verification Requirements

Every implementation must be reconciled against:

- Story
- Specification
- Design
- Plan
- Tests

Verification artifacts should be written to:

```text
docs/verification
```

---

# Git Policy

Generated changes must remain scoped to the active story.

Avoid unrelated refactoring.

Avoid opportunistic changes.

Avoid modifying files unrelated to the current Plan.

---

# Observability

Tool usage, validation results and workflow events may be logged.

Artifacts generated for observability should be written under:

```text
docs/evidence
```

or

```text
docs/hooks
```

---

# Agent Behavior

When information is missing:

Do not assume.

Do not invent requirements.

Do not invent security rules.

Do not invent business rules.

Instead:

1. Record an Open Decision.
2. Explain the uncertainty.
3. Request clarification.

Clarification is preferred over hallucination.

# Canonical Product Context

Product context is stored in:

docs/product/

Skills should load only the subset
of product documents required
for the task being executed.

Avoid loading entire product context
unless performing reconciliation
or full project analysis.

The following documents are available:

- product-vision.md
- epic-map.md
- business-glossary.md
- business-rules.md
- personas.md
- non-functional-requirements.md

When requirements, specifications, implementation plans, tests or reviews are produced, these documents should be considered authoritative sources of product knowledge.
