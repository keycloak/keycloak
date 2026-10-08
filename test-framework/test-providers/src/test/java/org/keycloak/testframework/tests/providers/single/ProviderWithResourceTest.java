package org.keycloak.testframework.tests.providers.single;

import java.util.Collection;
import java.util.List;

import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = ProviderWithResourceTest.KeycloakServerConfig.class)
class ProviderWithResourceTest extends AbstractSingleProviderTest {

    @Override
    ExpectedProvider getExpectedProvider() {
        return new ExpectedProvider(RealmResourceProvider.class, ProviderWithResourceProviderFactory.ID, ProviderWithResourceProviderFactory.class);
    }

    @Override
    Collection<ExpectedProvider> getIgnoredProviderClasses() {
        return List.of(
                new ProviderAndSpiTest().getExpectedProvider(),
                new ProviderIsAlsoProviderFactoryTest().getExpectedProvider(),
                new ProviderWithExtraResourcesTest().getExpectedProvider()
        );
    }

    final static class KeycloakServerConfig implements org.keycloak.testframework.server.KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.deploy(ProviderWithResourceProviderFactory.class);
        }
    }
}
