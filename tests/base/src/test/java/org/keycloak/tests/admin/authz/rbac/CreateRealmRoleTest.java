package org.keycloak.tests.admin.authz.rbac;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class CreateRealmRoleTest extends AbstractAdminRBACTest {

    @BeforeEach
    public void onBeforeEach() {
        grantCreateRealmRole();
    }

    @Test
    public void testCreateRealmForbidden() {
        revokeRealmRole(masterUser, AdminRoles.CREATE_REALM);
        runAs(masterRealm.getName(), masterUser.getUsername(),
                client -> assertForbidden("Should not be able to create realm yet", () -> {
                    createRealm(client, "myrealm");
                }));
    }

    @Test
    public void testCreateRealm() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            String realmName = realm.toRepresentation().getRealm();
            List<String> realms = client.realms().findAll().stream().map(RealmRepresentation::getRealm).toList();
            // create-realm role allows users to view the realms they created
            assertTrue(realms.contains(realmName));
        });
    }

    @Test
    public void testMapNonAdminRole() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            UserRepresentation user = createUser(realm, "myadmin");
            // create-realm role allows users to map non-admin realm roles
            grantRealmRole(realm, user, "myrole");
        });
    }

    @Test
    public void testAllowMapNonAdminRealmRoleFGAPEnabled() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            RealmRepresentation rep = realm.toRepresentation();
            rep.setAdminPermissionsEnabled(true);
            realm.update(rep);
            UserRepresentation user = createUser(realm, "myadmin");
            // create-realm role allows users to map non-admin realm roles even if FGAP is enabled
            grantRealmRole(realm, user, "myrole");
        });
    }

    @Test
    public void testMapRealmManagementRole() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            UserRepresentation user = createUser(realm, "myadmin");
            // create-realm role allows users to map realm-management roles
            grantRealmManagementRole(realm, user.getUsername(), AdminRoles.MANAGE_USERS);
        });
    }

    @Test
    public void testForbiddenGrantAdminRoleFGAPEnabled() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            RealmRepresentation rep = realm.toRepresentation();
            rep.setAdminPermissionsEnabled(true);
            realm.update(rep);
            UserRepresentation user = createUser(realm, "myadmin");
            // create-realm role does not allow users to map admin roles if FGAP is enabled
            assertForbidden("Should not be able to map admin roles because user is not a server/realm admin",
                    () -> grantRealmManagementRole(realm, user.getUsername(), AdminRoles.MANAGE_USERS));
        });
    }

    @Test
    public void testMapAdminRoleForbidden() {
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.MANAGE_USERS);
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.VIEW_REALM);
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource masterRealm = client.realm(Config.getAdminRealm());
            UserRepresentation user = createUser(masterRealm, "myadmin");
            // create-realm role allows users to map server admin roles in the master realm
            assertForbidden("Should not be able to map admin role",
                    () -> grantRealmRole(masterRealm, user, AdminRoles.ADMIN));
        });
    }

    @Test
    public void testMapRealmAdminRoleForbidden() {
        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            RealmResource realm = createRealm(client, "myrealm");
            UserRepresentation user = createUser(realm, "myadmin");
            // create-realm role does not allow users to map realm-admin role
            assertForbidden("Should not be able to map realm-admin role",
                    () -> grantRealmManagementRole(realm, user.getUsername(), AdminRoles.REALM_ADMIN));
        });
    }

    private void grantCreateRealmRole() {
        grantRealmRole(masterRealm.admin(), masterUser.admin().toRepresentation(), AdminRoles.CREATE_REALM);
    }
}
