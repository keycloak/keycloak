package org.keycloak.testframework.tests.providers.single;

import java.util.Collection;
import java.util.List;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = ProviderAndSpiTest.KeycloakServerConfig.class)
class ProviderAndSpiTest extends AbstractSingleProviderTest {

    @Override
    ExpectedProvider getExpectedProvider() {
        return new ExpectedProvider(ProviderAndSpiProvider.class, ProviderAndSpiProviderFactoryImpl.ID, ProviderAndSpiProviderFactoryImpl.class);
    }

    @Override
    Collection<ExpectedProvider> getIgnoredProviderClasses() {
        return List.of(
                new ProviderWithResourceTest().getExpectedProvider(),
                new ProviderIsAlsoProviderFactoryTest().getExpectedProvider(),
                new ProviderWithExtraResourcesTest().getExpectedProvider()
        );
    }

    final static class KeycloakServerConfig implements org.keycloak.testframework.server.KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.deploy(ProviderAndSpiProviderFactoryImpl.class, ProviderAndSpiSpi.class);
        }
    }
}
