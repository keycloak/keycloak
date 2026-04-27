package org.keycloak.ssf.transmitter;

import java.util.Set;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.store.SsfEventStore;

/**
 * Factory-scoped context bundle for the SSF transmitter. Holds the
 * long-lived configuration and shared collaborators that don't depend
 * on a {@link KeycloakSession} — created once at SPI {@code init()}
 * time and reused across every per-session
 * {@link SsfTransmitterProvider} instance.
 *
 * <p>Deliberately does <em>not</em> hold a {@code KeycloakSession}
 * reference — sessions are per-request and bundling them here would
 * make the lifecycle ambiguous and invite use-after-close bugs.
 * Per-session services materialize lazily on the provider; the
 * provider's {@link SsfTransmitterProvider#session() session()}
 * accessor is the single canonical place a session lives.
 */
public final class SsfTransmitterContext {

    private final SsfTransmitterConfig config;
    private final Set<String> defaultSupportedEventAliases;
    private final SsfMetricsBinder metricsBinder;
    private final Function<KeycloakSession, SsfEventStore> pendingEventStoreFactory;
    private final Function<KeycloakSession, String> issuerUrlFactory;
    private final SsfTransmitterServiceBuilder services;

    public SsfTransmitterContext(SsfTransmitterConfig config,
                                 Set<String> defaultSupportedEventAliases,
                                 SsfMetricsBinder metricsBinder,
                                 Function<KeycloakSession, SsfEventStore> pendingEventStoreFactory,
                                 Function<KeycloakSession, String> issuerUrlFactory,
                                 SsfTransmitterServiceBuilder services) {
        this.config = config;
        this.defaultSupportedEventAliases = defaultSupportedEventAliases;
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
        this.pendingEventStoreFactory = pendingEventStoreFactory;
        this.issuerUrlFactory = issuerUrlFactory;
        this.services = services;
    }

    public SsfTransmitterConfig config() {
        return config;
    }

    /**
     * Aliases (or full URIs) of events the transmitter advertises as
     * "default supported" for receivers that don't configure their own
     * list. {@code null} means fall back to every event type known to
     * the registry.
     */
    public Set<String> defaultSupportedEventAliases() {
        return defaultSupportedEventAliases;
    }

    public SsfMetricsBinder metrics() {
        return metricsBinder;
    }

    /**
     * Resolves an {@link SsfEventStore} for the given session.
     * Indirection so test subclasses can plug in a custom store
     * without overriding the entire context.
     */
    public SsfEventStore pendingEventStore(KeycloakSession session) {
        return pendingEventStoreFactory.apply(session);
    }

    /**
     * Function reference variant — passed to constructors that want a
     * {@code Function<KeycloakSession, SsfEventStore>} (e.g.
     * the dispatcher and the stream service for cascade purges).
     */
    public Function<KeycloakSession, SsfEventStore> pendingEventStoreFactory() {
        return pendingEventStoreFactory;
    }

    /**
     * Resolves the realm-scoped issuer URL ({@code iss} claim) for
     * the given session. Captured here so the metadata service and
     * the SET mapper share one source of truth.
     */
    public String issuerUrl(KeycloakSession session) {
        return issuerUrlFactory.apply(session);
    }

    public Function<KeycloakSession, String> issuerUrlFactory() {
        return issuerUrlFactory;
    }

    public SsfTransmitterServiceBuilder services() {
        return services;
    }
}
