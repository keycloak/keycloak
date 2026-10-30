# Loading Marker + CAS Pattern

This document describes the implementation protocol used when loading or creating sessions
in the Infinispan cache. The authoritative guarantees, trust boundaries, and accepted failure
assumptions are documented in [`../../docs/security-models/sessions.md`](../../docs/security-models/sessions.md).
This document explains how the implementation realizes those guarantees:

- **Prevent session resurrection**: a concurrent cache reader must not re-import a
  session that was just deleted.
- **Prevent stale data in the cache**: a cache import must not overwrite or bypass an
  in-flight update from the changelog-based transaction, and must not introduce data
  that missed a concurrent modification.

## 1. The Race (without markers)

```
Reader                              Deleter
──────                              ───────
cache.get(key) → null
                                    DB: DELETE session
                                    cache.remove(key)
DB: SELECT session → row (stale)
cache.putIfAbsent(key, data)
→ session resurrected in cache
```

## 2. Loading Marker — Read Path

A **loading marker** is a lightweight `SessionEntityWrapper` with a `"loading"` key in
`localMetadata` and a UUID-based version for CAS identity. Markers have a 60-second TTL
as a safety net against leaks.

### 2.1 Single-session load (user sessions and standalone client sessions)

```
reader:
    marker = createLoadingMarker(minimalEntity)
    existing = cache.putIfAbsent(key, marker, 60s)

    if existing == null:
        // We own the marker.
        try:
            dbEntity = loadFromDB(key)
            if dbEntity == null:
                return null                     // session doesn't exist

            // Volatile path:
            replaced = cache.replace(key, marker, dbEntity)
            consumeLoadingMarker(key)           // remove from ownership map after CAS

            // Persistent path:
            marker = removeLoadingMarker(key)   // remove from ownership map before CAS
            replaced = cache.replace(key, marker, dbEntity)

            // Both paths: bind the DB-loaded entity to the transaction regardless of
            // CAS result. On CAS success, the entity is also in the cache.
            // On CAS failure, the entity serves this request only (see §5).
            track(key, dbEntity)
        finally:
            cleanupLoadingMarker(key)           // no-op if already consumed/removed

    else if existing is real data:
        // Cache hit — track the cached entity in the transaction.
        track(key, existing)

    else if existing is another reader's marker:
        // Don't place a second marker. Load from DB for this request only.
        dbEntity = loadFromDB(key)
        track(key, dbEntity)                    // bind without caching (see §3)
```

An import that did not establish marker ownership before its database snapshot was read may
reuse an existing complete cache entry, but it must not publish the snapshot into an empty
cache. It remains request-local.

### 2.2 Bulk-session load (user session + its client sessions)

Client session markers are placed **in bulk before** the user session import. A committed
client-session mutation conditionally removes an in-progress parent marker before publishing
its own cache operation. This makes the parent CAS fail if any client snapshot became stale.

```
reader:
    clientSessionsById = collectClientSessions(userSession)
    clientTx.placeLoadingMarkers(clientSessionsById)
    try:
        result = importUserSession(...)

        if result == EXISTING:
            return result.session               // another reader already imported

        if result == REQUEST_LOCAL:
            clientTx.bindSessionsWithoutCaching(clientSessionsById)
            return userSession                  // valid only for this in-flight request

        // Only PUBLISHED permits publication of the preloaded child snapshots.
        clientTx.importSessionsConcurrently(clientSessionsById)
        return userSession
    finally:
        clientTx.cleanupLoadingMarkers(clientSessionsById)
```

The `finally` block is essential: if `importUserSession` or `importSessionsConcurrently`
throws, unconsumed client markers must be removed. `cleanupLoadingMarkers` is idempotent —
already-consumed markers are no longer in the ownership map, so the CAS
`cache.remove(key, marker)` is a no-op for them.

Request-local parent loads also bind their preloaded child snapshots to the client-session
transaction without caching. This prevents a later child lookup in the same request from
reloading through the persistence context and publishing the stale snapshot. Binding keeps
existing transaction entries and tasks and excludes expired child sessions.

