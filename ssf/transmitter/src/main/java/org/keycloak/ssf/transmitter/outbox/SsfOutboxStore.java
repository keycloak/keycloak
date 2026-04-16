package org.keycloak.ssf.transmitter.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.hibernate.LockMode;
import org.hibernate.query.SelectionQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Thin JPA repository over {@link SsfPendingEventEntity}. Does not
 * manage transactions — callers run inside the ambient
 * {@link KeycloakSession} transaction, which Keycloak wraps around
 * request handlers and scheduled-task executions.
 */
public class SsfOutboxStore {

    private static final Logger log = Logger.getLogger(SsfOutboxStore.class);

    private final KeycloakSession session;

    public SsfOutboxStore(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Enqueues a SET for asynchronous delivery. Idempotent on
     * {@code (clientId, jti)} — if a row already exists for that pair
     * (e.g. the same event is re-dispatched after a partial failure)
     * the existing row is kept and this call is a no-op, so retry
     * state isn't reset.
     *
     * @return the enqueued row id, or the pre-existing row id if the
     *         insert was deduped.
     */
    public String enqueuePendingPush(String realmId,
                                     String clientId,
                                     String streamId,
                                     String jti,
                                     String eventType,
                                     String encodedSet) {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(jti, "jti");
        Objects.requireNonNull(encodedSet, "encodedSet");

        EntityManager em = getEntityManager();

        // Dedup scan — relies on the (CLIENT_ID, JTI) unique index but
        // checked in code first so we don't have to catch a constraint
        // violation from the JPA provider's batched flush for the
        // common no-op path.
        SsfPendingEventEntity existing = findByClientAndJti(em, clientId, jti);
        if (existing != null) {
            log.debugf("SSF outbox enqueue deduplicated. clientId=%s jti=%s existingId=%s status=%s",
                    clientId, jti, existing.getId(), existing.getStatus());
            return existing.getId();
        }

        Instant now = Instant.now();
        SsfPendingEventEntity entity = new SsfPendingEventEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRealmId(realmId);
        entity.setClientId(clientId);
        entity.setStreamId(streamId);
        entity.setJti(jti);
        entity.setEventType(eventType);
        entity.setEncodedSet(encodedSet);
        entity.setDeliveryMethod(SsfPendingEventEntity.DELIVERY_METHOD_PUSH);
        entity.setStatus(SsfPendingEventStatus.PENDING);
        entity.setAttempts(0);
        // Schedule the first attempt immediately — the drainer picks it
        // up on its next tick.
        entity.setNextAttemptAt(now);
        entity.setCreatedAt(now);

        em.persist(entity);
        log.debugf("SSF outbox enqueued. id=%s clientId=%s streamId=%s jti=%s eventType=%s",
                entity.getId(), clientId, streamId, jti, eventType);
        return entity.getId();
    }

    /**
     * Fetches up to {@code limit} pending push rows whose
     * {@code next_attempt_at} is due, locking them for update so
     * concurrent drainer runs on other cluster nodes don't pick the
     * same rows. Callers MUST transition each returned row to a
     * terminal status within the same transaction.
     */
    public List<SsfPendingEventEntity> lockDueForPush(int limit) {
        TypedQuery<SsfPendingEventEntity> q = getEntityManager()
                .createNamedQuery("SsfPendingEvent.findDueForPush", SsfPendingEventEntity.class)
                .setParameter("status", SsfPendingEventStatus.PENDING)
                .setParameter("deliveryMethod", SsfPendingEventEntity.DELIVERY_METHOD_PUSH)
                .setParameter("now", Instant.now())
                .setMaxResults(limit);
        // UPGRADE_SKIPLOCKED is Hibernate's native lock mode for
        // "SELECT ... FOR UPDATE SKIP LOCKED": pessimistic-write
        // semantics plus skip-locked behaviour in one call, set via the
        // non-deprecated Hibernate 7 SelectionQuery API. Two drainers
        // running concurrently walk disjoint row sets instead of
        // blocking on each other's locks. On dialects without SKIP
        // LOCKED (e.g. H2) Hibernate falls back to plain FOR UPDATE,
        // which remains correct thanks to the
        // ClusterAwareScheduledTaskRunner wrapper that serializes ticks
        // cluster-wide.
        //
        // Binds to org.hibernate APIs intentionally — Keycloak only
        // ships with Hibernate as its JPA provider, and JPA's
        // LockModeType has no SKIP LOCKED equivalent.
        q.unwrap(SelectionQuery.class).setHibernateLockMode(LockMode.UPGRADE_SKIPLOCKED);
        return q.getResultList();
    }

    public void markDelivered(SsfPendingEventEntity entity) {
        entity.setStatus(SsfPendingEventStatus.DELIVERED);
        entity.setDeliveredAt(Instant.now());
        entity.setLastError(null);
        getEntityManager().merge(entity);
    }

    public void recordFailure(SsfPendingEventEntity entity, Instant nextAttemptAt, String lastError) {
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setNextAttemptAt(nextAttemptAt);
        entity.setLastError(truncateError(lastError));
        getEntityManager().merge(entity);
    }

    public void markDeadLetter(SsfPendingEventEntity entity, String lastError) {
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setStatus(SsfPendingEventStatus.DEAD_LETTER);
        entity.setLastError(truncateError(lastError));
        getEntityManager().merge(entity);
    }

    /**
     * Admin-initiated retry: resets a dead-letter row back to PENDING
     * with a fresh attempt counter and an immediate next-attempt-at
     * so the next drainer tick picks it up.
     */
    public void requeueFromDeadLetter(SsfPendingEventEntity entity) {
        entity.setStatus(SsfPendingEventStatus.PENDING);
        entity.setAttempts(0);
        entity.setNextAttemptAt(Instant.now());
        entity.setLastError(null);
        getEntityManager().merge(entity);
    }

    public long countByClientAndStatus(String clientId, SsfPendingEventStatus status) {
        return getEntityManager()
                .createNamedQuery("SsfPendingEvent.countByClientAndStatus", Long.class)
                .setParameter("clientId", clientId)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * Housekeeping: deletes {@link SsfPendingEventStatus#DELIVERED DELIVERED}
     * rows whose {@code delivered_at} is older than the retention cut-off.
     * Run from the drainer at a low cadence — the table should otherwise
     * grow unbounded since delivered rows stay around purely for jti dedup.
     *
     * @return the number of rows purged.
     */
    /**
     * Realm-scoped cascade: deletes every outbox row belonging to the
     * given realm regardless of status. Invoked from the
     * {@link org.keycloak.models.RealmModel.RealmRemovedEvent
     * RealmRemovedEvent} handler — REALM_ID is a plain column here
     * (not an FK), so there is no DB-level cascade and we purge
     * explicitly before the realm is gone.
     *
     * @return the number of rows deleted.
     */
    public int deleteByRealm(String realmId) {
        Objects.requireNonNull(realmId, "realmId");
        int deleted = getEntityManager()
                .createNamedQuery("SsfPendingEvent.deleteByRealm")
                .setParameter("realmId", realmId)
                .executeUpdate();
        if (deleted > 0) {
            log.debugf("SSF outbox purged %d rows for removed realm %s", deleted, realmId);
        }
        return deleted;
    }

    public int purgeDeliveredOlderThan(Instant cutoff) {
        int purged = getEntityManager()
                .createNamedQuery("SsfPendingEvent.deletePurgedDelivered")
                .setParameter("status", SsfPendingEventStatus.DELIVERED)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
        if (purged > 0) {
            log.debugf("SSF outbox purged %d delivered rows older than %s", purged, cutoff);
        }
        return purged;
    }

    private SsfPendingEventEntity findByClientAndJti(EntityManager em, String clientId, String jti) {
        List<SsfPendingEventEntity> hits = em
                .createQuery("SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId AND e.jti = :jti",
                        SsfPendingEventEntity.class)
                .setParameter("clientId", clientId)
                .setParameter("jti", jti)
                .setMaxResults(1)
                .getResultList();
        return hits.isEmpty() ? null : hits.get(0);
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        // Match LAST_ERROR VARCHAR(2048) column length.
        return error.length() <= 2048 ? error : error.substring(0, 2045) + "...";
    }
}
