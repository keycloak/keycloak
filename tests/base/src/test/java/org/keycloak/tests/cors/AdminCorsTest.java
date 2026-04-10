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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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

    private String adminToken;
    private String adminUrl;

    @BeforeEach
    public void setup() {
        adminClient.realm("master").clients().findByClientId("security-admin-console").stream()
                .findFirst().ifPresent(client -> {
                    if (!client.getWebOrigins().contains(VALID_ORIGIN)) {
                        client.getWebOrigins().add(VALID_ORIGIN);
                        adminClient.realm("master").clients().get(client.getId()).update(client);
                    }
                });

        adminToken = adminClient.tokenManager().getAccessTokenString();
        adminUrl = keycloakUrls.getBase() + "/admin/realms/master";
    }

    @Test
    public void testAdminApiWithValidOrigin() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(adminUrl + "/users?max=1")
                .auth(adminToken)
                .header("Origin", VALID_ORIGIN)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals(VALID_ORIGIN, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
            Assertions.assertEquals("true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }
    }

    @Test
    public void testAdminApiWithInvalidOrigin() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(adminUrl + "/users?max=1")
                .auth(adminToken)
                .header("Origin", INVALID_ORIGIN)
                .asResponse()) {
            Assertions.assertEquals(403, response.getStatus());
            Assertions.assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    public void testAdminApiWithoutOriginHeader() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doGet(adminUrl + "/users?max=1")
                .auth(adminToken)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    public void testPreflightWithAnyOriginIsBlocked() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doOptions(adminUrl + "/users")
                .header("Origin", INVALID_ORIGIN)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertNull(response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
