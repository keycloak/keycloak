package org.keycloak.ssf.transmitter.delivery.poll;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;

import org.jboss.logging.Logger;

/**
 * Orchestrates one RFC 8936 polling request: ack first (so already-
 * processed rows don't appear in the next batch), then read the next
 * batch of {@code PENDING} POLL rows for the calling receiver.
 *
 * <p>Stateless aside from the {@link KeycloakSession} hand-off — a fresh
 * service instance is built per request inside the JAX-RS resource.
 */
public class PollDeliveryService {

    private static final Logger log = Logger.getLogger(PollDeliveryService.class);

    /** RFC 8936 §2.1 default when the receiver omits {@code maxEvents}. */
    public static final int DEFAULT_MAX_EVENTS = 100;

    /**
     * Hard upper bound on a receiver-supplied {@code maxEvents}. Protects
     * the transmitter from a request that asks for the entire backlog at
     * once. Configurable later if the default proves too tight; for now
     * 1000 is well above what real receivers send (typical batches are
     * 50-100).
     */
    public static final int MAX_EVENTS_CAP = 1000;

    /**
     * Hard upper bound on the number of jtis a receiver may include in
     * a single {@code ack} or {@code setErrs} batch. Same limit for both
     * so the request stays bounded in memory and the per-(client, jti)
     * IN-clause query stays under Oracle's hard 1000-element ceiling.
     * The poll endpoint rejects oversized batches with
     * {@code 400 invalid_request} — the receiver should split into
     * multiple polls.
     */
    public static final int MAX_BATCH_CAP = 1000;

    protected final KeycloakSession session;

    protected final OutboxStore outboxStore;

    protected final SsfMetricsBinder metricsBinder;

    public PollDeliveryService(KeycloakSession session, OutboxStore outboxStore, SsfMetricsBinder metricsBinder) {
        this.session = session;
        this.outboxStore = outboxStore;
        this.metricsBinder = metricsBinder;
    }

    /**
     * Runs the ack + read pair and returns the response body to send
     * back to the receiver. Caller is responsible for validating that
     * {@code receiverClient} owns the stream identified in the URL —
     * this service operates on a pre-authorized client.
     */
    public PollResponse poll(ClientModel receiverClient, PollRequest request) {

        int maxEvents = clampMaxEvents(request.getMaxEvents());
        String realmName = currentRealmName();
        String labelClientId = receiverClient.getClientId();

        // 1. Ack first. The receiver's natural pattern is "ack what I
        //    got last time, fetch the next batch", so processing the ack
        //    before the read prevents the same rows from coming back
        //    inside one request.
        List<String> ack = request.getAck();
        if (ack != null && !ack.isEmpty()) {
            outboxStore.ackPendingForOwner(SsfOutboxKinds.POLL, receiverClient.getId(), ack);
            metricsBinder.recordPollAck(realmName, labelClientId, ack.size());
        }

        // 2. Then NACK (setErrs). Receiver-reported errors transition
        //    matching PENDING POLL rows to DEAD_LETTER with the
        //    receiver's error message in last_error. Done after ack so
        //    the natural "ack the ones I processed, NACK the ones I
        //    couldn't" sequence works.
        Map<String, Map<String, Object>> setErrs = request.getSetErrs();
        Map<String, String> errorByJti = toErrorMessages(setErrs);
        if (!errorByJti.isEmpty()) {
            outboxStore.nackPendingForOwner(SsfOutboxKinds.POLL, receiverClient.getId(), errorByJti);
            metricsBinder.recordPollNack(realmName, labelClientId, errorByJti.size());
        }

        // 3. Read the next batch — UPGRADE_SKIPLOCKED so concurrent
        //    pollers (e.g. multiple receiver pods on the same OAuth
        //    credentials) walk disjoint rows.
        List<OutboxEntryEntity> rows = outboxStore.lockPendingForOwner(SsfOutboxKinds.POLL,
                receiverClient.getId(), maxEvents);
        metricsBinder.recordPollServed(realmName, labelClientId, rows.size());

        Map<String, String> sets = new LinkedHashMap<>(rows.size());
        for (OutboxEntryEntity row : rows) {
            sets.put(row.getCorrelationId(), row.getPayload());
        }

        // 4. moreAvailable: if we filled the batch we have to assume
        //    there's more (count(*) is a wasted query for a probably-
        //    yes answer); if we didn't fill the batch there can't be
        //    more available than what we just locked.
        boolean moreAvailable = false;
        if (rows.size() == maxEvents) {
            long pending = outboxStore.countForOwnerByStatus(SsfOutboxKinds.POLL,
                    receiverClient.getId(), OutboxEntryStatus.PENDING);
            moreAvailable = pending > rows.size();
        }

        if (log.isDebugEnabled()) {
            log.debugf("SSF poll. clientId=%s ackCount=%d nackCount=%d returnedCount=%d moreAvailable=%s",
                    receiverClient.getClientId(),
                    ack == null ? 0 : ack.size(),
                    errorByJti.size(),
                    sets.size(),
                    moreAvailable);
        }

        PollResponse response = new PollResponse();
        response.setSets(sets);
        response.setMoreAvailable(moreAvailable);
        return response;
    }

    /**
     * Flattens the wire-shape {@code setErrs} object (per-jti error
     * descriptor with {@code err} + {@code description} fields per
     * RFC 8936 §2.1) into a per-jti error message string for storage
     * in the outbox row's {@code last_error} column. Receivers that
     * supply a partial descriptor (only {@code err}, only
     * {@code description}, or neither) get a best-effort string —
     * we never reject the request just because the descriptor is
     * malformed.
     */
    protected Map<String, String> toErrorMessages(Map<String, Map<String, Object>> setErrs) {
        if (setErrs == null || setErrs.isEmpty()) {
            return Map.of();
        }
        Map<String, String> messages = new LinkedHashMap<>(setErrs.size());
        for (Map.Entry<String, Map<String, Object>> entry : setErrs.entrySet()) {
            messages.put(entry.getKey(), formatNackMessage(entry.getValue()));
        }
        return messages;
    }

    protected String formatNackMessage(Map<String, Object> descriptor) {
        if (descriptor == null) {
            return "<receiver NACK with no descriptor>";
        }
        Object err = descriptor.get("err");
        Object description = descriptor.get("description");
        StringBuilder sb = new StringBuilder("Receiver NACK");
        if (err != null) {
            sb.append(" err=").append(err);
        }
        if (description != null) {
            sb.append(" description=").append(description);
        }
        return sb.toString();
    }

    /**
     * Safe accessor for the current realm <em>name</em> — used as the
     * {@code realm} metric label so dashboards show {@code realm="ssf-poc"}
     * rather than the opaque realm UUID.
     */
    protected String currentRealmName() {
        try {
            return session.getContext().getRealm().getName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected int clampMaxEvents(Integer requested) {
        if (requested == null) {
            return DEFAULT_MAX_EVENTS;
        }
        if (requested < 1) {
            return 1;
        }
        if (requested > MAX_EVENTS_CAP) {
            return MAX_EVENTS_CAP;
        }
        return requested;
    }
}
