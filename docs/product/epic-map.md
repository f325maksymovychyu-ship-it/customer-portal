# Epic Map

Epics 2–5 are broken into stories under `docs/backlog/`. Epic 1 is covered by the
existing `CP-101` specification and has no backlog entry yet.

### Epic 1 — Users

* Register User — specified as `CP-101`
* Verify Email
* Update Profile
* Deactivate Account

> Only registration has been specified. The remaining three features have neither
> stories nor specifications, and `US-3.3` and `US-3.4` cover the administrator's
> side of profile updates and deactivation rather than the customer's own.

### Epic 2 — Authentication

* Login — `US-2.1`
* Logout — `US-2.2`, and across all devices `US-2.3`
* Refresh Token — `US-2.4`
* Password Reset — request `US-2.5`, confirm `US-2.6`

> MFA is deliberately out of Release 1.0. It intercepts the success path of `US-2.1`
> and rewrites `US-2.6`, so it must arrive as its own story rather than as extra
> criteria on existing ones.

### Epic 3 — Administration

* Manage Users — `US-3.1` list, `US-3.2` create, `US-3.3` update, `US-3.4` deactivate
* Manage Roles — `US-3.5` assign to a customer, `US-3.6` define roles and permissions
* View Audit Information — `US-3.7` read, `US-3.8` export

### Epic 4 — Feedback / Support

* Support Tickets — `US-4.1` create, `US-4.2` the customer's own list
* Ticket Assignment — `US-4.3`
* Ticket Replies — `US-4.4` customer, `US-4.5` agent and internal notes
* Ticket Resolution — `US-4.6` resolve and close, `US-4.7` reopen

### Epic 5 — Notifications

* Notification Centre — `US-5.1`, real-time delivery `US-5.2`, read state `US-5.3`
* Preferences — `US-5.4`, unsubscribe `US-5.6`
* Email Delivery — `US-5.5`, grouping and digest `US-5.7`
* System Announcements — `US-5.8`

> Epic 5 has no module in the canonical map in `AGENTS.md` §2.1. Whether it becomes a
> new bounded context or lives in `shared/` is an architect decision that blocks the
> whole epic.

### Reserved, with no stories

* Product Catalog — `catalog/`
* Ordering — `ordering/`
