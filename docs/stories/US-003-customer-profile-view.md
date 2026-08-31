---
id: US-003
epic: EPIC-1
title: View Customer Profile
slug: customer-profile-view
priority: MEDIUM
source:
  type: github_issue
  repository: null
  issue_number: null
  issue_url: null
  last_synced_at: null
# Lifecycle status is owned by docs/catalog/stories.yaml (not this file).
---

# User Story

As an authenticated Customer

I want to view my profile data

So that I can verify my information.

---

# Business Value

Allow customers to self-service basic profile information.

---

# Acceptance Criteria

## AC-001 View Own Profile

Given an authenticated customer

When profile information is requested

Then profile information is returned.

---

## AC-002 Ownership Enforcement

Given an authenticated customer

When another customer's profile is requested

Then access is denied.

---

## AC-003 Sensitive Data Exclusion

Given profile data is returned

When response is generated

Then the following must not be returned:

- password
- password hash

---

## AC-004 Consistent Response

Given profile data is returned

Then response follows API conventions.

---

# Out Of Scope

- Profile Update
- Role Management
- Account Administration
