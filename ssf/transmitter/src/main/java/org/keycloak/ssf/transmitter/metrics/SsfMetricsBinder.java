package org.keycloak.ssf.transmitter.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.common.util.Time;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.jboss.logging.Logger;

/**
 * Facade for SSF transmitter Prometheus metrics. All hot paths
 * (dispatcher, drainer, poll endpoint) route their telemetry through
 * this class so meter lookups + cardinality policy live in exactly
 * one place.
 *
 * <p>Follows Keycloak's existing Micrometer convention of using the
 * {@link Metrics#globalRegistry global registry} (see
 * {@code MicrometerUserEventMetricsEventListenerProviderFactory}). The
 * registry increments and timers are cheap even when the Quarkus
 * Prometheus endpoint is disabled — they land in an empty collector.
 *
 * <h3>Cardinality policy</h3>
 * <ul>
 *     <li>Counters / timers are labeled by {@code realm} + {@code client_id}.
 *         Label cardinality is bounded by the typical 1–5 SSF receiver
 *         clients per realm, so a per-client slice is cheap and gives
 *         operators the "which downstream is flaking?" signal they need.</li>
 *     <li>Outbox depth is a gauge labeled by {@code realm} + {@code status}
 *         only. Per-client gauges would blow up cardinality for large
 *         deployments and the admin UI already serves per-client depth
 *         on demand.</li>
 *     <li>The drainer tick counter is <em>not</em> labeled by node id:
 *         cluster-aggregate rate answers "is SSF draining somewhere?",
 *         and Kubernetes pod-name churn keeps node-labeled series
 *         otherwise clean.</li>
 * </ul>
 *
 * <h3>Depth gauges (cached)</h3>
 * Outbox depth would be an expensive per-scrape {@code COUNT(*)} if
 * bound as a normal gauge. Instead, the drainer calls
 * {@link #updateOutboxDepthSnapshot(Map)} once per tick with the result
 * of one grouped aggregate; gauges read from the in-memory snapshot
 * and scrapes pay nothing. Depth is therefore scrape-lagged by up to
 * one drainer tick — fine for "backlog growing" alerting.
 *
 * <h3>No-op fallback</h3>
 * When {@link SsfTransmitterConfig#isMetricsEnabled()} is false (or the
 * runtime omits Micrometer for some reason), the factory constructs
 * {@link #NOOP} instead of a real binder. Every method then becomes a
 * branch-predicted no-op — the hot paths can call the binder
 * unconditionally without a null-check cascade.
 */
public class SsfMetricsBinder {

    private static final Logger log = Logger.getLogger(SsfMetricsBinder.class);

    private static final String PREFIX = "keycloak.ssf.";

    // Counters --------------------------------------------------------------
    public static final String METER_EVENTS_ENQUEUED = PREFIX + "events.enqueued";
    public static final String METER_EVENTS_SUPPRESSED = PREFIX + "events.suppressed";
    public static final String METER_PUSH_DELIVERY = PREFIX + "push.delivery";
    public static final String METER_POLL_SERVED = PREFIX + "poll.served";
    public static final String METER_POLL_ACK = PREFIX + "poll.ack";
    public static final String METER_POLL_NACK = PREFIX + "poll.nack";
    public static final String METER_DRAINER_TICK = PREFIX + "drainer.tick";
    public static final String METER_VERIFICATION_REQUESTS = PREFIX + "verification.requests";

    // Timers ----------------------------------------------------------------
    public static final String METER_PUSH_DELIVERY_DURATION = PREFIX + "push.delivery.duration";
    public static final String METER_DRAINER_TICK_DURATION = PREFIX + "drainer.tick.duration";
    public static final String METER_VERIFICATION_DURATION = PREFIX + "verification.duration";

    // Gauges ----------------------------------------------------------------
    public static final String METER_OUTBOX_DEPTH = PREFIX + "outbox.depth";

