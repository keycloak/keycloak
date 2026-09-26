# Session security model

## Scope

This document describes security guarantees and assumptions for Keycloak user sessions and authenticated client sessions when session state is cached and, where configured, persisted in a database.

The primary scope includes login and session creation, logout and session revocation, token refresh, introspection, silent SSO, and other requests that read or update session state. It covers online and offline sessions, persistent-session and volatile-session providers, cache misses, cache eviction, concurrent requests, and cluster membership changes.

User, client, and realm deletion are adjacent operations. They must preserve the guarantees below, but may require additional subsystem-specific analysis because they can affect many sessions in one transaction.

## Assets

- The authoritative existence and state of user sessions and authenticated client sessions.
- Logout and revocation finality.
- The ability to refresh tokens or treat tokens as active.
- Session notes and state used by authentication and authorization decisions.
- The association between a user session and its authenticated client sessions.

## Trust boundaries

- Incoming requests to the Keycloak request transaction.
- Request-local session state to shared session caches.
- Shared session caches to persistent session storage.
- One Keycloak cluster member to other members and cache owners.
- Keycloak to custom session persistence provider implementations.

## Attacker capabilities

The attacker may possess a valid access token, refresh token, session cookie, or offline token before revocation.

The attacker may send concurrent login, logout, refresh, introspection, UserInfo, and browser requests; control their timing; repeatedly cause cache misses; and route requests to different Keycloak nodes through the deployment load balancer.

The attacker may exploit ordinary cache eviction and session expiration timing.

The attacker is not assumed to control the database, cache transport, Keycloak process, host, or cluster membership credentials. Accidental process and node failures are nevertheless included as failure conditions.

## Security guarantees

- Once a session-mutating request completes successfully, every request that starts afterward must observe that mutation.
- After logout or revocation completes successfully, a newly starting request must not authenticate, introspect, or refresh using the deleted session.
- A stale database snapshot used by an in-flight request must not repopulate shared cache state after a conflicting mutation completes.
- Internal cache coordination values, such as loading markers, must never be exposed or mutated as complete session data.
- The persistent database is authoritative for database-backed session state. Cache state is disposable and must not override a newer committed database state.

## Concurrency semantics

A request already in flight may finish using session state that was valid when it read that state. Logout does not synchronously cancel other in-flight requests.

The completion boundary for a session-mutating request includes its required cache operation. A successful response must not be returned before the database transaction and corresponding cache update or invalidation complete.

A cache-miss reader may use a database result for its current request after losing the right to publish that result. Losing publication ownership must prevent shared cache mutation, not necessarily abort the already in-flight request.

Cache import reports publication success, an existing complete entry, and lost publication ownership as distinct outcomes. Callers must not infer successful publication merely because a loading marker was consumed.

## Persistence and consistency

Database-backed session mutations use database-first ordering. The database mutation commits before the corresponding cache operation starts, and request completion waits for that cache operation.

Cache-miss publication must be conditional on ownership established before the database read. A concurrent mutation or another publisher must be able to revoke that ownership so an older snapshot cannot overwrite or recreate newer shared state.

Database isolation must not allow a reader to place a new publication claim after a completed invalidation and then publish data from a snapshot predating that invalidation. Supported configurations use `READ COMMITTED` semantics where required for this ordering.

Custom session persistence providers must provide equivalent transaction visibility and ordering. A cache publication protocol cannot compensate for a provider returning snapshots older than the invalidations that precede its read.

A persistence read used for shared-cache publication must not reuse provider-local identity-map or first-level-cache state loaded before the publication claim. `READ COMMITTED` governs database statements, but does not refresh an entity that was already loaded into the same persistence context. If freshness cannot be established without changing persistence-context state, the result remains request-local.

Online and offline session models belong to distinct persistence and cache domains. A request-local fallback may reuse a preloaded model only when its domain and primary key match the requested session; otherwise it must load the requested model. Crossing domains can discard pending changes or create a false miss.

Database-backed cache entries have a bounded maximum cache residence time. Replacing an entry must not reset the original deadline and indefinitely extend potentially stale state.

New database-backed sessions are not inserted into the cache by an unconditional operation after database commit. A creator that installed its own marker may publish by CAS-replacing that marker only if persistence stored its complete creation snapshot. An empty cache key does not prove that the database row was absent. User-session creation that merges into an existing row invalidates even its own marker because its local entity may omit existing notes or client associations. A creator that encountered existing coordination or session state also invalidates the key after commit and leaves reconstruction to a subsequent marker-fenced read. Volatile online sessions are excluded because the cache is their authoritative store.

## Failure assumptions

A process may fail after committing a database mutation but before completing its cache operation. Because the request has not crossed its successful completion boundary, this does not violate the completed-request guarantee.

