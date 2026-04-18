package org.keycloak.ssf.transmitter.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import org.hibernate.LockMode;
import org.hibernate.query.SelectionQuery;
import org.jboss.logging.Logger;

/**
 * Thin JPA repository over {@link SsfPendingEventEntity}. Does not
 * manage transactions — callers run inside the ambient
 * {@link KeycloakSession} transaction, which Keycloak wraps around
 * request handlers and scheduled-task executions.
 */
public class SsfPendingEventStore {

    private static final Logger log = Logger.getLogger(SsfPendingEventStore.class);

    protected final KeycloakSession session;

    public SsfPendingEventStore(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Enqueues a SET for asynchronous push delivery. Idempotent on
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
        return enqueuePending(realmId, clientId, streamId, jti, eventType, encodedSet,
                SsfPendingEventEntity.DELIVERY_METHOD_PUSH, SsfPendingEventStatus.PENDING);
    }

    /**
     * Enqueues a SET for asynchronous push delivery in the
     * {@link SsfPendingEventStatus#HELD HELD} state — used when the
     * owning stream is in the SSF {@code paused} status. The drainer
     * skips HELD rows; {@link #releaseHeldForClient(String)} flips them
     * to {@link SsfPendingEventStatus#PENDING PENDING} when the stream
     * is resumed.
     */
    public String enqueueHeldPush(String realmId,
                                  String clientId,
                                  String streamId,
                                  String jti,
                                  String eventType,
                                  String encodedSet) {
        return enqueuePending(realmId, clientId, streamId, jti, eventType, encodedSet,
                SsfPendingEventEntity.DELIVERY_METHOD_PUSH, SsfPendingEventStatus.HELD);
    }

    /**
     * Enqueues a SET for poll-based delivery. Same dedup semantics as
     * {@link #enqueuePendingPush} — one row per {@code (clientId, jti)}.
     * The row sits in {@code PENDING} until the receiver acks it via
     * the poll endpoint or the stream is deleted.
     *
     * @return the enqueued row id, or the pre-existing row id if the
     *         insert was deduped.
     */
    public String enqueuePendingPoll(String realmId,
                                     String clientId,
                                     String streamId,
                                     String jti,
                                     String eventType,
                                     String encodedSet) {
        return enqueuePending(realmId, clientId, streamId, jti, eventType, encodedSet,
                SsfPendingEventEntity.DELIVERY_METHOD_POLL, SsfPendingEventStatus.PENDING);
    }

    /**
     * Enqueues a SET for poll delivery in the
     * {@link SsfPendingEventStatus#HELD HELD} state — see
     * {@link #enqueueHeldPush}. The poll endpoint skips HELD rows.
     */
    public String enqueueHeldPoll(String realmId,
                                  String clientId,
                                  String streamId,
                                  String jti,
                                  String eventType,
                                  String encodedSet) {
        return enqueuePending(realmId, clientId, streamId, jti, eventType, encodedSet,
                SsfPendingEventEntity.DELIVERY_METHOD_POLL, SsfPendingEventStatus.HELD);
    }

