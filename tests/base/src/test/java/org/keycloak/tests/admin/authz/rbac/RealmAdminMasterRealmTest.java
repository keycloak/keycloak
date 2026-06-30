package org.keycloak.tests.admin.authz.rbac;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAdminMasterRealmTest extends AbstractAdminRBACTest {

    @Test
    public void testAccessIfRoleGrantedInMasterRealmClient() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);
        UserRepresentation newUser = UserBuilder.create().username("myuser").build();

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
    public void testIndividualAdminRolesCannotMapAdminRoles() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);

        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.MANAGE_USERS);
        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.MANAGE_EVENTS);
        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.VIEW_CLIENTS);

        String newUserName = "myuser";

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realmApi = client.realm(realm.getRealm());

            realmApi.users().create(UserBuilder.create()
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

            assertForbidden("Individual admin roles should not allow mapping admin roles without realm-admin",
                    () -> grantRealmManagementRole(realmApi, newUserName, AdminRoles.MANAGE_EVENTS));
        });
    }

    @Test
    public void testRealmAdminOnMasterClientCanMapAdminRoles() {
        RealmRepresentation realm = RealmBuilder.create().name("myrealm").build();
        adminClient.realms().create(realm);

        grantMasterRealmManagementRole(realm.getRealm(), masterUser.getUsername(), AdminRoles.REALM_ADMIN);

        String newUserName = "myuser";

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realmApi = client.realm(realm.getRealm());

            realmApi.users().create(UserBuilder.create()
                            .username(newUserName)
                            .password("password")
                            .email("myuser@keycloak.org")
                            .firstName("f")
                            .lastName("l")
                            .enabled(true)
                            .build())
                    .close();

            grantRealmManagementRole(realmApi, newUserName, AdminRoles.MANAGE_EVENTS);
        });

        runAs(realm.getRealm(), newUserName, (client) -> client.realm(realm.getRealm()).getEvents());
    }
}
