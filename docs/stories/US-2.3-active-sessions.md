# Epic 2 — Authentication: Active Session Management

**Story ID:** US-2.3
**Project:** Customer Portal
**AC prefix:** `SM-AC`
**Module:** `customer/`

## User Story
As a customer who suspects their account has been compromised,
I want to see my active sessions and end all of them at once,
So that I can cut off an intruder myself instead of waiting on support.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Re-authentication before the bulk action | Not required; a confirmation dialog is | Incident response is time-critical, and the attacker already holds a session either way |
| 2 | Current session | Preserved by default, with an opt-out | Signing the victim out of the device they are fixing the problem on is hostile |
| 3 | Location display | City-level, resolved from an offline IP database | Enough to recognise "not me" without storing coordinates |
| 4 | Propagation delay | Other devices lose access at their next refresh, at most 15 minutes | Bounded by the access-token TTL; anything tighter needs per-request state checks |

## In Scope
- `GET /api/v1/auth/sessions` — list the caller's own active sessions
- `DELETE /api/v1/auth/sessions/{sessionId}` — end one session
- `POST /api/v1/auth/sessions/revoke-all` — end every session
- Device, browser, approximate city and last-activity metadata per session

## Out of Scope
- Ending the current session (US-2.2)
- Administrative termination of another customer's sessions (US-3.4)
- Alerting on new-device sign-in — a notification concern, see US-5.4

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/auth/sessions` | Bearer | — | `200` `{"sessions": [{"id", "device", "browser", "city", "lastActiveAt", "current": bool}]}` |
| DELETE | `/api/v1/auth/sessions/{sessionId}` | Bearer | — | `204` |
| POST | `/api/v1/auth/sessions/revoke-all` | Bearer | `{"keepCurrent": bool}` | `200` `{"revokedCount": int}` |

## Data Model Notes
- `refresh_tokens` carries the session metadata: `device`, `userAgent`, `ipCity`, `lastActiveAt`, `familyId`
- A "session" in the UI is one token **family**, not one token — rotation (US-2.4) replaces the token but must not create a second session row
- No raw IP address is displayed; the resolved city is stored and the IP stays in `audit_events`

## Acceptance Criteria

### Happy path
**SM-AC1 — Listing active sessions**
```gherkin
Given the customer holds three active sessions on different devices
When GET /api/v1/auth/sessions is called
Then respond 200 with one entry per session
And each entry carries device, browser, approximate city and last-activity time
And exactly one entry is flagged as the current session
```

**SM-AC2 — Ending every session**
```gherkin
Given the customer holds three active sessions
When POST /api/v1/auth/sessions/revoke-all is called with keepCurrent=true
Then every refresh-token family except the current one is revoked
And respond 200 with the number of sessions ended
And the other devices lose access at their next refresh, and no later than 15 minutes
And the customer receives an email listing what was ended and when
```

**SM-AC3 — Ending one session**
```gherkin
Given the customer is viewing their session list
When DELETE /api/v1/auth/sessions/{sessionId} is called for another device
Then that session is revoked and respond 204
And the current session is unaffected
```

### Authorisation
**SM-AC4 — Ending someone else's session**
```gherkin
Given a session identifier that belongs to a different customer
When DELETE /api/v1/auth/sessions/{sessionId} is called
Then respond 404, not 403
And the target session remains active
And an audit_events entry records the unauthorised attempt
```

### Edge cases
**SM-AC5 — Revoking with no other sessions**
```gherkin
Given the current session is the only active one
When POST /api/v1/auth/sessions/revoke-all is called with keepCurrent=true
Then respond 200 with revokedCount=0
And no email is sent, because nothing changed
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/session-not-found",
  "title": "Session Not Found",
  "status": 404,
  "detail": "No such session exists for this account.",
  "instance": "/api/v1/auth/sessions/0193f2c1-0000-7000-8000-000000000000"
}
```
Error `type` slugs introduced by this story: `session-not-found`.

## Non-Functional / Security Requirements
- `404` rather than `403` on SM-AC4, so the response never confirms that a session identifier exists.
- The city lookup runs against a local database; no session metadata is sent to a third-party geolocation service.
- Session listing must not expose raw IP addresses to the customer — that data belongs in the audit log, behind `audit:read` (US-3.7).
- **Performance:** p95 ≤ 400 ms for the listing; the bulk revoke is allowed to be asynchronous provided the response reports the count accurately.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| SM-AC1, SM-AC3 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| SM-AC2 | Integration test asserting family-wide revocation and the preserved current session | `[gate]` |
| SM-AC4 | Functional test asserting `404` and an unchanged target session | `[gate]` |
| SM-AC5 | Functional test on the single-session case | `[gate]` |
| No raw IP in the response | Contract test on the response schema | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** The offline IP-to-city database is a new runtime asset with its own update cadence and licensing; it needs sign-off.
2. Should `revoke-all` also force a password change, or is ending the sessions enough? Current default: sessions only, since the customer may not yet know whether the password leaked.
3. How long should ended sessions stay visible in the list as history? Current default: they disappear immediately, which loses forensic value the customer might want.