    /**
     * Shared insert path used by both delivery methods. Differences
     * between PUSH and POLL boil down to the {@code deliveryMethod}
     * column — the drainer query filters on it for PUSH, the poll
     * read query filters on it for POLL.
     */
    protected String enqueuePending(String realmId,
                                    String clientId,
                                    String streamId,
                                    String jti,
                                    String eventType,
                                    String encodedSet,
                                    String deliveryMethod,
                                    SsfPendingEventStatus initialStatus) {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(jti, "jti");
        Objects.requireNonNull(encodedSet, "encodedSet");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
        Objects.requireNonNull(initialStatus, "initialStatus");

        EntityManager em = getEntityManager();

        // Dedup scan — relies on the (CLIENT_ID, JTI) unique index but
        // checked in code first so we don't have to catch a constraint
        // violation from the JPA provider's batched flush for the
        // common no-op path.
        SsfPendingEventEntity existing = findByClientAndJti(em, clientId, jti);
        if (existing != null) {
            log.debugf("SSF outbox enqueue deduplicated. realmId=%s clientId=%s jti=%s existingId=%s status=%s deliveryMethod=%s",
                    existing.getRealmId(), clientId, jti, existing.getId(), existing.getStatus(), existing.getDeliveryMethod());
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
        entity.setDeliveryMethod(deliveryMethod);
        entity.setStatus(initialStatus);
        entity.setAttempts(0);
        // PUSH path: the drainer picks it up on its next tick.
        // POLL path: nextAttemptAt is unused but the column is NOT NULL,
        //            so we still set it to now() — keeps the schema
        //            constraint satisfied without a nullable migration.
        entity.setNextAttemptAt(now);
        entity.setCreatedAt(now);

        em.persist(entity);
        log.debugf("SSF outbox enqueued. id=%s realmId=%s clientId=%s streamId=%s jti=%s eventType=%s deliveryMethod=%s",
                entity.getId(), realmId, clientId, streamId, jti, eventType, deliveryMethod);
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

    /**
     * Fetches the next batch of {@code PENDING} POLL rows for the given
     * receiver client, FIFO by {@code createdAt}. Locked with
     * {@code UPGRADE_SKIPLOCKED} so concurrent poll requests for the
     * same receiver (e.g. multiple receiver pods polling with the same
     * credentials) walk disjoint row sets within the same DB
     * transaction instead of returning duplicates. Caller MUST issue
     * the ack within the same transaction or release the locks by
     * letting the transaction commit/rollback.
     */
    public List<SsfPendingEventEntity> lockPendingForReceiverPoll(String clientId, int limit) {
        Objects.requireNonNull(clientId, "clientId");
        TypedQuery<SsfPendingEventEntity> q = getEntityManager()
                .createNamedQuery("SsfPendingEvent.findPendingForReceiverPoll", SsfPendingEventEntity.class)
                .setParameter("clientId", clientId)
                .setParameter("deliveryMethod", SsfPendingEventEntity.DELIVERY_METHOD_POLL)
                .setParameter("status", SsfPendingEventStatus.PENDING)
                .setMaxResults(limit);
        // Same Hibernate hook the PUSH drainer uses — see lockDueForPush
        // for the SKIP LOCKED rationale.
        q.unwrap(SelectionQuery.class).setHibernateLockMode(LockMode.UPGRADE_SKIPLOCKED);
        return q.getResultList();
    }

    /**
     * Counts {@code (clientId, deliveryMethod, status)} triples. Used by
     * the poll endpoint to decide whether to set {@code moreAvailable=true}
     * after returning a batch shorter than the read query's limit.
     */
    public long countByClientStatusAndMethod(String clientId, SsfPendingEventStatus status, String deliveryMethod) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
        // Inline JPQL — countByClientAndStatus already exists for
        // status-only counts; this variant filters on deliveryMethod
        // too so a mixed PUSH+POLL receiver (theoretical) doesn't get
        // a misleading moreAvailable flag.
        return getEntityManager().createQuery(
                        "SELECT COUNT(e) FROM SsfPendingEventEntity e"
                                + " WHERE e.clientId = :clientId"
                                + "   AND e.status = :status"
                                + "   AND e.deliveryMethod = :deliveryMethod",
                        Long.class)
                .setParameter("clientId", clientId)
                .setParameter("status", status)
                .setParameter("deliveryMethod", deliveryMethod)
                .getSingleResult();
    }

    /**
     * Acks the given JTIs for the receiver client by transitioning
     * matching {@code PENDING} POLL rows to {@code DELIVERED}.
     * Idempotent and silently scoped: jtis the receiver doesn't own
     * (different client) and jtis already in {@code DELIVERED} simply
     * don't appear in the lookup result, so no error and no leakage of
     * row existence to the caller.
     *
     * @return the set of jtis that were transitioned to DELIVERED.
     */
    public Set<String> ackPendingForReceiver(String clientId, Collection<String> jtis) {
        Objects.requireNonNull(clientId, "clientId");
        if (jtis == null || jtis.isEmpty()) {
            return Set.of();
        }
        List<SsfPendingEventEntity> rows = getEntityManager()
                .createNamedQuery("SsfPendingEvent.findPendingForReceiverAck", SsfPendingEventEntity.class)
                .setParameter("clientId", clientId)
                .setParameter("jtis", jtis)
                .setParameter("deliveryMethod", SsfPendingEventEntity.DELIVERY_METHOD_POLL)
                .setParameter("status", SsfPendingEventStatus.PENDING)
                .getResultList();

        if (rows.isEmpty()) {
            return Set.of();
        }
        Set<String> acked = new LinkedHashSet<>(rows.size());
        for (SsfPendingEventEntity row : rows) {
            markDelivered(row);
            acked.add(row.getJti());
        }
        log.debugf("SSF outbox poll ack. clientId=%s ackedCount=%d ackedJtis=%s",
                clientId, acked.size(), acked);
        return acked;
    }

    /**
     * Receiver-driven NACK for poll-delivered rows. Each entry of
     * {@code errorByJti} matches a {@code PENDING} POLL row owned by
     * the calling client; matched rows transition to
     * {@link SsfPendingEventStatus#DEAD_LETTER DEAD_LETTER} with the
     * receiver-supplied error message recorded in {@code last_error}.
     *
     * <p>For POLL rows, DEAD_LETTER is reached only via this explicit
     * NACK path — there is no transmitter-side retry-exhaustion
     * counter to bump. Idempotent and silently scoped: jtis the caller
     * doesn't own (different client) and jtis already terminal don't
     * appear in the lookup result.
     *
     * @return the set of jtis that were transitioned to DEAD_LETTER.
     */
    public Set<String> nackPendingForReceiver(String clientId,
                                              Map<String, String> errorByJti) {
        Objects.requireNonNull(clientId, "clientId");
        if (errorByJti == null || errorByJti.isEmpty()) {
            return Set.of();
        }
        List<SsfPendingEventEntity> rows = getEntityManager()
                .createNamedQuery("SsfPendingEvent.findPendingForReceiverAck", SsfPendingEventEntity.class)
                .setParameter("clientId", clientId)
                .setParameter("jtis", errorByJti.keySet())
                .setParameter("deliveryMethod", SsfPendingEventEntity.DELIVERY_METHOD_POLL)
                .setParameter("status", SsfPendingEventStatus.PENDING)
                .getResultList();

        if (rows.isEmpty()) {
            return Set.of();
        }
        Set<String> nacked = new LinkedHashSet<>(rows.size());
        for (SsfPendingEventEntity row : rows) {
            String error = errorByJti.get(row.getJti());
            markDeadLetter(row, error);
            nacked.add(row.getJti());
        }
        log.debugf("SSF outbox poll NACK. clientId=%s nackedCount=%d nackedJtis=%s",
                clientId, nacked.size(), nacked);
        return nacked;
    }

    /**
     * Cascade for stream/client delete. Removes every outbox row owned
     * by the given receiver client regardless of status. PUSH rows
     * eventually reach DEAD_LETTER on their own and could be left to
     * the dead-letter retention purge, but POLL rows have no consumer
     * once the stream is gone — explicit cascade keeps both paths
     * consistent.
     *
     * @return the number of rows deleted.
     */
    public int deleteByClient(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        int deleted = getEntityManager()
                .createNamedQuery("SsfPendingEvent.deleteByClient")
                .setParameter("clientId", clientId)
                .executeUpdate();
        if (deleted > 0) {
            log.debugf("SSF outbox purged %d rows for client %s", deleted, clientId);
        }
        return deleted;
    }

    /**
     * Bulk-transitions all {@link SsfPendingEventStatus#HELD HELD} rows
     * for the given receiver client back to
     * {@link SsfPendingEventStatus#PENDING PENDING}. Invoked from
     * {@code StreamService.updateStreamStatus} when a stream is resumed
     * (status returns to {@code enabled}).
     *
     * <p>For PUSH rows, {@code next_attempt_at} is reset to {@code now}
     * so the drainer picks them up on its next tick. POLL rows ignore
     * the field but it is set anyway to keep the schema constraint
     * satisfied.
     *
     * @return the number of rows that transitioned out of HELD.
     */
    public int releaseHeldForClient(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        int released = getEntityManager()
                .createNamedQuery("SsfPendingEvent.releaseHeldForClient")
                .setParameter("pending", SsfPendingEventStatus.PENDING)
                .setParameter("held", SsfPendingEventStatus.HELD)
                .setParameter("clientId", clientId)
                .setParameter("now", Instant.now())
                .executeUpdate();
        if (released > 0) {
            log.debugf("SSF outbox released %d held rows for client %s", released, clientId);
        }
        return released;
    }

    /**
     * Bulk-transitions all {@link SsfPendingEventStatus#PENDING PENDING}
     * rows for the given receiver client to
     * {@link SsfPendingEventStatus#HELD HELD}. Invoked from
     * {@code StreamService.updateStreamStatus} when a stream leaves
     * {@code enabled} (transitions to {@code paused} or {@code disabled})
     * so events already queued before the transition stop being delivered
     * — symmetric to {@link #releaseHeldForClient(String)}.
     *
     * @return the number of rows that transitioned PENDING → HELD.
     */
    public int holdPendingForClient(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        int held = getEntityManager()
                .createNamedQuery("SsfPendingEvent.holdPendingForClient")
                .setParameter("held", SsfPendingEventStatus.HELD)
                .setParameter("pending", SsfPendingEventStatus.PENDING)
                .setParameter("clientId", clientId)
                .executeUpdate();
        if (held > 0) {
            log.debugf("SSF outbox held %d pending rows for client %s", held, clientId);
        }
        return held;
    }

    /**
     * Drops every undelivered ({@link SsfPendingEventStatus#PENDING PENDING}
     * or {@link SsfPendingEventStatus#HELD HELD}) row for the receiver.
     * Invoked from {@code StreamService.updateStreamStatus} when a stream
     * transitions to {@code disabled} — the SSF spec forbids both
     * transmission and holding for disabled streams, so the in-flight
     * backlog must be discarded rather than parked.
     *
     * <p>{@link SsfPendingEventStatus#DELIVERED DELIVERED} rows are kept
     * for {@code jti}-dedup coverage and
     * {@link SsfPendingEventStatus#DEAD_LETTER DEAD_LETTER} rows are kept
     * for post-failure audit; only the in-flight backlog is purged.
     *
     * @return the number of rows that were deleted.
     */
    public int deleteUndeliveredForClient(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        int deleted = getEntityManager()
                .createNamedQuery("SsfPendingEvent.deleteUndeliveredForClient")
                .setParameter("clientId", clientId)
                .setParameter("statuses",
                        java.util.List.of(SsfPendingEventStatus.PENDING, SsfPendingEventStatus.HELD))
                .executeUpdate();
        if (deleted > 0) {
            log.debugf("SSF outbox discarded %d undelivered rows for client %s", deleted, clientId);
        }
        return deleted;
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

    /**
     * Lists every distinct {@code (realmId, clientId)} pair that owns at
     * least one non-{@code DELIVERED} outbox row. Used by the drainer's
     * per-client TTL pass to bound the receiver-attribute lookup loop to
     * clients that actually have stale candidates.
     */
    public List<Object[]> findRealmClientPairsForPurgeScan() {
        return getEntityManager()
                .createNamedQuery("SsfPendingEvent.findRealmClientPairsForPurgeScan", Object[].class)
                .setParameter("delivered", SsfPendingEventStatus.DELIVERED)
                .getResultList();
    }

    /**
     * Per-receiver TTL purge: drops every non-{@code DELIVERED} outbox
     * row for the given client whose {@code createdAt} predates
     * {@code cutoff}. Backs the {@code ssf.maxEventAgeSeconds} client
     * attribute — runs in the drainer's housekeeping pass <em>before</em>
     * the global delivered/dead-letter retention windows so receivers
     * with short-lived events shed stale rows promptly regardless of
     * their terminal status (PENDING / HELD / DEAD_LETTER).
     */
    public int purgeStaleForClient(String clientId, Instant cutoff) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(cutoff, "cutoff");
        int purged = getEntityManager()
                .createNamedQuery("SsfPendingEvent.purgeStaleForClient")
                .setParameter("clientId", clientId)
                .setParameter("delivered", SsfPendingEventStatus.DELIVERED)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
        if (purged > 0) {
            log.debugf("SSF outbox purged %d stale rows for client %s (cutoff=%s)", purged, clientId, cutoff);
        }
        return purged;
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

    /**
     * Purges {@link SsfPendingEventStatus#DEAD_LETTER DEAD_LETTER} rows
     * older than {@code cutoff}, anchored on {@code createdAt} (see the
     * named query javadoc on {@link SsfPendingEventEntity} for the
     * rationale).
     */
    public int purgeDeadLetterOlderThan(Instant cutoff) {
        int purged = getEntityManager()
                .createNamedQuery("SsfPendingEvent.deletePurgedDeadLetter")
                .setParameter("status", SsfPendingEventStatus.DEAD_LETTER)
                .setParameter("olderThan", cutoff)
                .executeUpdate();
        if (purged > 0) {
            log.debugf("SSF outbox purged %d dead-letter rows older than %s", purged, cutoff);
        }
        return purged;
    }

    /**
     * Public lookup for an outbox row by {@code (clientId, jti)} —
     * used by the admin "Pending Events" lookup endpoint so an
     * operator can inspect the delivery state of a specific SET.
     * Scoped on {@code clientId} so callers cannot probe rows that
     * belong to a different receiver.
     */
    public SsfPendingEventEntity findByClientAndJti(String clientId, String jti) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(jti, "jti");
        return findByClientAndJti(getEntityManager(), clientId, jti);
    }

    protected SsfPendingEventEntity findByClientAndJti(EntityManager em, String clientId, String jti) {
        List<SsfPendingEventEntity> hits = em
                .createNamedQuery("SsfPendingEvent.findByClientAndJti", SsfPendingEventEntity.class)
                .setParameter("clientId", clientId)
                .setParameter("jti", jti)
                .setMaxResults(1)
                .getResultList();
        return hits.isEmpty() ? null : hits.get(0);
    }

    protected EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected String truncateError(String error) {
        if (error == null) {
            return null;
        }
        // Match LAST_ERROR VARCHAR(2048) column length.
        return error.length() <= 2048 ? error : error.substring(0, 2045) + "...";
    }
}
