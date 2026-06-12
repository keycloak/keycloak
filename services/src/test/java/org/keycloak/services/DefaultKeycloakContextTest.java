package org.keycloak.services;

import java.util.HashMap;
import java.util.Optional;

import jakarta.enterprise.context.ContextNotActiveException;

import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.url.HostnameV2ProviderFactory;
import org.keycloak.url.HostnameV2ProviderFactoryTest;
import org.keycloak.urls.HostnameProvider;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultKeycloakContextTest {

    @Test
    public void testNonActiveWithFullUrlHostname() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory() {

            @Override
            public KeycloakSession create() {
                return new DefaultKeycloakSession(this) {

                    @Override
                    protected DefaultKeycloakContext createKeycloakContext(KeycloakSession session) {
                        return new DefaultKeycloakContext(session) {

                            @Override
                            protected Optional<HttpResponse> createHttpResponse() {
                                return Optional.empty();
                            }

                            @Override
                            protected Optional<HttpRequest> createHttpRequest() {
                                return Optional.empty();
                            }
                        };
                    }
                };
            }
        };

        HostnameV2ProviderFactory v2Factory = HostnameV2ProviderFactoryTest.init("https://full.host.name/path");
        HashMap<String, ProviderFactory> map = new HashMap<>();
        map.put(null, v2Factory);

        factory.factoriesMap.put(HostnameProvider.class, map);

        // the following can be inferred from the full hostname, so an exception is not expected
        try (KeycloakSession keycloakSession = factory.create()) {
            KeycloakContext context = keycloakSession.getContext();
            assertNotNull(context.getUri());
            assertEquals("https://full.host.name/path/", context.getAuthServerUrl().toString());
            assertEquals("/path/", context.getContextPath());

            assertEquals(0, context.getConnection().getLocalPort());

            assertThrows(ContextNotActiveException.class, () -> context.getHttpRequest());
        } finally {
            factory.close();
        }
    }

}
