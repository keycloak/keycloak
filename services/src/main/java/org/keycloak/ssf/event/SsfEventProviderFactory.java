package org.keycloak.ssf.event;

import java.util.Collection;
import java.util.Set;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory contract for {@link SsfEventProvider} implementations.
 *
 * <p>Each registered factory contributes a set of {@link SsfEvent} instances
 * via {@link #getContributedEvents()}. At startup, the contributions of every
 * registered factory are merged into a single {@link SsfEventRegistry}, which
 * is then exposed through {@link SsfEventProvider#getRegistry()}.
 *
 * <p>To register custom events, drop a factory that implements this interface
 * into {@code META-INF/services/org.keycloak.protocol.ssf.event.SsfEventProviderFactory}
 * and return your additional events from {@link #getContributedEvents()}.
 */
public interface SsfEventProviderFactory extends ProviderFactory<SsfEventProvider> {

    /**
     * Returns the set of {@link SsfEvent} instances this factory contributes
     * to the global event registry. Called once at startup, after
     * {@link ProviderFactory#init(org.keycloak.Config.Scope)} but before
     * {@link ProviderFactory#postInit(KeycloakSessionFactory)}.
     *
     * <p>The default implementation returns an empty set.
     */
    default Set<SsfEvent> getContributedEvents() {
        return Set.of();
    }

    /**
     * Returns the subset of {@link #getContributedEvents()} that this
     * extension actively emits from the transmitter (i.e. a Keycloak
     * event or other transmitter-side trigger is wired to produce an
     * SSF Security Event Token carrying the returned event type URI).
     *
     * <p>Events contributed purely for inbound parsing on the receiver
     * side MUST NOT be returned here. The transmitter aggregates the
     * contributions of every registered factory into the "default
     * supported events" set advertised to receiver clients that do
     * not configure their own
     * {@code ssf.supportedEvents} attribute. Advertising an event that
     * the transmitter cannot actually emit would mislead receivers.
     *
     * <p>The default implementation returns an empty set.
     */
    default Set<String> getEmittableEventTypes() {
        return Set.of();
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
