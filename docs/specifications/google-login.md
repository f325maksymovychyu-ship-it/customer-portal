---
story_id: none
title: Log in with Google
source: docs/stories/google-login.md
status: draft
revision: 1
last_updated: 2026-08-28
---

# Log in with Google

## 1. Story

> As a visitor or existing customer of the web app, I want to sign up and sign in using my Google account, so that I can get in without creating or remembering another password.

The source story's original one-line form was: *"As a user, I want to log in with my Google account so I don't have to remember another password."* The clarified source reconstructs the frame and marks it `[PROPOSED — confirm]`; that reconstruction is carried here as A-1. The source treats this as one story spanning three flows — first-time sign-up via Google, linking Google to an existing account, and signing in with an already-linked Google account.

## 2. Acceptance Criteria

Verbatim from `docs/stories/google-login.md`. The source labels seven **acceptance criteria** (AC-1…AC-7) and seven **edge-case / error scenarios** (EC-1…EC-7). Both sets are reproduced verbatim below and both are treated as the requirement set for this spec. Identifiers are kept exactly as the source numbers them.

### 2.1 Acceptance criteria

| ID | Acceptance Criterion |
|----|----------------------|
| AC-1 | **New user signs up with Google (just-in-time provisioning).** Given I am not signed in and no account exists for the email on my Google profile, when I select "Continue with Google" and approve the Google consent screen, then a new account is created, keyed to my Google identity (provider = google, provider_user_id = the Google subject id), and my name and profile picture from Google are stored on the new account, and I am signed in with the same session type a password login would produce, and I land on the same post-login destination as a password login. |
| AC-2 | **Returning user signs in with a linked Google account.** Given an account exists that is already linked to my Google identity, when I select "Continue with Google" and approve (or am already approved at Google), then I am signed in to that account, and no new account is created. |
| AC-3 | **Google email matches an existing password account.** Given an email + password account exists for "user@example.com" and that account has no Google identity linked, when I select "Continue with Google" and the Google profile email is "user@example.com", then I am not signed in yet, and I am asked to enter the current password for "user@example.com" to confirm the account is mine. When I enter the correct password, then my Google identity is linked to that existing account, and I am signed in to that account, and no second account is created. |
| AC-4 | **Link Google from account settings.** Given I am signed in to an account that has a password and no Google identity, when I choose "Connect Google account" in settings and approve the Google consent screen, then my Google identity is linked to my current account, and I can subsequently sign in with either my password or Google. |
| AC-5 | **Unlink Google when another sign-in method remains.** Given I am signed in to an account that has both a usable password and a linked Google identity, when I choose "Disconnect Google account" in settings, then the Google identity is removed from my account, and I can still sign in with my password. |
| AC-6 | **Unlink is blocked when it would lock the user out.** Given I am signed in to an account whose only sign-in method is Google (no password set), when I choose "Disconnect Google account", then the action is refused with an explanation that I must set a password first, and the Google identity remains linked. |
| AC-7 | **Identity is stored provider-agnostically.** Given the account-to-identity data model, when a Google identity is linked to an account, then it is stored as a row identifying the provider and the provider-scoped user id, not as a Google-specific column on the account, and the same structure could hold a non-Google identity without a schema change. |

### 2.2 Edge-case / error scenarios

| ID | Scenario |
|----|----------|
| EC-1 | **User cancels or denies consent at Google.** Given I have been redirected to the Google consent screen, when I cancel, or deny the requested permissions, then I am returned to the sign-in screen with a neutral message ("Sign-in with Google was cancelled"), and no account is created and I am not signed in. |
| EC-2 | **Google profile has no email, or an unverified email.** Given I approve the Google consent screen and Google returns no email address, or reports the email as not verified, then sign-in does not complete, and I see a message explaining a verified email is required to use Google sign-in, and no account is created. |
| EC-3 | **OAuth error or state / CSRF mismatch on the callback.** Given I return from Google to the app callback, when the state parameter does not match, or Google returns an error, or the authorization code exchange fails, then sign-in is aborted, and I see a generic "couldn't complete sign-in, please try again" message, and the failure is logged with a correlation id, and no partial account or session is created. |
| EC-4 | **This Google identity is already linked to a different account.** Given my Google identity is already linked to account A, when I attempt "Continue with Google" in a context that would attach it to a different account B (e.g. linking from settings while signed in as B), then the link is refused with a message that this Google account is already connected to another account, and account B is unchanged. |
| EC-5 | **Already signed in, then uses "Continue with Google".** Given I am already signed in as account B, when I click "Continue with Google" and authenticate as a Google identity linked to account A, then I am signed in as account A (the session switches), and I am not silently left on account B. |
| EC-6 | **The user's Google email changes after linking.** Given my Google identity is linked to my account and I later change the email address on my Google account, when I next sign in with Google, then I am still signed in to the same account, because the link is keyed to the Google subject id, not the email, and the app's own account email is not changed by this event. |
| EC-7 | **Password confirmation fails during a link attempt (AC-3 / AC-4 path).** Given I am being asked to confirm my existing password to link Google, when I enter the wrong password, then the link does not happen and I am not signed in, and repeated failures are rate-limited on the same terms as a normal failed login. |

