package org.keycloak.tests.admin;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest
public class AdminEndpointAccessibilityTest {

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    /**
     * Verifies that the user does not have access to Keycloak Admin endpoint when role is not
     * assigned to that user.
     *
     * @link https://issues.jboss.org/browse/KEYCLOAK-2964
     */
    @Test
    public void noAdminEndpointAccessWhenNoRoleAssigned() {
        String userName = "user-" + UUID.randomUUID();
        UserRepresentation user = UserConfigBuilder.create()
                .username(userName)
                .password("pwd")
                .build();
        final String realmName = "master";
        final String userUuid = ApiUtil.getCreatedId(adminClient.realm(realmName).users().create(user));

        Keycloak userClient = adminClientFactory.create().realm(realmName)
                .username(userName).password("pwd")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
        ClientErrorException e = Assertions.assertThrows(ClientErrorException.class,
                () -> userClient.realms().findAll()  // Any admin operation will do
        );
        assertThat(e.getMessage(), containsString(String.valueOf(Response.Status.FORBIDDEN.getStatusCode())));
        adminClient.realm(realmName).users().get(userUuid).remove();
    }

    /**
     * Verifies that the role assigned to a user is correctly handled by Keycloak Admin endpoint.
     *
     * @link https://issues.jboss.org/browse/KEYCLOAK-2964
     */
    @Test
    public void adminEndpointAccessibleWhenAdminRoleAssignedToUser() {
        String userName = "user-" + UUID.randomUUID();
        UserRepresentation user = UserConfigBuilder.create()
                .username(userName)
                .password("pwd")
                .build();

        final String realmName = "master";
        RealmResource realm = adminClient.realms().realm(realmName);
        RoleRepresentation adminRole = realm.roles().get(AdminRoles.ADMIN).toRepresentation();
        assertThat(adminRole, notNullValue());
        assertThat(adminRole.getId(), notNullValue());

        final String userUuid = ApiUtil.getCreatedId(adminClient.realm(realmName).users().create(user));
        assertThat(userUuid, notNullValue());

        RoleMappingResource mappings = realm.users().get(userUuid).roles();
        mappings.realmLevel().add(List.of(adminRole));

        Keycloak userClient = adminClientFactory.create().realm(realmName)
                .username(userName).password("pwd")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();

        assertFalse(userClient.realms().findAll().isEmpty()); // Any admin operation will do
        adminClient.realm(realmName).users().get(userUuid).remove();
    }

    /**
     * Verifies that the role assigned to a user's group is correctly handled by Keycloak Admin endpoint.
     *
     * @link https://issues.jboss.org/browse/KEYCLOAK-2964
     */
    @Test
    public void adminEndpointAccessibleWhenAdminRoleAssignedToGroup() {
        String userName = "user-" + UUID.randomUUID();
        String groupName = "group-" + UUID.randomUUID();

        final String realmName = "master";
        RealmResource realm = adminClient.realms().realm(realmName);
        RoleRepresentation adminRole = realm.roles().get(AdminRoles.ADMIN).toRepresentation();
        assertThat(adminRole, notNullValue());
        assertThat(adminRole.getId(), notNullValue());

        UserRepresentation user = UserConfigBuilder.create()
                .username(userName)
                .password("pwd")
                .build();
        final String userUuid = ApiUtil.getCreatedId(adminClient.realm(realmName).users().create(user));
        assertThat(userUuid, notNullValue());

        GroupRepresentation group = GroupConfigBuilder.create().name(groupName).build();
        Response response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);

        RoleMappingResource mappings = realm.groups().group(groupId).roles();
        mappings.realmLevel().add(List.of(adminRole));

        realm.users().get(userUuid).joinGroup(groupId);

        Keycloak userClient = adminClientFactory.create().realm(realmName)
                .username(userName).password("pwd")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
        assertFalse(userClient.realms().findAll().isEmpty()); // Any admin operation will do

        adminClient.realm(realmName).groups().group(groupId).remove();
        adminClient.realm(realmName).users().get(userUuid).remove();
    }

    /**
     * Verifies that the role assigned to a user's group is correctly handled by Keycloak Admin endpoint.
     *
     * @link https://issues.jboss.org/browse/KEYCLOAK-2964
     */
    @Test
    public void adminEndpointAccessibleWhenAdminRoleAssignedToGroupAfterUserJoinedIt() {
        String userName = "user-" + UUID.randomUUID();
        String groupName = "group-" + UUID.randomUUID();
        final String realmName = "master";

        RealmResource realm = adminClient.realms().realm(realmName);
        RoleRepresentation adminRole = realm.roles().get(AdminRoles.ADMIN).toRepresentation();
        assertThat(adminRole, notNullValue());
        assertThat(adminRole.getId(), notNullValue());

        UserRepresentation user = UserConfigBuilder.create()
                .username(userName)
                .password("pwd")
                .build();
        final String userUuid = ApiUtil.getCreatedId(adminClient.realm(realmName).users().create(user));
        assertThat(userUuid, notNullValue());

        GroupRepresentation group = GroupConfigBuilder.create().name(groupName).build();
        Response response = realm.groups().add(group);
        String groupId = ApiUtil.getCreatedId(response);

        realm.users().get(userUuid).joinGroup(groupId);

        RoleMappingResource mappings = realm.groups().group(groupId).roles();

        mappings.realmLevel().add(List.of(adminRole));

        Keycloak userClient = adminClientFactory.create().realm(realmName)
                .username(userName).password("pwd")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
        assertFalse(userClient.realms().findAll().isEmpty()); // Any admin operation will do

        adminClient.realm(realmName).groups().group(groupId).remove();
        adminClient.realm(realmName).users().get(userUuid).remove();
    }

}
