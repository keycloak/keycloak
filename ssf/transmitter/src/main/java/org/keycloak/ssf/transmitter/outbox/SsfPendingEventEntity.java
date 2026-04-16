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
                // receiver client. Keep the uniqueness scope to (client, jti)
                // rather than just (jti) — jti is globally unique in practice
                // but the DB-level dedup only needs to match the application
                // semantics, and this leaves room for fan-out schemes where
                // one jti could legitimately appear once per receiver.
                @UniqueConstraint(name = "UC_SSF_PENDING_CLIENT_JTI",
                        columnNames = {"CLIENT_ID", "JTI"})
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
        @NamedQuery(
                name = "SsfPendingEvent.findDueForPush",
                query = "SELECT e FROM SsfPendingEventEntity e"
                        + " WHERE e.status = :status"
                        + "   AND e.deliveryMethod = :deliveryMethod"
                        + "   AND e.nextAttemptAt <= :now"
                        + " ORDER BY e.nextAttemptAt ASC"),
        @NamedQuery(
                name = "SsfPendingEvent.countByClientAndStatus",
                query = "SELECT COUNT(e) FROM SsfPendingEventEntity e"
                        + " WHERE e.clientId = :clientId AND e.status = :status"),
        @NamedQuery(
                name = "SsfPendingEvent.deletePurgedDelivered",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.status = :status AND e.deliveredAt < :olderThan"),
        @NamedQuery(
                name = "SsfPendingEvent.deleteByRealm",
                query = "DELETE FROM SsfPendingEventEntity e"
                        + " WHERE e.realmId = :realmId")
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
