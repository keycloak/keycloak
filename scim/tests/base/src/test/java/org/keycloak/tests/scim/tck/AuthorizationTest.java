package org.keycloak.tests.scim.tck;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class AuthorizationTest extends AbstractScimTest {

    @InjectRealm(config = ScimRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectScimClient(clientId = "scim-client-restricted", clientSecret = "secret", attachTo = "scim-client-restricted")
    ScimClient noAccessClient;

    @InjectUser
    ManagedUser managedUser;

    @BeforeEach
    public void onBefore() {
        realm.admin().clients().create(ClientConfigBuilder
                .create()
                .clientId("scim-client-restricted")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build()).close();
    }

    @Test
    public void testUsersAccessDeniedIfRolesNotGranted() {
        User user = new User();
        user.setUserName("test");
        assertAccessDenied(() -> noAccessClient.users().create(user));
        user.setId(managedUser.getId());
        assertAccessDenied(() -> noAccessClient.users().update(managedUser.getId(), user));
        assertAccessDenied(() -> noAccessClient.users().patch(managedUser.getId(), PatchRequest.create()
                .add("name.givenName", "new given name")
                .build()));
        assertAccessDenied(() -> noAccessClient.users().get(managedUser.getId()));
        assertAccessDenied(() -> noAccessClient.users().search(""));
        assertAccessDenied(() -> noAccessClient.users().getAll());
        assertAccessDenied(() -> noAccessClient.users().delete(managedUser.getId()));
    }

    @Test
    public void testUsersCanQueryIfQueryRoleGranted() {
        grantAdminRole(AdminRoles.QUERY_USERS);
        assertEquals(1, noAccessClient.users().search("").getTotalResults());
    }

    @Test
    public void testUsersAccessIfViewUsersRoleGranted() {
        grantAdminRole(AdminRoles.VIEW_USERS);
        User user = noAccessClient.users().get(managedUser.getId());
        assertNotNull(user);
        ListResponse<User> users = noAccessClient.users().getAll();
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());
        assertAccessDenied(() -> noAccessClient.users().create(new User()));
        assertAccessDenied(() -> noAccessClient.users().update(user.getId(), user));
        assertAccessDenied(() -> noAccessClient.users().patch(user.getId(), PatchRequest.create()
                .add("name.givenName", "new given name")
                .build()));
        assertAccessDenied(() -> noAccessClient.users().delete(user.getId()));
    }

    @Test
    public void testUsersAccessIfManageUsersRoleGranted() {
        grantAdminRole(AdminRoles.MANAGE_USERS);
        User user = noAccessClient.users().get(managedUser.getId());
        assertNotNull(user);
        ListResponse<User> users = noAccessClient.users().getAll();
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());
        User newUser = new User();
        newUser.setUserName("newuser");
        newUser.setFirstName("new user");
        newUser.setLastName("new user");
        newUser.setEmail("newuser@keycloak.org");
        newUser = noAccessClient.users().create(newUser);
        assertNotNull(newUser);
        noAccessClient.users().update(newUser.getId(), newUser);
        noAccessClient.users().patch(newUser.getId(), PatchRequest.create()
                .add("name.givenName", "new given name")
                .build());
        noAccessClient.users().delete(newUser.getId());
    }

    @Test
    public void testGroupsAccessDeniedIfRolesNotGranted() {
        Group group = new Group();
        group.setDisplayName("test");
        assertAccessDenied(() -> noAccessClient.groups().create(group));
        group.setId(createGroup().getId());
        assertAccessDenied(() -> noAccessClient.groups().update(group.getId(), group));
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("displayName", "new display name")
                .build()));
        assertAccessDenied(() -> noAccessClient.groups().get(group.getId()));
        assertAccessDenied(() -> noAccessClient.groups().getAll(""));
        assertAccessDenied(() -> noAccessClient.groups().delete(group.getId()));
    }

    @Test
    public void testGroupsAccessIfViewUsersRoleGranted() {
        grantAdminRole(AdminRoles.VIEW_USERS);
        Group group = noAccessClient.groups().get(createGroup().getId());
        assertNotNull(group);
        ListResponse<Group> groups = noAccessClient.groups().getAll("");
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());
        assertAccessDenied(() -> noAccessClient.groups().create(new Group()));
        assertAccessDenied(() -> noAccessClient.groups().update(group.getId(), group));
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("displayName", "new name")
                .build()));
        assertAccessDenied(() -> noAccessClient.groups().delete(group.getId()));
    }

    @Test
    public void testGroupsAccessIfManageUsersRoleGranted() {
        grantAdminRole(AdminRoles.MANAGE_USERS);
        Group group = noAccessClient.groups().get(createGroup().getId());
        assertNotNull(group);
        ListResponse<Group> groups = noAccessClient.groups().getAll("");
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());
        Group newGroup = new Group();
        newGroup.setDisplayName("newgroup");
        newGroup = noAccessClient.groups().create(newGroup);
        assertNotNull(newGroup);
        noAccessClient.groups().update(newGroup.getId(), newGroup);
        noAccessClient.groups().patch(newGroup.getId(), PatchRequest.create()
                .add("displayName", "new name")
                .build());
        noAccessClient.groups().delete(newGroup.getId());
    }

    @Test
    public void testDiscoveryEndpointsDeniedIfRolesNotGranted() {
        assertAccessDenied(() -> noAccessClient.config().get());
        assertAccessDenied(() -> noAccessClient.schemas().getAll());
        assertAccessDenied(() -> noAccessClient.resourceTypes().getAll());
    }

    @Test
    public void testDiscoveryEndpointsAccessIfViewRealmRoleGranted() {
        grantAdminRole(AdminRoles.VIEW_REALM);
        assertNotNull(noAccessClient.config().get());
        assertNotNull(noAccessClient.schemas().getAll());
        assertNotNull(noAccessClient.resourceTypes().getAll());
    }

    @Test
    public void testGroupsCanQueryIfQueryRoleGranted() {
        createGroup();
        grantAdminRole(AdminRoles.QUERY_GROUPS);
        assertEquals(1, noAccessClient.groups().getAll("").getTotalResults());
    }

    @Test
    public void testAllowUsersFromGroupFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);
        grantAdminRole(AdminRoles.QUERY_USERS);

        ListResponse<User> users = noAccessClient.users().getAll();
        assertEquals(0, users.getTotalResults());

        GroupRepresentation group = createGroup();
        managedUser.admin().joinGroup(group.getId());
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName("Allow SCIM access to users in group");
        permission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        permission.setResources(Set.of(group.getId()));
        permission.setScopes(Set.of(AdminPermissionsSchema.VIEW_MEMBERS, AdminPermissionsSchema.MANAGE_MEMBERS));
        permission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(permission).close();
        users = noAccessClient.users().getAll();
        assertEquals(1, users.getTotalResults());
    }

    @Test
    public void testAllowGroupsFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);
        grantAdminRole(AdminRoles.QUERY_GROUPS);

        ListResponse<Group> groups = noAccessClient.groups().getAll("");
        assertEquals(0, groups.getTotalResults());

        GroupRepresentation group = createGroup();
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName("Allow SCIM access to a group");
        permission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        permission.setResources(Set.of(group.getId()));
        permission.setScopes(Set.of(AdminPermissionsSchema.VIEW));
        permission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(permission).close();
        groups = noAccessClient.groups().getAll("");
        assertEquals(1, groups.getTotalResults());
    }

    @Test
    public void testGroupMembershipDeniedWithoutManageGroupMembershipFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM user management");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage on users (but NOT manage-group-membership)
        ScopePermissionRepresentation userPermission = new ScopePermissionRepresentation();
        userPermission.setName("Allow SCIM manage users");
        userPermission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        userPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW));
        userPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(userPermission).close();

        // Grant view on groups (so the SCIM client can resolve group IDs)
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM view groups");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.VIEW));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Verify the client can manage users (e.g., update name)
        User user = noAccessClient.users().get(managedUser.getId());
        assertNotNull(user);
        noAccessClient.users().patch(managedUser.getId(), PatchRequest.create()
                .add("name.givenName", "updated name")
                .build());

        // Verify the client CANNOT add group membership via PATCH
        assertAccessDenied(() -> noAccessClient.users().patch(managedUser.getId(), PatchRequest.create()
                .add("groups", group.getId())
                .build()));

        // Verify the client CANNOT set group membership via PUT
        user = noAccessClient.users().get(managedUser.getId());
        user.addGroup(group.getId());
        User finalUser = user;
        assertAccessDenied(() -> noAccessClient.users().update(finalUser.getId(), finalUser));
    }

    private ClientRepresentation getScimClient() {
        return realm.admin().clients().findByClientId("scim-client-restricted").get(0);
    }

    private GroupRepresentation createGroup() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("test-group");
        try (Response response = realm.admin().groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        realm.cleanup().add(realm -> realm.groups().group(group.getId()).remove());
        return group;
    }

    private void assertAccessDenied(Runnable action) {
        try {
            action.run();
            fail("Expected access denied");
        } catch (ScimClientException sce) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), sce.getError().getStatusInt());
        }
    }

    private void grantAdminRole(String viewUsers) {
        ClientRepresentation realmMgmt = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation viewUserRole = realm.admin().clients().get(realmMgmt.getId()).roles().get(viewUsers).toRepresentation();
        ClientRepresentation clientRep = getScimClient();
        UserRepresentation serviceAccountUser = realm.admin().clients().get(clientRep.getId()).getServiceAccountUser();
        realm.admin().users().get(serviceAccountUser.getId()).roles().clientLevel(realmMgmt.getId()).add(List.of(viewUserRole));
    }
}
