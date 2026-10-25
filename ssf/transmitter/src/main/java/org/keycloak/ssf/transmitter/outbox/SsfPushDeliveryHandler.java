package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.events.outbox.OutboxDelivery;
import org.keycloak.events.outbox.OutboxDeliveryHandler;
import org.keycloak.events.outbox.OutboxDeliveryResult;
import org.keycloak.events.outbox.OutboxEntry;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterContext;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryOutcome;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;

/**
 * SSF push handler for the generic outbox. The drainer invokes
 * {@link #prepare(KeycloakSession, OutboxEntry)} for each claimed row
 * inside a short transaction; this implementation resolves the
 * realm/client/stream the row targets there and returns a delivery
 * that hands the encoded SET to {@link PushDeliveryService} outside
 * any transaction. {@link PushDeliveryService} captures the HTTP
 * client at construction and needs no session during the call.
 *
 * <p>Resolve-then-deliver-then-classify behavior:
 *
 * <ul>
 *   <li>Realm / client / stream gone → {@link OutboxDeliveryOutcome#ORPHANED}.</li>
 *   <li>Push succeeds → {@link OutboxDeliveryOutcome#DELIVERED}.</li>
 *   <li>Push fails → {@link OutboxDeliveryOutcome#RETRY}; the drainer
 *       decides RETRY-vs-DEAD_LETTER based on attempts.</li>
 * </ul>
 *
 * <p>Emits the {@code keycloak.ssf.push.delivery} meter on every
 * attempt — DELIVERED / RETRY / ORPHANED. The DEAD_LETTER outcome is
 * a drainer decision (attempt budget exhausted) and is counted by
 * {@link SsfOutboxMetricsListener} from the drainer's transition
 * callback, together with the tick and outbox-depth meters.
 */
public class SsfPushDeliveryHandler implements OutboxDeliveryHandler {

    private static final Logger log = Logger.getLogger(SsfPushDeliveryHandler.class);

    protected final SsfTransmitterContext context;
    protected final BiFunction<KeycloakSession, SsfTransmitterContext, PushDeliveryService> pushDeliveryServiceFactory;
    protected final SsfMetricsBinder metricsBinder;

    public SsfPushDeliveryHandler(SsfTransmitterContext context,
                                  BiFunction<KeycloakSession, SsfTransmitterContext, PushDeliveryService> pushDeliveryServiceFactory,
                                  SsfMetricsBinder metricsBinder) {
        this.context = Objects.requireNonNull(context, "context");
        this.pushDeliveryServiceFactory = Objects.requireNonNull(pushDeliveryServiceFactory, "pushDeliveryServiceFactory");
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
    }

    @Override
    public String entryKind() {
        return SsfOutboxKinds.PUSH;
    }