    /**
     * Epoch-second timestamp of the most recent drainer tick attempt.
     * Lets operators alert on
     * {@code time() - keycloak_ssf_drainer_tick_last_at_seconds > 120}
     * for a stalled drainer — complements
     * {@link #METER_DRAINER_TICK} (counter-rate based) with an absolute
     * "how long ago" answer in a single gauge query. Exposed as `0`
     * before the first tick so a freshly-started server doesn't register
     * as instantly stalled — alerting rules should ignore the value
     * until it's been observed non-zero at least once.
     */
    public static final String METER_DRAINER_TICK_LAST_AT = PREFIX + "drainer.tick.last_at_seconds";

    /**
     * Dispatcher outcome classifications used as the {@code reason}
     * label on the suppressed counter. Stable string values so
     * Prometheus alerting rules can match them.
     */
    public enum SuppressReason {
        STATUS_DISABLED("status_disabled"),
        STATUS_PAUSED_HELD("status_paused_held"),
        EVENT_NOT_REQUESTED("event_not_requested"),
        SUBJECT_GATE("subject_gate");

        private final String label;

        SuppressReason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Drainer outcome classifications for one pending row.
     */
    public enum PushOutcome {
        DELIVERED("delivered"),
        RETRY("retry"),
        DEAD_LETTER("dead_letter"),
        ORPHANED("orphaned");

        private final String label;

