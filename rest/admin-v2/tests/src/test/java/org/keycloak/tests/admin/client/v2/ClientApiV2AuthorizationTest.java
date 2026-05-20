package org.keycloak.tests.admin.client.v2;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

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
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @InjectRealm(config = AuthorizationRealmConfig.class)
    ManagedRealm testRealm;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient adminPermissionClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "realm-admin",
        client = "test-client",
        ref = "realmAdmin")
    Keycloak realmAdminAdminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "view-clients",
        client = "test-client",
        ref = "viewClients")
    Keycloak viewClientsAdminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "query-clients",
        client = "test-client",
        ref = "queryClients")
    Keycloak queryClientsAdminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "manage-clients",
        client = "test-client",
        ref = "manageClients")
    Keycloak manageClientsAdminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "no-access",
        client = "test-client",
        ref = "noAccess")
    Keycloak noAccessAdminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM,
        user = "fgap-user",
        client = "test-client",
        ref = "fgapUser")
    Keycloak fgapAdminClient;

    @Override
    public String getRealmName() {
        return testRealm.getName();
    }

    /**
     * GET /clients
     * Permissions: auth.clients().requireList() || auth.clients().requireView()
     */
    @Test
    public void listClients() {
        // realm-admin: should be able to list clients (has all permissions)
        try (var response = getClientsApi(realmAdminAdminClient).getClients()) {
            assertThat(response.toList().size(), greaterThan(0));
        }

        // view-clients: should be able to list clients (has requireList via canView)
        try (var response = getClientsApi(viewClientsAdminClient).getClients()) {
            assertThat(response.toList().size(), greaterThan(0));
        }

        // query-clients: should be able to list clients (has requireList via QUERY_CLIENTS role)
        try (var response = getClientsApi(queryClientsAdminClient).getClients()) {
            assertThat(response.toList().size(), equalTo(0));
        }

        // manage-clients: should be able to list clients (has requireList via MANAGE_CLIENTS role)
        try (var response = getClientsApi(manageClientsAdminClient).getClients()) {
            assertThat(response.toList().size(), greaterThan(0));
        }

        // no-access: should get 403 (lacks requireList)
        ForbiddenException ex = assertThrows(
            ForbiddenException.class,
            () -> getClientsApi(noAccessAdminClient).getClients()
        );

        assertTrue(ex.getMessage().contains("HTTP 403 Forbidden"));
    }

    /**
     * POST /clients
     * Permissions: auth.clients().requireManage() + ClientPolicyEvent.REGISTER
     */
    @Test
    public void createClient() throws Exception {
        // realm-admin: should be able to create clients (has requireManage)
        try (var response = getClientsApi(realmAdminAdminClient).createClient(createClientRep("test-create-admin", "new-role1", "new-role2"))) {
            assertThat(response.getStatus(), is(201));
        }

        // manage-clients: should be able to create clients (has requireManage)
        try (var response = getClientsApi(manageClientsAdminClient).createClient(createClientRep("test-create-manage", "new-role1", "new-role2"))) {
            assertThat(response.getStatus(), is(201));
        }

        var testRep = createClientRep("test-create-view", "new-role1", "new-role2");
        // view-clients: should get 403 (lacks requireManage)
        try (var response = getClientsApi(viewClientsAdminClient).createClient(testRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden when creating client without access");
        }

        // query-clients: should get 403 (lacks requireManage)
        try (var response = getClientsApi(queryClientsAdminClient).createClient(testRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden when creating client without access");
        }

        // no-access: should get 403 (lacks requireManage)
        try (var response = getClientsApi(noAccessAdminClient).createClient(testRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden when creating client without access");
        }
    }

    /**
     * GET /clients/{client}
     * Permissions: auth.clients().requireView(client) + ClientPolicyEvent.VIEW
     */
    @Test
    public void getClient() {
        // view-clients: should be able to get individual client (has requireView)
        String testClientId = "test-client";
        BaseClientRepresentation baseRep = getClientsApi(viewClientsAdminClient).client(testClientId).getClient();
        assertThat(baseRep.getClientId(), is(testClientId));

        // manage-clients: should be able to get individual client (has requireView)
        baseRep = getClientsApi(manageClientsAdminClient).client(testClientId).getClient();
        assertThat(baseRep.getClientId(), is(testClientId));

        // query-clients: should get 403 (can list but not view individual clients)
        ForbiddenException ex = assertThrows(
            ForbiddenException.class,
            () -> getClientsApi(queryClientsAdminClient).client(testClientId).getClient()
        );
        assertTrue(ex.getMessage().contains("HTTP 403 Forbidden"));

        // no-access: should get 403 (lacks requireView)
        ex = assertThrows(
            ForbiddenException.class,
            () -> getClientsApi(noAccessAdminClient).client(testClientId).getClient()
        );
        assertTrue(ex.getMessage().contains("HTTP 403 Forbidden"));
    }

    /**
     * GET /clients/{client} (client == null)
     * Permissions: if auth.clients().canList() return 404, else return 403
     */
    @Test
    public void getNonExistentClient() {
        String nonExistentClientId = "non-existent-client";

        // view-clients: should get 404 (has canList, client doesn't exist)
        assertThrows(NotFoundException.class,
            () -> getClientsApi(viewClientsAdminClient).client(nonExistentClientId).getClient());

        // manage-clients: should get 404 (has canList, client doesn't exist)
        assertThrows(NotFoundException.class,
            () -> getClientsApi(manageClientsAdminClient).client(nonExistentClientId).getClient());

        // query-clients: should get 404 (has canList, client doesn't exist)
        assertThrows(NotFoundException.class,
            () -> getClientsApi(queryClientsAdminClient).client(nonExistentClientId).getClient());

        // no-access: should get 403 (lacks canList, prevents ID phishing)
        ForbiddenException ex = assertThrows(ForbiddenException.class,
            () -> getClientsApi(noAccessAdminClient).client(nonExistentClientId).getClient());
        assertTrue(ex.getMessage().contains("HTTP 403 Forbidden"));
    }

    /**
     * PUT /clients/{client}
     * Permissions: auth.clients().requireConfigure(client) + ClientPolicyEvent.UPDATE + ClientPolicyEvent.UPDATED
     */
    @Test
    public void updateClient() throws Exception {
        // realm-admin: should be able to update clients (has requireConfigure)
        createTestClient("test-update-admin");
        BaseClientRepresentation updateRep = createClientRep("test-update-admin", "role123", "my-role");
        try (var response = getClientApi(realmAdminAdminClient, getRealmName(), "test-update-admin").createOrUpdateClient(updateRep)) {
            assertThat(response.getStatus(), is(200));
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-update-admin"));
            assertThat(client.getRoles(), containsInAnyOrder("role123", "my-role"));
        }

        // manage-clients: should be able to update clients (has requireConfigure)
        createTestClient("test-update-manage");
        updateRep = createClientRep("test-update-manage", "role123", "my-role");
        try (var response = getClientApi(manageClientsAdminClient, getRealmName(), "test-update-manage").createOrUpdateClient(updateRep)) {
            assertThat(response.getStatus(), is(200));
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            assertThat(client.getClientId(), is("test-update-manage"));
            assertThat(client.getRoles(), containsInAnyOrder("role123", "my-role"));
        }

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-update-view");
        BaseClientRepresentation viewRep = createClientRep("test-update-view", "role123", "my-role");
        try (var response = getClientApi(viewClientsAdminClient, getRealmName(), "test-update-view").createOrUpdateClient(viewRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden when updating client without access");
        }

        // query-clients: should get 403 (lacks requireConfigure)
        try (var response = getClientApi(queryClientsAdminClient, getRealmName(), "test-update-view").createOrUpdateClient(viewRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden when updating client without access");
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
        final OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched");
        getClientApi(realmAdminAdminClient, getRealmName(), "test-patch-admin").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch)));

        // view-clients: should get 403 (lacks requireConfigure)
        createTestClient("test-patch-view");
        assertThrows(ForbiddenException.class,
            () -> getClientApi(viewClientsAdminClient, getRealmName(), "test-patch-view").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch))));

        // query-clients: should get 403 (lacks requireConfigure)
        assertThrows(ForbiddenException.class,
            () -> getClientApi(queryClientsAdminClient, getRealmName(), "test-patch-view").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch))));

        // manage-clients: should be able to patch clients (has manage-clients)
        getClientApi(manageClientsAdminClient, getRealmName(), "test-patch-view").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch)));

        // does not exist
        // no-access: not existing - should get 403 (lacks canList, prevents ID phishing)
        final OIDCClientRepresentation noAccessPatch = new OIDCClientRepresentation();
        noAccessPatch.setDescription("Patched-non-existing");
        assertThrows(ForbiddenException.class,
            () -> getClientApi(noAccessAdminClient, getRealmName(), "does-not-exist").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(noAccessPatch))));

        // view-clients: not existing - should get 404
        assertThrows(NotFoundException.class,
            () -> getClientApi(viewClientsAdminClient, getRealmName(), "does-not-exist").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(noAccessPatch))));
    }

    /**
     * DELETE /clients/{client}
     * Permissions: auth.clients().requireManage(client) + !isAdminPermissionClient + ClientPolicyEvent.UNREGISTER
     */
    @Test
    public void deleteClient() throws Exception {
        // realm-admin: should be able to delete clients (has requireManage)
        createTestClient("test-delete-admin");
        try (var response = getClientApi(realmAdminAdminClient, getRealmName(), "test-delete-admin").deleteClient()) {
            assertEquals(204, response.getStatus());
        }

        // manage-clients: should be able to delete clients (has requireManage)
        createTestClient("test-delete-manage");
        try (var response = getClientApi(manageClientsAdminClient, getRealmName(), "test-delete-manage").deleteClient()) {
            assertEquals(204, response.getStatus());
        }

        // view-clients: should get 403 (lacks requireManage)
        createTestClient("test-delete-view");
        try (var response = getClientApi(viewClientsAdminClient, getRealmName(), "test-delete-view").deleteClient()) {
            assertEquals(403, response.getStatus());
        }

        // query-clients: should get 403 (lacks requireManage)
        try (var response = getClientApi(queryClientsAdminClient, getRealmName(), "test-delete-view").deleteClient()) {
            assertEquals(403, response.getStatus());
        }

        // no-access: should get 403 (lacks requireManage)
        try (var response = getClientApi(noAccessAdminClient, getRealmName(), "test-delete-view").deleteClient()) {
            assertEquals(403, response.getStatus());
        }

        // no-access: not existing - should get 403 (lacks canList, prevents ID phishing)
        try (var response = getClientApi(noAccessAdminClient, getRealmName(), "does-not-exist").deleteClient()) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden");
        }

        // view-clients: not existing - should get 404
        try (var response = getClientApi(viewClientsAdminClient, getRealmName(), "does-not-exist").deleteClient()) {
            assertEquals(404, response.getStatus());
        }
    }

    /**
     * DELETE /clients/{client} - Admin Permissions Client
     * Should not allow deletion of the admin permissions client (used for FGAP)
     */
    @Test
    public void cannotDeleteAdminPermissionsClient() {
        var adminPermissionsClientRep = Optional.ofNullable(testRealm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID))
            .filter(f -> !f.isEmpty())
            .map(f -> f.get(0))
            .orElseThrow(() -> new AssertionError("Cannot find admin permissions client"));

        try (var response = getClientApi(realmAdminAdminClient, getRealmName(), adminPermissionsClientRep.getClientId()).deleteClient()) {
            assertThat(response.getStatus(), is(400));
            var body = response.readEntity(String.class);
            assertThat(body, containsString("Not supported for this client"));
        }

        // Verify the client still exists (was not deleted)
        BaseClientRepresentation rep = getClientApi(realmAdminAdminClient, getRealmName(), adminPermissionsClientRep.getClientId()).getClient();
        assertThat(rep.getClientId(), is(adminPermissionsClientRep.getClientId()));
    }

    /**
     * GET /admin/realms/{realm}
     * Permissions: if realm == null return 404, if !isAdministrationRealm && !auth.realm.equals(realm) return 403
     */
    @Test
    public void getRealmAdmin() {
        // should successfully access own realm (200 OK)
        assertThat(realmAdminAdminClient.realm(getRealmName()).toRepresentation().getRealm(), is(getRealmName()));

        // Test accessing non-existent realm - should return 404
        assertThrows(NotFoundException.class,
            () -> realmAdminAdminClient.realm("non-existent-realm").toRepresentation());

        // When accessing a different realm from a non-administration realm, should return 403
        // Note: This test validates that when authenticated to 'authztest' realm (which is not an admin realm),
        // trying to access another realm's admin resource should be forbidden
        assertThrows(ForbiddenException.class,
            () -> realmAdminAdminClient.realm("master").toRepresentation());
    }

    @Test
    public void fgapGetClient() throws Exception {
        createTestClient("fgap-view-test");

        // BEFORE permission: fgap-user cannot view fgap-view-test (403)
        assertThrows(ForbiddenException.class,
            () -> getClientApi(fgapAdminClient, getRealmName(), "fgap-view-test").getClient());

        // Create FGAP permission for fgap-view-test
        createFgapPermissionForClient("fgap-view-test");

        // AFTER permission: fgap-user CAN view fgap-view-test (200)
        BaseClientRepresentation rep = getClientApi(fgapAdminClient, getRealmName(), "fgap-view-test").getClient();
        assertThat(rep.getClientId(), is("fgap-view-test"));

        // fgap-user CANNOT view fgap-denied-client (no permission granted) (403)
        assertThrows(ForbiddenException.class,
            () -> getClientApi(fgapAdminClient, getRealmName(), "fgap-denied-client").getClient());
    }

    @Test
    public void fgapUpdateClient() throws Exception {
        createTestClient("fgap-update-test");
        BaseClientRepresentation updateRep = createClientRep("fgap-update-test", "updated-role");

        // BEFORE permission: fgap-user cannot update fgap-update-test (403)
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-update-test").createOrUpdateClient(updateRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden before permission is granted");
            assertThat(response.readEntity(String.class), containsString("HTTP 403 Forbidden"));
        }

        // Create FGAP permission for fgap-update-test
        createFgapPermissionForClient("fgap-update-test");

        // AFTER permission: fgap-user CAN update fgap-update-test (200)
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-update-test").createOrUpdateClient(updateRep)) {
            assertThat(response.getStatus(), is(200));
        }

        // fgap-user CANNOT update fgap-denied-client (no permission granted) (403)
        BaseClientRepresentation deniedRep = createClientRep("fgap-denied-client", "updated-role");
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-denied-client").createOrUpdateClient(deniedRep)) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden for denied client");
            assertThat(response.readEntity(String.class), containsString("HTTP 403 Forbidden"));
        }
    }

    @Test
    public void fgapPatchClient() throws Exception {
        createTestClient("fgap-patch-test");
        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("Patched by FGAP user");

        // BEFORE permission: fgap-user cannot patch fgap-patch-test (403)
        assertThrows(ForbiddenException.class,
            () -> getClientApi(fgapAdminClient, getRealmName(), "fgap-patch-test").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch))));

        // Create FGAP permission for fgap-patch-test
        createFgapPermissionForClient("fgap-patch-test");

        // AFTER permission: fgap-user CAN patch fgap-patch-test (200)
        getClientApi(fgapAdminClient, getRealmName(), "fgap-patch-test").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch)));

        // fgap-user CANNOT patch fgap-denied-client (no permission granted) (403)
        patch.setDescription("Should not be patched");
        assertThrows(ForbiddenException.class,
            () -> getClientApi(fgapAdminClient, getRealmName(), "fgap-denied-client").patchClient(new ByteArrayInputStream(mapper.writeValueAsBytes(patch))));
    }

    @Test
    public void fgapDeleteClient() throws Exception {
        createTestClient("fgap-delete-test");

        // BEFORE permission: fgap-user cannot delete fgap-delete-test (403)
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-delete-test").deleteClient()) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden before permission is granted");
            assertThat(response.readEntity(String.class), containsString("HTTP 403 Forbidden"));
        }

        // Create FGAP permission for fgap-delete-test
        createFgapPermissionForClient("fgap-delete-test");

        // AFTER permission: fgap-user CAN delete fgap-delete-test (204)
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-delete-test").deleteClient()) {
            assertThat(response.getStatus(), is(204));
        }

        // fgap-user CANNOT delete fgap-denied-client (no permission granted) (403)
        try (var response = getClientApi(fgapAdminClient, getRealmName(), "fgap-denied-client").deleteClient()) {
            assertEquals(403, response.getStatus(), "Expected 403 Forbidden for denied client");
            assertThat(response.readEntity(String.class), containsString("HTTP 403 Forbidden"));
        }
    }

    private OIDCClientRepresentation createClientRep(String clientId, String... roles) {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(clientId);
        rep.setEnabled(true);
        rep.setRoles(new HashSet<>(List.of(roles)));
        return rep;
    }

    private void createTestClient(String clientId) {
        try (var response = getClientsApi(realmAdminAdminClient).createClient(createClientRep(clientId, "test-role1", "test-role2"))) {
            assertThat(response.getStatus(), is(201));
        }
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

    public static class AuthorizationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("authztest");
            realm.adminPermissionsEnabled(true);
            realm.clients(ClientBuilder.create("test-client").secret("test-secret").directAccessGrantsEnabled(true));

            // Role: realm-admin
            realm.users(UserBuilder.create("realm-admin")
                    .name("Realm", "Admin")
                    .email("realmadmin@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));

            // Role: view-clients
            realm.users(UserBuilder.create("view-clients")
                    .name("View", "Clients")
                    .email("viewclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_CLIENTS));

            // Role: manage-clients
            realm.users(UserBuilder.create("manage-clients")
                    .name("Manage", "Clients")
                    .email("manageclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS));

            // Role: query-clients
            realm.users(UserBuilder.create("query-clients")
                    .name("Query", "Clients")
                    .email("queryclients@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS));

            // NO role
            realm.users(UserBuilder.create("no-access")
                    .name("No", "Access")
                    .email("noaccess@localhost")
                    .emailVerified(true)
                    .password("password"));

            // Role: manage-realm
            realm.users(UserBuilder.create("manage-realm")
                    .name("Manage", "Realm")
                    .email("managerealm@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM));

            // FGAP v2
            // fgap-user has QUERY_CLIENTS role but will be granted fine-grained permissions for specific clients
            realm.users(UserBuilder.create("fgap-user")
                    .id(FGAP_USER_ID)
                    .name("FGAP", "User")
                    .email("fgapuser@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS));

            realm.clients(ClientBuilder.create("fgap-denied-client").enabled(true));

            return realm;
        }
    }
}
