# Non-Functional Requirements

## NFR-001 Security

Passwords must be stored using BCrypt.

---

## NFR-002 Validation

User input must be validated on server side.

---

## NFR-003 API Design

REST API conventions must be followed.

---

## NFR-004 Persistence

Entities must define:

- explicit lengths;
- nullability;
- uniqueness constraints.

---

## NFR-005 Testing

New functionality must include:

- happy path tests;
- validation tests;
- security tests.

---

## NFR-006 Traceability

Every implementation must be traceable to:

- User Story
- Specification
- Test Cases

---

## NFR-007 Build Stability

Build must succeed before a change is considered complete.

---

## NFR-008 Architecture

Controller → Service → Repository layering is mandatory.