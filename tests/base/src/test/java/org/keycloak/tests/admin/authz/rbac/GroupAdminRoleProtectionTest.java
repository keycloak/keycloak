package org.keycloak.tests.admin.authz.rbac;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class GroupAdminRoleProtectionTest extends AbstractAdminRBACTest {

    @Test
    public void testNonRealmAdminCannotAddUserToGroupWithAdminRole() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        String groupId = createGroup(realmApi, "admin-group");
        assignRealmManagementRoleToGroup(realmApi, groupId, AdminRoles.MANAGE_USERS);

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            assertForbidden("Non-realm-admin should not be able to add user to group with admin role",
                    () -> userRealmApi.users().get(targetUser.getId()).joinGroup(groupId));
        });
    }

    @Test
    public void testRealmAdminCanAddUserToGroupWithAdminRole() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        String groupId = createGroup(realmApi, "admin-group");
        assignRealmManagementRoleToGroup(realmApi, groupId, AdminRoles.MANAGE_USERS);

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.REALM_ADMIN);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            userRealmApi.users().get(targetUser.getId()).joinGroup(groupId);
        });
    }

    @Test
    public void testNonRealmAdminCannotAddUserToChildGroupOfParentWithAdminRole() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        String parentGroupId = createGroup(realmApi, "parent-group");
        assignRealmManagementRoleToGroup(realmApi, parentGroupId, AdminRoles.MANAGE_EVENTS);

        GroupRepresentation child = new GroupRepresentation();
        child.setName("child-group");
        try (Response response = realmApi.groups().group(parentGroupId).subGroup(child)) {
            child.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            assertForbidden("Non-realm-admin should not be able to add user to child of group with admin role",
                    () -> userRealmApi.users().get(targetUser.getId()).joinGroup(child.getId()));
        });
    }

    @Test
    public void testNonRealmAdminCanAddUserToGroupWithoutAdminRoles() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        String groupId = createGroup(realmApi, "normal-group");

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            userRealmApi.users().get(targetUser.getId()).joinGroup(groupId);
        });
    }

    @Test
    public void testNonRealmAdminCannotMapCompositeRoleContainingAdminRole() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        // Create a composite role that transitively contains an admin role
        RoleRepresentation innerComposite = new RoleRepresentation();
        innerComposite.setName("inner-composite");
        realmApi.roles().create(innerComposite);

        ClientRepresentation mgmtClient = realmApi.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation manageUsers = realmApi.clients().get(mgmtClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        realmApi.roles().get("inner-composite").addComposites(List.of(manageUsers));

        RoleRepresentation outerComposite = new RoleRepresentation();
        outerComposite.setName("outer-composite");
        realmApi.roles().create(outerComposite);

        RoleRepresentation innerRep = realmApi.roles().get("inner-composite").toRepresentation();
        realmApi.roles().get("outer-composite").addComposites(List.of(innerRep));

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        RoleRepresentation outerRole = realmApi.roles().get("outer-composite").toRepresentation();

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            assertForbidden("Non-realm-admin should not be able to map composite role containing admin role",
                    () -> userRealmApi.users().get(targetUser.getId()).roles().realmLevel().add(List.of(outerRole)));
        });
    }

    @Test
    public void testNonRealmAdminCannotAddUserToGroupWithCompositeAdminRole() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        RealmResource realmApi = adminClient.realm("myrealm");

        // Create a composite role that contains an admin role
        RoleRepresentation compositeRole = new RoleRepresentation();
        compositeRole.setName("custom-composite");
        compositeRole.setComposite(false);
        realmApi.roles().create(compositeRole);

        ClientRepresentation mgmtClient = realmApi.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation manageUsers = realmApi.clients().get(mgmtClient.getId()).roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        realmApi.roles().get("custom-composite").addComposites(List.of(manageUsers));

        // Assign the composite role to a group
        String groupId = createGroup(realmApi, "composite-admin-group");
        RoleRepresentation compositeRoleRep = realmApi.roles().get("custom-composite").toRepresentation();
        realmApi.groups().group(groupId).roles().realmLevel().add(List.of(compositeRoleRep));

        UserRepresentation targetUser = createUser(realmApi, "targetuser");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource userRealmApi = client.realm("myrealm");
            assertForbidden("Non-realm-admin should not be able to add user to group with composite admin role",
                    () -> userRealmApi.users().get(targetUser.getId()).joinGroup(groupId));
        });
    }

    private String createGroup(RealmResource realm, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = realm.groups().add(group)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private void assignRealmManagementRoleToGroup(RealmResource realm, String groupId, String roleName) {
        ClientRepresentation mgmtClient = realm.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation role = realm.clients().get(mgmtClient.getId()).roles().get(roleName).toRepresentation();
        realm.groups().group(groupId).roles().clientLevel(mgmtClient.getId()).add(List.of(role));
    }
}