## 3. Load Without Caching

**Rule: only the marker owner may import data into the cache. Every other code path
that obtains session data — second concurrent readers, bulk queries, pre-loaded data —
must bind the entity to the transaction without caching.**

This prevents two CAS contenders from racing and avoids resurrection of stale bulk-query
data.

```
reader (non-marker-owner):
    entity = loadFromDB(key)
    if entity != null:
        track(key, entity)                      // visible within this request
    // No cache import — the marker owner will cache the data.
```

## 4. Delete Path — DB-First Ordering

**Rule: every code path that deletes a session must commit the DB delete before removing
the cache entry.** Otherwise, a concurrent reader could:

1. See a cache miss (entry just removed)
2. Load the session from the database (DELETE not yet committed)
3. Cache the stale data

### 4.1 Cache-behind-DB (DB-first ordering)

Cache publication and invalidation operations — ADD_IF_ABSENT, REPLACE, and REMOVE — are
deferred until after the database commit. The one intentional pre-commit cache write is the
short-lived creation marker described in §4.2; it is coordination state, not session data.
During `asyncCommit`, DB writes are registered and publication operations are collected as
`PendingCacheOp` records. After `commitDatabaseUpdates` completes,
`asyncPostDatabaseCommit` executes the collected operations.

```
asyncCommit:
    for each tracked entry:
        pendingCacheOps.add(entry)              // collect, don't execute yet
        registerDatabaseWrite(entry)            // queue for DB commit

commitDatabaseUpdates:
    persist all changes to DB                   // all writes commit here

asyncPostDatabaseCommit:
    for each pendingCacheOp:
        runOperationInCluster(op)               // now safe to update cache
```

No shared session data is written, updated, or removed until the corresponding DB change is
committed. The pre-commit creation marker contains coordination state only. A concurrent
reader that loads from the database during or after `asyncPostDatabaseCommit` sees committed
data, subject to the database-isolation prerequisite in §10.

In the volatile path, online sessions (no DB persistence) run cache operations immediately
in `asyncCommit` since there is no DB to wait for. Offline sessions that persist to the
database use the same DB-first pattern: cache operations are collected and deferred to
`asyncPostDatabaseCommit`.

### 4.2 Creation fencing

Database-backed `ADD_IF_ABSENT` attempts to install a short-lived marker before the database
write. A marker installed by this transaction is its publication fence. After commit, the
created entry is published only by CAS-replacing that exact marker and only if persistence
stored the complete creation snapshot. User-session creation that merged into an existing row
must invalidate even its own marker: an empty cache key does not imply an absent database row.
The persistence outcome is reset on each database attempt and read after the successful commit.
Client creation stores the complete supplied snapshot, including when it overwrites an existing row.
If another marker or a
complete entry already exists, the newly constructed wrapper is not known to contain a
complete merge of concurrent login state.
The transaction therefore invalidates the key after commit instead of publishing or retaining
that entry. It also invalidates the key if replacement of its own marker fails: the marker may
have expired, and a replacement value may contain a database snapshot read before creation
committed. The next reader reconstructs the merged state from the database.

The creating request retains its transaction-local entity whether or not publication wins.
A failed transaction or process crash may leave a marker behind; it expires after the marker
TTL, and readers that do not own it may still load request-local database state. No rollback
cleanup is required for correctness.

```
before DB write:
    fence = cache.putIfAbsent(key, creationMarker, markerTTL)
    if fence == null:
        fence = creationMarker
    else:
        fence = null                            // another marker or complete entry

after DB commit:
    if fence != null and creationSnapshotComplete:
        if not cache.replace(key, fence, createdSession):
            cache.remove(key)                   // displaced value may predate DB commit
    else:
        cache.remove(key)                       // force reconstruction from DB
```

Cache-only volatile online sessions do not need this database fence because their cache is
authoritative.

### 4.3 User deletion (`onUserRemoved`)

