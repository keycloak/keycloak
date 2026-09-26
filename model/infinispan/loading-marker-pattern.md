# Loading Marker + CAS Pattern

This document describes the cache consistency patterns used when loading sessions from the
database into the Infinispan cache. The goals are:

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

### 2.2 Bulk-session load (user session + its client sessions)

Client session markers are placed **in bulk before** the user session import. This
ensures that if the user session CAS succeeds, no concurrent reader can slip a client
session into the cache between the user session import and the client session imports.

```
reader:
    clientSessionsById = collectClientSessions(userSession)
    clientTx.placeLoadingMarkers(clientSessionsById)
    try:
        existingUserSession = importUserSession(...)

        if existingUserSession != null:
            // Persistent: if the existing entry is another reader's
            // loading marker, the session was likely deleted during
            // import — treat as unsuccessful.
            if existingUserSession.isLoadingMarker():
                return null                     // concurrent delete or conflict
            return existingUserSession          // another reader already imported

        // Volatile only: check whether the user session marker was
        // consumed by a successful CAS. wasMarkerConsumed checks if
        // consumeLoadingMarker removed it from the ownership map.
        if not wasMarkerConsumed(sessionId):
            return null                         // concurrent delete or conflict

        clientTx.importSessionsConcurrently(clientSessionsById)
        return userSession
    finally:
        clientTx.cleanupLoadingMarkers(clientSessionsById)
```

The `finally` block is essential: if `importUserSession` or `importSessionsConcurrently`
throws, unconsumed client markers must be removed. `cleanupLoadingMarkers` is idempotent —
already-consumed markers are no longer in the ownership map, so the CAS
`cache.remove(key, marker)` is a no-op for them.

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

**All** cache operations — ADD, ADD_IF_ABSENT, REPLACE, and REMOVE — are deferred until
after the database commit. During `asyncCommit`, only DB writes are registered; all cache
operations are collected as `PendingCacheOp` records. After `commitDatabaseUpdates`
completes, `asyncPostDatabaseCommit` executes the collected cache operations.

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

This eliminates the race window entirely: no cache entry is written, updated, or removed
until the corresponding DB change is committed. A concurrent reader that loads from the
database during or after `asyncPostDatabaseCommit` will always see committed data, and
any cache import it attempts will reflect the committed state.

In the volatile path, online sessions (no DB persistence) run cache operations immediately
in `asyncCommit` since there is no DB to wait for. Offline sessions that persist to the
database use the same DB-first pattern: cache operations are collected and deferred to
`asyncPostDatabaseCommit`.

### 4.2 User deletion (`onUserRemoved`)

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

### 4.3 Client session parent invalidation

When client sessions are removed, their parent user session cache entries are invalidated
**first** (before the client session cache entries), using **unconditional** `removeAsync`
(not `getAsync` + conditional remove) to avoid a TOCTOU race. This sequencing ensures a
concurrent reader that loads the user session will re-load its client sessions from the
database rather than finding orphaned client cache entries.

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

## 10. Transaction Isolation Level

**Rule: MySQL, MariaDB, and TiDB must use `READ COMMITTED` isolation unconditionally.**
Under `REPEATABLE READ`, a reader whose transaction snapshot predates a DELETE can load
the deleted row from its stale snapshot and CAS it into the cache — no cache-level
mechanism can prevent this. `READ COMMITTED` ensures every SELECT sees the latest
committed state, eliminating stale-snapshot resurrection.

Alternatives considered and dismissed:
- **Cache tombstones**: only prevent resurrection of deleted sessions, not stale updates.
- **SELECT FOR UPDATE**: sees current state but serializes concurrent reads and risks deadlocks.

## 11. Post-Completion Guarantees

With DB-first ordering (§4.1), both delete and update guarantees follow the same
commit sequence:

1. `asyncCommit` — collects `PendingCacheOp` records; registers DB writes
2. `commitDatabaseUpdates` — all DB changes commit atomically
3. `asyncPostDatabaseCommit` — all cache operations execute
4. Join — waits for all cache operations to complete
5. Response returns

### 11.1 Delete (logout)

**Guarantee: once a logout request returns, any new request that starts after it will
never see the deleted session. Only requests that were already in-flight before the
logout completed may observe the session (bounded to their own transaction, §5).**

After step 2, the DB no longer has the session. After step 4, the cache entry is also
removed. A concurrent reader whose marker was removed in step 3 has its CAS fail — it
binds data to its own transaction only (§5), never to the cache. A new reader arriving
after step 5 finds the cache empty, loads from DB (which returns nothing), and correctly
sees no session.

Volatile-path: the provider calls the persister (DB delete) before enqueuing the REMOVE
task. The cache REMOVE runs in `asyncPostDatabaseCommit`. Both complete before the response returns.