    @Override
    public OutboxDelivery prepare(KeycloakSession session, OutboxEntry row) {
        Instant rowStart = Instant.now();

        RealmModel realm = session.realms().getRealm(row.getRealmId());
        if (realm == null) {
            log.warnf("SSF push handler: row references unknown realm — orphaning. id=%s realmId=%s correlationId=%s",
                    row.getId(), row.getRealmId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(row.getRealmId(), row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDelivery.settled(OutboxDeliveryResult.orphaned("unknown realm: " + row.getRealmId()));
        }
        String realmLabel = realm.getName();

        ClientModel receiverClient = realm.getClientById(row.getOwnerId());
        if (receiverClient == null) {
            log.warnf("SSF push handler: row references unknown client — orphaning. id=%s ownerId=%s correlationId=%s",
                    row.getId(), row.getOwnerId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(realmLabel, row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDelivery.settled(OutboxDeliveryResult.orphaned("unknown client: " + row.getOwnerId()));
        }
        String clientLabel = receiverClient.getClientId();

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            // Feature unavailable mid-flight — not a failed delivery
            // attempt, so defer to the next tick without spending one
            // of the row's attempts.
            log.warnf("SSF push handler: transmitter provider unavailable — deferring row %s to the next tick", row.getId());
            return OutboxDelivery.settled(OutboxDeliveryResult.defer(Instant.now(), "transmitter provider unavailable"));
        }

        String expectedStreamId = row.getContainerId();
        StreamConfig stream = transmitter.streamStore().getStreamForClient(receiverClient);
        if (stream == null
                || (expectedStreamId != null && !expectedStreamId.equals(stream.getStreamId()))) {
            log.warnf("SSF push handler: row's stream is gone — orphaning. id=%s ownerId=%s pendingStreamId=%s currentStreamId=%s",
                    row.getId(), row.getOwnerId(), expectedStreamId,
                    stream == null ? "<none>" : stream.getStreamId());
            metricsBinder.recordPushDelivery(realmLabel, clientLabel,
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDelivery.settled(OutboxDeliveryResult.orphaned(
                    stream == null ? "stream removed" : "stream replaced (current=" + stream.getStreamId() + ")"));
        }

        // Everything below runs outside the transaction: the push
        // service holds the HTTP client, the stream config and the row
        // are plain values.
        PushDeliveryService push = pushDeliveryServiceFactory.apply(session, context);
        return () -> deliver(push, stream, row, realmLabel, clientLabel, rowStart);
    }

    /**
     * Phase two: the HTTP push and its classification. No session, no
     * transaction.
     */
    protected OutboxDeliveryResult deliver(PushDeliveryService push, StreamConfig stream, OutboxEntry row,
                                           String realmLabel, String clientLabel, Instant rowStart) {
        PushDeliveryOutcome outcome = deliverEncoded(push, stream, row);
        if (outcome.delivered()) {
            log.debugf("SSF push handler delivered. id=%s ownerId=%s streamId=%s correlationId=%s attempts=%d",
                    row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId(), row.getAttempts() + 1);
            metricsBinder.recordPushDelivery(realmLabel, clientLabel,
                    SsfMetricsBinder.PushOutcome.DELIVERED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryResult.delivered();
        }

        // Push failed: the drainer will compute next_attempt_at or
        // dead-letter based on attempt budget; the terminal DEAD_LETTER
        // outcome is counted by SsfOutboxMetricsListener.
        String lastError = formatLastError(outcome);
        log.debugf("SSF push handler delivery failed. id=%s ownerId=%s streamId=%s correlationId=%s lastError=%s",
                row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId(), lastError);
        metricsBinder.recordPushDelivery(realmLabel, clientLabel,
                SsfMetricsBinder.PushOutcome.RETRY, Duration.between(rowStart, Instant.now()));
        return OutboxDeliveryResult.retry(lastError);
    }

    /**
     * Delivers the row's stored encoded SET via the
     * {@link PushDeliveryService} built in {@code prepare}. The service
     * is stateless beyond its captured HTTP client + transmitter
     * config, so per-row construction is cheap. A minimal stub
     * {@link SsfSecurityEventToken} carries the correlation id (jti)
     * so the push service's logging stays useful — the actual payload
     * on the wire is the row's {@code payload} (signed encoded SET).
     *
     * <p>Catches any RuntimeException so the structured failure path
     * stays the only way out — the drainer's own catch-all would
     * otherwise erase the {@link PushDeliveryOutcome} detail.
     */
    protected PushDeliveryOutcome deliverEncoded(PushDeliveryService push, StreamConfig stream, OutboxEntry row) {
        SsfSecurityEventToken stub = new SsfSecurityEventToken();
        stub.setJti(row.getCorrelationId());
        try {
            return push.deliverEvent(stream, stub, row.getPayload());
        } catch (RuntimeException e) {
            log.warnf(e, "SSF push handler: push threw. id=%s ownerId=%s correlationId=%s",
                    row.getId(), row.getOwnerId(), row.getCorrelationId());
            String endpointUrl = stream != null && stream.getDelivery() != null
                    ? stream.getDelivery().getEndpointUrl() : null;
            return PushDeliveryOutcome.transportFailure(e, endpointUrl);
        }
    }

    /**
     * Builds the {@code last_error} summary line. Three shapes mirror
     * {@link PushDeliveryOutcome}:
     *
     * <ul>
     *   <li>HTTP non-2xx: {@code "HTTP <status> <url>: <body excerpt>"}</li>
     *   <li>Transport failure: {@code "<ExceptionClass> <url>: <message>"}</li>
     *   <li>Invalid stream config: {@code "InvalidStreamConfig: <reason>"}</li>
     * </ul>
     *
     * <p>The body / exception message is truncated at
     * {@link #LAST_ERROR_DETAIL_MAX} so the column ({@code VARCHAR(2048)})
     * comfortably absorbs the prefix + url + detail. The store's own
     * {@code truncateError} provides a final hard cap as defense in
     * depth.
     */
    protected String formatLastError(PushDeliveryOutcome push) {
        if (push.status() != null) {
            String body = truncateDetail(push.responseBody());
            return "HTTP " + push.status() + " " + nullToEmpty(push.endpointUrl()) + ": " + nullToEmpty(body);
        }
        if (push.exceptionClass() != null) {
            String message = truncateDetail(push.exceptionMessage());
            String url = push.endpointUrl();
            String simpleClass = push.exceptionClass().contains(".")
                    ? push.exceptionClass().substring(push.exceptionClass().lastIndexOf('.') + 1)
                    : push.exceptionClass();
            if (url == null) {
                return simpleClass + ": " + nullToEmpty(message);
            }
            return simpleClass + " " + url + ": " + nullToEmpty(message);
        }
        return "delivery failed";
    }

    protected static final int LAST_ERROR_DETAIL_MAX = 1024;

    protected static String truncateDetail(String detail) {
        if (detail == null) {
            return null;
        }
        if (detail.length() <= LAST_ERROR_DETAIL_MAX) {
            return detail;
        }
        return detail.substring(0, LAST_ERROR_DETAIL_MAX) + "...";
    }

    protected static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

}
