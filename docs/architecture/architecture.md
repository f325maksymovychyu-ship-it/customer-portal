# Architecture

Explicit architecture decisions for the Customer Portal project. These are
project decisions, not general framework advice. Skills that read designs or
code (`file-impact-analyzer`, `implementation-planner`) treat this file as
authoritative; stages with no implemented skill yet (see
`docs/workflow/gaps.md`) must still honor it once they exist.

## AD-1 Build & module layout

- Single Gradle module (`customer-portal`), Kotlin DSL. No multi-module split
  in this project.
- Production code under `src/main/java/org/example/customerportal`.
- Test code mirrors that package tree under `src/test/java`.
- No new Gradle subprojects or source sets without an approved decision.

## AD-2 Layered architecture

```
Controller → Service → Repository → Database
```

| Layer | Package | Responsibility | Must not |
|---|---|---|---|
| Controller | `controller` | HTTP mapping, request/response DTO binding, delegate to a Service, map Service outcomes to HTTP status | contain business rules; call a Repository; return an entity |
| Service | `service` | all business logic, orchestration, transaction boundaries, mapping between entities and DTOs | depend on `jakarta.servlet` / `HttpServletRequest` / MVC types; call another Controller |
| Repository | `repository` | Spring Data JPA interfaces, persistence queries only | contain business logic; call a Service |
| Entity | `model.entity` | persisted domain state | be serialized as an API request/response |

Allowed dependency directions: `controller → service → repository`. Everything
else in that set is forbidden (`controller → repository`,
`controller → model.entity` as an API type, `repository → service`,
`repository → controller`, `service → controller`).

## AD-3 Transaction boundary policy

- Transactions begin and end in the **Service** layer.
- Write operations: annotate the Service method (or class) with
  `@Transactional`.
- Read-only query methods: `@Transactional(readOnly = true)` when they issue
  more than one repository call or need a consistent snapshot; otherwise the
  repository call's implicit transaction is acceptable.
- Controllers and Repositories must not open transactions.
- No `@Transactional` on `private` methods or self-invoked methods (Spring
  proxy limitation) — restructure instead.

## AD-4 DTO / entity boundary

- Every API request body binds to a class in `model.request`.
- Every API response body is a class in `model.dto`.
- Entities (`model.entity`) never appear in a Controller signature, a request
  body, or a response body.
- Mapping entity ↔ DTO/request happens in the Service layer (a dedicated
  mapper class is allowed; a mapping library is not added without an approved
  decision).
- A response DTO includes only fields the API contract lists. Credential and
  token fields (password, password hash, refresh-token value) are never
  present on a response DTO, even as `null`.

## AD-5 Validation boundary

- Request-shape validation (required, length, format, allowed values): Bean
  Validation annotations on the `model.request` class, triggered by `@Valid`
  on the Controller parameter. `spring-boot-starter-validation` is a required
  dependency for this.
- Business-rule validation (uniqueness, cross-field rules, state checks): in
  the Service layer, before persistence.
- Custom constraint annotations live in the `validation` package.
- Validation is server-side and independent of any client.

## AD-6 Exception handling architecture

- One `@RestControllerAdvice` class in the `exception` package is the single
  place that maps exceptions to HTTP responses.
- Domain/application exceptions are declared in the `exception` package (e.g.
  `DuplicateEmailException`, `ResourceNotFoundException`). Services throw
  these; they carry no HTTP concepts.
- The advice maps: bean-validation failure → 400; domain "not found" → 404;
  domain "conflict/duplicate" → 409; authn failure → 401; authz failure → 403;
  anything unmapped → 500.
- Every error response body uses the structure defined in
  `api-conventions.md` (§ Errors). Stack traces, SQL, entity/class names,
  database paths, and secrets are never in a response body.

## AD-7 Configuration boundaries

- Framework/infrastructure `@Configuration` classes live in `config`.
- Security configuration lives in `security` (see `security-conventions.md`).
- No business logic in a `@Configuration` class.
- Application settings come from `application.yaml` / profile files, never
  hard-coded; secrets never committed (see `security-conventions.md`).

## AD-8 Reuse over duplication

Before creating a component, check for an existing one that can be extended
within these rules. New packages beyond the map in `package-map.md` require an
approved decision (an Open Decision resolved by a human, not a silent
addition).

## AD-9 Reserved bounded contexts

`docs/product/epic-map.md` groups work into Users, Authentication,
Administration, Support Tickets, and Notifications. This project has not yet
made a Modulith/multi-module split; all of it lives in the single package tree
under AD-1 until an approved decision says otherwise. Whether Notifications
gets its own top-level package is an open question tracked in
`docs/workflow/gaps.md`.
