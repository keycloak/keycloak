package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
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

    protected final SsfTransmitterConfig transmitterConfig;

    protected final BiFunction<KeycloakSession, SsfTransmitterConfig, PushDeliveryService> pushDeliveryServiceFactory;

    public SsfPushOutboxDrainerTask(int batchSize,
                                    SsfPushOutboxBackoff backoff,
                                    Duration deadLetterRetention,
                                    Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory,
                                    SsfTransmitterConfig transmitterConfig,
                                    BiFunction<KeycloakSession, SsfTransmitterConfig, PushDeliveryService> pushDeliveryServiceFactory) {
        this.batchSize = batchSize;
        this.backoff = backoff;
        this.deadLetterRetention = deadLetterRetention;
        this.pendingSsfEventStoreFactory = pendingSsfEventStoreFactory;
        this.transmitterConfig = transmitterConfig;
        this.pushDeliveryServiceFactory = pushDeliveryServiceFactory;
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
            // Per-receiver TTL pass first so receivers with a tighter
            // ssf.maxEventAgeSeconds shed stale rows before the broader
            // global retention windows touch them.
            purgeStalePerClient(session, store);
            purgeDeliveredOlderThanRetention(store);
            purgeDeadLetterOlderThanRetention(store);
            // SSF 1.0 §8.1.1: auto-pause streams whose receiver has
            // been idle beyond the configured inactivity_timeout.
            pauseInactiveStreams(session);
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

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            // Feature disabled mid-flight — leave the row pending, try
            // again next tick. No counter bump: this isn't the receiver's
            // fault.
            log.warnf("SSF outbox: transmitter provider unavailable — skipping row %s", row.getId());
            return;
        }

        StreamConfig stream = transmitter.streamStore().getStreamForClient(receiverClient);
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

        boolean delivered = deliverEncoded(session, stream, row);
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
                                     StreamConfig stream,
                                     SsfPendingEventEntity row) {

        PushDeliveryService push = pushDeliveryServiceFactory.apply(session, transmitterConfig);
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

    /**
     * Per-receiver TTL housekeeping. For every receiver that owns at
     * least one non-{@code DELIVERED} outbox row, looks up the
     * {@code ssf.maxEventAgeSeconds} client attribute and purges
     * non-{@code DELIVERED} rows for that client whose {@code createdAt}
     * is older than {@code now - maxEventAgeSeconds}. Receivers that
     * don't set the attribute fall back to the transmitter-wide
     * retention windows.
     *
     * <p>Bounded by the distinct (realmId, clientId) pairs returned from
     * {@link SsfPendingEventStore#findRealmClientPairsForPurgeScan},
     * so it scales with the number of <em>active</em> receivers, not
     * the total client count in the realm.
     */
    protected void purgeStalePerClient(KeycloakSession session, SsfPendingEventStore store) {
        List<Object[]> pairs = store.findRealmClientPairsForPurgeScan();
        if (pairs.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (Object[] pair : pairs) {
            String realmId = (String) pair[0];
            String clientId = (String) pair[1];
            Long maxAgeSeconds = readMaxEventAgeSeconds(session, realmId, clientId);
            if (maxAgeSeconds == null || maxAgeSeconds <= 0) {
                continue;
            }
            Instant cutoff = now.minusSeconds(maxAgeSeconds);
            store.purgeStaleForClient(clientId, cutoff);
        }
    }

    /**
     * Reads {@link ClientStreamStore#SSF_MAX_EVENT_AGE_SECONDS_KEY} off
     * the receiver client. Returns {@code null} when the realm or client
     * is gone (the row will be reaped by realm-removed cascade or the
     * drainer's own dead-letter path) or when the attribute is unset /
     * malformed.
     */
    protected Long readMaxEventAgeSeconds(KeycloakSession session, String realmId, String clientId) {
        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return null;
        }
        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            return null;
        }
        String raw = client.getAttribute(ClientStreamStore.SSF_MAX_EVENT_AGE_SECONDS_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.debugf("Ignoring malformed %s='%s' on client %s",
                    ClientStreamStore.SSF_MAX_EVENT_AGE_SECONDS_KEY, raw, clientId);
            return null;
        }
    }

    /**
     * SSF 1.0 §8.1.1 {@code inactivity_timeout}: walks every realm's
     * receivers that have a registered stream AND a configured
     * {@code ssf.inactivityTimeoutSeconds} attribute, and transitions
     * stale streams (no activity within the window) from {@code enabled}
     * to {@code paused}. The status change itself goes through
     * {@code StreamService.updateStreamStatus} so the hold-backlog
     * machinery and the {@code stream-updated} SET dispatch run
     * exactly as they would for a receiver-driven status change.
     *
     * <p>Only scans clients that have {@code ssf.streamId} set — the
     * inactivity check is irrelevant for anything without a stream.
     * Already-paused or already-disabled streams are skipped; the
     * transition only fires when the current status is {@code enabled}.
     */
    protected void pauseInactiveStreams(KeycloakSession session) {
        long now = Time.currentTime();
        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            return;
        }
        session.realms().getRealmsStream().forEach(realm -> {
            session.getContext().setRealm(realm);
            realm.getClientsStream()
                    .filter(c -> c.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY) != null)
                    .filter(c -> c.getAttribute(ClientStreamStore.SSF_INACTIVITY_TIMEOUT_SECONDS_KEY) != null)
                    .forEach(client -> pauseIfInactive(session, transmitter, client, now));
        });
    }

    private void pauseIfInactive(KeycloakSession session,
                                 SsfTransmitterProvider transmitter,
                                 ClientModel client,
                                 long now) {
        String timeoutRaw = client.getAttribute(ClientStreamStore.SSF_INACTIVITY_TIMEOUT_SECONDS_KEY);
        long timeoutSeconds;
        try {
            timeoutSeconds = Long.parseLong(timeoutRaw.trim());
        } catch (NumberFormatException e) {
            log.debugf("Ignoring malformed %s='%s' on client %s",
                    ClientStreamStore.SSF_INACTIVITY_TIMEOUT_SECONDS_KEY, timeoutRaw, client.getClientId());
            return;
        }
        if (timeoutSeconds <= 0) {
            return;
        }

        String status = client.getAttribute(ClientStreamStore.SSF_STATUS_KEY);
        if (!StreamStatusValue.enabled.name().equals(status)) {
            return;
        }

        String lastActivityRaw = client.getAttribute(ClientStreamStore.SSF_LAST_ACTIVITY_TIMESLOT_KEY);
        long lastActivity;
        try {
            lastActivity = lastActivityRaw != null ? Long.parseLong(lastActivityRaw.trim()) : 0L;
        } catch (NumberFormatException e) {
            lastActivity = 0L;
        }
        // Never-active streams: stamp now as a baseline and let the
        // next tick evaluate — avoids pausing a stream that was just
        // created but hasn't seen any API hit yet.
        if (lastActivity == 0L) {
            client.setAttribute(ClientStreamStore.SSF_LAST_ACTIVITY_TIMESLOT_KEY, String.valueOf(now));
            return;
        }
        if (now - lastActivity < timeoutSeconds) {
            return;
        }

        String streamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        log.infof("SSF inactivity timeout exceeded — pausing stream. clientId=%s streamId=%s idleSeconds=%d timeout=%d",
                client.getClientId(), streamId, (now - lastActivity), timeoutSeconds);

        StreamStatus pauseStatus = new StreamStatus();
        pauseStatus.setStreamId(streamId);
        pauseStatus.setStatus(StreamStatusValue.paused.getStatusCode());
        pauseStatus.setReason("Stream paused due to receiver inactivity");
        try {
            // updateStreamStatus reads the receiver from session.getContext().getClient()
            // for logging and the stream-updated dispatch; set it up briefly.
            ClientModel previousClient = session.getContext().getClient();
            session.getContext().setClient(client);
            try {
                transmitter.streamService().updateStreamStatus(pauseStatus);
            } finally {
                session.getContext().setClient(previousClient);
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to pause inactive stream. clientId=%s streamId=%s",
                    client.getClientId(), streamId);
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
