package org.keycloak.tests.providers.ssf;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.ssf.event.DefaultSsfEventProvider;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.event.SsfEventProvider;
import org.keycloak.ssf.event.SsfEventProviderFactory;
import org.keycloak.ssf.event.SsfEventRegistry;

/**
 * Test-only {@link SsfEventProviderFactory} that contributes a single
 * custom event ({@link TestSsfEvent}) to the global SSF event registry.
 *
 * <p>Deployed into the embedded test server via the
 * {@code keycloak-tests-custom-providers} module and picked up through
 * {@code META-INF/services/org.keycloak.protocol.ssf.event.SsfEventProviderFactory}.
 *
 * <p>Runtime resolution of {@code SsfEventProvider} still picks the
 * built-in {@code default} factory; this factory's contribution is
 * merged into the aggregate registry by
 * {@link SsfEventProviderFactory#buildRegistry(KeycloakSessionFactory)}.
 */
public class TestSsfEventProviderFactory implements SsfEventProviderFactory {

    public static final String PROVIDER_ID = "test-ssf-events";

    private SsfEventRegistry registry;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, Supplier<? extends SsfEvent>> getContributedEventFactories() {
        return Map.of(TestSsfEvent.TYPE, TestSsfEvent::new);
    }

    @Override
    public Set<String> getEmittableEventTypes() {
        // Declare the custom event as "transmitter-emittable" so it is
        // picked up by DefaultSsfTransmitterProvider.getDefaultSupportedEvents()
        // when the supported-events SPI property is unset. A real extension
        // would also wire up a Keycloak event → TestSsfEvent mapping path;
        // here we just need the registry declaration for the tests that
        // exercise the "default supported events" advertisement.
        return Set.of(TestSsfEvent.TYPE);
    }

    @Override
    public SsfEventProvider create(KeycloakSession session) {
        return new DefaultSsfEventProvider(registry);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.registry = SsfEventProviderFactory.buildRegistry(factory);
    }
}
