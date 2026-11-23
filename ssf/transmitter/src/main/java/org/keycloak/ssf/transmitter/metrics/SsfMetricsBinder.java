package org.keycloak.ssf.transmitter.metrics;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.jboss.logging.Logger;

/**
 * Facade for SSF transmitter Prometheus metrics. All hot paths
 * (dispatcher, push handler, poll endpoint) route their telemetry
 * through this class so meter lookups + cardinality policy live in
 * exactly one place.
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
 * </ul>
 *
 * <h3>Drainer and outbox-depth meters</h3>
 * Tick rate / duration / last-tick timestamp, row transitions,
 * dead-letters and outbox depth are not SSF-specific and come from the
 * generic {@link org.keycloak.events.outbox.OutboxMetricsListener}
 * ({@code keycloak.outbox.*}, labeled by kind); SSF attaches it via
 * {@link org.keycloak.ssf.transmitter.outbox.SsfOutboxMetricsListener},
 * which adds the per-receiver {@code dead_letter} outcome to
 * {@link #METER_PUSH_DELIVERY}.
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

    public static final String PREFIX = "keycloak.ssf.";

    // Counters --------------------------------------------------------------
    public static final String METER_EVENTS_ENQUEUED = PREFIX + "events.enqueued";
    public static final String METER_EVENTS_SUPPRESSED = PREFIX + "events.suppressed";
    public static final String METER_PUSH_DELIVERY = PREFIX + "push.delivery";
    public static final String METER_POLL_SERVED = PREFIX + "poll.served";
    public static final String METER_POLL_ACK = PREFIX + "poll.ack";
    public static final String METER_POLL_NACK = PREFIX + "poll.nack";
    public static final String METER_VERIFICATION_REQUESTS = PREFIX + "verification.requests";

    // Timers ----------------------------------------------------------------
    public static final String METER_PUSH_DELIVERY_DURATION = PREFIX + "push.delivery.duration";
    public static final String METER_VERIFICATION_DURATION = PREFIX + "verification.duration";

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
     * unavailable. Every method is a no-op, so hot-path callers can
     * invoke the binder without null-checks or conditionals.
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
        public void recordPushDeadLetter(String realmId, String clientId) {
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
        public void recordVerification(String realmName, String clientId,
                                       VerificationInitiator initiator,
                                       VerificationOutcome outcome,
                                       Duration took) {
        }
    };

    private final MeterRegistry registry;

    public SsfMetricsBinder() {
        this(Metrics.globalRegistry);
    }

    public SsfMetricsBinder(MeterRegistry registry) {
        this.registry = registry;
    }

    // private constructor only used by NOOP to skip registry wiring.
    private SsfMetricsBinder(boolean skipRegistry) {
        this.registry = null;
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

    /**
     * Counts a push row reaching {@code DEAD_LETTER} (attempts
     * exhausted, handler verdict, or orphaned). Counter only: the
     * failing attempt's duration was already recorded as
     * {@link PushOutcome#RETRY} / {@link PushOutcome#ORPHANED} by the
     * handler, so no timer sample is added here.
     */
    public void recordPushDeadLetter(String realmId, String clientId) {
        counter(METER_PUSH_DELIVERY,
                "realm", safe(realmId),
                "client_id", safe(clientId),
                "outcome", PushOutcome.DEAD_LETTER.label())
                .increment();
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
