package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Drains the SSF push outbox: loads due pending rows, pushes them to
 * each receiver, and transitions rows based on the outcome.
 *
 * <p>Runs in a fresh {@link KeycloakSession} per tick via Keycloak's
 * {@link org.keycloak.timer.TimerProvider TimerProvider}. Wrapped in a
 * {@code ClusterAwareScheduledTaskRunner} at scheduling time so in an
 * HA deployment only one node drains per interval.
 *
 * <p>Concurrency within a single tick is cheap because rows are locked
 * {@link jakarta.persistence.LockModeType#PESSIMISTIC_WRITE PESSIMISTIC_WRITE}
 * by the DAO, and each row is transitioned to a terminal state
 * (DELIVERED / back to PENDING with a future {@code next_attempt_at} /
 * DEAD_LETTER) before the transaction commits.
 */
public class SsfPushOutboxDrainerTask implements ScheduledTask {

    private static final Logger log = Logger.getLogger(SsfPushOutboxDrainerTask.class);

    /**
     * Retain DELIVERED rows for 24h so we keep jti-dedup coverage for
     * at-least-once enqueue paths that might retry shortly after a
     * successful delivery. Well beyond the maximum backoff window, so
     * no in-flight retry can outlive this.
     */
    protected static final Duration DELIVERED_RETENTION = Duration.ofHours(24);

    protected final int batchSize;

    protected final SsfPushOutboxBackoff backoff;

    /**
     * Retention for DEAD_LETTER rows. {@code null} or {@code Duration.ZERO}
     * disables the purge (operators may want to retain dead-letters
     * indefinitely for forensic/audit purposes).
     */
    protected final Duration deadLetterRetention;

    /**
     * Session-scoped factory for the outbox DAO. Extension point for
     * deployments that want to plug in a custom {@link SsfPendingEventStore}
     * subclass (e.g. for instrumentation or schema overrides) — the
     * default is {@code SsfPendingEventStore::new}.
     */
    protected final Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory;

    public SsfPushOutboxDrainerTask(int batchSize,
                                    SsfPushOutboxBackoff backoff,
                                    Duration deadLetterRetention,
                                    Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory) {
        this.batchSize = batchSize;
        this.backoff = backoff;
        this.deadLetterRetention = deadLetterRetention;
        this.pendingSsfEventStoreFactory = pendingSsfEventStoreFactory;
    }

    @Override
    public void run(KeycloakSession session) {
        // KeycloakSessionUtil is how SsfTransmitter.current() finds its
        // provider — the session isn't otherwise threaded through.
        KeycloakSession previous = KeycloakSessionUtil.getKeycloakSession();
        KeycloakSessionUtil.setKeycloakSession(session);
        try {
            SsfPendingEventStore store = pendingSsfEventStoreFactory.apply(session);
            drain(session, store);
            purgeDeliveredOlderThanRetention(store);
            purgeDeadLetterOlderThanRetention(store);
        } finally {
            KeycloakSessionUtil.setKeycloakSession(previous);
        }
    }

    protected void drain(KeycloakSession session, SsfPendingEventStore store) {
        List<SsfPendingEventEntity> due = store.lockDueForPush(batchSize);
        if (due.isEmpty()) {
            return;
        }
        log.debugf("SSF outbox drainer processing %d due row(s)", due.size());

        for (SsfPendingEventEntity pendingEvent : due) {
            processPendingEvent(session, store, pendingEvent);
        }
    }

    protected void processPendingEvent(KeycloakSession session, SsfPendingEventStore store, SsfPendingEventEntity row) {
        // Resolve the realm + receiver client the row targets. The row
        // may outlive the client (e.g. admin deleted the client while a
        // push was pending) — treat that as a terminal failure so the
        // row doesn't loop forever.
        RealmModel realm = session.realms().getRealm(row.getRealmId());
        if (realm == null) {
            log.warnf("SSF outbox row references unknown realm — dead-lettering. id=%s realmId=%s jti=%s",
                    row.getId(), row.getRealmId(), row.getJti());
            store.markDeadLetter(row, "realm no longer exists: " + row.getRealmId());
            return;
        }

        ClientModel receiverClient = realm.getClientById(row.getClientId());
        if (receiverClient == null) {
            log.warnf("SSF outbox row references unknown client — dead-lettering. id=%s clientId=%s jti=%s",
                    row.getId(), row.getClientId(), row.getJti());
            store.markDeadLetter(row, "client no longer exists: " + row.getClientId());
            return;
        }

        StreamConfig stream = new ClientStreamStore(session).getStreamForClient(receiverClient);
        if (stream == null || !row.getStreamId().equals(stream.getStreamId())) {
            // Stream was deleted (or recreated with a different id) while
            // this SET was in the outbox. Either way the receiver isn't
            // in a state to accept it meaningfully — drop.
            log.warnf("SSF outbox row's stream is gone — dead-lettering. id=%s clientId=%s pendingStreamId=%s currentStreamId=%s",
                    row.getId(), row.getClientId(), row.getStreamId(),
                    stream == null ? "<none>" : stream.getStreamId());
            store.markDeadLetter(row, "stream no longer exists: " + row.getStreamId());
            return;
        }

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            // Feature disabled mid-flight — leave the row pending, try
            // again next tick. No counter bump: this isn't the receiver's
            // fault.
            log.warnf("SSF outbox: transmitter provider unavailable — skipping row %s", row.getId());
            return;
        }

        boolean delivered = deliverEncoded(session, transmitter, stream, row);
        if (delivered) {
            log.debugf("SSF outbox delivered. id=%s clientId=%s streamId=%s jti=%s attempts=%d",
                    row.getId(), row.getClientId(), row.getStreamId(), row.getJti(), row.getAttempts() + 1);
            store.markDelivered(row);
            return;
        }

        int nextAttempts = row.getAttempts() + 1;
        String lastError = "push delivery failed (attempt " + nextAttempts + ")";
        if (backoff.isExhausted(nextAttempts)) {
            log.warnf("SSF outbox dead-lettered after %d attempts. id=%s clientId=%s streamId=%s jti=%s",
                    nextAttempts, row.getId(), row.getClientId(), row.getStreamId(), row.getJti());
            store.markDeadLetter(row, lastError);
            return;
        }
        Instant nextAttemptAt = backoff.computeNextAttemptAt(Instant.now(), nextAttempts);
        log.debugf("SSF outbox scheduling retry. id=%s attempts=%d nextAttemptAt=%s",
                row.getId(), nextAttempts, nextAttemptAt);
        store.recordFailure(row, nextAttemptAt, lastError);
    }

    /**
     * Pushes the stored encoded SET via a fresh
     * {@link PushDeliveryService} scoped to the current drainer
     * session. {@code PushDeliveryService} is stateless beyond the
     * captured {@link org.apache.http.client.HttpClient} and transmitter
     * config, so constructing it per row is cheap. A minimal stub
     * {@link SsfSecurityEventToken} carries the jti so the push
     * service's logging stays useful — the actual payload on the wire
     * is the already-signed {@code encoded_set} from the row.
     */
    protected boolean deliverEncoded(KeycloakSession session,
                                     SsfTransmitterProvider transmitter,
                                     StreamConfig stream,
                                     SsfPendingEventEntity row) {
        PushDeliveryService push = new PushDeliveryService(session, transmitter.getConfig());
        SsfSecurityEventToken stub = new SsfSecurityEventToken();
        stub.setJti(row.getJti());
        try {
            return push.deliverEvent(stream, stub, row.getEncodedSet());
        } catch (Exception e) {
            log.warnf(e, "SSF outbox push threw. id=%s clientId=%s jti=%s",
                    row.getId(), row.getClientId(), row.getJti());
            return false;
        }
    }

    protected void purgeDeliveredOlderThanRetention(SsfPendingEventStore store) {
        Instant cutoff = Instant.now().minus(DELIVERED_RETENTION);
        store.purgeDeliveredOlderThan(cutoff);
    }

    protected void purgeDeadLetterOlderThanRetention(SsfPendingEventStore store) {
        if (deadLetterRetention == null || deadLetterRetention.isZero() || deadLetterRetention.isNegative()) {
            return;
        }
        Instant cutoff = Instant.now().minus(deadLetterRetention);
        store.purgeDeadLetterOlderThan(cutoff);
    }
}
