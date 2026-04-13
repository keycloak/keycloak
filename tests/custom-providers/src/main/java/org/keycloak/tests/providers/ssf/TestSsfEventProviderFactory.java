package org.keycloak.tests.providers.ssf;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.event.DefaultSsfEventProvider;
import org.keycloak.protocol.ssf.event.SsfEvent;
import org.keycloak.protocol.ssf.event.SsfEventProvider;
import org.keycloak.protocol.ssf.event.SsfEventProviderFactory;
import org.keycloak.protocol.ssf.event.SsfEventRegistry;

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

    private volatile SsfEventRegistry registry;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<SsfEvent> getContributedEvents() {
        return Set.of(new TestSsfEvent());
    }

    @Override
    public SsfEventProvider create(KeycloakSession session) {
        return new DefaultSsfEventProvider(registry);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.registry = SsfEventProviderFactory.buildRegistry(factory);
    }

    @Override
    public void close() {
        // no-op
    }
}
