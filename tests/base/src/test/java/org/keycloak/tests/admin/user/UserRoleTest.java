package org.keycloak.tests.admin.user;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.Assert.assertNames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserRoleTest extends AbstractUserTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void roleMappings() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation realmCompositeRole = RoleConfigBuilder.create().name("realm-composite").singleAttribute("attribute1", "value1").build();

        realm.roles().create(RoleConfigBuilder.create().name("realm-role").build());
        realm.roles().create(realmCompositeRole);
        realm.roles().create(RoleConfigBuilder.create().name("realm-child").build());
        realm.roles().get("realm-composite").addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));

        final String clientUuid;
        try (Response response = realm.clients().create(ClientConfigBuilder.create().clientId("myclient").build())) {
            clientUuid = ApiUtil.getCreatedId(response);
        }

        RoleRepresentation clientCompositeRole = RoleConfigBuilder.create().name("client-composite").singleAttribute("attribute1", "value1").build();


        realm.clients().get(clientUuid).roles().create(RoleConfigBuilder.create().name("client-role").build());
        realm.clients().get(clientUuid).roles().create(RoleConfigBuilder.create().name("client-role2").build());
        realm.clients().get(clientUuid).roles().create(clientCompositeRole);
        realm.clients().get(clientUuid).roles().create(RoleConfigBuilder.create().name("client-child").build());
        realm.clients().get(clientUuid).roles().get("client-composite").addComposites(Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-child").toRepresentation()));

        final String userId;
        try (Response response = realm.users().create(UserConfigBuilder.create().username("myuser").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        // Admin events for creating role, client or user tested already in other places
        adminEvents.clear();

        RoleMappingResource roles = realm.users().get(userId).roles();
        assertNames(roles.realmLevel().listAll(), Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertNames(roles.realmLevel().listEffective(), "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        // Add realm roles
        List<RoleRepresentation> l = new LinkedList<>();
        l.add(realm.roles().get("realm-role").toRepresentation());
        l.add(realm.roles().get("realm-composite").toRepresentation());
        roles.realmLevel().add(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userRealmRoleMappingsPath(userId), l, ResourceType.REALM_ROLE_MAPPING);

        // Add client roles
        List<RoleRepresentation> list = Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-role").toRepresentation());
        roles.clientLevel(clientUuid).add(list);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), list, ResourceType.CLIENT_ROLE_MAPPING);

        list = Collections.singletonList(realm.clients().get(clientUuid).roles().get("client-composite").toRepresentation());
        roles.clientLevel(clientUuid).add(list);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), ResourceType.CLIENT_ROLE_MAPPING);

        // List realm roles
        assertNames(roles.realmLevel().listAll(), "realm-role", "realm-composite", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertNames(roles.realmLevel().listAvailable(), "realm-child", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);
        assertNames(roles.realmLevel().listEffective(), "realm-role", "realm-composite", "realm-child", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        // List realm effective role with full representation
        List<RoleRepresentation> realmRolesFullRepresentations = roles.realmLevel().listEffective(false);
        RoleRepresentation realmCompositeRoleFromList = getRoleByName("realm-composite", realmRolesFullRepresentations);
        assertNotNull(realmCompositeRoleFromList);
        assertTrue(realmCompositeRoleFromList.getAttributes().containsKey("attribute1"));

        // List client roles
        assertNames(roles.clientLevel(clientUuid).listAll(), "client-role", "client-composite");
        assertNames(roles.clientLevel(clientUuid).listAvailable(), "client-role2", "client-child");
        assertNames(roles.clientLevel(clientUuid).listEffective(), "client-role", "client-composite", "client-child");

        // List client effective role with full representation
        List<RoleRepresentation> rolesFullRepresentations = roles.clientLevel(clientUuid).listEffective(false);
        RoleRepresentation clientCompositeRoleFromList = getRoleByName("client-composite", rolesFullRepresentations);
        assertNotNull(clientCompositeRoleFromList);
        assertTrue(clientCompositeRoleFromList.getAttributes().containsKey("attribute1"));

        // Get mapping representation
        MappingsRepresentation all = roles.getAll();
        assertNames(all.getRealmMappings(), "realm-role", "realm-composite", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertEquals(1, all.getClientMappings().size());
        assertNames(all.getClientMappings().get("myclient").getMappings(), "client-role", "client-composite");

        // Remove realm role
        RoleRepresentation realmRoleRep = realm.roles().get("realm-role").toRepresentation();
        roles.realmLevel().remove(Collections.singletonList(realmRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userRealmRoleMappingsPath(userId), Collections.singletonList(realmRoleRep), ResourceType.REALM_ROLE_MAPPING);

        assertNames(roles.realmLevel().listAll(), "realm-composite", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        // Remove client role
        RoleRepresentation clientRoleRep = realm.clients().get(clientUuid).roles().get("client-role").toRepresentation();
        roles.clientLevel(clientUuid).remove(Collections.singletonList(clientRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userClientRoleMappingsPath(userId, clientUuid), Collections.singletonList(clientRoleRep), ResourceType.CLIENT_ROLE_MAPPING);

        assertNames(roles.clientLevel(clientUuid).listAll(), "client-composite");
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAssignedEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        RealmResource realm = managedRealm.admin();

        RoleRepresentation realmCompositeRole = RoleConfigBuilder.create().name("realm-composite").build();
        realm.roles().create(realmCompositeRole);
        realm.roles().create(RoleConfigBuilder.create().name("realm-child").build());
        realm.roles().get("realm-composite")
                .addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));
        realm.roles().create(RoleConfigBuilder.create().name("realm-role-in-group").build());

        Response response = realm.clients().create(ClientConfigBuilder.create().clientId("myclient").build());
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        RoleRepresentation clientCompositeRole = RoleConfigBuilder.create().name("client-composite").build();
        realm.clients().get(clientUuid).roles().create(clientCompositeRole);
        realm.clients().get(clientUuid).roles().create(RoleConfigBuilder.create().name("client-child").build());
        realm.clients().get(clientUuid).roles().get("client-composite").addComposites(Collections
                .singletonList(realm.clients().get(clientUuid).roles().get("client-child").toRepresentation()));
        realm.clients().get(clientUuid).roles().create(RoleConfigBuilder.create().name("client-role-in-group").build());

        GroupRepresentation group = GroupConfigBuilder.create().name("mygroup").build();
        response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);
        response.close();

        response = realm.users().create(UserConfigBuilder.create().username("myuser").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        // Make indirect assignments
        // .. add roles to the group and add it to the user
        realm.groups().group(groupId).roles().realmLevel()
                .add(Collections.singletonList(realm.roles().get("realm-role-in-group").toRepresentation()));
        realm.groups().group(groupId).roles().clientLevel(clientUuid).add(Collections
                .singletonList(realm.clients().get(clientUuid).roles().get("client-role-in-group").toRepresentation()));
        realm.users().get(userId).joinGroup(groupId);
        // .. assign composite roles
        RoleMappingResource userRoles = realm.users().get(userId).roles();
        userRoles.realmLevel().add(Collections.singletonList(realm.roles().get("realm-composite").toRepresentation()));
        userRoles.clientLevel(clientUuid).add(Collections
                .singletonList(realm.clients().get(clientUuid).roles().get("client-composite").toRepresentation()));

        // check state before making the direct assignments
        assertNames(userRoles.realmLevel().listAll(), "realm-composite", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertNames(userRoles.realmLevel().listAvailable(), "realm-child", "realm-role-in-group", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);
        assertNames(userRoles.realmLevel().listEffective(), "realm-composite", "realm-child", "realm-role-in-group", "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION,
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        assertNames(userRoles.clientLevel(clientUuid).listAll(), "client-composite");
        assertNames(userRoles.clientLevel(clientUuid).listAvailable(), "client-child",
                "client-role-in-group");
        assertNames(userRoles.clientLevel(clientUuid).listEffective(), "client-composite", "client-child",
                "client-role-in-group");

        // Make direct assignments for roles which are already indirectly assigned
        userRoles.realmLevel().add(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));
        userRoles.realmLevel()
                .add(Collections.singletonList(realm.roles().get("realm-role-in-group").toRepresentation()));
        userRoles.clientLevel(clientUuid).add(Collections
                .singletonList(realm.clients().get(clientUuid).roles().get("client-child").toRepresentation()));
        userRoles.clientLevel(clientUuid).add(Collections
                .singletonList(realm.clients().get(clientUuid).roles().get("client-role-in-group").toRepresentation()));

        // List realm roles
        assertNames(userRoles.realmLevel().listAll(), "realm-composite",
                "realm-child", "realm-role-in-group", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertNames(userRoles.realmLevel().listAvailable(), "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION);
        assertNames(userRoles.realmLevel().listEffective(), "realm-composite", "realm-child", "realm-role-in-group",
                "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION,
                Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        // List client roles
        assertNames(userRoles.clientLevel(clientUuid).listAll(), "client-composite", "client-child",
                "client-role-in-group");
        assertNames(userRoles.clientLevel(clientUuid).listAvailable());
        assertNames(userRoles.clientLevel(clientUuid).listEffective(), "client-composite", "client-child",
                "client-role-in-group");

        // Get mapping representation
        MappingsRepresentation all = userRoles.getAll();
        assertNames(all.getRealmMappings(), "realm-composite",
                "realm-child", "realm-role-in-group", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        assertEquals(1, all.getClientMappings().size());
        assertNames(all.getClientMappings().get("myclient").getMappings(), "client-composite", "client-child",
                "client-role-in-group");
    }

    private RoleRepresentation getRoleByName(String name, List<RoleRepresentation> roles) {
        for(RoleRepresentation role : roles) {
            if(role.getName().equalsIgnoreCase(name)) {
                return role;
            }
        }

        return null;
    }
}
