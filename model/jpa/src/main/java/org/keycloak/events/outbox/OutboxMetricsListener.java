package org.keycloak.events.outbox;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.jboss.logging.Logger;

/**
 * {@link OutboxDrainerListener} that publishes the drainer's activity
 * as Micrometer meters, labeled by outbox {@code kind} so every
 * consumer of the outbox (SSF, SCIM, webhooks, ...) gets the same
 * operational signal without writing its own:
 *
 * <ul>
 *   <li>{@value #METER_TICK} counter and {@value #METER_TICK_DURATION}
 *       timer per tick, {@value #METER_TICK_LAST_AT} gauge with the
 *       epoch second of the last tick — alert on
 *       {@code time() - last_at > N} for a stalled drainer.</li>
 *   <li>{@value #METER_TRANSITIONS} counter labeled by {@code realm} and
 *       {@code outcome} ({@code delivered}, {@code retry}, {@code defer},
 *       {@code dead_letter}) and {@value #METER_DEAD_LETTER} labeled by
 *       {@code realm} and {@code cause}.</li>
 *   <li>{@value #METER_DEPTH} gauge labeled by {@code realm} and
 *       {@code status}, refreshed once per tick from one grouped
 *       aggregate ({@link OutboxStore#countStatusesGroupedByRealm}) so
 *       scrapes never touch the database; the value is therefore up to
 *       one tick behind.</li>
 * </ul>
 *
 * <p>Cardinality policy: labels are {@code kind}, {@code realm} and a
 * small enum, never the owner. A consumer whose owners are few and
 * stable (SSF receivers) may add owner-labeled meters by overriding
 * the transition callbacks and calling {@code super}; one with many
 * owners (webhooks) must not.
 *
 * <p>The {@code realm} label is the realm name, falling back to the
 * raw id when the realm no longer exists. Override {@link #depthKinds()}
 * when the depth snapshot should span more kinds than the drainer's
 * own (SSF: push and poll).
 *
 * <p>Meter registration failures are logged and swallowed — metrics
 * are best-effort and never affect draining.
 */
public class OutboxMetricsListener implements OutboxDrainerListener {

    private static final Logger log = Logger.getLogger(OutboxMetricsListener.class);

    public static final String PREFIX = "keycloak.outbox.";
    public static final String METER_TICK = PREFIX + "drainer.tick";
    public static final String METER_TICK_DURATION = PREFIX + "drainer.tick.duration";
    public static final String METER_TICK_LAST_AT = PREFIX + "drainer.tick.last_at_seconds";
    public static final String METER_TRANSITIONS = PREFIX + "transitions";
    public static final String METER_DEAD_LETTER = PREFIX + "dead_letter";
    public static final String METER_DEPTH = PREFIX + "depth";

    public static final String TAG_KIND = "kind";
    public static final String TAG_REALM = "realm";
    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_CAUSE = "cause";
    public static final String TAG_STATUS = "status";

    /** Key of the depth snapshot. */
    public record KindRealmStatus(String kind, String realm, OutboxEntryStatus status) {
    }

    protected final String entryKind;
    protected final MeterRegistry registry;
    protected final Function<KeycloakSession, OutboxStore> storeFactory;

    private volatile Map<KindRealmStatus, Long> depthSnapshot = Map.of();
    private final ConcurrentHashMap<KindRealmStatus, Boolean> registeredDepthGauges = new ConcurrentHashMap<>();
    private volatile long lastTickEpochSeconds;

    public OutboxMetricsListener(String entryKind, Function<KeycloakSession, OutboxStore> storeFactory) {
        this(entryKind, storeFactory, Metrics.globalRegistry);
    }

    public OutboxMetricsListener(String entryKind, Function<KeycloakSession, OutboxStore> storeFactory, MeterRegistry registry) {
        this.entryKind = Objects.requireNonNull(entryKind, "entryKind");
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory");
        this.registry = Objects.requireNonNull(registry, "registry");
        register(() -> Gauge.builder(METER_TICK_LAST_AT, this, l -> l.lastTickEpochSeconds)
                .description("Epoch second of the most recent outbox drainer tick for this kind; 0 before the first tick.")
                .baseUnit("seconds")
                .tag(TAG_KIND, entryKind)
                .register(registry));
    }

    // -- OutboxDrainerListener ---------------------------------------------

    @Override
    public void onTickEnd(KeycloakSession session, OutboxDrainerTickSummary summary) {
        register(() -> {
            counter(METER_TICK, Tags.of(TAG_KIND, entryKind)).increment();
            Timer.builder(METER_TICK_DURATION)
                    .description("Outbox drainer tick duration.")
                    .tag(TAG_KIND, entryKind)
                    .register(registry)
                    .record(summary.duration());
        });
        lastTickEpochSeconds = Instant.now().getEpochSecond();
        try {
            updateDepthSnapshot(snapshotDepth(session));
        } catch (RuntimeException e) {
            log.debugf(e, "Outbox depth snapshot failed for kind=%s", entryKind);
        }
    }

