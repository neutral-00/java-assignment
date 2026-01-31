# Research and Analysis On This Codebase

## Questions

### Q1. What are different strategies for database access layer?

- JDBC (plain): direct SQL execution using java.sql. Minimal abstraction, highest control, good for simple/very optimized queries.
- DAO / Repository pattern: encapsulate persistence logic behind interfaces; improves testability and separation of concerns.
- JPA / Hibernate (ORM): maps entities to tables, powerful for CRUD and relationships; reduces boilerplate but can hide SQL and cause N+1 issues.
- Spring Data / Quarkus Panache-style repositories: higher-level repository abstractions that generate queries from method names or annotations.
- Query builders / DSLs (jOOQ, QueryDSL): type-safe SQL construction with fine-grained control and compile-time checking.
- MyBatis / SQL mappers: XML/annotation driven SQL mapping — good when you need explicit SQL but want mapping helpers.
- Reactive drivers (R2DBC, Vert.x): non-blocking DB access for high-concurrency apps.
- Micro-ORMs / Lightweight mappers (e.g., jdbi): balance between raw JDBC and full ORM.

Trade-offs to consider: development speed vs. runtime control, ease of testing, transaction management, performance characteristics, and how well the approach fits complex domain relationships.

### Q2. What are the pros and cos of Open API yaml file?

Pros:
- API-first design: promotes clear contract between services and clients early in development.
- Auto-generated docs: tools (Swagger UI, Redoc) provide human-friendly API docs automatically.
- Client/server code generation: generates client SDKs and server stubs to speed integration.
- Validation & tooling: can validate requests/responses against the schema and integrate with CI.
- Versioning & discovery: a single source of truth for API surface that helps during versioning and change management.

Cons:
- Drift risk: if the YAML isn't the single source of truth (or not auto-generated), it can become outdated.
- Maintenance overhead: keeping the spec in sync with code needs process/automation.
- Verbosity: large specs can be noisy and require tooling support to manage.
- Tooling friction: different tools/version mismatches can cause subtle incompatibilities.

Practical tips:
- Treat the OpenAPI document as the contract (or generate it from code consistently).
- Validate the spec as part of CI and generate client stubs automatically where helpful.
- Use $ref and components to reduce duplication.

### Q3. How to ensure test coverage remains effective over time?

- Follow the test pyramid: many fast unit tests, some integration tests, few end-to-end tests.
- Test intent, not lines: coverage metrics are signals — prioritize meaningful behavior tests over mechanically covering code.
- Enforce coverage gates in CI for critical modules, but avoid overly rigid global thresholds that encourage brittle tests.
- Use mutation testing (pitest) periodically to assess test quality, not just quantity.
- Keep tests deterministic and fast: isolate external systems with mocks or use lightweight testcontainers for integration checks.
- Maintain fixture hygiene: use builders/test-data factories and avoid heavy shared mutable fixtures.
- Automate coverage reporting (coverage badges, PR comments) and monitor trends over time to catch regressions.
- Review tests during code review: require behavioral tests for new features/bugs and remove or refactor flaky tests.
- Include contract tests (e.g., provider/consumer) for APIs and schema validation for inputs/outputs.
