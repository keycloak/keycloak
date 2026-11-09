package org.keycloak.tests.oidc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.providers.wellknown.TestMediaTypeWellKnownProvider;
import org.keycloak.tests.providers.wellknown.TestMediaTypeWellKnownProviderFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = WellKnownMediaTypeTest.CustomProvidersServerConfig.class)
public class WellKnownMediaTypeTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void providerMediaType() throws Exception {
        assertProviderMediaType(get(realmUrl(), "*/*"));
    }

    @Test
    public void providerMediaTypeAccepted() throws Exception {
        assertProviderMediaType(get(realmUrl(), TestMediaTypeWellKnownProvider.MEDIA_TYPE));
    }

    @Test
    public void providerMediaTypeViaServerMetadata() throws Exception {
        assertProviderMediaType(get(serverMetadataUrl(), "*/*"));
    }

    @Test
    public void providerMediaTypeAcceptedViaServerMetadata() throws Exception {
        assertProviderMediaType(get(serverMetadataUrl(), TestMediaTypeWellKnownProvider.MEDIA_TYPE));
    }

    @Test
    public void defaultMediaTypeUnchanged() throws Exception {
        String url = realm.getBaseUrl() + "/.well-known/openid-configuration";

        for (String accept : new String[] { "*/*", "application/json" }) {
            HttpResponse<String> response = get(url, accept);
            assertEquals(200, response.statusCode());
            assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null));
        }
    }

    private String realmUrl() {
        return realm.getBaseUrl() + "/.well-known/" + TestMediaTypeWellKnownProviderFactory.ID;
    }

    private String serverMetadataUrl() {
        return keycloakUrls.getBaseUrl() + "/.well-known/" + TestMediaTypeWellKnownProviderFactory.ID + "/realms/" + realm.getName();
    }

    private void assertProviderMediaType(HttpResponse<String> response) {
        assertEquals(200, response.statusCode());
        assertEquals(TestMediaTypeWellKnownProvider.MEDIA_TYPE, response.headers().firstValue("Content-Type").orElse(null));
        assertEquals(TestMediaTypeWellKnownProvider.CONFIG, response.body());
    }

    private HttpResponse<String> get(String url, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Accept", accept).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static class CustomProvidersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
