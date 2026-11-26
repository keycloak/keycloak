package org.keycloak.ssf.event;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory contract for {@link SsfEventProvider} implementations.
 *
 * <p>Each registered factory contributes a map of event-type-URI to
 * {@link SsfEvent} factory via {@link #getContributedEventFactories()}. At
 * startup, the contributions of every registered factory are merged into a
 * single {@link SsfEventRegistry}, which is then exposed through
 * {@link SsfEventProvider#getRegistry()}.
 *
 * <p>To register custom events, drop a factory that implements this interface
 * into {@code META-INF/services/org.keycloak.protocol.ssf.event.SsfEventProviderFactory}
 * and return your additional events from {@link #getContributedEventFactories()}.
 */
public interface SsfEventProviderFactory extends ProviderFactory<SsfEventProvider> {

    /**
     * Returns the map of event-type URI to factory for the {@link SsfEvent}
     * instances this provider contributes to the global event registry.
     * Called once at startup, after
     * {@link ProviderFactory#init(org.keycloak.Config.Scope)} but before
     * {@link ProviderFactory#postInit(KeycloakSessionFactory)}.
     *
     * <p>The factory (typically a {@code SomeEvent::new} method reference)
     * lets the registry mint fresh instances at runtime without reflection —
     * used by the synthetic event emitter when the caller doesn't supply an
     * event body. Keying by URI lets the registry skip a probing
     * instantiation just to learn the event type.
     *
     * <p>The default implementation returns an empty map.
     */
    default Map<String, Supplier<? extends SsfEvent>> getContributedEventFactories() {
        return Map.of();
    }

    /**
     * Returns the subset of {@link #getContributedEventFactories()} that the
     * transmitter can actually ship out — either because a Keycloak listener
     * / transmitter-side trigger produces them automatically, or because they
     * can be raised on demand via the admin emit API. Combined, the
     * contributions of every registered factory drive both the default
     * {@code events_supported} set advertised to receiver clients and the
     * whitelist of types the emit API will accept.
     *
     * <p>Events contributed purely for inbound parsing on the receiver
     * side MUST NOT be returned here. Advertising an event the transmitter
     * cannot ship would mislead receivers.
     *
     * <p>The default implementation returns an empty set.
     */
    default Set<String> getEmittableEventTypes() {
        return Set.of();
    }

    /**
     * Returns the further subset of {@link #getEmittableEventTypes()} that the
     * transmitter emits natively — i.e. driven by Keycloak listener / trigger
     * logic rather than only by an explicit admin-API call. The admin UI
     * surfaces this set as the "built-in" badge on event entries: events the
     * operator does not have to script anything to receive.
     *
     * <p>Events that are emittable but only via the admin emit API
     * (no listener wiring on the transmitter side) MUST NOT be returned here.
     *
     * <p>The default implementation falls back to {@link #getEmittableEventTypes()}
     * so existing factories that don't distinguish keep their previous behaviour.
     */
    default Set<String> getNativelyEmittedEventTypes() {
        return getEmittableEventTypes();
    }

    @Override
    default void init(Config.Scope config) {
        // no-op
    }

    @Override
    default void close() {
        // no-op
    }

    /**
     * Most factories registered through this SPI exist only to contribute
     * events to the shared {@link SsfEventRegistry} via
     * {@link #getContributedEventFactories()} — they don't supply a per-
     * session {@link SsfEventProvider} instance because callers resolve
     * {@code SsfEventProvider} through the configured default factory
     * (id {@code "default"}, shipped as {@code DefaultSsfEventProviderFactory}).
     *
     * <p>The default implementation returns {@code null} so contribution-
     * only extensions can keep their factory class to {@code getId()} +
     * {@code isSupported()} + the {@code getContributedEventFactories()}
     * map. Override only when your factory is intended to replace the
     * default provider entirely.
     */
    @Override
    default SsfEventProvider create(KeycloakSession session) {
        return null;
    }

    /**
     * Aggregates the events contributed by all registered
     * {@link SsfEventProviderFactory} instances into a single immutable
     * {@link SsfEventRegistry}.
     */
    static SsfEventRegistry buildRegistry(KeycloakSessionFactory sessionFactory) {
        Collection<? extends SsfEventProviderFactory> factories = sessionFactory
                .getProviderFactoriesStream(SsfEventProvider.class)
                .map(SsfEventProviderFactory.class::cast)
                .toList();

        return SsfEventRegistry.from(factories);
    }
}
