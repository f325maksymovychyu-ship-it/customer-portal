# Package Map

Base package: `org.example.customerportal`. Test packages mirror this tree
under `src/test/java`. Adding a package not listed here requires an approved
decision.

| Package | Contains | Depends on (allowed) | Notes |
|---|---|---|---|
| `controller` | `@RestController` classes | `service`, `model.dto`, `model.request` | No business logic. No repository access. No entity in a signature. |
| `service` | business logic, orchestration, entity↔DTO mapping, transaction boundaries | `repository`, `model.entity`, `model.dto`, `model.request`, `exception`, `validation`, `security` (read-only helpers) | Owns `@Transactional`. No MVC / servlet types. |
| `repository` | Spring Data JPA repository interfaces | `model.entity` | Queries only. No business logic. |
| `model.entity` | `@Entity` classes — persisted domain state | (none — leaf) | Never used as an API request/response type. |
| `model.dto` | API **response** DTOs | (none — leaf) | No credential or raw-token fields, ever. |
| `model.request` | API **request** DTOs with Bean Validation annotations | `validation` | Bound with `@Valid` in controllers. |
| `validation` | custom constraint annotations + validators | `model.entity` (read-only, when a validator must query) | |
| `security` | Spring Security config, JWT issuing/verification, `UserDetailsService`, password encoder bean, auth entry points | `repository`, `model.entity`, `config` | See `security-conventions.md`. |
| `config` | framework `@Configuration` (Jackson, OpenAPI, etc.) | framework only | No business logic. |
| `exception` | domain exception classes + one `@RestControllerAdvice` | `model.dto` (error body) | Single place that maps exceptions → HTTP. |

## Dependency direction rules

- `controller` may depend on `service`, `model.dto`, `model.request` — nothing
  else in the app.
- `service` may depend on everything except `controller`.
- `repository` may depend only on `model.entity`.
- `model.entity`, `model.dto` are leaves (no intra-app dependencies).
- No cycles. `repository → service`, `repository → controller`,
  `service → controller`, `controller → repository` are all forbidden and are
  architecture violations (Major or Critical finding depending on impact).

## Test package rule

For a production class `org.example.customerportal.<pkg>.<Name>`, its tests
live in `org.example.customerportal.<pkg>` under `src/test/java`. Integration
tests that span layers may sit in a `…​.<feature>` package but still under the
base package.
