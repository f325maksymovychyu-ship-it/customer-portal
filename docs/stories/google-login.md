---
title: Log in with Google
story_id: TBD
status: Draft for review
author: story-clarifier
last_updated: 2026-08-28
related: Future stories for additional providers (Apple, Microsoft) — not covered here
---

# Log in with Google

## 1. User Story Statement

**As a** visitor or existing customer of the web app
**I want** to sign up and sign in using my Google account
**So that** I can get in without creating or remembering another password

> `[PROPOSED — confirm]` The original story ("As a user, I want to log in with my Google account so I don't have to remember another password") named only a generic "user" and a single action. It actually spans three flows — first-time sign-up via Google, linking Google to an existing account, and signing in with an already-linked Google account — all included here as one story. Split into three if the team wants smaller slices.

## 2. Business Value & Impact

- **Problem being solved:** Email + password is the only way in today. It adds friction to sign-up (a barrier at the top of the funnel) and generates password-reset support load.
- **Expected outcome / success metric:** `[ASSUMPTION — not confirmed]` Higher sign-up completion rate and a measurable share of logins via Google; fewer password-reset requests. Target values not supplied.
- **Priority driver:** UX / conversion, with a secondary support-cost reduction.
- **Cost of not doing it:** Continued sign-up drop-off and reset-ticket volume; a gap against competitors offering social login.

## 3. Scope

**In scope:**
- A "Continue with Google" action on both the sign-in and sign-up screens.
- First Google sign-in with no matching account creates the account automatically (just-in-time provisioning) and signs the user in.
- When the Google email matches an existing email + password account, the user proves ownership with their current password once, then Google is linked to that account.
- Linking Google from account settings while already signed in.
- Unlinking Google from account settings, blocked when it would leave the account with no usable sign-in method.
- A provider-agnostic identity model (provider + provider-scoped user id) with Google as the first provider.

**Out of scope:**
- Additional providers (Apple, Microsoft, etc.) — the foundation is built for them, but no second provider ships here.
- Enterprise SSO / SAML / SCIM.
- `[ASSUMPTION — not confirmed]` Restricting sign-in to specific Google Workspace domains.
- `[ASSUMPTION — not confirmed]` Migrating or merging two pre-existing separate accounts that turn out to be the same person.

## 4. Acceptance Criteria

> Given-When-Then form only. Each scenario is independently testable.

### AC1 — New user signs up with Google (just-in-time provisioning)

```gherkin
Given I am not signed in
  And no account exists for the email on my Google profile
When I select "Continue with Google" and approve the Google consent screen
Then a new account is created, keyed to my Google identity (provider = google, provider_user_id = the Google subject id)
  And my name and profile picture from Google are stored on the new account
  And I am signed in with the same session type a password login would produce
  And I land on the same post-login destination as a password login
```

### AC2 — Returning user signs in with a linked Google account

```gherkin
Given an account exists that is already linked to my Google identity
When I select "Continue with Google" and approve (or am already approved at Google)
Then I am signed in to that account
  And no new account is created
```

### AC3 — Google email matches an existing password account

```gherkin
Given an email + password account exists for "user@example.com"
  And that account has no Google identity linked
When I select "Continue with Google" and the Google profile email is "user@example.com"
Then I am not signed in yet
  And I am asked to enter the current password for "user@example.com" to confirm the account is mine
When I enter the correct password
Then my Google identity is linked to that existing account
  And I am signed in to that account
  And no second account is created
```

### AC4 — Link Google from account settings

```gherkin
Given I am signed in to an account that has a password and no Google identity
When I choose "Connect Google account" in settings and approve the Google consent screen
Then my Google identity is linked to my current account
  And I can subsequently sign in with either my password or Google
```

### AC5 — Unlink Google when another sign-in method remains

```gherkin
Given I am signed in to an account that has both a usable password and a linked Google identity
When I choose "Disconnect Google account" in settings
Then the Google identity is removed from my account
  And I can still sign in with my password
```

### AC6 — Unlink is blocked when it would lock the user out

```gherkin
Given I am signed in to an account whose only sign-in method is Google (no password set)
When I choose "Disconnect Google account"
Then the action is refused with an explanation that I must set a password first
  And the Google identity remains linked
```

