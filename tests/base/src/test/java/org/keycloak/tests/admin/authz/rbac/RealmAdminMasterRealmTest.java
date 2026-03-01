package org.keycloak.tests.admin.authz.rbac;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAdminMasterRealmTest extends AbstractAdminRBACTest {

    @Test
    public void testAccessIfRoleGrantedInMasterRealmClient() {
        RealmRepresentation realm = RealmConfigBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        UserRepresentation newUser = UserConfigBuilder.create().username("myuser").build();

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            // user not allowed to list users
            RealmResource realmApi = client.realm(realm.getRealm());

            assertForbidden("Should not be able to list users in realm",
                    () -> realmApi.users().list());

            try (Response response = realmApi.users().create(newUser)) {
                // user not allowed to create users
                assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
            }
        });

        // grant manage-users role in the new realm to master realm admin via master realm
        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.MANAGE_USERS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            // user not allowed to list users
            RealmResource realmApi = client.realm(realm.getRealm());
            List<UserRepresentation> users = realmApi.users().list();
            assertTrue(users.isEmpty());
            realmApi.users().create(newUser).close();
            users = realmApi.users().search(newUser.getUsername());
            assertFalse(users.isEmpty());
        });
    }

    @Test
    public void testGrantRealmManagementRoleIfGrantedInMasterRealmClient() {
        RealmRepresentation realm = RealmConfigBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realmApi = client.realm(realm.getRealm());
            // user not allowed to list events
            assertForbidden("Should not be able to list events in realm", () -> realmApi.getEvents());
        });

        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.MANAGE_USERS);
        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.VIEW_CLIENTS);

        String newUserName = "myuser";

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realmApi = client.realm(realm.getRealm());
            // user not allowed to list events
            assertForbidden("Should not be able to list events in realm", () -> realmApi.getEvents());

            // user can create a new user
            realmApi.users().create(UserConfigBuilder.create()
                            .username(newUserName)
                            .password("password")
                            .email("myuser@keycloak.org")
                            .firstName("f")
                            .lastName("l")
                            .enabled(true)
                            .build())
                    .close();
            List<UserRepresentation> users = realmApi.users().search(newUserName);
            assertFalse(users.isEmpty());
            UserRepresentation user = users.get(0);

            // master realm user not allowed to grant manage-events role in the new realm via realm-management client
            assertForbidden("Should not be able to grant role the user does not have",
                    () -> grantRealmManagementRole(realmApi, user.getUsername(), AdminRoles.MANAGE_EVENTS));
        });

        // grant manage-events role in the new realm to master realm admin via master realm
        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.MANAGE_EVENTS);

        runAs(realm.getRealm(), newUserName, (client) -> {
            // master user is not allowed to list events
            assertForbidden("Should not be able to list events in realm", () -> client.realm(realm.getRealm()).getEvents());
        });

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realmApi = client.realm(realm.getRealm());
            // master realm user can now list events
            realmApi.getEvents();
            // now grant manage-events role to the master user
            grantRealmManagementRole(realmApi, newUserName, AdminRoles.MANAGE_EVENTS);
        });

        runAs(realm.getRealm(), newUserName, (client) -> client.realm(realm.getRealm()).getEvents());
    }
}
