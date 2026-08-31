---
id: US-001
epic: EPIC-1
title: Customer Registration
slug: register-customer
priority: HIGH
source:
  type: github_issue
  repository: null
  issue_number: null
  issue_url: null
  last_synced_at: null
# Lifecycle status is owned by docs/catalog/stories.yaml (not this file).
---

# User Story

As a Customer

I want to register a new account using my email address and password

So that I can access the Customer Portal.

---

# Business Value

Allow customers to self-register without administrator involvement.

---

# Acceptance Criteria

## AC-001 Successful Registration

Given a customer provides:

- valid email
- valid password

When registration is submitted

Then:

- account is created
- customer receives role CUSTOMER
- customer can authenticate later

---

## AC-002 Unique Email

Given a customer account already exists

When another registration uses the same email

Then:

- registration is rejected
- duplicate account is not created

---

## AC-003 Email Validation

Given an invalid email format

When registration is submitted

Then:

- registration is rejected
- validation error is returned

---

## AC-004 Password Storage

Given registration succeeds

When account data is persisted

Then:

- password is not stored in plain text

---

## AC-005 Secure Response

Given registration succeeds

When response is returned

Then:

- password is not returned
- password hash is not returned

---

# Out Of Scope

- Login
- Password reset
- Email verification
- MFA
- Account activation workflow
