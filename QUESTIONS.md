# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**

Observed in this repo:
- Warehouse persistence uses a Panache-based repository (`WarehouseRepository` implements `PanacheRepository<DbWarehouse>`). This is a JPA/ORM approach and is used for create/read/update/delete and simple queries.
- `LocationGateway` is an in-memory list of `Location` objects (simple gateway/resolver pattern). It's effectively a hard-coded lookup table for location metadata.
- Generated JAX-RS beans for `Warehouse` (from OpenAPI) are present in `target/generated-*` and mapped at the REST adapter layer to domain models.

Would I refactor? Yes, but selectively and conservatively:
- Unify the persistence abstraction: keep the `WarehouseStore` interface (already present) as the contract. Prefer keeping `PanacheRepository` in adapters but avoid leaking Panache/JPA types beyond the repository adapter. Ensure other data access implementations (if any) also implement the same `WarehouseStore` interface to make swapping/testing easy.
- Keep `LocationGateway` as a Gateway if the intention is that location metadata is static or provided by an external system. If locations are eventually persisted, replace the in-memory list with a proper repository implementation (same `LocationResolver` interface) and migrate the gateway to read from DB or an external API. The interface-based approach already makes this a low-risk change.
- Separate generated API models from domain models explicitly and centralize mapping code in an adapter layer (already done partially by `WarehouseResourceImpl::toDomainModel` / `toWarehouseResponse`). This prevents generated code changes from leaking into business logic.
- Standardize error handling and transaction boundaries in use-cases (explicit @Transactional on repository or use-case boundaries), to avoid subtle behavior differences between manual and ORM-managed updates.

Why:
- Consistency improves readability and reduces cognitive load for future contributors.
- Keeping clear adapter/domain separation prevents the generated OpenAPI classes from becoming the domain model (which causes coupling and makes refactors harder).
- Interface-based design (ports/adapters) already present is a good pattern—lean into it rather than remove it.

When I would not refactor:
- Avoid rewriting working, simple in-memory test fixtures into DB-backed stores unless there is a functional need. The in-memory `LocationGateway` is fine for demo/test data if its intent is to stay static.

Practical small refactors to consider first:
- Add mapping helpers and a small set of unit tests that assert mapping correctness between generated beans and domain models.
- Add integration tests for `WarehouseRepository` using Testcontainers Postgres to ensure Panache mappings behave correctly.
- Document the intended role of `LocationGateway` (static config vs. future DB-backed source) in the code or README.

----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**

Pros of generated API (OpenAPI-first):
- Single source of truth: the spec documents the contract and can be used to generate server stubs and client SDKs.
- Faster integration: clients can be generated early and integration tests written against the spec.
- Documentation & validation: tools like Swagger UI and request/response validation are easier to enable.

Cons of generated API:
- Generated models can differ from ideal domain models. Without a mapping layer you risk leaking generated DTOs into business logic.
- Requires discipline/automation: if the spec is edited by hand, it can drift from implementation; if generated from code, generation must be repeated on change.
- Tooling and versioning pain: generator options, customizations, and keeping generated code in source control can be fiddly.

Pros of hand-coded endpoints (Product/Store):
- Full control over resource shapes, validations, and subtle behavior.
- Easier to iterate quickly during prototyping if the API is small or internal.

Cons of hand-coded endpoints:
- Potential for inconsistent contracts and less machine-verified documentation.
- More manual work to produce client SDKs or keep docs up to date.

My recommendation for this repo:
- Keep the OpenAPI-generated approach for public/stable APIs (Warehouse) because it provides a clear contract and helps generate clients and docs.
- For Product and Store, choose one of two paths: either
  1) Formalize and add OpenAPI specs for them as well (recommended if they are public or shared), then generate server stubs and map to domain models; or
  2) Keep them hand-coded but add an automated way to document and test the contract (e.g., add an OpenAPI fragment and a nightly job that composes/validates specs), plus consumer contract tests.
- Whichever path you pick, enforce a mapping layer between transport (generated beans or request DTOs) and domain models. Never use generated beans as domain objects directly.

Reasoning: the combination of generated Warehouse API and manual Product/Store endpoints is a maintenance burden and can cause inconsistent contracts. Standardizing on a single process (preferably spec-first for public APIs) reduces drift and streamlines client integration.

----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**

Prioritization (practical roadmap):
1. Unit tests for domain logic / use-cases: Create/Replace/Archive Warehouse use-cases, `LocationResolver` behavior, validation rules (capacity, max warehouses, stock checks). These are fast and give the highest ROI.
2. Repository tests (integration against a real DB): tests for `WarehouseRepository` using Testcontainers (Postgres) to validate entity mappings and queries. These ensure persistence correctness.
3. Resource tests: Quarkus `@QuarkusTest` tests for `WarehouseResourceImpl` covering the HTTP layer and mapping between generated beans and domain models; mock the use-cases where appropriate for faster tests.
4. Contract tests: ensure the implemented API matches the OpenAPI spec. For generated Warehouse API this is simpler; for Product/Store create or generate specs and add tests that validate responses against the schema.
5. End-to-end smoke/integration tests: run a lightweight suite that boots the application (or Docker image) and exercises critical flows (create warehouse, get warehouse, list warehouses).

Test types and rationale:
- Unit tests: target business rules and edge cases. Fast, deterministic, easy to run on each commit.
- Integration tests (DB): catch mapping, transaction, and schema-related bugs. Run on PRs/CI where possible but keep scope limited to a few key scenarios.
- Contract tests: prevent API drift; use generated clients for testing the running server against the OpenAPI spec.
- E2E tests: minimal set for production-like validation (nightly or gated runs), not every commit.

Keeping coverage effective over time:
- Test intent > percent: use coverage as a signal but focus reviews on whether tests cover behavior, not just lines.
- CI gates: enforce module-level coverage thresholds for critical modules (e.g., domain/use-cases), but avoid one-size-fits-all global thresholds that encourage brittle tests.
- Mutation testing: run pitest periodically (nightly or weekly) on critical modules to ensure tests catch faults.
- Flaky test hygiene: detect and quarantine flaky tests quickly; add retry logic or investigate root causes.
- Automated contract validation: validate OpenAPI and run schema checks in CI to ensure runtime responses conform to the contract.
- Test data and fixtures: use builders and factories to keep tests readable and avoid brittle fixtures.

----