### AC7 — Identity is stored provider-agnostically

```gherkin
Given the account-to-identity data model
When a Google identity is linked to an account
Then it is stored as a row identifying the provider and the provider-scoped user id, not as a Google-specific column on the account
  And the same structure could hold a non-Google identity without a schema change
```

## 5. Edge Cases & Error Handling

> Behavior for the non-happy paths.

### EC1 — User cancels or denies consent at Google

```gherkin
Given I have been redirected to the Google consent screen
When I cancel, or deny the requested permissions
Then I am returned to the sign-in screen with a neutral message ("Sign-in with Google was cancelled")
  And no account is created and I am not signed in
```

### EC2 — Google profile has no email, or an unverified email

```gherkin
Given I approve the Google consent screen
  And Google returns no email address, or reports the email as not verified
Then sign-in does not complete
  And I see a message explaining a verified email is required to use Google sign-in
  And no account is created
```

### EC3 — OAuth error or state / CSRF mismatch on the callback

```gherkin
Given I return from Google to the app callback
When the state parameter does not match, or Google returns an error, or the authorization code exchange fails
Then sign-in is aborted
  And I see a generic "couldn't complete sign-in, please try again" message
  And the failure is logged with a correlation id
  And no partial account or session is created
```

### EC4 — This Google identity is already linked to a different account

```gherkin
Given my Google identity is already linked to account A
When I attempt "Continue with Google" in a context that would attach it to a different account B (e.g. linking from settings while signed in as B)
Then the link is refused with a message that this Google account is already connected to another account
  And account B is unchanged
```

### EC5 — Already signed in, then uses "Continue with Google"

```gherkin
Given I am already signed in as account B
When I click "Continue with Google" and authenticate as a Google identity linked to account A
Then I am signed in as account A (the session switches)
  And I am not silently left on account B
```

### EC6 — The user's Google email changes after linking

```gherkin
Given my Google identity is linked to my account
  And I later change the email address on my Google account
When I next sign in with Google
Then I am still signed in to the same account, because the link is keyed to the Google subject id, not the email
  And the app's own account email is not changed by this event
```

### EC7 — Password confirmation fails during a link attempt (AC3 / AC4 path)

```gherkin
Given I am being asked to confirm my existing password to link Google
When I enter the wrong password
Then the link does not happen and I am not signed in
  And repeated failures are rate-limited on the same terms as a normal failed login
```

| #   | Condition | Trigger | Expected behavior | Message / recovery |
|-----|-----------|---------|-------------------|--------------------|
| EC1 | User cancels/denies at Google | Return from consent screen | No account, no session | Neutral "cancelled" notice on sign-in screen |
| EC2 | No email / unverified email from Google | Callback processed | Sign-in aborted, no account | "A verified email is required" |
| EC3 | OAuth error or state mismatch | App callback | Abort, log with correlation id | Generic retry message |
| EC4 | Google identity already linked elsewhere | Link attempt to a second account | Link refused | "Already connected to another account" |
| EC5 | Already signed in as another account | "Continue with Google" | Session switches to the linked account | — |
| EC6 | Google email changed post-link | Next Google sign-in | Same account (keyed on subject id) | — |
| EC7 | Wrong password during link | Password confirmation step | No link, no session, rate-limited | Inline "incorrect password" |

## 6. Technical & Security Considerations

> Non-functional constraints only. No functional behavior in this section.

**Security**
- Authentication: OAuth 2.0 authorization-code flow with PKCE; `state` for CSRF protection and `nonce` for replay protection on the ID token; ID token signature and `aud` / `iss` / `exp` validated against Google's published keys.
- Scopes: `openid email profile` only. No Drive, Contacts, or other scopes.
- Authorization / roles: A Google sign-in grants exactly the same role and permissions the account already has; linking never elevates privileges.
- Account-takeover resistance: `[ASSUMPTION — not confirmed]` The AC3 password challenge exists specifically so a compromised Google inbox cannot silently absorb an existing password account. Confirm this is the intended security posture.
- Data sensitivity: Google `sub`, email, display name, and avatar URL are PII. `[ASSUMPTION — not confirmed]` Stored under the app's existing PII handling; legal basis is the existing sign-up terms/consent. No additional Google data is requested or retained.
- Session: Identical lifetime, cookie flags, and revocation behavior as a password login.
- Secrets: OAuth client secret held in the existing secret manager, never in source or client-side code.
- Input validation: The callback treats all Google-supplied fields as untrusted until the ID token is verified.
- Audit / logging: Record `sso_sign_in`, `identity_linked`, `identity_unlink_attempt`, and `identity_unlinked` with account id, provider, timestamp, and source IP. Never log tokens.

