package org.keycloak.tests.cors;

import java.io.IOException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.services.cors.Cors;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class AdminCorsTest {

    private static final String VALID_ORIGIN = "https://valid-origin.example.com";
    private static final String INVALID_ORIGIN = "https://evil.example.com";

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private String token;
    private String usersUrl;

    @BeforeEach
    public void setup() {
        adminClient.realm("master").clients().findByClientId("security-admin-console").forEach(client -> {
            if (!client.getWebOrigins().contains(VALID_ORIGIN)) {
                client.getWebOrigins().add(VALID_ORIGIN);
                adminClient.realm("master").clients().get(client.getId()).update(client);
            }
        });

        token = adminClient.tokenManager().getAccessTokenString();
        usersUrl = keycloakUrls.getAdminBuilder().path("/realms/master/users").build().toString();
    }

    @Test
    public void validOriginReturnsHeaders() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(usersUrl + "?max=1")
                .auth(token).header("Origin", VALID_ORIGIN).asResponse()) {
            assertEquals(200, response.getStatus());
            assertEquals(VALID_ORIGIN, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
            assertEquals("true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }
    }

    @Test
    public void invalidOriginIsRejected() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(usersUrl + "?max=1")
                .auth(token).header("Origin", INVALID_ORIGIN).asResponse()) {
            assertEquals(403, response.getStatus());
            assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    public void noOriginHeaderIsUnaffected() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(usersUrl + "?max=1")
                .auth(token).asResponse()) {
            assertEquals(200, response.getStatus());
            assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    public void preflightRejectsInvalidOrigin() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doOptions(usersUrl)
                .header("Origin", INVALID_ORIGIN).asResponse()) {
            assertEquals(200, response.getStatus());
            assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
