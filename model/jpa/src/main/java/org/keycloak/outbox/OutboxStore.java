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
package org.keycloak.outbox;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

import org.hibernate.LockMode;
import org.hibernate.query.SelectionQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;

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
 * inlining queries. Mirrors what the SSF-specific {@code SsfEventStore}
 * established; SSF migrates to this surface in a follow-up.
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
                                 String correlationId,
                                 String entryType,
                                 String payload,
                                 String metadata) {
        Objects.requireNonNull(entryKind, "entryKind");
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(entryType, "entryType");
        Objects.requireNonNull(payload, "payload");

        OutboxEntryEntity existing = findByOwnerAndCorrelationId(entryKind, ownerId, correlationId);
        if (existing != null) {
            log.debugf("Outbox enqueue deduplicated. entryKind=%s ownerId=%s correlationId=%s existingId=%s status=%s",
                    entryKind, ownerId, correlationId, existing.getId(), existing.getStatus());
            return existing.getId();
        }

        Instant now = Instant.now();
        OutboxEntryEntity entity = new OutboxEntryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEntryKind(entryKind);
        entity.setRealmId(realmId);
        entity.setOwnerId(ownerId);
        entity.setCorrelationId(correlationId);
        entity.setEntryType(entryType);
        entity.setPayload(payload);
        entity.setMetadata(metadata);
        entity.setStatus(OutboxEntryStatus.PENDING);
        entity.setAttempts(0);
        entity.setNextAttemptAt(now);
        entity.setCreatedAt(now);

        getEntityManager().persist(entity);
        log.debugf("Outbox enqueued. id=%s entryKind=%s realmId=%s ownerId=%s correlationId=%s entryType=%s",
                entity.getId(), entryKind, realmId, ownerId, correlationId, entryType);
        return entity.getId();
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