User deletion collects the user's session IDs **before** the DB delete, then defers
cache removal to an `afterCompletion` transaction (fires after DB commit). Removal is
by exact key to avoid disrupting concurrent readers for other users.

```
onUserRemoved:
    sessionIds = findUserSessionsByUserId(user)         // before DB delete
    persister.onUserRemoved(user)                       // DB delete

    enlistAfterCompletion:
        commitImpl:
            for each sessionId in sessionIds:
                cache.remove(sessionId)                 // after DB commit
            // volatile path additionally: predicate scan for any
            // sessions not known to the persister
```

### 4.4 Client session parent invalidation

When client sessions are removed, their parent user session cache entries are invalidated
**first** (before the client session cache entries), using **unconditional** `removeAsync`
(not `getAsync` + conditional remove) to avoid a TOCTOU race. This sequencing ensures a
concurrent reader that loads the user session will re-load its client sessions from the
database rather than finding orphaned client cache entries.

For non-remove client-session mutations, the transaction conditionally removes a parent
loading marker before publishing the child mutation. It does not unconditionally remove a
complete parent; any associated parent membership change is handled by the parent transaction.
If parent publication wins the marker-removal race, child markers have already been installed:
the child mutation therefore either defeats the stale child CAS or updates the child entry
after it is published. Client-session loads do not invalidate the parent marker.

### 4.5 Realm-wide removal

The existing logout-all and realm-removal events invoke cleanup on each node for online and
offline caches. Each handler first removes matching local parent entries and awaits completion,
then independently scans and removes matching local child entries and awaits completion.
Both scans include loading markers and remove entries by their cache keys. Child discovery must
not depend on the parent membership set: the parent can be absent, a marker, or incomplete.

The scans retain `CACHE_MODE_LOCAL`; they reuse the existing event broadcast and add no periodic
cleanup or per-child remote lookup. Parent-before-child ordering is local to each handler, not a
new cluster-wide barrier. A child marker present during its owner's sweep is removed, preventing
its delayed loader from publishing afterward. Entries from other realms are left untouched.

## 5. CAS Failure Handling

**Rule: every code path that attempts a CAS replace must track the DB-loaded entity in
the transaction regardless of whether the CAS succeeded.** The CAS result determines
only whether the entity is also in the cache — it must never determine whether the
entity is visible to the current request.

Rationale: the reader loaded valid data at SELECT time. The CAS failure means the data
won't be cached (correct — the session was deleted), but dropping the entity would cause
valid in-flight requests to lose sessions during concurrent deletion.

This rule applies wherever `cache.replace(key, marker, realData)` is called: single-
session loads, bulk user session imports, and bulk client session imports.

```
anyCASReplace:
    replaced = cache.replace(key, marker, realData)
    consumeLoadingMarker(key)
    track(key, entity)                          // always — even if CAS failed

    if not replaced:
        log("CAS failed — entity bound to transaction without caching")
```

## 6. REMOVE on Loading Markers

**Rule: a REMOVE task that encounters a loading marker in the cache must still be
tracked and executed — it must never be silently dropped.** Non-REMOVE tasks (updates)
on markers are skipped, since the marker's minimal entity cannot receive updates.

Some operations enqueue REMOVE tasks for sessions they haven't loaded. For example,
`removeAuthenticatedClientSessions` knows the client session keys from the user session
entity's `clientSessions` map and enqueues a REMOVE for each — without loading the
client session first. Similarly, `restartSession` removes all client sessions by key.

When `addTask(key, removeTask)` is called and the key is not already tracked in the
transaction, `lookupAndAndExecuteTask` looks up the key in the cache to create a
tracking entry. If a concurrent reader happens to be loading that same client session
(its loading marker is in the cache), the lookup finds the marker.

The marker entity is tracked with the REMOVE task so that:

- The cache entry (marker or real data placed by a concurrent CAS) is removed on commit.
- The DB delete fires via `JpaChangesPerformer` (persistent path).