## 3. Functional Specification

Every normative statement below carries the AC or EC it derives from. Security mechanisms that no AC or EC names (PKCE, nonce, scope minimization, ID-token validation) are **not** in this section — they are recorded as assumptions in §6, per the source story.

### 3.1 Entry points

The app presents a "Continue with Google" action to an unauthenticated user, from which the flows in §3.2–§3.4 begin. `[AC-1]` `[AC-2]` `[AC-3]`

The same capability is offered to a signed-in user from account settings as "Connect Google account". `[AC-4]`

The set of screens that carry the unauthenticated button (sign-in only, or sign-in and sign-up) is not fixed by any AC — see OQ-3; A-4 records the position taken.

### 3.2 First Google sign-in — no matching account

When a user completes Google authentication and no account exists for the Google profile's email, the app:

- creates a new account and stores a linked identity for it, recording the provider as `google` and the provider-scoped user id as the Google subject (`sub`) id; `[AC-1]` `[AC-7]`
- stores the user's name and profile picture from the Google profile on the new account; `[AC-1]`
- establishes a session of the same type and with the same properties a password login produces; `[AC-1]`
- routes the user to the same post-login destination a password login uses. `[AC-1]`

No intermediate profile-completion step is specified — see OQ-4.

### 3.3 Sign-in with an already-linked Google identity

When a user completes Google authentication and the resulting identity (`google`, `sub`) is already linked to an account, the app signs the user in to that account and creates no new account. `[AC-2]`

Because the stored link is keyed on the Google subject id, a later change to the email address on the user's Google account does not affect which app account they reach, and does not change the app account's own email. `[EC-6]`

If the user is already signed in as a different account when they do this, the session switches to the linked account rather than leaving them on the previous one. `[EC-5]`

### 3.4 First Google sign-in — email matches an existing password account

When a user completes Google authentication, the identity is not yet linked, and the Google profile email matches an existing account that has a password and no linked Google identity, the app:

- does not sign the user in yet; `[AC-3]`
- prompts the user to enter the current password for that account to confirm ownership; `[AC-3]`
- on a correct password, links the Google identity to that existing account, signs the user in to it, and creates no second account; `[AC-3]`
- on an incorrect password, does not link and does not sign the user in, and rate-limits repeated failures on the same terms as a normal failed login. `[EC-7]`

The behavior above is what AC-3 states. Whether this password-confirmation step is the desired security posture, versus trusting Google's verified email and linking automatically, is unresolved — see OQ-1, which may cause AC-3 to be rewritten.

### 3.5 Linking and unlinking from account settings

A signed-in user with a password and no linked Google identity can link one by choosing "Connect Google account" and approving the Google consent screen; afterwards either the password or Google can be used to sign in. `[AC-4]`

A signed-in user whose account has both a usable password and a linked Google identity can remove the Google identity by choosing "Disconnect Google account"; afterwards the password still works. `[AC-5]`

If the account's only sign-in method is Google (no password set), "Disconnect Google account" is refused, with an explanation that a password must be set first, and the identity remains linked. `[AC-6]` The meaning of "usable password" / "password set" is taken as an assumption — see A-7.

An attempt to link a Google identity that is already linked to another account is refused, with a message that the Google account is already connected elsewhere, and the target account is left unchanged. `[EC-4]`

### 3.6 Identity data model

A linked identity is stored as a row that names the provider and the provider-scoped user id, separate from the account record — not as Google-specific columns on the account. The structure holds a non-Google identity without a schema change. `[AC-7]` See §4 for the fields the ACs name or imply.

### 3.7 Failure and cancellation handling

