## Product Vision

### Customer Portal

Customer Portal is a backend system that lets customers:

* register accounts;
* authenticate and manage their own sessions;
* manage profiles;
* raise support tickets and hold a conversation with an agent;
* control which notifications reach them, and through which channel.

The system exposes APIs and follows API-first development practices, as defined by the
technical contract in `AGENTS.md`.

### Product Goals

1. **Secure authentication.** Short-lived access tokens, refresh rotation with reuse
   detection, and no path by which the response reveals whether an account exists.
2. **User self-service.** A customer recovers their own access, ends their own sessions
   and raises their own tickets without contacting anyone.
3. **Administrative functionality.** Accounts, roles and permissions are managed without
   a release, under a privilege ceiling that no administrator can raise for themselves.
4. **Traceable agent-assisted delivery.** Every requirement carries an identifier that
   reaches the test that proves it, and every administrative action leaves an
   append-only record.

### What is deliberately not in Release 1.0

* Multi-factor authentication — see `docs/backlog/README.md` and the Epic 2 notes.
* Product catalogue and ordering — modules reserved, no stories.
* Channels beyond in-app and email, including SMS and browser push.
* Quiet hours, workload-based ticket routing, and ticket merging.

### Standing constraints

* Deletion is a state transition, never a row delete, except under an approved
  data-subject erasure request.
* The audit log is append-only, and no role in the system can edit it.
* Security notifications cannot be switched off on any channel.
* Any new runtime dependency — cache, broker, mail provider, object storage — requires
  human approval under `AGENTS.md` §7.5 before the story that needs it is estimated.
  Six such dependencies are currently unapproved and are listed in
  `docs/backlog/README.md`.
