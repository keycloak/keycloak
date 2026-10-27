package org.keycloak.tests.admin.authz.rbac;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for CVE-2026-94215: a per-request "by-id" cache (e.g.
 * {@code RealmCacheSession#managedApplications/managedRoles}, {@code UserCacheSession#managedUsers}) used to return
 * an entity resolved under one realm's context to a lookup scoped to a different realm, as long as both lookups
 * used the same id within the same request. A {@code create-realm} holder could exploit this to read/write
 * resources belonging to the master realm (or any other realm) by addressing them through a realm they control.
 */
@KeycloakIntegrationTest
public class CrossRealmCacheIsolationTest extends AbstractAdminRBACTest {

    @BeforeEach
    public void onBeforeEach() {
        grantRealmRole(masterRealm.admin(), masterUser.admin().toRepresentation(), AdminRoles.CREATE_REALM);
    }

    @Test
    public void testCannotReadMasterClientThroughOwnRealm() {
        ClientRepresentation masterAdminCli = masterRealm.admin().clients().findByClientId("admin-cli").get(0);

        runAs(masterRealm.getName(), masterUser.getUsername(), attackerClient -> {
            RealmResource attackerRealm = createRealm(attackerClient, "attacker-realm");
            String attackerRealmName = attackerRealm.toRepresentation().getRealm();

            // the attacker only holds create-realm in master; resolving a master client through their own
            // realm's path must not leak the master client, even though the id is the same
            assertThrows(NotFoundException.class, () ->
                    attackerClient.realm(attackerRealmName).clients().get(masterAdminCli.getId()).toRepresentation());
        });
    }

    @Test
    public void testCannotReadMasterRoleThroughOwnRealm() {
        RoleRepresentation masterCreateRealmRole = masterRealm.admin().roles().get(AdminRoles.CREATE_REALM).toRepresentation();

        runAs(masterRealm.getName(), masterUser.getUsername(), attackerClient -> {
            RealmResource attackerRealm = createRealm(attackerClient, "attacker-realm");
            String attackerRealmName = attackerRealm.toRepresentation().getRealm();

            assertThrows(NotFoundException.class, () ->
                    attackerClient.realm(attackerRealmName).rolesById().getRole(masterCreateRealmRole.getId()));
        });
    }

    @Test
    public void testCannotReadMasterUserThroughOwnRealm() {
        String attackerMasterUserId = masterUser.admin().toRepresentation().getId();

        runAs(masterRealm.getName(), masterUser.getUsername(), attackerClient -> {
            RealmResource attackerRealm = createRealm(attackerClient, "attacker-realm");
            String attackerRealmName = attackerRealm.toRepresentation().getRealm();

            // the attacker's own master user id gets resolved against master during bearer token
            // validation (the user owning the active session); it must not leak through a different
            // realm's path just because the id happens to be the same
            assertThrows(NotFoundException.class, () ->
                    attackerClient.realm(attackerRealmName).users().get(attackerMasterUserId).toRepresentation());
        });
    }
}