In the persistent path, client session REMOVEs call `prepareMarkerEntityForRemoval` to
populate the marker entity with `userSessionId` and `clientId` from the
`EmbeddedClientSessionKey`, since `JpaChangesPerformer.removeClientSession()` needs these
fields. The volatile path does not need this step (no JPA removal).

```
lookupAndAndExecuteTask(key, task):
    wrappedEntity = cache.get(key)

    if wrappedEntity == null:
        return                                  // nothing to do

    if wrappedEntity.isLoadingMarker():
        if task.operation == REMOVE:
            prepareMarkerEntityForRemoval(key, entity)
            track(key, entity)
            addTask(REMOVE)
            return
        else:
            return                              // skip non-REMOVE on marker

    // normal path: track and execute task
```

## 6.1 DB Fallback Must Respect Pending REMOVEs

**Rule: the DB/cache fallback path for offline sessions must not execute when the
session is already scheduled for REMOVE in the current transaction.**

When `get(key)` returns null because a REMOVE is pending, the caller must not
fall through to the persistence provider fallback (`getClientSessionEntityFromPersistenceProvider`,
`getUserSessionEntityFromPersistenceProvider`). The fallback reads from the Infinispan
cache directly (via `putIfAbsent`), finds the real entry (the cache REMOVE hasn't
executed yet), and calls `addTask(key, null, entity, PERSISTENT)` — which unconditionally
replaces the `updates` map entry, discarding the pending REMOVE.

This causes two problems:

1. **REMOVE is lost**: the cache entry is never removed at commit time (the new
   `SessionUpdatesList` has no tasks, so `asyncCommit` skips it).
2. **Stale data returned**: the caller sees the session as still present, which can
   prevent dependent cleanup (e.g., `checkOfflineUserSessionHasClientSessions`
   not removing a user session whose last client session was just detached).

The guard uses `isScheduledForRemoval(key)` — a simple check on the transaction's
`updates` map — before entering the fallback path.

```
getClientSession(userSession, client, offline):
    entity = getClientSessionEntity(key, offline)     // tx.get — returns null for REMOVE
    if entity != null:
        return wrap(entity)

    if offline and not clientTx.isScheduledForRemoval(key):
        return getClientSessionEntityFromPersistenceProvider(...)

    return null                                       // REMOVE pending — don't re-import
```

## 7. Concurrent Updates — REPLACE Retry Loop

**Rule: every concurrent update must either be applied via a retry loop or force a
fresh DB load — updates must never be silently lost or overwritten by stale data.**

The REPLACE path uses `computeIfPresentAsync` with version matching. If the version in
the cache doesn't match (because a concurrent reader or writer changed it), the function
returns the current value unchanged. The caller detects the mismatch, **re-applies the
update task** to the current cache value, and retries. This loop runs up to
`MAXIMUM_REPLACE_RETRIES` times.

If the REPLACE encounters a **loading marker**, the update cannot be applied to the
marker's minimal entity. The `handleReplaceResponse` method detects the marker and
issues a `removeAsync` to invalidate it. This causes the original marker owner's CAS
to fail (version mismatch) — the owner tracks its DB data for the current request only
(§5). The marker removal is safe because all cache operations run after DB commit
(§4.1): by the time the REPLACE executes, the DB already contains the committed update.
The next reader loads fresh data from the database.

Similarly, the `handlePutIfAbsentResponse` method detects a returned loading marker
and issues a `removeAsync` to invalidate it, preventing stale data from being imported.

```
replaceIteration(key, task, expectedSession):
    newVersion = newVersionOf(expectedSession)
    result = cache.computeIfPresent(key, matchVersion(expected, new))

    if result.version == newVersion:
        return                                  // success

    if result.isLoadingMarker():
        cache.removeAsync(key)                  // invalidate marker
        return                                  // marker owner falls back to §5

    // Version mismatch: another writer (or CAS reader) changed the entry.
    task.runUpdate(result.entity)               // re-apply update to current state
    replaceIteration(key, task, result)         // retry with current version
```

This ensures that concurrent updates are never lost: either the retry loop re-applies
the update on a version mismatch, or marker invalidation forces a fresh DB load that
includes the committed update.