### 11.2 Update

**Guarantee: once an update request returns, any new request that starts after it will
see the updated data.**

After step 2, the DB contains the update. In step 3, the cache is updated via the
REPLACE retry loop. If a loading marker is encountered, it is removed (§7) — this is
safe because the DB already has the committed update. The next reader loads from DB and
gets the updated data.

Because all cache operations run after DB commit, there is no window where a concurrent
reader could cache pre-commit data: the DB is always ahead of or in sync with the cache.

## 12. Max Cache Lifespan Cap

**Rule: every DB-backed session cache entry has its lifespan capped at a configurable
maximum (default: 1 hour). An update must not extend it.** 
This provides eventual consistency as a safety net for
scenarios where cache operations fail to propagate — most notably when a node dies
after committing DB changes but before executing post-commit cache operations.

Session caches that are backed by a database already skip state transfer on topology
changes, so most cache entries are effectively cleared when a node departs. However,
entries on segments that remain assigned to the same surviving node retain their
original lifespan and could serve stale data until natural expiry. The max lifespan
cap bounds this window.

The cap is applied only to DB-backed sessions — volatile (online-only) sessions are
not affected because they have no DB to fall back to.

### 12.1 `cachedAt` tracking

To provide a hard guarantee, the cap tracks when the entry was **first cached** using
a `cachedAt` timestamp in the entry's `localMetadata`. The REPLACE path preserves
`localMetadata` across updates (§7), so the original `cachedAt` survives all subsequent
writes. On each cache write, the effective lifespan is:

```
if cachedAt not set:
    cachedAt = now
    effectiveLifespan = min(remainingSessionLifespan, maxCacheLifespanMs)
else:
    maxRemaining = cachedAt + maxCacheLifespanMs - now
    effectiveLifespan = min(remainingSessionLifespan, maxRemaining)
```

This ensures the entry expires at most `maxCacheLifespanMs` after it was first cached,
regardless of how many updates happen in between. Without this tracking, each REPLACE
would reset the cap window, allowing frequently-updated entries to live indefinitely
without a DB reload.

When `maxRemaining <= 0`, the lifespan is set to 1ms — the entry expires immediately
on the next cache cycle, and the next reader loads from DB with a fresh `cachedAt`.

Note: the lifespan must always be computed from the actual wrapper being written
(`MergedUpdate.getEffectiveLifespanMs(wrapper)`), not pre-computed from a different
wrapper. A REPLACE retry (§7) picks up the existing cache entry whose `cachedAt` may
differ from the transaction-local wrapper.

Configuration: `--spi-user-sessions--infinispan--max-cache-lifespan` (seconds). Default: 3600.
Set to 0 or -1 to disable the cap.

## 13. Follow-Up Explorations

### 13.1 Unified DB persistence via `JpaChangesPerformer`

The volatile offline path now routes REPLACE and REMOVE operations through
`JpaChangesPerformer`, but CREATE (`ADD_IF_ABSENT`) is still handled by explicit
`persister.createUserSession()` / `persister.createClientSession()` calls in the
provider. Widening the filter to include all operations would let the transaction
handle DB persistence uniformly, eliminating the duplicate persister calls in
`createOfflineUserSession`, `createOfflineClientSession`, and `removeOfflineUserSession`.

### 13.2 Cache-only online sessions in the persistent provider

The persistent provider currently writes online sessions to the database. Adding a
mode where online sessions use an unbounded Infinispan cache (no DB persistence) would
make the volatile provider (`InfinispanUserSessionProvider`, `InfinispanChangelogBasedTransaction`)
redundant. Key changes: skip DB writes for non-offline sessions, reroute online queries
to cache scans, enable state transfer for online caches. This would significantly reduce
the code surface and eliminate the need to maintain two parallel provider implementations.

Why suggesting this: People might be concerned by the additional database load the persistent provider generates for
online sessions. A cache-only mode would reduce the load on the database and improve performance for online sessions,
while still allowing offline sessions to be persisted in the database for long-term storage.
It comes with the cost of rebalancing, and would be useful only for users with very few sessions.

### 13.3 Prevent thundering herd on session entries

From https://www.usenix.org/system/files/conference/nsdi13/nsdi13-final170_update.pdf:

* When a load marker is discovered, wait a short a mount of time and retry to see if the value appears. Only then go to the database.
  (still, this would most likely only apply to entities used by multiple callers, which isn't usually the case for sessions).
* If access to the cache fails, load from the database but do not fail the request. As this can happen in bulk if a Keycloak node
  disappears unexpectedly, this could lead to a thundering herd of requests hitting the database. With the latest changes
  to the graceful leaving of ISPN instances, this should hopefully no longer be a problem.