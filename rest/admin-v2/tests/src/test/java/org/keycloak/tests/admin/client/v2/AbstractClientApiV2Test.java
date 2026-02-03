package org.keycloak.tests.admin.client.v2;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.Keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractClientApiV2Test {
    protected static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/master/clients/v2";
    protected static ObjectMapper mapper;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    // TODO Rewrite the tests to not need explicit auth. They should use the admin client directly.
    static void setAuthHeader(HttpMessage request, Keycloak adminClient) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
