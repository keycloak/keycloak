package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

/**
 * Schedules the SSF push outbox drainer on server startup.
 *
 * <p>Hooks into the {@link EventListenerProviderFactory#postInit(KeycloakSessionFactory)
 * postInit} lifecycle — the same place Keycloak's workflow engine
 * installs its step-runner timer — to register a
 * {@link ClusterAwareScheduledTaskRunner} with the
 * {@link TimerProvider}. In an HA deployment the cluster-aware wrapper
 * ensures only one node drains per interval even though every node
 * registers the timer.
 *
 * <p>The {@link EventListenerProvider} instance this factory produces
 * is a deliberate no-op — we're not actually listening for user or
 * admin events here, we're only piggy-backing on the EventListener SPI
 * lifecycle as a convenient, feature-gated startup hook.
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@code spi-events-listener--ssf-push-outbox-drainer--interval}
 *       — how often the drainer ticks (default {@code 30s}).</li>
 *   <li>{@code spi-events-listener--ssf-push-outbox-drainer--batch-size}
 *       — max rows processed per tick (default {@code 50}).</li>
 *   <li>{@code spi-events-listener--ssf-push-outbox-drainer--max-attempts}
 *       — retries before dead-lettering (default
 *       {@value SsfPushOutboxBackoff#DEFAULT_MAX_ATTEMPTS}).</li>
 * </ul>
 */
public class SsfPushOutboxListenerFactory
        implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger log = Logger.getLogger(SsfPushOutboxListenerFactory.class);

    public static final String ID = "ssf-push-outbox-drainer";

    private static final long DEFAULT_INTERVAL_MILLIS = Duration.ofSeconds(30).toMillis();
    private static final int DEFAULT_BATCH_SIZE = 50;

    private long intervalMillis = DEFAULT_INTERVAL_MILLIS;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int maxAttempts = SsfPushOutboxBackoff.DEFAULT_MAX_ATTEMPTS;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return NOOP_LISTENER;
    }

    @Override
    public void init(Config.Scope config) {
        String intervalStr = config.get("interval");
        if (intervalStr != null) {
            this.intervalMillis = parseDurationMillis(intervalStr, DEFAULT_INTERVAL_MILLIS);
        }
        Integer batchStr = config.getInt("batch-size");
        if (batchStr != null && batchStr > 0) {
            this.batchSize = batchStr;
        }
        Integer attempts = config.getInt("max-attempts");
        if (attempts != null && attempts > 0) {
            this.maxAttempts = attempts;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (!Profile.isFeatureEnabled(Profile.Feature.SSF)) {
            return;
        }
        SsfPushOutboxBackoff backoff = new SsfPushOutboxBackoff(maxAttempts);
        SsfPushOutboxDrainerTask task = new SsfPushOutboxDrainerTask(batchSize, backoff);

        try (KeycloakSession session = factory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            ClusterAwareScheduledTaskRunner runner =
                    new ClusterAwareScheduledTaskRunner(factory, task, intervalMillis);
            timer.schedule(runner, intervalMillis, intervalMillis, task.getClass().getSimpleName());
            log.infof("SSF push outbox drainer scheduled: interval=%sms, batchSize=%d, maxAttempts=%d",
                    intervalMillis, batchSize, maxAttempts);
        }

        // Cascade-purge outbox rows when a realm is removed. REALM_ID is
        // not a foreign key on the SSF_PENDING_EVENT table (the table is
        // plugin-contributed via JpaEntityProvider, so it can't declare
        // FKs into core Keycloak tables), so we handle the cleanup
        // explicitly in-transaction with the realm delete.
        factory.register(realmRemovedPurgeListener);
    }

    private final ProviderEventListener realmRemovedPurgeListener = new ProviderEventListener() {
        @Override
        public void onEvent(ProviderEvent event) {
            if (event instanceof RealmModel.RealmRemovedEvent ev) {
                try {
                    new SsfOutboxStore(ev.getKeycloakSession())
                            .deleteByRealm(ev.getRealm().getId());
                } catch (RuntimeException e) {
                    // Don't block realm removal on outbox cleanup failures —
                    // orphaned rows are harmless (the drainer dead-letters
                    // them once it can't resolve the realm) and we don't
                    // want to leave the realm half-deleted.
                    log.warnf(e, "SSF outbox realm-removed cleanup failed for realm %s",
                            ev.getRealm().getId());
                }
            }
        }
    };

    @Override
    public void close() {
        // NOOP — timer task is unregistered implicitly with the session factory.
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    private static long parseDurationMillis(String value, long defaultMillis) {
        // Minimal parser: supports "30s", "500ms", "2m", "1h" suffixes,
        // falling back to seconds when no unit is given. Keeps this
        // module free of a dependency on keycloak-common's
        // DurationConverter (which lives behind keycloak-server-spi-private
        // visibility boundaries we may not want to import here).
        try {
            String trimmed = value.trim().toLowerCase();
            if (trimmed.endsWith("ms")) {
                return Long.parseLong(trimmed.substring(0, trimmed.length() - 2).trim());
            }
            if (trimmed.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            if (trimmed.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            if (trimmed.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            return Duration.ofSeconds(Long.parseLong(trimmed)).toMillis();
        } catch (NumberFormatException e) {
            log.warnf("Invalid SSF push outbox interval '%s' — falling back to default %dms",
                    value, defaultMillis);
            return defaultMillis;
        }
    }

    private static final EventListenerProvider NOOP_LISTENER = new EventListenerProvider() {
        @Override public void onEvent(Event event) { /* not a real event listener */ }
        @Override public void onEvent(AdminEvent event, boolean includeRepresentation) { /* not a real event listener */ }
        @Override public void close() { }
    };
}
