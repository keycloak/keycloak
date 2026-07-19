/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.events.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;

import org.hibernate.LockMode;
import org.hibernate.query.SelectionQuery;
import org.jboss.logging.Logger;

/**
 * DAO over {@link OutboxEntryEntity}. Every method takes the row's
 * {@code entryKind} so the same store instance can serve multiple
 * subsystems sharing the underlying {@code OUTBOX_ENTRY} table — the
 * compound indexes on {@code (ENTRY_KIND, ...)} keep cross-kind
 * traffic from interfering with each other's hot paths.
 *
 * <p>Read patterns (drainer, admin stats, retention purges) and write
 * patterns (enqueue, transition, bulk delete) are split here so the
 * runtime drainer / cleanup tasks compose primitives rather than
 * inlining queries.
 */
public class OutboxStore {

    private static final Logger log = Logger.getLogger(OutboxStore.class);

    /**
     * Hard cap on the {@code last_error} column width — matches the
     * VARCHAR(2048) defined in the changelog. Truncation is applied
     * here so callers can pass arbitrarily long exception messages
     * without worrying about persistence-layer rejection.
     */
    public static final int MAX_LAST_ERROR_LENGTH = 2048;

    protected final KeycloakSession session;

    public OutboxStore(KeycloakSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    protected EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    // -- Enqueue -----------------------------------------------------------

    /**
     * Inserts a fresh PENDING row, deduplicating on
     * {@code (entryKind, ownerId, correlationId)}. Returns the id of
     * the persisted (or pre-existing) row so the caller can correlate
     * across at-least-once enqueue paths.
     */
    public String enqueuePending(String entryKind,
                                 String realmId,
                                 String ownerId,
                                 String containerId,
                                 String correlationId,
                                 String entryType,
                                 String payload,
                                 String metadata) {
        return enqueueInStatus(OutboxEntryStatus.PENDING, entryKind, realmId, ownerId, containerId,
                correlationId, entryType, payload, metadata);
    }

    /**
     * Inserts a fresh HELD row — used when the upstream channel is in
     * a paused state at enqueue time (e.g. SSF stream paused) and the
     * row should not be drained until {@link #releaseHeldForOwner} is
     * called. Same dedup contract as {@link #enqueuePending}.
     */
    public String enqueueHeld(String entryKind,
                              String realmId,
                              String ownerId,
                              String containerId,
                              String correlationId,
                              String entryType,
                              String payload,
                              String metadata) {
        return enqueueInStatus(OutboxEntryStatus.HELD, entryKind, realmId, ownerId, containerId,
                correlationId, entryType, payload, metadata);
    }

    protected String enqueueInStatus(OutboxEntryStatus status,
                                     String entryKind,
                                     String realmId,
                                     String ownerId,
                                     String containerId,
                                     String correlationId,
                                     String entryType,
                                     String payload,
                                     String metadata) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(entryType, "entryType");
        Objects.requireNonNull(payload, "payload");

        // Optimistic local fast-path: if we already wrote the row in this
        // transaction (or it's recent enough to be in the row cache),
        // skip the INSERT and return the existing id. Correctness does
        // not depend on this — the storage-engine ON CONFLICT DO NOTHING
        // below dedups regardless. Cheap savings for retry-style callers.
        OutboxEntryEntity existing = findByOwnerAndCorrelationId(entryKind, ownerId, correlationId);
        if (existing != null) {
            log.debugf("Outbox enqueue deduplicated. entryKind=%s ownerId=%s correlationId=%s existingId=%s status=%s",
                    entryKind, ownerId, correlationId, existing.getId(), existing.getStatus());
            return existing.getId();
        }

        Instant now = Instant.now();
        String id = generateEntryId();
        // Race-safe insert. ON CONFLICT DO NOTHING (HQL, Hibernate 6.5+)
        // resolves the dedup race at the storage engine: a concurrent
        // sibling insert of the same (entryKind, ownerId, correlationId)
        // triple causes our INSERT to no-op (executeUpdate returns 0)
        // instead of throwing ConstraintViolationException and marking
        // the JTA transaction rollback-only. The caller's surrounding
        // transaction therefore survives the race cleanly.
        //
        // next_attempt_at is meaningful only for PENDING rows the drainer
        // locks; HELD rows ignore it but the column is NOT NULL, so we
        // set it to "now" as a harmless seed.
        int inserted = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.insertIfAbsent")
                .setParameter("id", id)
                .setParameter("entryKind", entryKind)
                .setParameter("realmId", realmId)
                .setParameter("ownerId", ownerId)
                .setParameter("containerId", containerId)
                .setParameter("correlationId", correlationId)
                .setParameter("entryType", entryType)
                .setParameter("payload", payload)
                .setParameter("metadata", metadata)
                .setParameter("status", status)
                .setParameter("attempts", 0)
                .setParameter("nextAttemptAt", now)
                .setParameter("createdAt", now)
                .executeUpdate();

        if (inserted == 0) {
            // Lost the dedup race against a sibling. Their row is in
            // storage and will be drained, so the event is captured
            // at-most-once as intended. Re-fetch and return their id
            // so the caller can correlate.
            OutboxEntryEntity racingRow = findByOwnerAndCorrelationId(entryKind, ownerId, correlationId);
            log.debugf("Outbox enqueue lost dedup race; sibling already inserted. "
                    + "entryKind=%s realmId=%s ownerId=%s correlationId=%s racingId=%s",
                    entryKind, realmId, ownerId, correlationId,
                    racingRow != null ? racingRow.getId() : "(unresolved)");
            return racingRow != null ? racingRow.getId() : id;
        }

        log.debugf("Outbox enqueued. id=%s status=%s entryKind=%s realmId=%s ownerId=%s containerId=%s correlationId=%s entryType=%s",
                id, status, entryKind, realmId, ownerId, containerId, correlationId, entryType);
        return id;
    }

