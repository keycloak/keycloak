package org.keycloak.tests.admin.client.v2;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.api.AdminRootV2;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractClientApiV2Test {
    protected static ObjectMapper mapper;

    @InjectAdminClient
    protected Keycloak adminClient;

    public String getRealmName() {
        return "master";
    }

    public String getClientsApiUrl() {
        return "http://localhost:8080/admin/api/%s/clients/v2".formatted(getRealmName());
    }
    public String getClientApiUrl(String clientId) {
        return getClientApiUrl(getRealmName(), clientId);
    }

    public String getClientApiUrl(String realmName, String clientId) {
        return "http://localhost:8080/admin/api/%s/clients/v2/%s".formatted(realmName, clientId);
    }

    public ClientsApi getClientsApi() {
        return adminClient.proxy(AdminRootV2.class).adminApi(getRealmName()).clientsV2();
    }

    public ClientsApi getClientsApi(String realmName) {
        return getClientsApi(adminClient, realmName);
    }

    public ClientsApi getClientsApi(Keycloak adminClient) {
        return getClientsApi(adminClient, getRealmName());
    }

    public ClientsApi getClientsApi(Keycloak adminClient, String realmName) {
        return adminClient.proxy(AdminRootV2.class).adminApi(realmName).clientsV2();
    }

    public ClientApi getClientApi(String clientId) {
        return getClientApi(getRealmName(), clientId);
    }

    public ClientApi getClientApi(String realmName, String clientId) {
        return getClientApi(adminClient, realmName, clientId);
    }

    public ClientApi getClientApi(Keycloak adminClient, String realmName, String clientId) {
        return adminClient.proxy(AdminRootV2.class).adminApi(realmName).clientsV2().client(clientId);
    }

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    protected void setAuthHeader(HttpMessage request) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    // TODO Rewrite the tests to not need explicit auth. They should use the admin client directly.
    protected static void setAuthHeader(HttpMessage request, Keycloak adminClient) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