    @Override
    public void onDelivered(KeycloakSession session, OutboxEntry row) {
        transition(session, row, "delivered");
    }

    @Override
    public void onRetryScheduled(KeycloakSession session, OutboxEntry row, Instant nextAttemptAt, String reason) {
        transition(session, row, "retry");
    }

    @Override
    public void onDeferred(KeycloakSession session, OutboxEntry row, Instant notBefore, String reason) {
        transition(session, row, "defer");
    }

    @Override
    public void onDeadLetter(KeycloakSession session, OutboxEntry row, DeadLetterCause cause, String reason) {
        String realm = realmLabel(session, row.getRealmId());
        transition(realm, "dead_letter");
        register(() -> counter(METER_DEAD_LETTER, Tags.of(
                TAG_KIND, row.getEntryKind(),
                TAG_REALM, realm,
                TAG_CAUSE, cause.name().toLowerCase())).increment());
    }

    // -- hooks -------------------------------------------------------------

    /**
     * Kinds the depth snapshot covers; the drainer's own kind by
     * default.
     */
    protected List<String> depthKinds() {
        return List.of(entryKind);
    }

    /**
     * Realm name for the label, falling back to the id when the realm
     * is gone or no session is available.
     */
    protected String realmLabel(KeycloakSession session, String realmId) {
        if (session != null && realmId != null) {
            try {
                RealmModel realm = session.realms().getRealm(realmId);
                if (realm != null) {
                    return realm.getName();
                }
            } catch (RuntimeException e) {
                log.debugf(e, "Could not resolve realm %s for outbox metrics label", realmId);
            }
        }
        return safe(realmId);
    }

    /**
     * Per-realm, per-status row counts of one kind. Separated so the
     * aggregate source can be replaced (tests, custom stores).
     */
    protected Map<String, Map<OutboxEntryStatus, Long>> countDepth(KeycloakSession session, String kind) {
        return storeFactory.apply(session).countStatusesGroupedByRealm(kind);
    }

    // -- internals ---------------------------------------------------------

    protected void transition(KeycloakSession session, OutboxEntry row, String outcome) {
        transition(realmLabel(session, row.getRealmId()), outcome);
    }

    protected void transition(String realm, String outcome) {
        register(() -> counter(METER_TRANSITIONS, Tags.of(
                TAG_KIND, entryKind,
                TAG_REALM, realm,
                TAG_OUTCOME, outcome)).increment());
    }

    protected Map<KindRealmStatus, Long> snapshotDepth(KeycloakSession session) {
        Map<KindRealmStatus, Long> snapshot = new HashMap<>();
        for (String kind : depthKinds()) {
            for (Map.Entry<String, Map<OutboxEntryStatus, Long>> realm : countDepth(session, kind).entrySet()) {
                String realmLabel = realmLabel(session, realm.getKey());
                for (Map.Entry<OutboxEntryStatus, Long> status : realm.getValue().entrySet()) {
                    snapshot.merge(new KindRealmStatus(kind, realmLabel, status.getKey()), status.getValue(), Long::sum);
                }
            }
        }
        return snapshot;
    }

    /**
     * Swaps the snapshot the depth gauges read from and registers a
     * gauge for every key not seen before. Keys that disappear from a
     * later snapshot read as 0 rather than being unregistered.
     */
    protected void updateDepthSnapshot(Map<KindRealmStatus, Long> snapshot) {
        this.depthSnapshot = snapshot == null ? Map.of() : snapshot;
        for (KindRealmStatus key : this.depthSnapshot.keySet()) {
            if (registeredDepthGauges.putIfAbsent(key, Boolean.TRUE) != null) {
                continue;
            }
            boolean registered = register(() -> Gauge.builder(METER_DEPTH, this, l -> {
                        Long v = l.depthSnapshot.get(key);
                        return v == null ? 0.0 : v.doubleValue();
                    })
                    .description("Outbox row count, snapshot from the last drainer tick.")
                    .baseUnit(BaseUnits.ROWS)
                    .tags(Tags.of(TAG_KIND, key.kind(), TAG_REALM, key.realm(), TAG_STATUS, key.status().name()))
                    .register(registry));
            if (!registered) {
                registeredDepthGauges.remove(key);
            }
        }
    }

    protected Counter counter(String name, Tags tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }

    /** Runs a meter operation, logging and swallowing failures. */
    protected boolean register(Runnable meterOperation) {
        try {
            meterOperation.run();
            return true;
        } catch (RuntimeException e) {
            log.debugf(e, "Outbox meter operation failed for kind=%s", entryKind);
            return false;
        }
    }

    protected static String safe(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }
}
