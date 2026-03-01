package org.keycloak.tests.admin.client.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.PatchTypeNames;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
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
import static org.hamcrest.Matchers.containsString;
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
public class ClientApiV2AuthorizationTest extends AbstractClientApiV2Test {
    private static final String FGAP_USER_ID = "00000000000000000000";

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectRealm(config = AuthorizationRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient adminPermissionClient;

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
            assertThat(response.getStatusLine().getStatusCode(), is(201));
        }

        // manage-clients: should be able to create clients (has requireManage)
        request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("manage-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-manage", "new-role1", "new-role2"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode(), is(201));
        }

        // view-clients: should get 403 (lacks requireManage)
        request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request, adminClients.get("view-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-view", "new-role1", "new-role2"))));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // query-clients: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // no-access: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
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
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // no-access: should get 403 (lacks requireView)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
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
            assertThat(response.getStatusLine().getStatusCode(), is(404));
        }

        // manage-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
        }

        // query-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
        }

        // no-access: should get 403 (lacks canList, prevents ID phishing)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("HTTP 403 Forbidden"));
        }
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
            assertThat(response.getStatusLine().getStatusCode(), is(200));
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
            assertThat(response.getStatusLine().getStatusCode(), is(200));
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
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // query-clients: should get 403 (lacks requireConfigure)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
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
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-patch-view");
        request = new HttpPatch(getClientsApiUrl() + "/test-patch-view");
        setAuthHeader(request, adminClients.get("view-clients"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // query-clients: should get 403 (lacks requireConfigure)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // manage-clients: should be able to patch clients (has manage-clients)
        setAuthHeader(request, adminClients.get("manage-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // does not exist
        request = new HttpPatch(getClientsApiUrl() + "/does-not-exist");
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        patch = new OIDCClientRepresentation();
        patch.setDescription("Patched-non-existing");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

        // no-access: not existing - should get 403 (lacks canList, prevents ID phishing)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("HTTP 403 Forbidden"));
        }

        // view-clients: not existing - should get 404
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
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
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // query-clients: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("query-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // no-access: should get 403 (lacks requireManage)
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // no-access: not existing - should get 403 (lacks canList, prevents ID phishing)
        request = new HttpDelete(getClientsApiUrl() + "/does-not-exist");
        setAuthHeader(request, adminClients.get("no-access"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("HTTP 403 Forbidden"));
        }

        // view-clients: not existing - should get 404
        setAuthHeader(request, adminClients.get("view-clients"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
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
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // Test accessing non-existent realm - should return 404
        String nonExistentRealmUrl = "http://localhost:8080/admin/realms/non-existent-realm";
        request = new HttpGet(nonExistentRealmUrl);
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
        }

        // When accessing a different realm from a non-administration realm, should return 403
        // Note: This test validates that when authenticated to 'authztest' realm (which is not an admin realm),
        // trying to access another realm's admin resource should be forbidden
        String differentRealmUrl = "http://localhost:8080/admin/realms/master";
        request = new HttpGet(differentRealmUrl);
        setAuthHeader(request, adminClients.get("realm-admin"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }
    }

    @Test
    public void fgapGetClient() throws Exception {
        createTestClient("fgap-view-test");

        // BEFORE permission: fgap-user cannot view fgap-view-test (403)
        HttpGet request = new HttpGet(getClientsApiUrl() + "/fgap-view-test");
        setAuthHeader(request, adminClients.get("fgap-user"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // Create FGAP permission for fgap-view-test
        createFgapPermissionForClient("fgap-view-test");

        // AFTER permission: fgap-user CAN view fgap-view-test (200)
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // fgap-user CANNOT view fgap-denied-client (no permission granted) (403)
        request = new HttpGet(getClientsApiUrl() + "/fgap-denied-client");
        setAuthHeader(request, adminClients.get("fgap-user"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }
    }

    @Test
    public void fgapUpdateClient() throws Exception {
        createTestClient("fgap-update-test");

        // BEFORE permission: fgap-user cannot update fgap-update-test (403)
        HttpPut request = new HttpPut(getClientsApiUrl() + "/fgap-update-test");
        setAuthHeader(request, adminClients.get("fgap-user"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("fgap-update-test", "updated-role"))));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // Create FGAP permission for fgap-update-test
        createFgapPermissionForClient("fgap-update-test");

        // AFTER permission: fgap-user CAN update fgap-update-test (200)
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // fgap-user CANNOT update fgap-denied-client (no permission granted) (403)
        request = new HttpPut(getClientsApiUrl() + "/fgap-denied-client");
        setAuthHeader(request, adminClients.get("fgap-user"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("fgap-denied-client", "updated-role"))));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }
    }

    @Test
    public void fgapPatchClient() throws Exception {
        createTestClient("fgap-patch-test");

        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched by FGAP user");

        // BEFORE permission: fgap-user cannot patch fgap-patch-test (403)  
        HttpPatch request = new HttpPatch(getClientsApiUrl() + "/fgap-patch-test");
        setAuthHeader(request, adminClients.get("fgap-user"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // Create FGAP permission for fgap-patch-test
        createFgapPermissionForClient("fgap-patch-test");

        // AFTER permission: fgap-user CAN patch fgap-patch-test (200)
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }

        // fgap-user CANNOT patch fgap-denied-client (no permission granted) (403)
        request = new HttpPatch(getClientsApiUrl() + "/fgap-denied-client");
        setAuthHeader(request, adminClients.get("fgap-user"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        patch.setDescription("Should not be patched");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }
    }

    @Test
    public void fgapDeleteClient() throws Exception {
        createTestClient("fgap-delete-test");

        // BEFORE permission: fgap-user cannot delete fgap-delete-test (403)
        HttpDelete request = new HttpDelete(getClientsApiUrl() + "/fgap-delete-test");
        setAuthHeader(request, adminClients.get("fgap-user"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
        }

        // Create FGAP permission for fgap-delete-test
        createFgapPermissionForClient("fgap-delete-test");

        // AFTER permission: fgap-user CAN delete fgap-delete-test (204)
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(204));
        }

        // fgap-user CANNOT delete fgap-denied-client (no permission granted) (403)
        request = new HttpDelete(getClientsApiUrl() + "/fgap-denied-client");
        setAuthHeader(request, adminClients.get("fgap-user"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(403));
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
            assertThat(response.getStatusLine().getStatusCode(), is(201));
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

    private void createFgapPermissionForClient(String clientId) throws Exception {
        createFgapPermissionForClient(clientId, FGAP_USER_ID, AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW);
    }

    /**
     * Creates a fine-grained permission for a specific client.
     * This grants MANAGE and VIEW scopes for the specified client.
     */
    private void createFgapPermissionForClient(String clientId, String userId, String... scopes) throws Exception {
        var clientUuid = Optional.ofNullable(testRealm.admin().clients().findByClientId(clientId))
                .filter(f -> !f.isEmpty())
                .map(f -> f.get(0))
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new AssertionError("Cannot find client"));

        // Create user policy for fgap-user
        UserPolicyRepresentation userPolicy = new UserPolicyRepresentation();
        userPolicy.setName("fgap-user-policy-" + clientUuid);
        userPolicy.setUsers(Set.of(userId));
        userPolicy.setLogic(Logic.POSITIVE);

        String userPolicyId;
        try (var response = adminPermissionClient.admin().authorization().policies().user().create(userPolicy)) {
            assertThat(response.getStatusInfo().getStatusCode(), is(201));
            userPolicyId = response.readEntity(UserPolicyRepresentation.class).getId();
        }

        // Create scope permission for the specific client
        ScopePermissionRepresentation scopePermission = new ScopePermissionRepresentation();
        scopePermission.setName("fgap-permission-" + clientUuid);
        scopePermission.setResourceType(AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE);
        scopePermission.setResources(Set.of(clientUuid));
        scopePermission.setScopes(Set.of(scopes));
        scopePermission.setPolicies(Set.of(userPolicyId));
        scopePermission.setLogic(Logic.POSITIVE);

        try (var response = adminPermissionClient.admin().authorization().permissions().scope().create(scopePermission)) {
            assertThat(response.getStatusInfo().getStatusCode(), is(201));
        }
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
            realm.adminPermissionsEnabled(true);
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

            // FGAP v2
            // fgap-user has QUERY_CLIENTS role but will be granted fine-grained permissions for specific clients
            realm.addUser("fgap-user")
                    .id(FGAP_USER_ID)
                    .name("FGAP", "User")
                    .email("fgapuser@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS);
            CURRENT_USERS.add("fgap-user");

            realm.addClient("fgap-denied-client")
                    .enabled(true);

            return realm;
        }
    }
}