    protected String generateEntryId() {
        return UUID.randomUUID().toString();
    }

    public OutboxEntryEntity findById(String id) {
        return getEntityManager().find(OutboxEntryEntity.class, id);
    }

    public OutboxEntryEntity findByOwnerAndCorrelationId(String entryKind, String ownerId, String correlationId) {
        try {
            return getEntityManager()
                    .createNamedQuery("OutboxEntryEntity.findByOwnerAndCorrelationId", OutboxEntryEntity.class)
                    .setParameter("entryKind", entryKind)
                    .setParameter("ownerId", ownerId)
                    .setParameter("correlationId", correlationId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // -- Drainer reads -----------------------------------------------------

    /**
     * Locks up to {@code limit} due PENDING rows for delivery in the
     * current transaction. Uses {@code FOR UPDATE SKIP LOCKED} so
     * cluster-aware drainers don't fight for the same rows.
     */
    public List<OutboxEntryEntity> lockDueForDrain(String entryKind, int limit) {
        Objects.requireNonNull(entryKind, "entryKind");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }
        var query = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.findDueForDrain", OutboxEntryEntity.class)
                .setParameter("entryKind", entryKind)
                .setParameter("status", OutboxEntryStatus.PENDING)
                .setParameter("now", Instant.now())
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE);
        // Skip rows another tick / node already holds — a sibling
        // drainer will get them on its own pass without blocking us.
        try {
            query.unwrap(SelectionQuery.class).setHibernateLockMode(LockMode.UPGRADE_SKIPLOCKED);
        } catch (RuntimeException e) {
            log.debugf(e, "Could not set UPGRADE_SKIPLOCKED on outbox drain query — proceeding without skip-locked");
        }
        return query.getResultList();
    }

    // -- Row transitions ---------------------------------------------------

    public void markDelivered(OutboxEntryEntity entity) {
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setStatus(OutboxEntryStatus.DELIVERED);
        entity.setDeliveredAt(Instant.now());
        entity.setLastError(null);
        getEntityManager().merge(entity);
    }

    public void recordFailure(OutboxEntryEntity entity, Instant nextAttemptAt, String lastError) {
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setNextAttemptAt(nextAttemptAt);
        entity.setLastError(truncateError(lastError));
        getEntityManager().merge(entity);
    }

    public void markDeadLetter(OutboxEntryEntity entity, String lastError) {
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setStatus(OutboxEntryStatus.DEAD_LETTER);
        entity.setLastError(truncateError(lastError));
        getEntityManager().merge(entity);
    }

    /**
     * Bulk-promotes every {@link OutboxEntryStatus#QUEUED queued} row
     * in the given kind whose {@code createdAt} is older than the
     * supplied cutoff to {@code DEAD_LETTER}. Used by the drainer as a
     * backstop so rows that get stuck in PENDING/HELD eventually
     * graduate to a terminal state and are caught by the dead-letter
     * retention purge.
     *
     * <p>Does not bump {@code attempts}: these rows didn't actually
     * retry, they aged out. The {@code last_error} captures the reason.
     */
    public int promoteStaleQueuedToDeadLetter(String entryKind, Instant cutoff, String reason) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(cutoff, "cutoff");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.promoteStaleQueuedToDeadLetter")
                .setParameter("entryKind", entryKind)
                .setParameter("dead", OutboxEntryStatus.DEAD_LETTER)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .setParameter("olderThan", cutoff)
                .setParameter("reason", truncateError(reason))
                .executeUpdate();
    }

