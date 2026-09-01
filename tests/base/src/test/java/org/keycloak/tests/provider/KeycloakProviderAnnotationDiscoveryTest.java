package org.keycloak.tests.provider;

import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.events.EventListenerSpi;
import org.keycloak.provider.KeycloakProvider;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.events.AnnotatedTestEventListenerProviderFactory;
import org.keycloak.tests.providers.events.TestEventsListenerContextDetailsProviderFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check of the build-time provider discovery via {@link KeycloakProvider}.
 * <p>
 * The {@code keycloak-tests-custom-providers} jar is deployed to the server under test. It contains
 * {@link AnnotatedTestEventListenerProviderFactory}, which carries {@link KeycloakProvider} but has no
 * {@code META-INF/services} entry, and {@link TestEventsListenerContextDetailsProviderFactory}, which is
 * registered only through {@code META-INF/services}. Both must be visible in the running server: the former
 * proves the Jandex scan → build item → provider loader chain, the latter that ServiceLoader discovery is
 * unaffected by the new mechanism.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KeycloakProviderAnnotationDiscoveryTest {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void annotatedProviderIsDiscoveredAlongsideServiceLoaderProviders() {
        SpiInfoRepresentation eventListenerSpi = adminClient.serverInfo().getInfo().getProviders().get(new EventListenerSpi().getName());
        assertNotNull(eventListenerSpi, "eventsListener SPI missing from server info");

        Map<String, ProviderRepresentation> providers = eventListenerSpi.getProviders();

        assertTrue(providers.containsKey(AnnotatedTestEventListenerProviderFactory.PROVIDER_ID),
                "provider discovered via @KeycloakProvider (no META-INF/services entry) must be loaded, got: " + providers.keySet());
        assertTrue(providers.containsKey(new TestEventsListenerContextDetailsProviderFactory().getId()),
                "provider discovered via META-INF/services must still be loaded, got: " + providers.keySet());
    }

}
