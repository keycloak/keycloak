package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.events.outbox.OutboxDeliveryHandler;
import org.keycloak.events.outbox.OutboxDeliveryResult;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterContext;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryOutcome;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;

/**
 * SSF push handler for the generic outbox. The drainer looks up this
 * handler by {@code entryKind = "ssf-push"} and invokes
 * {@link #deliver(KeycloakSession, OutboxEntryEntity)} for each due
 * row; this implementation resolves the realm/client/stream the row
 * targets and hands the encoded SET to {@link PushDeliveryService}.
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
 * outcome — DELIVERED / RETRY / ORPHANED. The DEAD_LETTER counter
 * is bumped here
 * when the handler can detect attempt-exhaustion ahead of time
 * (handler returns RETRY but knows the next attempt will be the
 * final one); the drainer's escalation path ensures the row's
 * transition matches.
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
    public OutboxDeliveryResult deliver(KeycloakSession session, OutboxEntryEntity row) {
        Instant rowStart = Instant.now();

        RealmModel realm = session.realms().getRealm(row.getRealmId());
        if (realm == null) {
            log.warnf("SSF push handler: row references unknown realm — orphaning. id=%s realmId=%s correlationId=%s",
                    row.getId(), row.getRealmId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(row.getRealmId(), row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryResult.orphaned("unknown realm: " + row.getRealmId());
        }
        String realmLabel = realm.getName();

        ClientModel receiverClient = realm.getClientById(row.getOwnerId());
        if (receiverClient == null) {
            log.warnf("SSF push handler: row references unknown client — orphaning. id=%s ownerId=%s correlationId=%s",
                    row.getId(), row.getOwnerId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(realmLabel, row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryResult.orphaned("unknown client: " + row.getOwnerId());
        }

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            // Feature unavailable mid-flight — leave the row pending,
            // try again next tick. Returning RETRY without bumping
            // attempts would be ideal, but the drainer always bumps;
            // a feature-disabled scenario is rare enough that one
            // wasted attempt is acceptable here.
            log.warnf("SSF push handler: transmitter provider unavailable — retrying row %s next tick", row.getId());
            return OutboxDeliveryResult.retry("transmitter provider unavailable");
        }

        String expectedStreamId = row.getContainerId();
        StreamConfig stream = transmitter.streamStore().getStreamForClient(receiverClient);
        if (stream == null
                || (expectedStreamId != null && !expectedStreamId.equals(stream.getStreamId()))) {
            log.warnf("SSF push handler: row's stream is gone — orphaning. id=%s ownerId=%s pendingStreamId=%s currentStreamId=%s",
                    row.getId(), row.getOwnerId(), expectedStreamId,
                    stream == null ? "<none>" : stream.getStreamId());
            metricsBinder.recordPushDelivery(realmLabel, receiverClient.getClientId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryResult.orphaned(
                    stream == null ? "stream removed" : "stream replaced (current=" + stream.getStreamId() + ")");
        }

        PushDeliveryOutcome push = deliverEncoded(session, stream, row);
        if (push.delivered()) {
            log.debugf("SSF push handler delivered. id=%s ownerId=%s streamId=%s correlationId=%s attempts=%d",
                    row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId(), row.getAttempts() + 1);
            metricsBinder.recordPushDelivery(realmLabel, receiverClient.getClientId(),
                    SsfMetricsBinder.PushOutcome.DELIVERED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryResult.delivered();
        }

        // Push failed: the drainer will compute next_attempt_at or
        // dead-letter based on attempt budget. Emit the metric here
        // with RETRY semantics; if the drainer escalates to
        // DEAD_LETTER on exhaustion, that's a separate metric path
        // the drainer can hook in a follow-up.
        String lastError = formatLastError(push);
        log.debugf("SSF push handler delivery failed. id=%s ownerId=%s streamId=%s correlationId=%s lastError=%s",
                row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId(), lastError);
        metricsBinder.recordPushDelivery(realmLabel, receiverClient.getClientId(),
                SsfMetricsBinder.PushOutcome.RETRY, Duration.between(rowStart, Instant.now()));
        return OutboxDeliveryResult.retry(lastError);
    }

    /**
     * Delivers the row's stored encoded SET via a fresh
     * {@link PushDeliveryService}. {@code PushDeliveryService} is
     * stateless beyond its captured HTTP client + transmitter config,
     * so per-row construction is cheap. A minimal stub
     * {@link SsfSecurityEventToken} carries the correlation id (jti)
     * so the push service's logging stays useful — the actual payload
     * on the wire is the row's {@code payload} (signed encoded SET).
     *
     * <p>Catches any RuntimeException so the structured failure path
     * stays the only way out — the drainer's own catch-all would
     * otherwise erase the {@link PushDeliveryOutcome} detail.
     */
    protected PushDeliveryOutcome deliverEncoded(KeycloakSession session, StreamConfig stream, OutboxEntryEntity row) {
        PushDeliveryService push = pushDeliveryServiceFactory.apply(session, context);
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