After a cluster view loses a member, surviving members conservatively clear their local database-backed session caches. This normally shortens recovery from the database-committed/cache-not-updated failure window. The configured maximum cache lifespan remains a fallback bound if departure recovery is delayed or does not run.

The cache manager currently cannot distinguish an explicit graceful leave from removal after failure detection. Cache clearing therefore also occurs during graceful rolling restarts and may cause additional database load and cache thrashing.

## Accepted limitations

- In-flight requests are not synchronously cancelled by concurrent logout or revocation.
- Session caches are not fully linearizable replicas of the database.
- Distributed locks and `SELECT FOR UPDATE` are not required for ordinary cache-miss reads.
- A crashed, unfinished request may have committed database state that was not yet reflected in the cache until member-removal recovery or cache expiration occurs.
- Conservative cache clearing prioritizes recovery over cache availability and efficiency after membership changes.

## Security-sensitive implementation invariants

The following mechanisms currently enforce the guarantees in this document. Replacing a mechanism is acceptable only if the replacement preserves the associated guarantee.

- A database-backed cache-miss reader installs a unique, short-lived loading marker before reading persistent state.
- Only the owner of that exact marker may publish the loaded session, using a compare-and-set replacement.
- Marker cleanup is conditional on marker identity and cannot remove a subsequently installed value.
- A reader observing another reader's marker may load request-local state but must not publish it.
- A database snapshot may be published only through a marker owned before that snapshot was read. Markerless imports may reuse an existing complete cache entry but otherwise remain request-local.
- Before a marker-owner persistence read may publish, the provider confirms that its persistence context cannot supply an older entity for the same primary key. A user-session graph also checks managed child entities belonging to that user session. An ineligible read remains request-local and does not flush, detach, refresh, or clear managed entities.
- Persistence providers fail closed: providers that do not explicitly establish freshness eligibility keep loaded state request-local instead of publishing it to the shared cache.
- Parent user-session publication failure prevents publication of stale child client sessions.
- Before publishing a committed client-session mutation, the transaction conditionally removes an in-progress parent user-session loading marker. Client-session deletion removes the complete parent cache entry because it also changes the parent's client-session membership.
- Realm-wide removal independently scans local parent and child caches by realm, including loading markers, rather than discovering children through parent membership. Each node completes its parent removals before scanning children and awaits child removals before its handler returns. This reuses the existing cluster events; it does not establish a new cluster-wide ordering barrier.
- A database-backed replacement publishes after commit only when the cache entry still has the UUID observed by the transaction. If the UUID changed, the same atomic cache operation removes the conflicting value instead of replaying update tasks onto possibly newer state. Atomic conflict removal is required because the conflicting value may be a loading marker whose owner can concurrently replace it with a pre-commit snapshot; a separate conditional removal of only the observed wrapper is insufficient, while a separate unconditional removal could delete a later update. Database commit order and cache-publication order can differ; after a conflict, the next marker-fenced read reconstructs the authoritative committed state.
- If parent publication wins a race with conditional marker removal, child marker CAS and update retry/invalidation still prevent publication of a stale child snapshot. Parent membership changes are published by the parent transaction.
- Client-session loads do not invalidate the parent marker because they do not change authoritative state.
- Parent import publishes its preloaded client-session snapshots only after successful parent publication; otherwise the complete loaded graph is bound request-locally without caching. Existing child transaction tasks take precedence, and expired children are not bound.
- Session mutations commit database writes before starting cache updates or invalidations.
- Request transaction completion waits for the aggregate post-database-commit cache operations.
- Loading markers are excluded from ordinary session reads, scans, immutable-session construction, mutation, and expiration processing where appropriate.
- The original cache deadline is preserved over repeated replacements of database-backed entries.
- Before creating a database-backed session, the transaction attempts to install a short-lived creation marker. After the database commit, it publishes the created session only by CAS-replacing a marker it installed itself and only if persistence stored the complete creation snapshot. This persistence outcome is reset for every database attempt, so a retry that merges into an existing user-session row cannot retain permission to publish from a failed insert attempt. Client creation stores the complete supplied snapshot even when overwriting an existing row. If creation merged into an existing user-session row, another marker or a complete entry already existed, or replacement of its own marker fails, the transaction invalidates the key after commit. The newly constructed entity is not a complete merged snapshot, and a value that displaced an expired marker may have been read before creation committed. Creation still succeeds request-locally, and a later marker-fenced read repopulates the cache.
- On a shrinking cluster view, each surviving node asynchronously clears its local database-backed session caches. Volatile in-memory-only online sessions are not included in this clear.

## Related implementation and issues

- Session cache and transaction implementation: `model/infinispan`.
- Cache and database configuration: `model/infinispan` and `model/jpa`.
- Infinispan departure-reason enhancement: [infinispan/infinispan#18396](https://github.com/infinispan/infinispan/issues/18396).
