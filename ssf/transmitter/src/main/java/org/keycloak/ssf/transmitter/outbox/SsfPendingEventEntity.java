package org.keycloak.ssf.transmitter.outbox;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Persistent row representing a single SSF Security Event Token (SET)
 * whose delivery to a receiver is outstanding.
 *
 * <p>For push streams the drainer picks rows in
 * {@link SsfPendingEventStatus#PENDING PENDING} whose
 * {@link #nextAttemptAt} is due, POSTs the SET to the receiver's push
 * endpoint, and transitions the row based on the response. For poll
 * streams (future) the poll endpoint reads pending rows scoped to the
 * requesting receiver and transitions them on ack. The
 * {@link #deliveryMethod} column is what distinguishes the two paths.
 *
 * <p>The table carries {@link #encodedSet} (the signed JWT) verbatim so
 * the drainer doesn't need to re-sign on every retry — keys may rotate
 * between the original enqueue and a later retry, and the receiver
 * expects a stable signature over the same jti.
 */
@Entity
@Table(name = "SSF_PENDING_EVENT",
        uniqueConstraints = {
                // Dedup: the same SET is never enqueued twice for the same
                // receiver client. The application-level dedup is the
                // explicit findByClientAndJti lookup in
                // SsfPendingEventStore.enqueuePending*; this DB-level
                // unique is the safety net.
                //
                // CREATED_AT is part of the constraint solely so PostgreSQL /
                // MySQL declarative partitioning by created_at is possible —
                // those engines require the partition key to be a member of
                // every UNIQUE constraint on the table. The application code
                // never enqueues two rows with the same (client_id, jti) at
                // different timestamps, so the operational uniqueness scope
                // is effectively still (client_id, jti). See
                // ssf-changelog-1.0.1.xml.
                @UniqueConstraint(name = "UC_SSF_PENDING_CLIENT_JTI",
                        columnNames = {"CLIENT_ID", "JTI", "CREATED_AT"})
        },
        indexes = {
                // Drainer query: WHERE status = PENDING
                //                  AND delivery_method = PUSH
                //                  AND next_attempt_at <= now()
                @Index(name = "IDX_SSF_PENDING_DRAIN",
                        columnList = "STATUS,DELIVERY_METHOD,NEXT_ATTEMPT_AT"),
                // Admin UI count + poll-endpoint query: per-client scope.
                @Index(name = "IDX_SSF_PENDING_CLIENT",
                        columnList = "CLIENT_ID,STATUS"),
                // Realm-scoped reads and realm-removed cascade deletes
                // (REALM_ID is a plain column here, not an FK, so a
                // RealmRemovedEvent listener runs the purge explicitly).
                @Index(name = "IDX_SSF_PENDING_REALM",
                        columnList = "REALM_ID,STATUS")
        })
@NamedQueries({
        // ORDER BY (nextAttemptAt, createdAt, jti): the createdAt
        // tiebreaker matters when many rows are due at the same
        // instant — most importantly after releaseHeldForClient flips
        // every HELD row's nextAttemptAt to a single Instant.now() on
        // resume-from-pause, but also for same-millisecond enqueues.
        // The SSF spec requires that successive events affecting the
        // same Subject Principal be transmitted in time-of-generation
        // order; sorting by createdAt within a single drainer batch
        // satisfies this on a single node. The jti tiebreak yields a
        // total order for repeatable queries. Cross-node strict order
        // is NOT guaranteed because the SKIP LOCKED claim splits
        // batches across drainers.
        @NamedQuery(
                name = "SsfPendingEvent.findDueForPush",
                query = "SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.status = :status"
                        + "   AND e.deliveryMethod = :deliveryMethod"
                        + "   AND e.nextAttemptAt <= :now"
                        + " ORDER BY e.nextAttemptAt ASC, e.createdAt ASC, e.jti ASC"),
        @NamedQuery(
                name = "SsfPendingEvent.findByClientAndJti",
                query = "SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId AND e.jti = :jti"),
        @NamedQuery(
                name = "SsfPendingEvent.countByClientAndStatus",
                query = "SELECT COUNT(e) FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId AND e.status = :status"),
        // Poll endpoint read query: returns the next batch of PENDING POLL
        // rows for the requesting receiver, FIFO by createdAt with a jti
        // tiebreak so concurrent enqueues that hit the same millisecond
        // produce a stable order.
        @NamedQuery(
                name = "SsfPendingEvent.findPendingForReceiverPoll",
                query = "SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.deliveryMethod = :deliveryMethod"
                        + "   AND e.status = :status"
                        + " ORDER BY e.createdAt ASC, e.jti ASC"),
        // Per-(client, jti) ack lookup. Scoped on clientId AND jti so a
        // receiver can never ack another receiver's row even if it
        // managed to guess the jti. Used by both the ack and NACK
        // (setErrs) paths — the only difference between them is the
        // terminal status the matched rows are transitioned to.
        @NamedQuery(
                name = "SsfPendingEvent.findPendingForReceiverAck",
                query = "SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.jti IN :jtis"
                        + "   AND e.deliveryMethod = :deliveryMethod"
                        + "   AND e.status = :status"),
        @NamedQuery(
                name = "SsfPendingEvent.deletePurgedDelivered",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.status = :status AND e.deliveredAt < :olderThan"),
        // Dead-letter rows don't have a deliveredAt timestamp, so we anchor
        // retention on createdAt. This over-retains by at most the drainer's
        // backoff window (seconds to minutes with defaults) relative to the
        // point at which the row actually reached DEAD_LETTER — cheap vs
        // adding a terminalAt column and a schema migration.
        @NamedQuery(
                name = "SsfPendingEvent.deletePurgedDeadLetter",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.status = :status AND e.createdAt < :olderThan"),
        @NamedQuery(
                name = "SsfPendingEvent.deleteByRealm",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.realmId = :realmId"),
        // Stream-delete cascade for POLL rows: PUSH rows would eventually
        // reach DEAD_LETTER on their own, but POLL rows can sit forever
        // with no consumer once the stream is gone, so the stream/client-
        // delete path explicitly purges them.
        @NamedQuery(
                name = "SsfPendingEvent.deleteByClient",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId"),
        // Resume-from-pause hook: bulk-flips every HELD row for the
        // receiver back to PENDING and resets next_attempt_at so the
        // PUSH drainer picks them up on its next tick. POLL rows ignore
        // next_attempt_at but the column is NOT NULL — set it anyway.
        @NamedQuery(
                name = "SsfPendingEvent.releaseHeldForClient",
                query = "UPDATE SsfPendingEventEntity e"
                        + " SET e.status = :pending,"
                        + "     e.nextAttemptAt = :now"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.status = :held"),
        // Enter-pause hook: bulk-flips every PENDING row for the
        // receiver to HELD when the stream transitions enabled →
        // paused. Symmetric to releaseHeldForClient so events already
        // queued before the pause stop draining / serving in line with
        // the new status, instead of leaking through the in-flight
        // backlog. NOT used on enabled → disabled — see
        // deleteUndeliveredForClient (the SSF spec says a disabled
        // stream "will not hold any events for later transmission").
        @NamedQuery(
                name = "SsfPendingEvent.holdPendingForClient",
                query = "UPDATE SsfPendingEventEntity e"
                        + " SET e.status = :held"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.status = :pending"),
        // Enter-disabled hook: drops every undelivered row for the
        // receiver — both PENDING and HELD. The SSF spec says a
        // disabled stream MUST NOT transmit events AND MUST NOT hold
        // any for later transmission, so we cannot leak the backlog
        // into a future re-enable. DELIVERED rows are kept for jti
        // dedup; DEAD_LETTER rows are kept for post-failure audit.
        @NamedQuery(
                name = "SsfPendingEvent.deleteUndeliveredForClient",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.status IN :statuses"),
        // Stream-replace / PATCH with a delivery-method change hook:
        // migrates already-queued non-terminal rows so the new
        // channel picks them up instead of orphaning them. The
        // encoded SET itself is channel-agnostic (a JWS string),
        // so changing the routing column is sufficient. Applies
        // only to PENDING + HELD (DELIVERED + DEAD_LETTER are
        // terminal and stay in their bucket for audit / dedup).
        @NamedQuery(
                name = "SsfPendingEvent.migrateDeliveryMethodForClient",
                query = "UPDATE SsfPendingEventEntity e"
                        + " SET e.deliveryMethod = :newMethod"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.deliveryMethod <> :newMethod"
                        + "   AND e.status IN :statuses"),
        // Per-receiver TTL housekeeping pre-scan: enumerates the
        // (realmId, clientId) pairs that own at least one
        // non-DELIVERED row, so the drainer's per-client purge loop
        // visits only receivers that actually have stale candidates.
        @NamedQuery(
                name = "SsfPendingEvent.findRealmClientPairsForPurgeScan",
                query = "SELECT DISTINCT e.realmId, e.clientId FROM SsfPendingEventEntity e"
                        + " WHERE e.status <> :delivered"),
        // Grouped outbox-depth aggregate for the Prometheus
        // metrics binder. Runs at the end of each drainer tick
        // and populates the cached depth snapshot that the
        // outbox-depth gauges read from — scrapes pay no DB cost.
        // Covered by the existing IDX_SSF_PENDING_REALM (REALM_ID,
        // STATUS) index so the aggregate stays cheap even for
        // large tables. Plain COUNT takes no row locks, so the
        // drainer's FOR UPDATE SKIP LOCKED claims don't interact.
        @NamedQuery(
                name = "SsfPendingEvent.countByRealmAndStatus",
                query = "SELECT e.realmId, e.status, COUNT(e) FROM SsfPendingEventEntity e"
                        + " GROUP BY e.realmId, e.status"),
        // Per-receiver TTL purge: drops every non-DELIVERED row for the
        // client whose createdAt predates the receiver-specific cutoff
        // derived from ssf.maxEventAgeSeconds.
        @NamedQuery(
                name = "SsfPendingEvent.purgeStaleForClient",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId"
                        + "   AND e.status <> :delivered"
                        + "   AND e.createdAt < :cutoff")
})
public class SsfPendingEventEntity {

    public static final String DELIVERY_METHOD_PUSH = "PUSH";

    public static final String DELIVERY_METHOD_POLL = "POLL";

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "REALM_ID", nullable = false, length = 36)
    private String realmId;

    @Column(name = "CLIENT_ID", nullable = false, length = 36)
    private String clientId;

    @Column(name = "STREAM_ID", nullable = false, length = 64)
    private String streamId;

    @Column(name = "JTI", nullable = false, length = 64)
    private String jti;

    @Column(name = "EVENT_TYPE", nullable = false, length = 256)
    private String eventType;

    /**
     * The signed SET JWT. CLOB because the signed payload can exceed a
     * standard VARCHAR column (stream config plus event payload plus
     * subject claim plus signature + headers easily breaches 2-4KB).
     */
    @Lob
    @Column(name = "ENCODED_SET", nullable = false)
    private String encodedSet;

    @Column(name = "DELIVERY_METHOD", nullable = false, length = 16)
    private String deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 16)
    private SsfPendingEventStatus status;

    @Column(name = "ATTEMPTS", nullable = false)
    private int attempts;

    @Column(name = "NEXT_ATTEMPT_AT", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "LAST_ERROR", length = 2048)
    private String lastError;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    @Column(name = "DELIVERED_AT")
    private Instant deliveredAt;

    // --- getters / setters ---------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEncodedSet() { return encodedSet; }
    public void setEncodedSet(String encodedSet) { this.encodedSet = encodedSet; }

    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }

    public SsfPendingEventStatus getStatus() { return status; }
    public void setStatus(SsfPendingEventStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
