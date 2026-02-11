package org.keycloak.tests.admin.client.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Authorization tests for Client API V2.
 * <p>
 * These tests verify that the Client API V2 follows the same authorization rules as the Client V1 API.
 * <p>
 * Based on this spike <a href="https://github.com/keycloak/keycloak/issues/45940#issuecomment-3840875460">GitHub Issue #45940</a>
 */
@KeycloakIntegrationTest(config = ClientApiV2AuthorizationTest.AdminV2WithAuthzConfig.class)
public class ClientApiV2AuthorizationTest extends AbstractClientApiV2Test{

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectRealm(config = AuthorizationRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Override
    public String getRealmName() {
        return "authztest";
    }

    private static final Map<String, Keycloak> adminClients = new HashMap<>();

    @BeforeEach
    public void setupClients() {
        if (adminClients.isEmpty()) {
            for (String currentUser : CURRENT_USERS) {
                adminClients.put(currentUser, createAdminClient(currentUser));
            }
        }
    }

    /**
     * GET /clients
     * Permissions: auth.clients().requireList() || auth.clients().requireView()
     */
    @Test
    public void listClients() throws Exception {
        HttpGet request = new HttpGet(getClientsApiUrl());

        // realm-admin: should be able to list clients (has all permissions)
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            var clients = mapper.readValue(response.getEntity().getContent(), new TypeReference<List<BaseClientRepresentation>>() {
            });
            assertThat(clients.size(), greaterThan(0));
        }

        // view-clients: should be able to list clients (has requireList via canView)
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // query-clients: should be able to list clients (has requireList via QUERY_CLIENTS role)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to list clients (has requireList via MANAGE_CLIENTS role)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireList)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * POST /clients
     * Permissions: auth.clients().requireManage() + ClientPolicyEvent.REGISTER
     */
    @Test
    public void createClient() throws Exception {
        // realm-admin: should be able to create clients (has requireManage)
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("realm-admin"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-realm-admin", "new-role1", "new-role2"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to create clients (has requireManage)
        request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("manage-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-manage", "new-role1", "new-role2"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireManage)
        request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("view-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-view", "new-role1", "new-role2"))));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * GET /clients/{client}
     * Permissions: auth.clients().requireView(client) + ClientPolicyEvent.VIEW
     */
    @Test
    public void getClient() throws Exception {
        HttpGet request = new HttpGet(getClientsApiUrl() + "/test-client");

        // view-clients: should be able to get individual client (has requireView)
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-client"));
        }

