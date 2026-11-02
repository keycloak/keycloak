package org.keycloak.testframework.tests.providers.single;

import java.io.Serializable;
import java.util.Collection;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
abstract class AbstractSingleProviderTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void providerDeployedTest() {
        ExpectedProvider expectedProvider = getExpectedProvider();
        Class<? extends ProviderFactory> expectedProviderFactoryClass = expectedProvider.expectedProviderFactoryClass();
        Class<? extends Provider> baseProviderClass = expectedProvider.baseProviderClass();
        String expectedProviderId = expectedProvider.expectedProviderId();

        runOnServer.run(session -> {
            ProviderFactory providerFactory = session.getKeycloakSessionFactory().getProviderFactory(baseProviderClass, expectedProviderId);
            assertNotNull(providerFactory);
            assertInstanceOf(expectedProviderFactoryClass, providerFactory);
        });
    }

    @Test
    void providersIgnoredTest() {
        Collection<ExpectedProvider> ignoredProviderClasses = getIgnoredProviderClasses();

        runOnServer.run(session -> {
            for (ExpectedProvider ignoredProvider : ignoredProviderClasses) {
                Class<? extends Provider> baseProviderClass = ignoredProvider.baseProviderClass();
                String ignoredProviderId = ignoredProvider.expectedProviderId();

                ProviderFactory providerFactory = session.getKeycloakSessionFactory().getProviderFactory(baseProviderClass, ignoredProviderId);
                assertNull(providerFactory);
            }
        });
    }

    abstract ExpectedProvider getExpectedProvider();

    abstract Collection<ExpectedProvider> getIgnoredProviderClasses();

    record ExpectedProvider(
            Class<? extends Provider> baseProviderClass,
            String expectedProviderId,
            Class<? extends ProviderFactory> expectedProviderFactoryClass
    ) implements Serializable {}
}
