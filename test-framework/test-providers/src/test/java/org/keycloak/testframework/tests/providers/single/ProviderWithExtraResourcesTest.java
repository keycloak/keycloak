package org.keycloak.testframework.tests.providers.single;

import java.util.Collection;
import java.util.List;

import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = ProviderWithExtraResourcesTest.KeycloakServerConfig.class)
class ProviderWithExtraResourcesTest extends AbstractSingleProviderTest {

    @Override
    ExpectedProvider getExpectedProvider() {
        return new ExpectedProvider(AccountResourceProvider.class, ProviderWithExtraResourcesProviderFactory.ID, ProviderWithExtraResourcesProviderFactory.class);
    }

    @Override
    Collection<ExpectedProvider> getIgnoredProviderClasses() {
        return List.of(
                new ProviderWithResourceTest().getExpectedProvider(),
                new ProviderIsAlsoProviderFactoryTest().getExpectedProvider(),
                new ProviderAndSpiTest().getExpectedProvider()
        );
    }

    final static class KeycloakServerConfig implements org.keycloak.testframework.server.KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.deploy(ProviderWithExtraResourcesProviderFactory.class, "META-INF/keycloak-themes.json", "theme/custom-account-provider/account/theme.properties");
        }

    }
}
