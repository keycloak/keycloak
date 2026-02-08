package org.keycloak.tests.admin.client.v2;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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

    private static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/authztest/clients/v2";

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectRealm(config = AuthorizationRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClient(ref = "realmAdminClient", client = "test-client", user = "realm-admin",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak realmAdminClient;

    @InjectAdminClient(ref = "viewClientsAdminClient", client = "test-client", user = "view-clients-admin",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak viewClientsAdminClient;

    @InjectAdminClient(ref = "manageClientsAdminClient", client = "test-client", user = "manage-clients-admin",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak manageClientsAdminClient;

    @InjectAdminClient(ref = "queryClientsAdminClient", client = "test-client", user = "query-clients-admin",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak queryClientsAdminClient;

    @InjectAdminClient(ref = "noAccessClient", client = "test-client", user = "no-access-user",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak noAccessAdminClient;

    @InjectAdminClient(ref = "manageRealmAdminClient", client = "test-client", user = "manage-realm-admin",
                      mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak manageRealmAdminClient;

    /**
     * GET /clients
     * Permissions: auth.clients().requireList() + auth.clients().canView() + auth.roles().requireList() + auth.roles().requireView()
     */
    @Test
    public void listClients() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN);

        // realm-admin: should be able to list clients (has all permissions)
        setAuthHeader(request, realmAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            var clients = mapper.readValue(response.getEntity().getContent(), new TypeReference<List<BaseClientRepresentation>>() {
            });
            assertThat(clients.size(), greaterThan(0));
        }

        // view-clients: should be able to list clients (has requireList via canView)
        setAuthHeader(request, viewClientsAdminClient);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // query-clients: should be able to list clients (has requireList via QUERY_CLIENTS role)
        setAuthHeader(request, queryClientsAdminClient);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireList)
        setAuthHeader(request, noAccessAdminClient);
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
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, realmAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-realm-admin"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to create clients (has requireManage)
        request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, manageClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-manage"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireManage)
        request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, viewClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-view"))));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireManage)
        request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, queryClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-query"))));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireManage)
        request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, noAccessAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-create-noaccess"))));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * GET /clients/{client}
     * Permissions: auth.clients().requireView(client) + auth.roles().requireView(client) + ClientPolicyEvent.VIEW
     */
    @Test
    public void getClient() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/test-client");

        // view-clients: should be able to get individual client (has requireView)
        setAuthHeader(request, viewClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-client"));
        }

        // query-clients: should get 403 (can list but not view individual clients)
        setAuthHeader(request, queryClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireView)
        setAuthHeader(request, noAccessAdminClient);
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
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/non-existent-client");

        // view-clients: should get 404 (has canList, client doesn't exist)
        setAuthHeader(request, viewClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // TODO - our V2 logic does not handle the anti-ID phishing yet
        // TODO - issue https://github.com/keycloak/keycloak/issues/46010
        // no-access: should get 403 (lacks canList, prevents ID phishing)
        // setAuthHeader(request, noAccessAdminClient);
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
        createTestClient("test-update-admin", realmAdminClient);
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-admin");
        setAuthHeader(request, realmAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-admin", "Updated"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to update clients (has requireConfigure)
        createTestClient("test-update-manage", realmAdminClient);
        request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-manage");
        setAuthHeader(request, manageClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-manage", "Updated"))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-update-view", realmAdminClient);
        request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-view");
        setAuthHeader(request, viewClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep("test-update-view", "Updated"))));
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
        createTestClient("test-patch-admin", realmAdminClient);
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/test-patch-admin");
        setAuthHeader(request, realmAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/merge-patch+json");
        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-patch-view", realmAdminClient);
        request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/test-patch-view");
        setAuthHeader(request, viewClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/merge-patch+json");
        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * DELETE /clients/{client}
     * Permissions: auth.clients().requireManage(client) + !isAdminPermissionClient + ClientPolicyEvent.UNREGISTER
     */
    @Test
    public void deleteClient() throws Exception {
        // realm-admin: should be able to delete clients (has requireManage)
        createTestClient("test-delete-admin", realmAdminClient);
        HttpDelete request = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/test-delete-admin");
        setAuthHeader(request, realmAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        // manage-clients: should be able to delete clients (has requireManage)
        createTestClient("test-delete-manage", realmAdminClient);
        request = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/test-delete-manage");
        setAuthHeader(request, manageClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        // view-clients: should get 403 (lacks requireManage)
        createTestClient("test-delete-view", realmAdminClient);
        request = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/test-delete-view");
        setAuthHeader(request, viewClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // query-clients: should get 403 (lacks requireManage)
        createTestClient("test-delete-query", realmAdminClient);
        request = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/test-delete-query");
        setAuthHeader(request, queryClientsAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // no-access: should get 403 (lacks requireManage)
        createTestClient("test-delete-noaccess", realmAdminClient);
        request = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/test-delete-noaccess");
        setAuthHeader(request, noAccessAdminClient);
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
        String ownRealmUrl = "http://localhost:8080/admin/realms/authztest";
        HttpGet request = new HttpGet(ownRealmUrl);

        // should successfully access own realm (200 OK)
        setAuthHeader(request, realmAdminClient);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        // Test accessing non-existent realm - should return 404
        String nonExistentRealmUrl = "http://localhost:8080/admin/realms/non-existent-realm";
        request = new HttpGet(nonExistentRealmUrl);
        setAuthHeader(request, realmAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        // When accessing a different realm from a non-administration realm, should return 403
        // Note: This test validates that when authenticated to 'authztest' realm (which is not an admin realm),
        // trying to access another realm's admin resource should be forbidden
        String differentRealmUrl = "http://localhost:8080/admin/realms/master";
        request = new HttpGet(differentRealmUrl);
        setAuthHeader(request, realmAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * POST /clients (with client roles)
     * Permissions: auth.clients().requireManage() + auth.roles().requireManage() + auth.roles().requireMapComposite()
     */
    @Test
    public void createClientWithRoles() throws Exception {
        // manage-clients: should be able to create clients with roles
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, manageClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        OIDCClientRepresentation rep = createClientRep("test-create-roles");
        rep.setRoles(Set.of("role1", "role2"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));
        try (var response = client.execute(request)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation created = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(created.getRoles(), is(Set.of("role1", "role2")));
        }

        // view-clients: should get 403 (lacks requireManage for clients)
        request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, viewClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        rep = createClientRep("test-create-roles-view");
        rep.setRoles(Set.of("role1"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    /**
     * PUT /clients/{client} (updating client roles)
     * Permissions: auth.clients().requireConfigure(client) + auth.roles().requireManage() + auth.roles().requireMapComposite()
     */
    @Test
    public void updateClientRoles() throws Exception {
        // manage-clients: should be able to update client roles
        createTestClient("test-update-roles-manage", realmAdminClient);
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-roles-manage");
        setAuthHeader(request, manageClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        OIDCClientRepresentation rep = createClientRep("test-update-roles-manage");
        rep.setRoles(Set.of("new-role1", "new-role2"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "new-role2")));
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-update-roles-view", realmAdminClient);
        request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-roles-view");
        setAuthHeader(request, viewClientsAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        rep = createClientRep("test-update-roles-view");
        rep.setRoles(Set.of("new-role"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        // manage-realm-admin: should get 403 (has MANAGE_REALM but lacks auth.roles().requireManage() for client-specific roles)
        createTestClient("test-update-roles-realm", realmAdminClient);
        request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/test-update-roles-realm");
        setAuthHeader(request, manageRealmAdminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        rep = createClientRep("test-update-roles-realm");
        rep.setRoles(Set.of("forbidden-role"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    private OIDCClientRepresentation createClientRep(String clientId) {
        return createClientRep(clientId, null);
    }

    private OIDCClientRepresentation createClientRep(String clientId, String description) {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(clientId);
        rep.setEnabled(true);
        if (description != null) {
            rep.setDescription(description);
        }
        return rep;
    }

    private void createTestClient(String clientId, Keycloak adminClient) throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request, adminClient);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.setEntity(new StringEntity(mapper.writeValueAsString(createClientRep(clientId))));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }
    }

    public static class AdminV2WithAuthzConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        }
    }

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

            // Role: view-clients
            realm.addUser("view-clients-admin")
                    .name("View", "Clients")
                    .email("viewclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_CLIENTS);

            // Role: manage-clients
            realm.addUser("manage-clients-admin")
                    .name("Manage", "Clients")
                    .email("manageclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);

            // Role: query-clients
            realm.addUser("query-clients-admin")
                    .name("Query", "Clients")
                    .email("queryclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS);

            // NO role
            realm.addUser("no-access-user")
                    .name("No", "Access")
                    .email("noaccess@localhost")
                    .emailVerified(true)
                    .password("password");

            // Role: manage-realm
            realm.addUser("manage-realm-admin")
                    .name("Manage", "Realm")
                    .email("managerealm@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM);

            return realm;
        }
    }
}
