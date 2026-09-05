# Epic 5 — Notifications: Read State

**Story ID:** US-5.3
**Project:** Customer Portal
**AC prefix:** `NR-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As a signed-in customer,
I want to mark notifications read individually or all at once, and to undo it,
So that I can clear the list quickly while keeping the ones I still need to act on visible.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Bulk operation | One request, not one per notification | A customer with 200 unread must not generate 200 writes to tidy up |
| 2 | Idempotency | Repeating any read operation returns success | The same account is often open on two devices doing the same thing |
| 3 | Reversibility | Read can be undone; notifications cannot be deleted | Deletion loses the record; unread restores the customer's own working state |
| 4 | Undo window | 5 seconds after an action taken under a filter | Long enough to catch a misclick, short enough not to hold state |

## In Scope
- `POST /api/v1/notifications/read` — mark one, several or all as read
- `POST /api/v1/notifications/unread` — reverse it
- Counter consistency under concurrent updates from several devices
- Undo affordance when acting inside the unread-only filter

## Out of Scope
- Listing and the counter itself (US-5.1)
- Deleting notifications — deliberately unsupported
- Read state of the underlying object, such as a ticket thread (US-4.2)

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/notifications/read` | Bearer | `{"notificationIds": [uuid]}` or `{"all": true}` | `200` `{"updated": int, "unreadCount": int}` |
| POST | `/api/v1/notifications/unread` | Bearer | `{"notificationIds": [uuid]}` | `200` `{"updated": int, "unreadCount": int}` |

## Data Model Notes
- Read state is `notifications.read_at`; a null value means unread, so no second table is needed
- The bulk call is a single conditional `UPDATE ... WHERE recipient_id = ? AND read_at IS NULL`, which is what makes NR-AC3 naturally idempotent
- The response returns the recalculated unread count so the client never has to issue a second request to stay consistent

## Acceptance Criteria

### Happy path
**NR-AC1 — Mark all as read**
```gherkin
Given a customer with 3 unread notifications
When POST /api/v1/notifications/read is called with all=true
Then respond 200 with updated=3 and unreadCount=0
And exactly one database statement performs the update, not one per row
And the notifications remain in the list, only their unread state changes
```

**NR-AC2 — Mark one as unread again**
```gherkin
Given a notification that has been read
When POST /api/v1/notifications/unread is called with its identifier
Then respond 200 with updated=1
And the entry is highlighted as unread again
And the unread counter increases by one
```

### Concurrency
**NR-AC3 — Repeating the action from another device**
```gherkin
Given the customer already marked everything read on another device
When POST /api/v1/notifications/read with all=true is called again
Then respond 200 with updated=0 and unreadCount=0
And no error is returned
And the counter never becomes negative under any interleaving of these calls
```

### Authorisation
**NR-AC4 — Identifiers belonging to someone else**
```gherkin
Given the request names a notification belonging to a different recipient
When it is submitted
Then that identifier is ignored rather than acted upon
And the response counts only the caller's own notifications in updated
And no error reveals whether the foreign identifier exists
```

### Client behaviour
**NR-AC5 — Acting inside the unread-only filter**
```gherkin
Given the unread-only filter is active
When a notification is marked read
Then it leaves the list
And an "Undo" action is available for 5 seconds and restores it
And the remaining rows do not jump, because the removed row's height is animated out
```

### Validation
**NR-AC6 — Malformed request**
```gherkin
Given a request that supplies neither notificationIds nor all
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
Given notificationIds contains more than 500 entries
Then respond 400 naming the limit
```

## Error Envelope (RFC 9457 `ProblemDetail`)
Reuses `validation-failed`. This story introduces no new `type` slug.

## Non-Functional / Security Requirements
- The bulk update must be a single statement scoped by recipient. A loop over identifiers is both slow and an opportunity to forget the ownership predicate.
- The unread count returned by these endpoints must be computed after the update inside the same transaction, or two devices will disagree about it.
- NR-AC4 silently ignores foreign identifiers rather than failing the whole request, so a stale client does not lose a legitimate bulk action to one bad entry.
- **Performance:** p95 ≤ 200 ms for a bulk read of 500 notifications.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NR-AC1 | Integration test asserting the result and the statement count | `[gate]` |
| NR-AC2 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| NR-AC3 | Integration test issuing the same bulk call twice concurrently | `[gate]` |
| NR-AC4 | Functional test mixing own and foreign identifiers | `[gate]` |
| NR-AC5 | Client unit test on the undo timer and list transition | `[gate]` |
| NR-AC6 | Slice test on the request record | `[gate]` |

## Open Questions
1. Should read state synchronise live across devices through the US-5.2 stream, or is convergence on the next load enough? Currently the latter, which means two open devices briefly disagree.
2. Is "mark all as read" scoped to the current filter or genuinely everything? NR-AC1 assumes everything, which will surprise a customer who applied a filter first.
3. Do customers want to archive rather than merely read notifications? Deletion is excluded by Decision 3, but archiving is a middle option nobody has evaluated.
