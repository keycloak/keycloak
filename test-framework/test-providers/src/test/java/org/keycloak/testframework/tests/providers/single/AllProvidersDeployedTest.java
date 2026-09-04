package org.keycloak.testframework.tests.providers.single;

import java.util.List;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.tests.providers.MyCustomRealmResourceProviderFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = AllProvidersDeployedTest.KeycloakServerConfig.class)
class AllProvidersDeployedTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void allProvidersDeployedTest() {
        List<AbstractSingleProviderTest.ExpectedProvider> allProviders = List.of(
                new ProviderWithResourceTest().getExpectedProvider(),
                new ProviderIsAlsoProviderFactoryTest().getExpectedProvider(),
                new ProviderAndSpiTest().getExpectedProvider(),
                new ProviderWithExtraResourcesTest().getExpectedProvider(),
                new AbstractSingleProviderTest.ExpectedProvider(RealmResourceProvider.class, MyCustomRealmResourceProviderFactory.ID, MyCustomRealmResourceProviderFactory.class)
        );

        runOnServer.run(session -> {
            for (AbstractSingleProviderTest.ExpectedProvider expectedProvider : allProviders) {
                Class<? extends ProviderFactory> expectedProviderFactoryClass = expectedProvider.expectedProviderFactoryClass();
                Class<? extends Provider> baseProviderClass = expectedProvider.baseProviderClass();
                String expectedProviderId = expectedProvider.expectedProviderId();


                ProviderFactory providerFactory = session.getKeycloakSessionFactory().getProviderFactory(baseProviderClass, expectedProviderId);
                assertNotNull(providerFactory);
                assertInstanceOf(expectedProviderFactoryClass, providerFactory);
            }
        });
    }


    final static class KeycloakServerConfig implements org.keycloak.testframework.server.KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .deploy(ProviderWithResourceProviderFactory.class)
                    .deploy(ProviderIsAlsoProviderFactory.class)
                    .deploy(ProviderAndSpiProviderFactoryImpl.class, ProviderAndSpiSpi.class)
                    .deploy(ProviderWithExtraResourcesProviderFactory.class, "META-INF/keycloak-themes.json", "theme/custom-account-provider/account/theme.properties")
                    .deploy(MyCustomRealmResourceProviderFactory.class);
        }
    }
}