- If the user cancels or denies consent at Google, they are returned to the sign-in screen with a neutral "Sign-in with Google was cancelled" message; no account is created and no session is established. `[EC-1]`
- If Google returns no email, or an email it reports as unverified, sign-in does not complete, the user is shown a message that a verified email is required, and no account is created. `[EC-2]` Whether a Google-verified email is accepted in place of the app's own email verification is a separate open decision — see OQ-2.
- If the `state` parameter on the callback does not match, or Google returns an error, or the authorization-code exchange fails, sign-in is aborted, the user sees a generic "couldn't complete sign-in, please try again" message, the failure is logged with a correlation id, and no partial account or session is created. `[EC-3]`

## 4. Data and Interfaces

Fields and structures the ACs name or clearly imply. Where an AC names something but constrains nothing about its format, the format is recorded as "not specified" rather than guessed.

| # | Field / structure | Format | Source |
|---|-------------------|--------|--------|
| 1 | Linked identity — provider | Enumerated provider key; value `google` for this story | `[AC-1]` `[AC-7]` |
| 2 | Linked identity — provider_user_id | The Google subject (`sub`) id; string; opaque | `[AC-1]` `[AC-7]` |
| 3 | Linked identity — relationship to account | A separate row/record referencing the account, not columns on the account; one structure reusable for other providers | `[AC-7]` |
| 4 | Account — name | Taken from the Google profile at creation; format not specified | `[AC-1]` |
| 5 | Account — profile picture | Taken from the Google profile at creation; representation (stored image vs. URL) not specified | `[AC-1]` |
| 6 | Existing-account password (confirmation input) | The account's current password, entered once to confirm ownership during linking | `[AC-3]` |
| 7 | Session established on Google sign-in | "Same session type" / "same properties" as a password login; concrete attributes not specified | `[AC-1]` |
| 8 | Post-login destination | "Same as a password login"; concrete route not specified | `[AC-1]` |
| 9 | OAuth callback — state parameter | Present and validated on return from Google; format not specified | `[EC-3]` |
| 10 | Failure log entry | Includes a correlation id; other fields not specified | `[EC-3]` |
| 11 | Google consent scopes requested | Not named by any AC/EC — see A-9 | — |
| 12 | `auth_identities` table columns and constraints | The source story §6 proposes `(account id, provider, provider_user_id, linked_at)` with a unique constraint on `(provider, provider_user_id)`; only the provider + provider_user_id shape is AC-backed (AC-7). The rest is recorded as A-15. | `[AC-7]` |

## 5. Out of Scope

- Additional identity providers (Apple, Microsoft, etc.) — the source says the model is built to accommodate them (AC-7) but no second provider ships in this story.
- Enterprise SSO / SAML / SCIM.
- Restricting sign-in to specific Google Workspace domains.
- Merging or reconciling two pre-existing separate app accounts that turn out to belong to the same person — see OQ-8.
- Any change to the existing email + password sign-in or password-reset flows beyond the linking/unlinking behavior in AC-4–AC-6.
- Re-synchronising name / profile picture from Google after account creation — see A-5.

## 6. Assumptions