        // manage-clients: should be able to get individual client (has requireView)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-client"));
        }

        // query-clients: should get 403 (can list but not view individual clients)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireView)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * GET /clients/{client} (client == null)
     * Permissions: if auth.clients().canList() return 404, else return 403
     */
    @Test
    public void getNonExistentClient() throws Exception {
        HttpGet request = new HttpGet(getClientsApiUrl() + "/non-existent-client");

        // view-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // TODO - our V2 logic does not handle the anti-ID phishing yet
        // TODO - issue https://github.com/keycloak/keycloak/issues/46010
        // no-access: should get 403 (lacks canList, prevents ID phishing)
        // setAuthHeader(request, clients.get("no-access"));
        // try (var response = client.execute(request)) {
        //     assertEquals(403, response.getStatusLine().getStatusCode());
        // }
    }

    /**
     * PUT /clients/{client}
     * Permissions: auth.clients().requireConfigure(client) + ClientPolicyEvent.UPDATE + ClientPolicyEvent.UPDATED
     */
    @Test
    public void updateClient() throws Exception {
        // realm-admin: should be able to update clients (has requireConfigure)
        createTestClient("test-update-admin");
        HttpPut request = new HttpPut(getClientsApiUrl() + "/test-update-admin");
        setAuthHeader(request, adminClients.get("realm-admin"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-admin", "role123", "my-role"))));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-update-admin"));
            assertThat(client.getRoles(), containsInAnyOrder("role123", "my-role"));
        }

        // manage-clients: should be able to update clients (has requireConfigure)
        createTestClient("test-update-manage");
        request = new HttpPut(getClientsApiUrl() + "/test-update-manage");
        setAuthHeader(request, adminClients.get("manage-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-manage", "role123", "my-role"))));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-update-manage"));
            assertThat(client.getRoles(), containsInAnyOrder("role123", "my-role"));
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-update-view");
        request = new HttpPut(getClientsApiUrl() + "/test-update-view");
        setAuthHeader(request, adminClients.get("view-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-view", "role123", "my-role"))));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireConfigure)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * PATCH /clients/{client}
     * Permissions: auth.clients().requireConfigure(client) + ClientPolicyEvent.UPDATE + ClientPolicyEvent.UPDATED
     */
    @Test
    public void patchClient() throws Exception {
        // realm-admin: should be able to patch clients (has requireConfigure)
        createTestClient("test-patch-admin");
        HttpPatch request = new HttpPatch(getClientsApiUrl() + "/test-patch-admin");
        setAuthHeader(request, adminClients.get("realm-admin"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/merge-patch+json");
        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-patch-view");
        request = new HttpPatch(getClientsApiUrl() + "/test-patch-view");
        setAuthHeader(request, adminClients.get("view-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/merge-patch+json");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireConfigure)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to patch clients (has manage-clients)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * DELETE /clients/{client}
     * Permissions: auth.clients().requireManage(client) + !isAdminPermissionClient + ClientPolicyEvent.UNREGISTER
     */
    @Test
    public void deleteClient() throws Exception {
        // realm-admin: should be able to delete clients (has requireManage)
        createTestClient("test-delete-admin");
        HttpDelete request = new HttpDelete(getClientsApiUrl() + "/test-delete-admin");
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to delete clients (has requireManage)
        createTestClient("test-delete-manage");
        request = new HttpDelete(getClientsApiUrl() + "/test-delete-manage");
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireManage)
        createTestClient("test-delete-view");
        request = new HttpDelete(getClientsApiUrl() + "/test-delete-view");
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * GET /admin/realms/{realm}
     * Permissions: if realm == null return 404, if !isAdministrationRealm && !auth.realm.equals(realm) return 403
     */
    @Test
    public void getRealmAdmin() throws Exception {
        // Users authenticated to 'authztest' should be able to access 'authztest' realm admin resources
        String ownRealmUrl = "http://localhost:8080/admin/realms/%s".formatted(getRealmName());
        HttpGet request = new HttpGet(ownRealmUrl);

        // should successfully access own realm (200 OK)
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // Test accessing non-existent realm - should return 404
        String nonExistentRealmUrl = "http://localhost:8080/admin/realms/non-existent-realm";
        request = new HttpGet(nonExistentRealmUrl);
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // When accessing a different realm from a non-administration realm, should return 403
        // Note: This test validates that when authenticated to 'authztest' realm (which is not an admin realm),
        // trying to access another realm's admin resource should be forbidden
        String differentRealmUrl = "http://localhost:8080/admin/realms/master";
        request = new HttpGet(differentRealmUrl);
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    private OIDCClientRepresentation createClientRep(String clientId, String... roles) {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(clientId);
        rep.setEnabled(true);
        rep.setRoles(new HashSet<>(List.of(roles)));
        return rep;
    }

    private void createTestClient(String clientId) throws Exception {
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("realm-admin"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep(clientId, "test-role1", "test-role2"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }
    }

    private Keycloak createAdminClient(String username) {
        return adminClientFactory.create()
                .realm(getRealmName())
                .clientId("test-client")
                .clientSecret("test-secret")
                .username(username)
                .password("password")
                .build();
    }

    public static class AdminV2WithAuthzConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }

    private static final Set<String> CURRENT_USERS = new HashSet<>();

    public static class AuthorizationRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("authztest");
            realm.addClient("test-client")
                    .secret("test-secret")
                    .directAccessGrantsEnabled(true);

            // Role: realm-admin
            realm.addUser("realm-admin")
                    .name("Realm", "Admin")
                    .email("realmadmin@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            CURRENT_USERS.add("realm-admin");

            // Role: view-clients
            realm.addUser("view-clients")
                    .name("View", "Clients")
                    .email("viewclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_CLIENTS);
            CURRENT_USERS.add("view-clients");

            // Role: manage-clients
            realm.addUser("manage-clients")
                    .name("Manage", "Clients")
                    .email("manageclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);
            CURRENT_USERS.add("manage-clients");

            // Role: query-clients
            realm.addUser("query-clients")
                    .name("Query", "Clients")
                    .email("queryclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS);
            CURRENT_USERS.add("query-clients");

            // NO role
            realm.addUser("no-access")
                    .name("No", "Access")
                    .email("noaccess@localhost")
                    .emailVerified(true)
                    .password("password");
            CURRENT_USERS.add("no-access");

            // Role: manage-realm
            realm.addUser("manage-realm")
                    .name("Manage", "Realm")
                    .email("managerealm@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM);
            CURRENT_USERS.add("manage-realm");
            return realm;
        }
    }
}