**Performance & scale**
- `[ASSUMPTION — not confirmed]` Sign-in round-trip (redirect to Google and back, token exchange, session issue) completes in p95 < 2s excluding time on Google's own screens.
- Volume expected to be a fraction of total auth traffic; no special capacity work anticipated.

**Reliability & observability**
- Degradation: If Google's OAuth endpoints are unreachable, the "Continue with Google" path fails with a retry message; email + password sign-in is unaffected.
- Metrics: Google sign-in attempts / successes / failures by failure reason, link successes/failures, unlink-blocked count.

**Dependencies**
- Google Cloud project with an OAuth 2.0 client (web) and a configured, verified consent screen. `[ASSUMPTION — not confirmed]` Consent-screen branding verification by Google can take several business days and should start before the sprint.
- New `auth_identities` table (account id, provider, provider_user_id, linked_at); unique constraint on (provider, provider_user_id).
- Existing session/auth service.
- Data migration / backfill: None. Existing accounts keep password-only sign-in until a user links Google.

**UX / accessibility**
- `[ASSUMPTION — not confirmed]` "Continue with Google" button appears on both the sign-in and sign-up screens, styled per Google's branding guidelines, using a full-page redirect (not a popup) for broad browser and mobile compatibility.
- `[ASSUMPTION — not confirmed]` On JIT account creation the user lands directly on the app (no extra profile step); name and avatar are taken from Google only at creation and never overwrite values the user later edits.
- `[ASSUMPTION — not confirmed]` Screens and messages meet WCAG 2.1 AA; copy owned by product/content.

**Assumptions (consolidated)**
- `[ASSUMPTION — not confirmed]` A Google-verified email is accepted as satisfying the app's email-verification requirement; an unverified Google email is rejected (EC2).
- `[ASSUMPTION — not confirmed]` Personal and Google Workspace accounts are treated identically; no domain allow-list.
- `[ASSUMPTION — not confirmed]` The button is on both sign-in and sign-up screens; redirect-based flow.
- `[ASSUMPTION — not confirmed]` Profile fields (name, avatar) are copied from Google at account creation only, never re-synced.
- `[ASSUMPTION — not confirmed]` No auto sign-in after the AC3 link step beyond the session that step creates.
- `[ASSUMPTION — not confirmed]` "Usable password" for the AC6 unlink guard means a password credential the user has actually set (not a null/placeholder).

## 7. Open Questions

| #   | Question | Owner | Blocking? |
|-----|----------|-------|-----------|
| 1   | Confirm the AC3 security posture: require the existing password before linking Google to a matching email account (assumed), vs. trust Google's verified email and auto-link. | Product + Security | Yes (assumption in place) |
| 2   | Is a Google-verified email accepted in place of the app's own email verification, and what happens if Google reports the email unverified? | Product + Security | No |
| 3   | Which screens carry the "Continue with Google" button — sign-in only, or sign-up too? | Product / Design | No |
| 4   | On JIT account creation, is there any required step before the user reaches the app (accept terms, choose a name/workspace)? | Product | No |
| 5   | Do we pull the Google profile picture and display name, and are they ever re-synced or only set once? | Product | No |
| 6   | Should personal Gmail and Google Workspace accounts be treated the same, or is a Workspace-domain restriction wanted (now or later)? | Product | No |
| 7   | Success metric: what does the team measure to call this a win (sign-up completion rate, share of logins via Google, reset-ticket volume)? | Product | No |
| 8   | Is any handling needed for the case where two separate existing accounts turn out to belong to the same person once Google is introduced? | Product | No |
| 9   | Confirm target languages / localization scope for the new screens and messages. | Product / Content | No |