Positions taken to make the spec coherent. Not requirements, not agreed. Most are carried directly from the `[ASSUMPTION — not confirmed]` and `[PROPOSED — confirm]` items in the source story.

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The "As a / I want / So that" statement in §1 is the source's proposed reconstruction of a one-line story, not a stakeholder-agreed frame. | The original input named only a generic "user" and one action; §1 needs a frame to write against. |
| A-2 | A Google-verified email satisfies the app's email-verification requirement; an email Google reports as unverified is rejected (EC-2). | AC-1 creates an account from a Google email without describing verification; EC-2 rejects unverified but doesn't say whether verified means "app-verified". |
| A-3 | Personal Gmail and Google Workspace accounts are treated identically; no domain allow-list or block-list. | No AC distinguishes account types; the flows have to apply to some defined population. |
| A-4 | The unauthenticated "Continue with Google" button appears on both the sign-in and sign-up screens, and the flow is a full-page redirect (not a popup). | AC-1/AC-2 imply an entry point but name no screen or interaction model. |
| A-5 | Name and profile picture are copied from Google only at account creation and never re-synced. | AC-1 stores them at creation; nothing addresses later changes. |
| A-6 | After the AC-3 password-confirmation link, the user is signed in only via the session that step creates — no separate auto-sign-in behavior. | AC-3 says "I am signed in to that account" without specifying session origin. |
| A-7 | "A usable password" / "no password set" (AC-5, AC-6) means a password credential the user has actively set, not a null or placeholder value. | AC-6's lock-out guard depends on a definition of when a password counts. |
| A-8 | Authentication uses the OAuth 2.0 authorization-code flow with PKCE, a `state` parameter, a `nonce`, and full ID-token validation (signature, `aud`, `iss`, `exp`). | No AC/EC names an OAuth flow; EC-3 only mentions `state`. A concrete flow is needed to build. |
| A-9 | Only `openid email profile` scopes are requested. | No AC names scopes; minimal scope is assumed. |
| A-10 | The session created by Google sign-in has the same lifetime, cookie flags, and revocation behavior as a password-login session. | AC-1 says "same session type" without enumerating properties. |
| A-11 | Google `sub`, email, name, and avatar are stored under the app's existing PII handling; the legal basis is the existing sign-up terms/consent. | AC-1 stores Google-derived PII; no AC states a handling or consent basis. |
| A-12 | Google consent-screen branding verification may take several business days and is started before implementation. | Named as a dependency in the source; affects sequencing, not behavior. |
| A-13 | The redirect-and-return sign-in round trip (excluding time on Google's screens) completes within p95 < 2s. | No AC states a performance target. |
| A-14 | The new screens and messages meet WCAG 2.1 AA. | No AC states an accessibility bar. |
| A-15 | The `auth_identities` table carries at least `(account_id, provider, provider_user_id, linked_at)` with a unique constraint on `(provider, provider_user_id)`. | AC-7 fixes only the provider + provider_user_id shape; a buildable schema needs the rest. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | Is the AC-3 posture correct — require the existing account password before linking Google to a matching email — or should the app trust Google's verified email and link automatically? | AC-3 (the AC may be rewritten) |
| OQ-2 | Is a Google-verified email accepted in place of the app's own email verification, and what exactly happens when Google reports the email unverified beyond "rejected"? | AC-1, EC-2 |
| OQ-3 | Which screens carry the "Continue with Google" button — sign-in only, or sign-up as well? | AC-1, AC-2 (entry point) |
| OQ-4 | On just-in-time account creation, is any step required before the user reaches the app (accept terms, choose a name or workspace)? | AC-1 |
| OQ-5 | Does the app pull the Google profile picture and display name, and are they ever re-synced or set once only? | AC-1 |
| OQ-6 | Should personal Gmail and Google Workspace accounts be treated the same, or is a Workspace-domain restriction wanted now or later? | AC-1, AC-2 |
| OQ-7 | What is the success metric for this story (sign-up completion rate, share of logins via Google, password-reset ticket volume)? | None — measurement only |
| OQ-8 | Is any handling required when two separate existing app accounts turn out to belong to the same person once Google sign-in is introduced? | AC-3 (adjacent) |
| OQ-9 | What are the target languages / localization scope for the new screens and messages? | AC-1 (UX) |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| AC-1 | JIT account creation on first Google sign-in | §3.1, §3.2, §4 | Covered — see OQ-3, OQ-4, OQ-5, A-2, A-4, A-5 |
| AC-2 | Sign in with an already-linked Google identity | §3.1, §3.3 | Covered — see OQ-3 |
| AC-3 | Email collision → password confirmation → link | §3.4 | Covered — see OQ-1 (AC may change), OQ-8 |
| AC-4 | Link Google from account settings | §3.1, §3.5 | Covered |
| AC-5 | Unlink Google when a password remains | §3.5 | Covered — see A-7 |
| AC-6 | Unlink blocked when Google is the only method | §3.5 | Covered — see A-7 |
| AC-7 | Provider-agnostic identity storage | §3.6, §4 | Covered — see A-15 |
| EC-1 | Cancel / deny consent at Google | §3.7 | Covered |
| EC-2 | No email or unverified email from Google | §3.7 | Covered — see OQ-2, A-2 |
| EC-3 | OAuth error or state mismatch on callback | §3.7, §4 | Covered — see A-8 |
| EC-4 | Google identity already linked to another account | §3.5 | Covered |
| EC-5 | Already signed in as another account | §3.3 | Covered |
| EC-6 | Google email changes after linking | §3.3 | Covered |
| EC-7 | Wrong password during a link attempt | §3.4 | Covered |

Status answers one question: can this AC be verified as written, from what the spec says?

- **Covered** — a tester could write a passing/failing test from the body. Referenced open questions and assumptions do not change this.
- **Partial** — something the AC itself asserts is left undefined.
- **Not covered** — nothing in the body addresses it.

All fourteen items are Covered: the source story was clarified before this spec was written, so no requirement in it is left untestable. The references in the right-hand column point to decisions that are still open *next to* the requirements, not gaps *inside* them — except OQ-1, which may cause AC-3 to be rewritten.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-28 | Initial specification from docs/stories/google-login.md. |
