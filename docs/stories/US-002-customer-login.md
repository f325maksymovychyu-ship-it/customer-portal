---
id: US-002
epic: EPIC-1
title: Customer Login
slug: customer-login
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

I want to authenticate using my email and password

So that I can access protected functionality.

---

# Business Value

Allow registered customers to access the portal securely.

---

# Acceptance Criteria

## AC-001 Successful Login

Given a registered customer

When valid credentials are provided

Then authentication succeeds.

---

## AC-002 Invalid Password

Given a registered customer

When an invalid password is provided

Then authentication fails.

---

## AC-003 Unknown Account

Given no account exists

When login is attempted

Then authentication fails.

---

## AC-004 Disabled Account

Given an account is disabled

When login is attempted

Then authentication fails.

---

## AC-005 Secure Authentication Response

Given authentication succeeds

When response is returned

Then sensitive credential information is never returned.

---

# Out Of Scope

- MFA
- OAuth
- Social Login
- Password Recovery