    // -- Stats (admin endpoints) -------------------------------------------

    public Map<OutboxEntryStatus, Long> countStatusesForRealm(String entryKind, String realmId) {
        return groupedCountQuery("OutboxEntryEntity.countByEntryKindRealmAndStatus",
                entryKind, "realmId", realmId);
    }

    public Map<OutboxEntryStatus, Long> countStatusesForOwner(String entryKind, String ownerId) {
        return groupedCountQuery("OutboxEntryEntity.countByEntryKindOwnerAndStatus",
                entryKind, "ownerId", ownerId);
    }

    public Map<OutboxEntryStatus, Instant> oldestCreatedAtPerStatusForRealm(String entryKind, String realmId) {
        return groupedInstantQuery("OutboxEntryEntity.oldestCreatedAtByEntryKindRealmAndStatus",
                entryKind, "realmId", realmId);
    }

    public Map<OutboxEntryStatus, Instant> oldestCreatedAtPerStatusForOwner(String entryKind, String ownerId) {
        return groupedInstantQuery("OutboxEntryEntity.oldestCreatedAtByEntryKindOwnerAndStatus",
                entryKind, "ownerId", ownerId);
    }

    @SuppressWarnings("unchecked")
    private Map<OutboxEntryStatus, Long> groupedCountQuery(String namedQuery, String entryKind,
                                                           String scopeParam, String scopeValue) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(scopeValue, scopeParam);
        List<Object[]> rows = getEntityManager()
                .createNamedQuery(namedQuery)
                .setParameter("entryKind", entryKind)
                .setParameter(scopeParam, scopeValue)
                .getResultList();
        Map<OutboxEntryStatus, Long> counts = new EnumMap<>(OutboxEntryStatus.class);
        for (Object[] row : rows) {
            counts.put((OutboxEntryStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @SuppressWarnings("unchecked")
    private Map<OutboxEntryStatus, Instant> groupedInstantQuery(String namedQuery, String entryKind,
                                                                String scopeParam, String scopeValue) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(scopeValue, scopeParam);
        List<Object[]> rows = getEntityManager()
                .createNamedQuery(namedQuery)
                .setParameter("entryKind", entryKind)
                .setParameter(scopeParam, scopeValue)
                .getResultList();
        Map<OutboxEntryStatus, Instant> oldest = new EnumMap<>(OutboxEntryStatus.class);
        for (Object[] row : rows) {
            oldest.put((OutboxEntryStatus) row[0], (Instant) row[1]);
        }
        return oldest;
    }

    // -- Receiver-driven reads (POLL) --------------------------------------

    /**
     * Locks up to {@code limit} PENDING rows for a receiver-driven
     * read (e.g. SSF POLL). Uses {@code FOR UPDATE SKIP LOCKED} so a
     * concurrent receiver request to the same owner doesn't block.
     * Unlike {@link #lockDueForDrain(String, int)} this does not gate
     * on {@code next_attempt_at} — receiver-pulled rows are served
     * on demand regardless of any backoff schedule.
     */
    public List<OutboxEntryEntity> lockPendingForOwner(String entryKind, String ownerId, int limit) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }
        var query = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.findPendingForOwner", OutboxEntryEntity.class)
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("status", OutboxEntryStatus.PENDING)
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE);
        try {
            query.unwrap(SelectionQuery.class).setHibernateLockMode(LockMode.UPGRADE_SKIPLOCKED);
        } catch (RuntimeException e) {
            log.debugf(e, "Could not set UPGRADE_SKIPLOCKED on owner-pending query — proceeding without skip-locked");
        }
        return query.getResultList();
    }

    /**
     * Counts an owner's rows in a given status. Used by receiver-driven
     * read paths to decide whether to advertise more available items
     * after returning a short batch.
     */
    public long countForOwnerByStatus(String entryKind, String ownerId, OutboxEntryStatus status) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(status, "status");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.countByEntryKindOwnerStatus", Long.class)
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * Receiver-driven ACK for the supplied correlation ids. Matching
     * PENDING rows owned by the given owner transition to DELIVERED.
     * Idempotent and silently scoped: ids the receiver doesn't own
     * (different owner) and ids already terminal don't appear in the
     * lookup result, so no error and no leakage of row existence.
     *
     * @return the set of correlation ids that were transitioned to
     *         DELIVERED.
     */
    public Set<String> ackPendingForOwner(String entryKind, String ownerId, Collection<String> correlationIds) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        if (correlationIds == null || correlationIds.isEmpty()) {
            return Set.of();
        }
        List<OutboxEntryEntity> rows = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.findPendingForOwnerByCorrelationIds", OutboxEntryEntity.class)
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("correlationIds", correlationIds)
                .setParameter("status", OutboxEntryStatus.PENDING)
                .getResultList();
        if (rows.isEmpty()) {
            return Set.of();
        }
        Set<String> acked = new LinkedHashSet<>(rows.size());
        for (OutboxEntryEntity row : rows) {
            markDelivered(row);
            acked.add(row.getCorrelationId());
        }
        log.debugf("Outbox ack. entryKind=%s ownerId=%s ackedCount=%d", entryKind, ownerId, acked.size());
        return acked;
    }

    /**
     * Receiver-driven NACK. Matching PENDING rows owned by the given
     * owner transition to DEAD_LETTER carrying the receiver-supplied
     * reason. For receiver-pulled flows, DEAD_LETTER is reached only
     * via this explicit NACK path (no transmitter-side retry-exhaustion
     * counter to bump). Idempotent and silently scoped, like
     * {@link #ackPendingForOwner}.
     *
     * @return the set of correlation ids that were transitioned to
     *         DEAD_LETTER.
     */
    public Set<String> nackPendingForOwner(String entryKind, String ownerId, Map<String, String> reasonByCorrelationId) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        if (reasonByCorrelationId == null || reasonByCorrelationId.isEmpty()) {
            return Set.of();
        }
        List<OutboxEntryEntity> rows = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.findPendingForOwnerByCorrelationIds", OutboxEntryEntity.class)
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("correlationIds", reasonByCorrelationId.keySet())
                .setParameter("status", OutboxEntryStatus.PENDING)
                .getResultList();
        if (rows.isEmpty()) {
            return Set.of();
        }
        Set<String> nacked = new LinkedHashSet<>(rows.size());
        for (OutboxEntryEntity row : rows) {
            String reason = reasonByCorrelationId.get(row.getCorrelationId());
            markDeadLetter(row, reason != null ? reason : "receiver nack");
            nacked.add(row.getCorrelationId());
        }
        log.debugf("Outbox nack. entryKind=%s ownerId=%s nackedCount=%d", entryKind, ownerId, nacked.size());
        return nacked;
    }

    // -- Owner-scoped lifecycle (pause/resume/disable/migrate) -------------

    /**
     * Bulk-transitions every {@link OutboxEntryStatus#HELD HELD} row
     * for the owner back to {@link OutboxEntryStatus#PENDING PENDING}
     * with {@code next_attempt_at = now} so the drainer picks them up
     * on its next tick. Symmetric to {@link #holdPendingForOwner}.
     *
     * @return the number of rows that transitioned out of HELD.
     */
    public int releaseHeldForOwner(String entryKind, String ownerId) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        int released = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.releaseHeldForOwner")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("pending", OutboxEntryStatus.PENDING)
                .setParameter("held", OutboxEntryStatus.HELD)
                .setParameter("now", Instant.now())
                .executeUpdate();
        if (released > 0) {
            log.debugf("Outbox released %d held row(s) for entryKind=%s ownerId=%s", released, entryKind, ownerId);
        }
        return released;
    }

    /**
     * Bulk-transitions every PENDING row for the owner to HELD —
     * "park" the queue when the upstream channel pauses (e.g. SSF
     * stream paused / disabled).
     *
     * @return the number of rows that transitioned PENDING → HELD.
     */
    public int holdPendingForOwner(String entryKind, String ownerId) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        int held = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.holdPendingForOwner")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("held", OutboxEntryStatus.HELD)
                .setParameter("pending", OutboxEntryStatus.PENDING)
                .executeUpdate();
        if (held > 0) {
            log.debugf("Outbox held %d pending row(s) for entryKind=%s ownerId=%s", held, entryKind, ownerId);
        }
        return held;
    }

    /**
     * Dead-letters every queued (PENDING + HELD) row for the owner
     * with the supplied reason. Used when the upstream forbids
     * holding (e.g. SSF stream disabled) and the rows must be
     * discarded rather than parked.
     */
    public int deadLetterQueuedForOwner(String entryKind, String ownerId, String reason) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(reason, "reason");
        int updated = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deadLetterQueuedForOwner")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("dead", OutboxEntryStatus.DEAD_LETTER)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .setParameter("reason", truncateError(reason))
                .executeUpdate();
        if (updated > 0) {
            log.debugf("Outbox dead-lettered %d queued row(s) for entryKind=%s ownerId=%s", updated, entryKind, ownerId);
        }
        return updated;
    }

    /**
     * Dead-letters queued rows for the owner whose {@code entryType}
     * is not in {@code allowedTypes}. Used when the upstream narrows
     * its accepted-type set (e.g. SSF receiver narrowing
     * {@code events_requested}) so already-signed rows of dropped
     * types stop being delivered without losing the audit trail.
     *
     * <p>If {@code allowedTypes} is empty, this method falls back to
     * {@link #deadLetterQueuedForOwner} since SQL {@code NOT IN ()}
     * is implementation-defined.
     */
    public int deadLetterQueuedForOwnerNotMatchingTypes(String entryKind, String ownerId,
                                                        Collection<String> allowedTypes,
                                                        String reason) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(allowedTypes, "allowedTypes");
        Objects.requireNonNull(reason, "reason");
        if (allowedTypes.isEmpty()) {
            return deadLetterQueuedForOwner(entryKind, ownerId, reason);
        }
        int updated = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deadLetterQueuedForOwnerNotMatchingTypes")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("dead", OutboxEntryStatus.DEAD_LETTER)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .setParameter("allowedTypes", allowedTypes)
                .setParameter("reason", truncateError(reason))
                .executeUpdate();
        if (updated > 0) {
            log.debugf("Outbox dead-lettered %d queued row(s) for entryKind=%s ownerId=%s with entryType outside the allow-list",
                    updated, entryKind, ownerId);
        }
        return updated;
    }

    /**
     * Migrates queued rows for the owner from one entryKind to another
     * (e.g. SSF receiver flipping push ↔ poll). Terminal rows
     * (DELIVERED, DEAD_LETTER) are left under the previous kind — they
     * are audit / dedup artifacts of the old channel.
     *
     * @return the number of rows whose entryKind was migrated.
     */
    public int migrateEntryKindForOwner(String currentKind, String newKind, String ownerId) {
        Objects.requireNonNull(currentKind, "currentKind");
        Objects.requireNonNull(newKind, "newKind");
        Objects.requireNonNull(ownerId, "ownerId");
        int migrated = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.migrateEntryKindForOwner")
                .setParameter("currentKind", currentKind)
                .setParameter("newKind", newKind)
                .setParameter("ownerId", ownerId)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .executeUpdate();
        if (migrated > 0) {
            log.debugf("Outbox migrated %d row(s) for ownerId=%s from %s to %s", migrated, ownerId, currentKind, newKind);
        }
        return migrated;
    }

    // -- Admin / cascade deletes -------------------------------------------

    public int deleteByRealm(String entryKind, String realmId) {
        return scopedDelete("OutboxEntryEntity.deleteByEntryKindAndRealm",
                entryKind, "realmId", realmId);
    }

    public int deleteByOwner(String entryKind, String ownerId) {
        return scopedDelete("OutboxEntryEntity.deleteByEntryKindAndOwner",
                entryKind, "ownerId", ownerId);
    }

    public int deleteByRealmAndStatus(String entryKind, String realmId, OutboxEntryStatus status) {
        Objects.requireNonNull(status, "status");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteByEntryKindRealmAndStatus")
                .setParameter("entryKind", entryKind)
                .setParameter("realmId", realmId)
                .setParameter("status", status)
                .executeUpdate();
    }

    public int deleteByRealmAndStatusOlderThan(String entryKind, String realmId,
                                               OutboxEntryStatus status, Instant cutoff) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(cutoff, "cutoff");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteByEntryKindRealmAndStatusOlderThan")
                .setParameter("entryKind", entryKind)
                .setParameter("realmId", realmId)
                .setParameter("status", status)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
    }

    public int deleteByOwnerAndStatus(String entryKind, String ownerId, OutboxEntryStatus status) {
        Objects.requireNonNull(status, "status");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteByEntryKindOwnerAndStatus")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("status", status)
                .executeUpdate();
    }

    public int deleteByOwnerAndStatusOlderThan(String entryKind, String ownerId,
                                               OutboxEntryStatus status, Instant cutoff) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(cutoff, "cutoff");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteByEntryKindOwnerAndStatusOlderThan")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("status", status)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
    }

    /**
     * Bulk-deletes every {@link OutboxEntryStatus#QUEUED queued} row
     * in the realm. Single-DML counterpart used by the realm-scoped
     * "purge queued" admin endpoint.
     */
    public int deleteQueuedByRealm(String entryKind, String realmId) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(realmId, "realmId");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteQueuedByEntryKindAndRealm")
                .setParameter("entryKind", entryKind)
                .setParameter("realmId", realmId)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .executeUpdate();
    }

    public int deleteQueuedByOwner(String entryKind, String ownerId) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(ownerId, "ownerId");
        return getEntityManager()
                .createNamedQuery("OutboxEntryEntity.deleteQueuedByEntryKindAndOwner")
                .setParameter("entryKind", entryKind)
                .setParameter("ownerId", ownerId)
                .setParameter("statuses", OutboxEntryStatus.QUEUED)
                .executeUpdate();
    }

    // -- Retention purges (drainer housekeeping) ---------------------------

    public int purgeDeliveredOlderThan(String entryKind, Instant cutoff) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(cutoff, "cutoff");
        int purged = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.purgeByEntryKindStatusOlderThanDelivered")
                .setParameter("entryKind", entryKind)
                .setParameter("status", OutboxEntryStatus.DELIVERED)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
        if (purged > 0) {
            log.debugf("Outbox purged %d DELIVERED row(s) older than %s for entryKind=%s", purged, cutoff, entryKind);
        }
        return purged;
    }

    public int purgeDeadLetterOlderThan(String entryKind, Instant cutoff) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(cutoff, "cutoff");
        int purged = getEntityManager()
                .createNamedQuery("OutboxEntryEntity.purgeByEntryKindStatusOlderThanCreated")
                .setParameter("entryKind", entryKind)
                .setParameter("status", OutboxEntryStatus.DEAD_LETTER)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
        if (purged > 0) {
            log.debugf("Outbox purged %d DEAD_LETTER row(s) older than %s for entryKind=%s", purged, cutoff, entryKind);
        }
        return purged;
    }

    // -- Helpers -----------------------------------------------------------

    private int scopedDelete(String namedQuery, String entryKind, String scopeParam, String scopeValue) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(scopeValue, scopeParam);
        return getEntityManager()
                .createNamedQuery(namedQuery)
                .setParameter("entryKind", entryKind)
                .setParameter(scopeParam, scopeValue)
                .executeUpdate();
    }

    /**
     * Truncates an error message to the column width with an ellipsis
     * marker. Returns {@code null} unchanged so an explicit "no error"
     * value (used by {@code markDelivered}) survives.
     */
    protected String truncateError(String error) {
        if (error == null) {
            return null;
        }
        if (error.length() <= MAX_LAST_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_LAST_ERROR_LENGTH - 3) + "...";
    }
}
