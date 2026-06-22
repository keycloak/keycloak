package org.keycloak.tests.scim.tck;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
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
import org.keycloak.scim.resource.user.GroupMembership;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class AuthorizationTest extends AbstractScimTest {

    @InjectRealm(config = ScimRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectScimClient(clientId = "scim-client-restricted", clientSecret = "secret", attachTo = "scim-client-restricted")
    ScimClient noAccessClient;

    @InjectUser
    ManagedUser managedUser;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void onBefore() {
        realm.admin().clients().create(ClientBuilder
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
    public void testDiscoveryEndpointsAccessIfGrantedAnySupportedAdminRole() {
        for (String role : List.of(AdminRoles.QUERY_USERS, AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS, AdminRoles.QUERY_GROUPS)) {
            grantAdminRole(role);
            assertNotNull(noAccessClient.config().get());
            assertNotNull(noAccessClient.schemas().getAll());
            assertNotNull(noAccessClient.resourceTypes().getAll());
            revokeAdminRole(role);
        }
    }

    @Test
    public void testGroupsCanQueryIfQueryRoleGranted() {
        createGroup();
        grantAdminRole(AdminRoles.QUERY_GROUPS);
        assertEquals(1, noAccessClient.groups().getAll("").getTotalResults());
    }

    @Test
    public void testUsersCanQueryIfFGAPViewScopeGranted() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        assertAccessDenied(() -> noAccessClient.users().getAll());

        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());
        createPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE,
                null,
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        ListResponse<User> users = noAccessClient.users().getAll();
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());
    }

    @Test
    public void testGroupsCanQueryIfFGAPViewScopeGranted() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        createGroup();
        assertAccessDenied(() -> noAccessClient.groups().getAll(""));

        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());
        createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                null,
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        ListResponse<Group> groups = noAccessClient.groups().getAll("");
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());
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
    public void testGroupMembersReadRequiresViewMembersFGAP() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        GroupRepresentation group = createGroup();
        managedUser.admin().joinGroup(group.getId());

        ClientResource permissionClient = getAdminPermissionsClient();
        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());
        ScopePermissionRepresentation permission = createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                Set.of(group.getId()),
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        Group fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertNull(fetched.getMembers());

        grantAdminRole(AdminRoles.VIEW_USERS);
        fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertNotNull(fetched.getMembers());
        assertEquals(1, fetched.getMembers().size());
        assertEquals(managedUser.getId(), fetched.getMembers().get(0).getValue());

        revokeAdminRole(AdminRoles.VIEW_USERS);
        permission = permissionClient.authorization().permissions().scope().findByName(permission.getName());
        permission.addScope(AdminPermissionsSchema.VIEW);
        permission.addScope(AdminPermissionsSchema.VIEW_MEMBERS);
        permissionClient.authorization().permissions().scope().findById(permission.getId()).update(permission);
        fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertNotNull(fetched.getMembers());
        assertEquals(1, fetched.getMembers().size());
        assertEquals(managedUser.getId(), fetched.getMembers().get(0).getValue());
    }

    @Test
    public void testUserGroupMembershipReadFiltersByPerGroupViewFGAP() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        // Create two groups and add managedUser to both
        GroupRepresentation groupA = new GroupRepresentation();
        groupA.setName("group-a");
        try (Response response = realm.admin().groups().add(groupA)) {
            groupA.setId(ApiUtil.getCreatedId(response));
        }

        GroupRepresentation groupB = new GroupRepresentation();
        groupB.setName("group-b");
        try (Response response = realm.admin().groups().add(groupB)) {
            groupB.setId(ApiUtil.getCreatedId(response));
        }

        managedUser.admin().joinGroup(groupA.getId());
        managedUser.admin().joinGroup(groupB.getId());

        // Set up a user policy referencing the restricted SCIM client's service account
        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());

        // Grant users:view on the managed user so the SCIM client can fetch it
        createPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE,
                Set.of(managedUser.getId()),
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        // Grant groups:view on groupA ONLY — groupB is intentionally not accessible
        createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                Set.of(groupA.getId()),
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        User fetched = noAccessClient.users().get(managedUser.getId(), List.of("groups"));
        assertNotNull(fetched.getGroups());
        List<String> returnedGroupIds = fetched.getGroups().stream()
                .map(GroupMembership::getValue)
                .toList();
        assertEquals(1, returnedGroupIds.size());
        assertTrue(returnedGroupIds.contains(groupA.getId()));

        grantAdminRole(AdminRoles.VIEW_USERS);
        fetched = noAccessClient.users().get(managedUser.getId(), List.of("groups"));
        assertNotNull(fetched.getGroups());
        returnedGroupIds = fetched.getGroups().stream()
                .map(GroupMembership::getValue)
                .toList();
        assertEquals(2, returnedGroupIds.size());
        assertTrue(returnedGroupIds.contains(groupA.getId()));
        assertTrue(returnedGroupIds.contains(groupB.getId()));

    }

    @Test
    public void testUserGroupMembershipAddDeniedWithoutGroupViewFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());

        createPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE,
                Set.of(managedUser.getId()),
                Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP),
                policy.getName());

        createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                Set.of(group.getId()),
                Set.of(AdminPermissionsSchema.MANAGE_MEMBERSHIP),
                policy.getName());

        ScimClientException exception = null;
        try {
            noAccessClient.users().patch(managedUser.getId(), PatchRequest.create()
                    .add("groups", group.getId())
                    .build());
        } catch (ScimClientException sce) {
            exception = sce;
        }

        assertNotNull(exception);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getError().getStatusInt());
    }

    @Test
    public void testGroupMembersAddDeniedWithoutUserViewFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        ClientResource permissionClient = getAdminPermissionsClient();
        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());

        createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                Set.of(group.getId()),
                Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW, AdminPermissionsSchema.MANAGE_MEMBERSHIP),
                policy.getName());

        createPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE,
                Set.of(managedUser.getId()),
                Set.of(AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP),
                policy.getName());

        ScimClientException exception = null;
        try {
            noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                    .add("members", managedUser.getId())
                    .build());
        } catch (ScimClientException sce) {
            exception = sce;
        }

        assertNotNull(exception);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getError().getStatusInt());
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

    @Test
    public void testGroupMembersAddDeniedWithoutManageUsersRole() {
        // Grant view-users (enough to see groups, but not to manage)
        grantAdminRole(AdminRoles.VIEW_USERS);
        GroupRepresentation group = createGroup();

        // PATCH add member on group should be denied (no manage permission on groups)
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("members", managedUser.getId())
                .build()));
    }

    @Test
    public void testGroupMembersRemoveDeniedWithoutManageUsersRole() {
        // Grant view-users (enough to see groups, but not to manage)
        grantAdminRole(AdminRoles.VIEW_USERS);
        GroupRepresentation group = createGroup();

        // Add the user to the group via admin API so we have something to remove
        managedUser.admin().joinGroup(group.getId());

        // PATCH remove member on group should be denied (no manage permission on groups)
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .remove("members[value eq \"" + managedUser.getId() + "\"]")
                .build()));
    }

    @Test
    public void testGroupMembersAccessWithManageUsersRole() {
        grantAdminRole(AdminRoles.MANAGE_USERS);
        GroupRepresentation group = createGroup();

        // PATCH add member should succeed with manage-users role
        noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("members", managedUser.getId())
                .build());

        // Verify member was added
        Group fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertNotNull(fetched.getMembers());
        assertEquals(1, fetched.getMembers().size());

        // PATCH remove member should succeed with manage-users role
        noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .remove("members[value eq \"" + managedUser.getId() + "\"]")
                .build());

        // Verify member was removed
        fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertTrue(fetched.getMembers() == null || fetched.getMembers().isEmpty());
    }

    @Test
    public void testGroupMembersAddDeniedWithoutManageMembershipFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage on the group (allows PATCH on non-member attributes)
        // but NOT manage-membership (required to manage group members)
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM manage group");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setResources(Set.of(group.getId()));
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Verify the client CAN patch non-member attributes (e.g., displayName)
        noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("displayName", "updated name")
                .build());

        // Verify the client CANNOT add members without manage-membership scope
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("members", managedUser.getId())
                .build()));
    }

    @Test
    public void testGroupMembersRemoveDeniedWithoutManageMembershipFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        // Add the user to the group via admin API
        managedUser.admin().joinGroup(group.getId());

        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage on the group but NOT manage-membership
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM manage group");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setResources(Set.of(group.getId()));
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Verify the client CANNOT remove members without manage-membership scope
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .remove("members[value eq \"" + managedUser.getId() + "\"]")
                .build()));
    }

    @Test
    public void testGroupMembersRemoveDeniedWithoutManageGroupMembershipOnUserFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        // Add the user to the group via admin API
        managedUser.admin().joinGroup(group.getId());

        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage + manage-membership on the group
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM manage group with membership");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setResources(Set.of(group.getId()));
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW, AdminPermissionsSchema.MANAGE_MEMBERSHIP));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Grant view on users but NOT manage-group-membership
        ScopePermissionRepresentation userPermission = new ScopePermissionRepresentation();
        userPermission.setName("Allow SCIM view users");
        userPermission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        userPermission.setScopes(Set.of(AdminPermissionsSchema.VIEW));
        userPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(userPermission).close();

        // Verify the client CANNOT remove members without manage-group-membership scope on the user
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .remove("members[value eq \"" + managedUser.getId() + "\"]")
                .build()));
    }

    @Test
    public void testGroupMembersAllowedWithAllFGAPScopes() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage + manage-membership on the group
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM full group access");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setResources(Set.of(group.getId()));
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW,
                AdminPermissionsSchema.MANAGE_MEMBERSHIP, AdminPermissionsSchema.VIEW_MEMBERS));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Grant manage-group-membership on users
        ScopePermissionRepresentation userPermission = new ScopePermissionRepresentation();
        userPermission.setName("Allow SCIM manage user group membership");
        userPermission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        userPermission.setScopes(Set.of(AdminPermissionsSchema.VIEW, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP));
        userPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(userPermission).close();

        // PATCH add member should succeed with all required FGAP scopes
        noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("members", managedUser.getId())
                .build());

        // Verify member was added
        Group fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertNotNull(fetched.getMembers());
        assertEquals(1, fetched.getMembers().size());

        // PATCH remove member should succeed with all required FGAP scopes
        noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .remove("members[value eq \"" + managedUser.getId() + "\"]")
                .build());

        // Verify member was removed
        fetched = noAccessClient.groups().get(group.getId(), List.of("members"));
        assertTrue(fetched.getMembers() == null || fetched.getMembers().isEmpty());
    }

    @Test
    public void testGroupMembersAddDeniedWithoutManageGroupMembershipOnUserFGAP() {
        RealmRepresentation realmRep = this.realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(true);
        realm.admin().update(realmRep);

        GroupRepresentation group = createGroup();
        ClientRepresentation client = getScimClient();
        UserRepresentation serviceAccount = realm.admin().clients().get(client.getId()).getServiceAccountUser();

        // Create policy for the SCIM service account
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Allow SCIM access");
        policy.addUser(serviceAccount.getId());
        ClientResource permissionClient = realm.admin().clients().get(
                realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId());
        permissionClient.authorization().policies().user().create(policy).close();

        // Grant manage + manage-membership on the group
        ScopePermissionRepresentation groupPermission = new ScopePermissionRepresentation();
        groupPermission.setName("Allow SCIM manage group with membership");
        groupPermission.setResourceType(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
        groupPermission.setResources(Set.of(group.getId()));
        groupPermission.setScopes(Set.of(AdminPermissionsSchema.MANAGE, AdminPermissionsSchema.VIEW, AdminPermissionsSchema.MANAGE_MEMBERSHIP));
        groupPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(groupPermission).close();

        // Grant view on users but NOT manage-group-membership
        ScopePermissionRepresentation userPermission = new ScopePermissionRepresentation();
        userPermission.setName("Allow SCIM view users");
        userPermission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        userPermission.setScopes(Set.of(AdminPermissionsSchema.VIEW));
        userPermission.addPolicy(policy.getName());
        permissionClient.authorization().permissions().scope().create(userPermission).close();

        // Verify the client CANNOT add members without manage-group-membership scope on the user
        assertAccessDenied(() -> noAccessClient.groups().patch(group.getId(), PatchRequest.create()
                .add("members", managedUser.getId())
                .build()));
    }

    @Test
    public void testFGAPPerUserViewAllowsGetByIdButNotList() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());
        createPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE,
                Set.of(managedUser.getId()),
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        User fetched = noAccessClient.users().get(managedUser.getId());
        assertNotNull(fetched);
        assertEquals(managedUser.getId(), fetched.getId());

        assertAccessDenied(() -> noAccessClient.users().getAll());

        grantAdminRole(AdminRoles.QUERY_USERS);
        ListResponse<User> users = noAccessClient.users().getAll();
        assertNotNull(users);
        assertEquals(1, users.getTotalResults());
    }

    @Test
    public void testFGAPPerGroupViewAllowsGetByIdButNotList() {
        realm.updateWithCleanup(realm -> realm.adminPermissionsEnabled(true));

        GroupRepresentation group = createGroup();
        UserPolicyRepresentation policy = createUserPolicy(getServiceAccount().getId());
        createPermission(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE,
                Set.of(group.getId()),
                Set.of(AdminPermissionsSchema.VIEW),
                policy.getName());

        Group fetched = noAccessClient.groups().get(group.getId());
        assertNotNull(fetched);
        assertEquals(group.getId(), fetched.getId());

        assertAccessDenied(() -> noAccessClient.groups().getAll(""));

        grantAdminRole(AdminRoles.QUERY_GROUPS);
        ListResponse<Group> groups = noAccessClient.groups().getAll("");
        assertNotNull(groups);
        assertEquals(1, groups.getTotalResults());
    }

    @Test
    public void testPublicClientAccessDenied() {
        ClientRepresentation publicClient = ClientBuilder.create()
                .clientId("public-scim-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .enabled(true)
                .build();
        realm.admin().clients().create(publicClient).close();
        UserRepresentation user = UserBuilder.create()
                .username("public-client-user")
                .firstName("f")
                .lastName("l")
                .email("user@keycloak.org")
                .enabled(true)
                .password("password")
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        grantAdminRole(AdminRoles.MANAGE_REALM, user);
        grantAdminRole(AdminRoles.MANAGE_USERS, user);

        String tokenEndpoint = keycloakUrls.getToken(realm.getName());
        ScimClient publicScimClient = ScimClient.create(httpClient)
                .withBaseUrl(keycloakUrls.getBase() + "/realms/" + realm.getName())
                .withAuthorization((http, request) -> {
                    try {
                        AccessTokenResponse response = http.doPost(tokenEndpoint)
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                                .param(OAuth2Constants.CLIENT_ID, publicClient.getClientId())
                                .param(OAuth2Constants.USERNAME, user.getUsername())
                                .param(OAuth2Constants.PASSWORD, "password")
                                .asJson(AccessTokenResponse.class);
                        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + response.getToken());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();

        assertAccessDenied(() -> publicScimClient.users().getAll());
        assertAccessDenied(() -> publicScimClient.groups().getAll(""));
        assertAccessDenied(() -> publicScimClient.resourceTypes().getAll());
        assertAccessDenied(() -> publicScimClient.schemas().getAll());
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

    private ClientResource getAdminPermissionsClient() {
        String permissionsClientId = realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0).getId();
        return realm.admin().clients().get(permissionsClientId);
    }

    private UserPolicyRepresentation createUserPolicy(String userId) {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.addUser(userId);
        getAdminPermissionsClient().authorization().policies().user().create(policy).close();

        return policy;
    }

    private UserRepresentation getServiceAccount() {
        return realm.admin().clients().get(getScimClient().getId()).getServiceAccountUser();
    }

    private ScopePermissionRepresentation createPermission(String resourceType, Set<String> resources, Set<String> scopes, String policyName) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(resourceType);
        if (resources != null && !resources.isEmpty()) {
            permission.setResources(resources);
        }
        permission.setScopes(scopes);
        permission.addPolicy(policyName);
        getAdminPermissionsClient().authorization().permissions().scope().create(permission).close();
        return permission;
    }

    private void assertAccessDenied(Runnable action) {
        try {
            action.run();
            fail("Expected access denied");
        } catch (ScimClientException sce) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), sce.getError().getStatusInt());
        }
    }

    private void grantAdminRole(String role) {
        ClientRepresentation clientRep = getScimClient();
        UserRepresentation serviceAccountUser = realm.admin().clients().get(clientRep.getId()).getServiceAccountUser();
        grantAdminRole(role, serviceAccountUser);
    }

    private void grantAdminRole(String role, UserRepresentation serviceAccountUser) {
        ClientRepresentation realmMgmt = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation viewUserRole = realm.admin().clients().get(realmMgmt.getId()).roles().get(role).toRepresentation();
        realm.admin().users().get(serviceAccountUser.getId()).roles().clientLevel(realmMgmt.getId()).add(List.of(viewUserRole));
    }

    private void revokeAdminRole(String name) {
        ClientRepresentation realmMgmt = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation role = realm.admin().clients().get(realmMgmt.getId()).roles().get(name).toRepresentation();
        ClientRepresentation clientRep = getScimClient();
        UserRepresentation serviceAccountUser = realm.admin().clients().get(clientRep.getId()).getServiceAccountUser();
        realm.admin().users().get(serviceAccountUser.getId()).roles().clientLevel(realmMgmt.getId()).remove(List.of(role));
    }
}
