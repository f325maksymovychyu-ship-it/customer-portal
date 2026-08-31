# API Conventions

Explicit decisions for HTTP APIs in this project. `openapi-designer` and
`design-reviewer` enforce these; `springboot-implementor` implements to them.

## AC-1 Versioning

- URI-path versioning: every endpoint is under `/api/v1/…`.
- A breaking change to an existing contract requires a new version prefix and an
  approved decision; it is never made in place.

## AC-2 Media type

- Request and response bodies are `application/json` (`UTF-8`).
- `Content-Type: application/json` is required on requests with a body;
  otherwise respond `415`.
- No XML, form-encoded, or multipart support unless a Story's approved design
  adds it explicitly.

## AC-3 Resource naming

- Plural nouns: `/api/v1/customers`, `/api/v1/customers/{id}`.
- Kebab-case for multi-word path segments; `camelCase` for JSON field names.
- No verbs in paths. Actions that are not CRUD get an approved design decision.

## AC-4 HTTP methods & success codes

| Method | Use | Success |
|---|---|---|
| `POST /collection` | create | `201 Created`, `Location` header, created resource body |
| `GET /collection` | list | `200 OK` |
| `GET /collection/{id}` | read one | `200 OK` |
| `PUT /collection/{id}` | full replace | `200 OK` (or `204` if no body) |
| `PATCH /collection/{id}` | partial update | `200 OK` |
| `DELETE /collection/{id}` | delete | `204 No Content` |

## AC-5 Error codes

| Status | When |
|---|---|
| `400 Bad Request` | request-shape / bean-validation failure, malformed JSON |
| `401 Unauthorized` | authentication required or failed |
| `403 Forbidden` | authenticated but not permitted |
| `404 Not Found` | resource does not exist (or is not visible to the caller) |
| `409 Conflict` | uniqueness / state conflict (e.g. duplicate email) |
| `415 Unsupported Media Type` | missing/wrong `Content-Type` |
| `500 Internal Server Error` | unmapped exception (must not leak internals) |

## AC-6 Error body

All error responses use exactly this JSON shape:

```json
{
  "timestamp": "2026-08-31T10:15:30Z",
  "status": 409,
  "error": "Conflict",
  "message": "An account with this email already exists.",
  "path": "/api/v1/customers"
}
```

- `message` is safe for a client to display; it never contains stack traces,
  SQL, class/package names, file paths, or secrets.
- Bean-validation failures may add a `fieldErrors` array of
  `{ "field": "...", "message": "..." }`.

## AC-7 Authentication header

- Session-based auth for the MVP (see `security-conventions.md`): the browser
  session cookie carries authentication; no `Authorization` header is expected.
- If a Story introduces token auth (approved decision), it uses
  `Authorization: Bearer <token>` and that Story updates this section.

## AC-8 Pagination

- Any endpoint returning a collection that can grow unbounded is paginated from
  day one.
- Query params: `page` (0-based, default `0`), `size` (default `20`, max `100`).
- Response body:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7
}
```

- Sorting via `sort=field,asc|desc` when the design lists sortable fields.

## AC-9 Exception-handler location

Exception → HTTP mapping is done in the single `@RestControllerAdvice` in the
`exception` package (see `architecture.md` AD-6). Controllers do not
`try/catch` to build error responses.