        PushOutcome(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum DrainerOutcome {
        OK("ok"),
        ERROR("error");

        private final String label;

        DrainerOutcome(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Who triggered a verification dispatch. Lets operators slice
     * {@code verification.requests} by entry point so a spike in
     * {@code initiator="receiver"} (over-polling) is distinguishable
     * from {@code initiator="transmitter"} (post-create auto-fire) or
     * {@code initiator="admin"} (UI / REST).
     */
    public enum VerificationInitiator {
        RECEIVER("receiver"),
        ADMIN("admin"),
        TRANSMITTER("transmitter");

        private final String label;

        VerificationInitiator(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Outcome of a verification request:
     * <ul>
     *     <li>{@code delivered} — receiver accepted the verification SET.</li>
     *     <li>{@code failed} — sync push to the receiver failed
     *         (network error, non-2xx, or the receiver-side stream
     *         lookup turned up empty).</li>
     *     <li>{@code rate_limited} — request rejected with 429 because
     *         the receiver-side {@code min_verification_interval} has
     *         not yet elapsed. Only fires on the receiver-initiated
     *         path.</li>
     * </ul>
     */
    public enum VerificationOutcome {
        DELIVERED("delivered"),
        FAILED("failed"),
        RATE_LIMITED("rate_limited");

        private final String label;

        VerificationOutcome(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * NOOP binder used when metrics are disabled or Micrometer is
     * unavailable. Every method is a no-op, including the snapshot
     * update — so hot-path callers can invoke the binder without
     * null-checks or conditionals.
     */
    public static final SsfMetricsBinder NOOP = new SsfMetricsBinder(true) {
        @Override
        public void recordEnqueued(String realmId, String clientId, String deliveryMethod, String eventType) {
        }

        @Override
        public void recordSuppressed(String realmId, String clientId, SuppressReason reason) {
        }

        @Override
        public void recordPushDelivery(String realmId, String clientId, PushOutcome outcome, Duration took) {
        }

        @Override
        public void recordPollServed(String realmId, String clientId, long count) {
        }

        @Override
        public void recordPollAck(String realmId, String clientId, long count) {
        }

        @Override
        public void recordPollNack(String realmId, String clientId, long count) {
        }

        @Override
        public void recordDrainerTick(DrainerOutcome outcome, Duration took) {
        }

        @Override
        public void recordVerification(String realmName, String clientId,
                                       VerificationInitiator initiator,
                                       VerificationOutcome outcome,
                                       Duration took) {
        }

        @Override
        public void updateOutboxDepthSnapshot(Map<RealmStatus, Long> snapshot) {
        }
    };

    private final MeterRegistry registry;

    /**
     * Cached outbox depth snapshot, refreshed at the end of each
     * drainer tick. Gauges read from this map; scrapes pay nothing
     * beyond a {@link ConcurrentHashMap} lookup.
     */
    private volatile Map<RealmStatus, Long> depthSnapshot = Collections.emptyMap();

    /**
     * Tracks which {@code (realm, status)} gauge keys we've already
     * registered so repeated snapshot updates don't register the same
     * gauge twice. Micrometer's {@code Gauge#builder} is idempotent in
     * principle, but guarding here avoids the log noise and keeps the
     * hot path tight.
     */
    private final ConcurrentHashMap<RealmStatus, Boolean> registeredDepthGauges = new ConcurrentHashMap<>();

    /**
     * Epoch-second timestamp of the most recent drainer tick. Stamped
     * from {@link #recordDrainerTick}; read by the
     * {@link #METER_DRAINER_TICK_LAST_AT} gauge bound in the
     * constructor. {@code volatile} because the drainer tick runs on a
     * scheduler thread while scrapes run on HTTP worker threads.
     */
    private volatile long drainerTickLastAtEpochSeconds = 0L;

    public SsfMetricsBinder() {
        this(Metrics.globalRegistry);
    }

    public SsfMetricsBinder(MeterRegistry registry) {
        this.registry = registry;
        registerDrainerLastTickGauge();
    }

    /**
     * Single, label-free gauge — bound eagerly in the constructor so
     * scrapes can read it immediately. The supplier reads the volatile
     * field, so the gauge always reflects the most recent stamp.
     */
    private void registerDrainerLastTickGauge() {
        try {
            Gauge.builder(METER_DRAINER_TICK_LAST_AT, this, b -> b.drainerTickLastAtEpochSeconds)
                    .description("Epoch-second of the most recent SSF outbox drainer tick. "
                            + "Operators alert on time() - this > N to detect a stalled drainer. "
                            + "Reports 0 before the first tick.")
                    .baseUnit("seconds")
                    .register(registry);
        } catch (RuntimeException e) {
            // Same swallow pattern as the per-receiver depth gauges:
            // metrics are best-effort, never break drainer behaviour.
            log.warnf(e, "Failed to register %s gauge", METER_DRAINER_TICK_LAST_AT);
        }
    }

    // private constructor only used by NOOP to skip registry wiring.
    private SsfMetricsBinder(boolean skipRegistry) {
        this.registry = null;
    }

    /**
     * Compound key for the outbox depth gauge map.
     */
    public record RealmStatus(String realmId, OutboxEntryStatus status) {
    }

    // ---------------------------------------------------------------- record

    public void recordEnqueued(String realmId, String clientId, String deliveryMethod, String eventType) {
        counter(METER_EVENTS_ENQUEUED,
                "realm", safe(realmId),
                "client_id", safe(clientId),
                "delivery_method", safe(deliveryMethod),
                "event_type", safe(eventType))
                .increment();
    }

    public void recordSuppressed(String realmId, String clientId, SuppressReason reason) {
        counter(METER_EVENTS_SUPPRESSED,
                "realm", safe(realmId),
                "client_id", safe(clientId),
                "reason", reason.label())
                .increment();
    }

    public void recordPushDelivery(String realmId, String clientId, PushOutcome outcome, Duration took) {
        counter(METER_PUSH_DELIVERY,
                "realm", safe(realmId),
                "client_id", safe(clientId),
                "outcome", outcome.label())
                .increment();
        Timer timer = Timer.builder(METER_PUSH_DELIVERY_DURATION)
                .description("Push delivery duration per outbox row.")
                .tags(Tags.of(
                        Tag.of("realm", safe(realmId)),
                        Tag.of("client_id", safe(clientId)),
                        Tag.of("outcome", outcome.label())))
                .register(registry);
        timer.record(took);
    }

    public void recordPollServed(String realmId, String clientId, long count) {
        if (count <= 0) {
            return;
        }
        counter(METER_POLL_SERVED,
                "realm", safe(realmId),
                "client_id", safe(clientId))
                .increment(count);
    }

    public void recordPollAck(String realmId, String clientId, long count) {
        if (count <= 0) {
            return;
        }
        counter(METER_POLL_ACK,
                "realm", safe(realmId),
                "client_id", safe(clientId))
                .increment(count);
    }

    public void recordPollNack(String realmId, String clientId, long count) {
        if (count <= 0) {
            return;
        }
        counter(METER_POLL_NACK,
                "realm", safe(realmId),
                "client_id", safe(clientId))
                .increment(count);
    }

    public void recordDrainerTick(DrainerOutcome outcome, Duration took) {
        counter(METER_DRAINER_TICK, "outcome", outcome.label()).increment();
        Timer.builder(METER_DRAINER_TICK_DURATION)
                .description("Total SSF outbox drainer tick duration.")
                .register(registry)
                .record(took);
        // Stamped on every tick (ok or error) so a failing-but-still-
        // ticking drainer reports a fresh timestamp; a *stuck* drainer
        // that never returns is the only thing that lets the gauge fall
        // behind. Time.currentTime() (epoch seconds, wall clock) is the
        // Keycloak-wide convention and is directly comparable to
        // Prometheus' time().
        drainerTickLastAtEpochSeconds = Time.currentTime();
    }

    /**
     * Records one verification dispatch. {@code took} is allowed to be
     * {@code null} for outcomes where there is no measured duration —
     * notably {@link VerificationOutcome#RATE_LIMITED}, which is rejected
     * before any HTTP push happens.
     */
    public void recordVerification(String realmName,
                                   String clientId,
                                   VerificationInitiator initiator,
                                   VerificationOutcome outcome,
                                   Duration took) {
        counter(METER_VERIFICATION_REQUESTS,
                "realm", safe(realmName),
                "client_id", safe(clientId),
                "initiator", initiator.label(),
                "outcome", outcome.label())
                .increment();
        if (took != null) {
            Timer.builder(METER_VERIFICATION_DURATION)
                    .description("Verification dispatch duration (sync push to receiver).")
                    .tags(Tags.of(
                            Tag.of("realm", safe(realmName)),
                            Tag.of("client_id", safe(clientId)),
                            Tag.of("initiator", initiator.label()),
                            Tag.of("outcome", outcome.label())))
                    .register(registry)
                    .record(took);
        }
    }

    /**
     * Swaps the current outbox-depth snapshot with the one produced by
     * the drainer tick. Registers any newly-seen {@code (realm, status)}
     * gauges lazily; gauges read from the cached map so scrapes don't
     * touch the database.
     */
    public void updateOutboxDepthSnapshot(Map<RealmStatus, Long> snapshot) {
        Map<RealmStatus, Long> safeSnapshot = snapshot == null ? Collections.emptyMap() : snapshot;
        this.depthSnapshot = safeSnapshot;
        for (RealmStatus key : safeSnapshot.keySet()) {
            ensureDepthGauge(key);
        }
    }

    private void ensureDepthGauge(RealmStatus key) {
        if (registeredDepthGauges.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        try {
            Gauge.builder(METER_OUTBOX_DEPTH, depthSnapshot, m -> {
                        Long v = m.get(key);
                        return v == null ? 0.0 : v.doubleValue();
                    })
                    .description("Outbox row count, snapshot from last drainer tick.")
                    .baseUnit(BaseUnits.ROWS)
                    .tags(Tags.of(
                            Tag.of("realm", safe(key.realmId())),
                            Tag.of("status", key.status().name())))
                    .register(registry);
        } catch (RuntimeException e) {
            // Never let a meter registration failure propagate back
            // into the drainer — the drainer's job is draining, not
            // metrics hygiene.
            log.debugf(e, "Failed to register SSF outbox depth gauge for %s", key);
            registeredDepthGauges.remove(key);
        }
    }

    // ------------------------------------------------------------ internals

    private Counter counter(String name, String... tagPairs) {
        return Counter.builder(name)
                .tags(tagPairs)
                .register(registry);
    }

    /**
     * Prometheus label values must be strings; null clients / realms
     * during startup races become a literal {@code "unknown"} so the
     * meter never silently drops the increment.
     */
    private static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
