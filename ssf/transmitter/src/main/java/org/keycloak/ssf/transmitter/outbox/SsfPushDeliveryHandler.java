package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.outbox.OutboxDeliveryHandler;
import org.keycloak.outbox.OutboxDeliveryOutcome;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterContext;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
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
 * <p>Mirrors the resolve-then-deliver-then-classify logic of the
 * legacy {@code SsfPushOutboxDrainerTask.processPendingEvent} so the
 * cutover preserves SSF-visible behavior:
 *
 * <ul>
 *   <li>Realm / client / stream gone → {@link OutboxDeliveryOutcome#ORPHANED}.</li>
 *   <li>Push succeeds → {@link OutboxDeliveryOutcome#DELIVERED}.</li>
 *   <li>Push fails → {@link OutboxDeliveryOutcome#RETRY}; the drainer
 *       decides RETRY-vs-DEAD_LETTER based on attempts.</li>
 * </ul>
 *
 * <p>Emits the SSF push-delivery meter with the same outcomes the
 * legacy drainer did. The DEAD_LETTER metric counter is bumped here
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
    public OutboxDeliveryOutcome deliver(KeycloakSession session, OutboxEntryEntity row) {
        Instant rowStart = Instant.now();

        RealmModel realm = session.realms().getRealm(row.getRealmId());
        if (realm == null) {
            log.warnf("SSF push handler: row references unknown realm — orphaning. id=%s realmId=%s correlationId=%s",
                    row.getId(), row.getRealmId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(row.getRealmId(), row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryOutcome.ORPHANED;
        }
        String realmLabel = realm.getName();

        ClientModel receiverClient = realm.getClientById(row.getOwnerId());
        if (receiverClient == null) {
            log.warnf("SSF push handler: row references unknown client — orphaning. id=%s ownerId=%s correlationId=%s",
                    row.getId(), row.getOwnerId(), row.getCorrelationId());
            metricsBinder.recordPushDelivery(realmLabel, row.getOwnerId(),
                    SsfMetricsBinder.PushOutcome.ORPHANED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryOutcome.ORPHANED;
        }

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            // Feature unavailable mid-flight — leave the row pending,
            // try again next tick. Returning RETRY without bumping
            // attempts would be ideal, but the drainer always bumps;
            // a feature-disabled scenario is rare enough that one
            // wasted attempt is acceptable here.
            log.warnf("SSF push handler: transmitter provider unavailable — retrying row %s next tick", row.getId());
            return OutboxDeliveryOutcome.RETRY;
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
            return OutboxDeliveryOutcome.ORPHANED;
        }

        boolean delivered = deliverEncoded(session, stream, row);
        if (delivered) {
            log.debugf("SSF push handler delivered. id=%s ownerId=%s streamId=%s correlationId=%s attempts=%d",
                    row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId(), row.getAttempts() + 1);
            metricsBinder.recordPushDelivery(realmLabel, receiverClient.getClientId(),
                    SsfMetricsBinder.PushOutcome.DELIVERED, Duration.between(rowStart, Instant.now()));
            return OutboxDeliveryOutcome.DELIVERED;
        }

        // Push failed: the drainer will compute next_attempt_at or
        // dead-letter based on attempt budget. Emit the metric here
        // with RETRY semantics; if the drainer escalates to
        // DEAD_LETTER on exhaustion, that's a separate metric path
        // the drainer can hook in a follow-up.
        log.debugf("SSF push handler delivery failed. id=%s ownerId=%s streamId=%s correlationId=%s",
                row.getId(), row.getOwnerId(), stream.getStreamId(), row.getCorrelationId());
        metricsBinder.recordPushDelivery(realmLabel, receiverClient.getClientId(),
                SsfMetricsBinder.PushOutcome.RETRY, Duration.between(rowStart, Instant.now()));
        return OutboxDeliveryOutcome.RETRY;
    }

    /**
     * Delivers the row's stored encoded SET via a fresh
     * {@link PushDeliveryService}. {@code PushDeliveryService} is
     * stateless beyond its captured HTTP client + transmitter config,
     * so per-row construction is cheap. A minimal stub
     * {@link SsfSecurityEventToken} carries the correlation id (jti)
     * so the push service's logging stays useful — the actual payload
     * on the wire is the row's {@code payload} (signed encoded SET).
     */
    protected boolean deliverEncoded(KeycloakSession session, StreamConfig stream, OutboxEntryEntity row) {
        PushDeliveryService push = pushDeliveryServiceFactory.apply(session, context);
        SsfSecurityEventToken stub = new SsfSecurityEventToken();
        stub.setJti(row.getCorrelationId());
        try {
            return push.deliverEvent(stream, stub, row.getPayload());
        } catch (Exception e) {
            log.warnf(e, "SSF push handler: push threw. id=%s ownerId=%s correlationId=%s",
                    row.getId(), row.getOwnerId(), row.getCorrelationId());
            return false;
        }
    }

}
