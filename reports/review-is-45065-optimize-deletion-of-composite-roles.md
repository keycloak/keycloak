### 🔍 PR Safety & Risk Analysis

**Authentication & User Lifecycle**
- Findings: Composite role relationships are now managed via a dedicated JPA entity and queries; cache entities now store cached composite/role-mapping sets to avoid lazy loading during stream predicates.
- Risks: Authorization checks that depend on composite role evaluation could be affected if cache invalidation is incomplete, leading to stale composite data in cached roles/groups.
- Mitigations (if applicable): Add/confirm cache invalidation on composite role updates and deletion; validate authorization behavior for composite roles in clustered/cache-heavy setups.

**Deployment Safety**
- Findings: No schema changes to the underlying `COMPOSITE_ROLE` table, but JPA mapping changes replace `@ManyToMany` with an entity and named queries.
- Risks: Potential persistence context edge cases if composite relations are modified concurrently; new delete query bypasses entity collection updates, which could leave in-memory state inconsistent within a transaction.
- Mitigations: Ensure entity state is refreshed or avoid relying on in-memory composite collections after deletion; test role deletion in transactions that previously touched composites.

**Backwards Compatibility**
- Findings: Behavior should be functionally equivalent, but composite role resolution now uses new queries and cached sets.
- Risks: Differences in ordering or visibility of composites during the same transaction could affect callers relying on immediate consistency from entity collections; cache predicate now uses cached sets populated only when getters run.
- Mitigations: Verify composite role list APIs and admin UI behavior under concurrent changes; add tests for composite role deletion and subsequent permission evaluation in the same request.

**Overall Risk Level**
- Medium

**Recommended Actions**
- Add regression tests for composite role deletion with cached roles/groups and immediate permission checks.
- Validate cluster/cache behavior for composite role updates/deletes.
- Review transaction-scoped consistency assumptions where composite roles are removed and re-queried.