## 8. Cache Predicate Safety

**Rule: every predicate-based cache scan must check `isLoadingMarker()` first and
return `false` for markers.** Markers have minimal entity fields (e.g., `user` is null),
so accessing entity fields without this guard causes NPEs. Markers must also be invisible
to query results.

**Exception: `SessionWrapperPredicate`** (used in `removeLocalUserSessions` and
`removeEntriesByRealm`) intentionally does NOT skip loading markers. During realm removal,
loading markers should be evicted together with real sessions. The callers only call
`removeAsync` on matched entries and access `getClientSessions()` (which is an empty set
on markers), so no NPE occurs.

## 9. Async Callback Safety

**Rule: cache operations inside `thenCompose`/`thenApply` callbacks must use async
cache methods (`removeAsync`, `computeIfPresentAsync`) — never synchronous equivalents.**
These callbacks may execute on Infinispan non-blocking threads, where a synchronous
cache call would block the event loop.

## 10. Persistence prerequisites

The protocol relies on database reads observing the latest committed state. Database
isolation requirements and the resulting externally visible guarantees are defined in the
[session security model](../../docs/security-models/sessions.md). The cache implementation
must not attempt to compensate for a stale database snapshot by publishing it.

`READ COMMITTED` applies to database statements, not to entities already retained in a
persistence context. Before a marker-owner read may publish, the provider reports whether its
identity map can supply an older entity for the same primary key. A user-session graph also
checks managed client-session entities belonging to that user session; unrelated session
entities do not suppress cache publication. An ineligible graph remains request-local. The
provider does not flush, detach, refresh, or clear managed entities; a later request with a
fresh persistence context can repopulate the cache.
Providers that do not implement this freshness check are ineligible by default, so their
loaded state remains request-local rather than being published to the shared cache.

Transaction completion follows this implementation sequence: collect cache operations,
commit database updates, execute post-commit cache operations, and join their aggregate stage
before returning the response.

For a database-backed replacement, the post-commit cache operation uses the wrapper UUID
observed by the transaction for a single compare-and-set. If that UUID is unchanged, the
replacement is published and the cache remains warm. If it changed, the conflicting value is
removed by that same atomic cache operation instead of replaying the committed update onto it. The
conflict may be a loading marker whose owner can CAS-publish a pre-commit snapshot. Atomic removal
orders against that CAS: it either removes the published snapshot or consumes the marker first.
This avoids both the stale-publication gap of a separate conditional removal and deletion of a
later update by a separate unconditional removal. Two database commits can reach post-commit cache
publication in the opposite order, and wrapper UUIDs do not encode database commit order.
Invalidation on conflict avoids overwriting newer committed state; the next marker-fenced read
reconstructs the entry from the database. Cache-only replacements can continue to use the
versioned retry algorithm.

A possible future optimization is to carry a comparable persistence revision, such as JPA's
entity version, in DB-backed cache entries. That would allow an older post-commit publication
to retain a cache entry known to represent a newer database commit instead of invalidating it.
This requires changes to the persistence-provider contract and to revision propagation after
database flush; it is not required for correctness of the UUID-CAS-plus-invalidation design.

## 11. DB-backed cache lifespan

DB-backed entries retain a `cachedAt` timestamp in `localMetadata`. Replacement preserves it,
and each write caps its effective lifespan without extending the original cache deadline:

```
if cachedAt not set:
    cachedAt = now
    effectiveLifespan = min(remainingSessionLifespan, maxCacheLifespanMs)
else:
    maxRemaining = cachedAt + maxCacheLifespanMs - now
    effectiveLifespan = min(remainingSessionLifespan, maxRemaining)
```

When `maxRemaining <= 0`, the lifespan is 1 ms. Lifespan must be computed from the actual
wrapper being written because a REPLACE retry (§7) may pick up a different `cachedAt` from
the cache.

Configuration: `--spi-user-sessions--infinispan--max-cache-lifespan` (seconds). Default: 3600.
Set to 0 or -1 to disable the cap.
