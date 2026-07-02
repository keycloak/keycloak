package org.keycloak.tests.admin.authz.rbac;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.VerificationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAdminAccessTest extends AbstractAdminRBACTest {

    @Test
    public void testIgnoreAdminRolesGrantedViaProtocolMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String username = "test-user";
        createUser(testRealm, username);
        ClientRepresentation client = createClient(testRealm, "test-client", toRepresentation(
                HardcodedRole.create("hardcoded-view-clients-mapper", Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.VIEW_CLIENTS)
        ));
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, client.getClientId(), username, userClient -> {
                userClient.realm(realmName).clients().findAll();
            });
        });
    }

    @Test
    public void testCannotEscalateByMapperInjectingGroupDerivedAdminRole() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        GroupRepresentation group = new GroupRepresentation();
        group.setName("limited-admin-group");
        try (Response response = testRealm.groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, group, AdminRoles.VIEW_CLIENTS);
        String username = "test-user";
        UserRepresentation user = createUser(testRealm, username);
        testRealm.users().get(user.getId()).joinGroup(group.getId());
        ClientRepresentation fullTokenClient = createClient(testRealm, "full-token-client",
                toRepresentation(HardcodedRole.create("inject-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS))
        );

        runAs(realmName, fullTokenClient.getClientId(), username, userClient -> {
            assertFalse(userClient.realm(realmName).clients().findAll().isEmpty());
        });

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, fullTokenClient.getClientId(), username, userClient -> {
                userClient.realm(realmName).users().list();
            });
        });
    }

    @Test
    public void testAdminRolesGrantedViaGroupMembership() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        GroupRepresentation group = new GroupRepresentation();
        group.setName("admin-group");
        try (Response response = testRealm.groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, group, AdminRoles.VIEW_CLIENTS);
        String username = "test-user";
        UserRepresentation user = createUser(testRealm, username);
        testRealm.users().get(user.getId()).joinGroup(group.getId());

        runAs(realmName, "admin-cli", username, userClient -> {
            assertFalse(userClient.realm(realmName).clients().findAll().isEmpty());
        });
    }

    @Test
    public void testCannotSelfGrantManageUsersViaHardcodedRoleMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "attacker";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_CLIENTS);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.VIEW_USERS);

        ClientRepresentation maliciousClient = createClient(testRealm, "malicious-client",
                toRepresentation(HardcodedRole.create("inject-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS))
        );

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), attackerName, attackerClient -> {
                grantRealmManagementRole(attackerClient.realm(realmName), attackerName, AdminRoles.MANAGE_USERS);
            });
        });
    }

    @Test
    public void testCannotEscalateViaMultipleInjectedAdminRoles() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "attacker";
        createUser(testRealm, attackerName);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_CLIENTS);

        ClientRepresentation maliciousClient = createClient(testRealm, "malicious-client", toRepresentation(HardcodedRole.create("inject-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS)),
                toRepresentation(HardcodedRole.create("inject-manage-clients",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_CLIENTS)),
                toRepresentation(HardcodedRole.create("inject-manage-realm",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM))
        );

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            attackerClient.realm(realmName).clients().create(maliciousClient).close();
        });

        // Unprivileged user authenticates through the malicious client
        String victimName = "unprivileged-user";
        createUser(testRealm, victimName);

        // All three injected roles must be rejected
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), victimName, userClient -> {
                userClient.realm(realmName).users().list();
            });
        });

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), victimName, userClient -> {
                userClient.realm(realmName).clients().findAll();
            });
        });

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), victimName, userClient -> {
                userClient.realm(realmName).toRepresentation();
            });
        });
    }

    @Test
    public void testCannotEscalateViaRoleNameMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "attacker";
        UserRepresentation attacker = createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_CLIENTS);
        grantRealmRole(testRealm, attacker, "harmless-role");

        // Attacker creates a client with a RoleNameMapper that remaps their legitimate
        // "harmless-role" into realm-management.manage-users in the token
        ClientRepresentation maliciousClient = createClient(testRealm, "malicious-client",
                toRepresentation(RoleNameMapper.create("remap-to-manage-users",
                        "harmless-role",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS))
        );

        // Token now has "manage-users" under realm-management via role renaming
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), attackerName, attackerClient -> {
                attackerClient.realm(realmName).users().list();
            });
        });
    }

    @Test
    public void testCannotImpersonateViaInjectedImpersonationRole() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "attacker";
        createUser(testRealm, attackerName);
        String victimName = "realm-admin-user";
        UserRepresentation victim = createUser(testRealm, victimName);
        grantRealmManagementRole(testRealm, victimName, AdminRoles.REALM_ADMIN);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_CLIENTS);

        ClientRepresentation maliciousClient = createClient(testRealm, "malicious-client",
                toRepresentation(HardcodedRole.create("inject-impersonation",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.IMPERSONATION))
        );

        // Attempt to impersonate the realm admin
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, maliciousClient.getClientId(), attackerName, attackerClient -> {
                attackerClient.realm(realmName).users().get(victim.getId()).impersonate();
            });
        });
    }

    @Test
    public void testCannotEscalateWhenUsingLightweightTokensAsRolesAreNotMapped() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "attacker";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_CLIENTS);

        // Attacker adds a protocol mapper to admin-cli
        ClientRepresentation adminCli = testRealm.clients()
                .findByClientId("admin-cli").get(0);
        ProtocolMapperRepresentation mapper = toRepresentation(
                HardcodedRole.create("inject-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS)
        );

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            attackerClient.realm(realmName).clients().get(adminCli.getId())
                    .getProtocolMappers().createMapper(mapper).close();
        });

        // Now even authenticating via admin-cli (the poisoned client), the injected
        // manage-users role must be ignored since it's not actually granted
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                attackerClient.realm(realmName).users().list();
            });
        });
    }

    @Test
    public void testLegitimateRoleNotStrippedWhenMapperAlsoPresent() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String username = "legit-admin";
        createUser(testRealm, username);

        // Legitimately grant view-clients
        grantRealmManagementRole(testRealm, username, AdminRoles.VIEW_CLIENTS);

        // Create a client that ALSO adds view-clients via mapper (redundant but shouldn't break)
        ClientRepresentation client = createClient(testRealm, "redundant-mapper-client",
                toRepresentation(HardcodedRole.create("redundant-view-clients",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.VIEW_CLIENTS))
        );

        // The legitimate role should still work
        runAs(realmName, client.getClientId(), username, userClient -> {
            assertFalse(userClient.realm(realmName).clients().findAll().isEmpty());
        });
    }

    @Test
    public void testManageUsersAdminCannotGrantManageRealm() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.VIEW_CLIENTS);

        // Create a non-lightweight client with a HardcodedRole mapper that injects manage-realm.
        // admin-cli uses lightweight tokens — roles are re-resolved from the user model on every
        // request, which coincidentally prevents the escalation. A full-token client is needed to
        // exercise the token-stripping protection in removeTransientAdminRoles. If the injected
        // role is not stripped, canManageRealm() sees manage-realm in the token and allows the grant.
        ClientRepresentation fullTokenClient = createClient(testRealm, "full-token-client",
                toRepresentation(HardcodedRole.create("inject-manage-realm",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM))
        );

        // Even with manage-realm injected into the full token, the attacker must not be able
        // to grant themselves manage-realm since it is not actually granted to them.
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, fullTokenClient.getClientId(), attackerName, attackerClient -> {
                grantRealmManagementRole(attackerClient.realm(realmName), attackerName, AdminRoles.MANAGE_REALM);
            });
        });
    }

    @Test
    public void testManageUsersAdminCannotGrantManageClients() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.VIEW_CLIENTS);

        // Create a non-lightweight client with a HardcodedRole mapper that injects manage-clients.
        // admin-cli uses lightweight tokens — roles are re-resolved from the user model on every
        // request, which coincidentally prevents the escalation. A full-token client is needed to
        // exercise the token-stripping protection in removeTransientAdminRoles. If the role is not
        // stripped, canMapRole() sees manage-clients in the token and allows the grant.
        ClientRepresentation fullTokenClient = createClient(testRealm, "full-token-client",
                toRepresentation(HardcodedRole.create("inject-manage-clients",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_CLIENTS))
        );

        // Even with manage-clients injected into the full token, the attacker must not be able
        // to grant themselves manage-clients since it is not actually granted to them.
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, fullTokenClient.getClientId(), attackerName, attackerClient -> {
                grantRealmManagementRole(attackerClient.realm(realmName), attackerName, AdminRoles.MANAGE_CLIENTS);
            });
        });
    }

    @Test
    public void testAdminRolesNotInTokenWhenNotGranted() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String username = "test-user";
        createUser(testRealm, username);

        ClientRepresentation client = createClient(testRealm, "test-client",
                toRepresentation(HardcodedRole.create("inject-view-clients",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.VIEW_CLIENTS)),
                toRepresentation(HardcodedRole.create("inject-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS)),
                toRepresentation(HardcodedRole.create("inject-manage-realm",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM))
        );

        runAs(realmName, client.getClientId(), username, userClient -> {
            AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
            try {
                AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();
                AccessToken.Access realmMgmtAccess = token.getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);

                if (realmMgmtAccess != null && realmMgmtAccess.getRoles() != null) {
                    Set<String> roles = realmMgmtAccess.getRoles();
                    assertFalse(roles.contains(AdminRoles.VIEW_CLIENTS),
                            AdminRoles.VIEW_CLIENTS + " should not be in the token");
                    assertFalse(roles.contains(AdminRoles.MANAGE_USERS),
                            AdminRoles.MANAGE_USERS + " should not be in the token");
                    assertFalse(roles.contains(AdminRoles.MANAGE_REALM),
                            AdminRoles.MANAGE_REALM + " should not be in the token");
                    assertTrue(roles.stream().noneMatch(AdminRoles.ALL_ROLES::contains),
                            "No admin roles should be in the token");
                }
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testIgnoreRealmLevelAdminRoleGrantedViaProtocolMapper() {
        ClientRepresentation client = createClient(masterRealm.admin(), "realm-level-mapper-client",
                toRepresentation(HardcodedRole.create("inject-create-realm", AdminRoles.CREATE_REALM))
        );
        try {
            assertThrows(ForbiddenException.class, () -> {
                runAs(masterRealm.getName(), client.getClientId(), masterUser.getUsername(), userClient -> {
                    createRealm(userClient, "injected-realm");
                });
            });
        } finally {
            masterRealm.admin().clients().get(client.getId()).remove();
        }
    }

    @Test
    public void testLegitimateRealmLevelAdminRoleNotStrippedWhenMapperAlsoPresent() {
        grantRealmRole(masterRealm.admin(), masterUser.admin().toRepresentation(), AdminRoles.CREATE_REALM);
        ClientRepresentation client = createClient(masterRealm.admin(), "redundant-realm-mapper-client",
                toRepresentation(HardcodedRole.create("redundant-create-realm", AdminRoles.CREATE_REALM))
        );
        try {
            runAs(masterRealm.getName(), client.getClientId(), masterUser.getUsername(), userClient -> {
                createRealm(userClient, "legit-realm");
            });
        } finally {
            masterRealm.admin().clients().get(client.getId()).remove();
        }
    }

    @Test
    public void testRealmLevelAdminRolesNotInTokenWhenNotGranted() {
        ClientRepresentation client = createClient(masterRealm.admin(), "realm-level-token-test-client",
                toRepresentation(HardcodedRole.create("inject-admin", AdminRoles.ADMIN)),
                toRepresentation(HardcodedRole.create("inject-create-realm", AdminRoles.CREATE_REALM))
        );
        try {
            runAs(masterRealm.getName(), client.getClientId(), masterUser.getUsername(), userClient -> {
                AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
                try {
                    AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();
                    AccessToken.Access realmAccess = token.getRealmAccess();

                    if (realmAccess != null && realmAccess.getRoles() != null) {
                        Set<String> roles = realmAccess.getRoles();
                        assertFalse(roles.contains(AdminRoles.ADMIN),
                                AdminRoles.ADMIN + " should not be in the token");
                        assertFalse(roles.contains(AdminRoles.CREATE_REALM),
                                AdminRoles.CREATE_REALM + " should not be in the token");
                        assertTrue(roles.stream().noneMatch(AdminRoles.ALL_ROLES::contains),
                                "No admin roles should be in the token's realm access");
                    }
                } catch (VerificationException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            masterRealm.admin().clients().get(client.getId()).remove();
        }
    }

    @Test
    public void testIgnoreCompositeRealmRoleWithRealmManagementAdminRoles() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        testRealm.roles().create(RoleConfigBuilder.create().name("custom-admin").build());
        ClientRepresentation realmMgmt = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation viewClients = testRealm.clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        testRealm.roles().get("custom-admin").addComposites(List.of(viewClients));

        String username = "test-user";
        createUser(testRealm, username);

        ClientRepresentation client = createClient(testRealm, "composite-mapper-client",
                toRepresentation(HardcodedRole.create("inject-custom-admin", "custom-admin"))
        );

        runAs(realmName, client.getClientId(), username, userClient -> {
            AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
            try {
                AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();
                AccessToken.Access realmAccess = token.getRealmAccess();

                if (realmAccess != null && realmAccess.getRoles() != null) {
                    assertFalse(realmAccess.getRoles().contains("custom-admin"),
                            "Composite realm role containing realm-management admin roles should be stripped");
                }
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testIgnoreCompositeRealmRoleWithMasterRealmClientAdminRoles() {
        String targetRealmName = "target-realm";
        createRealm(adminClient, targetRealmName);

        masterRealm.admin().roles().create(RoleConfigBuilder.create().name("cross-realm-admin").build());
        ClientRepresentation realmClient = masterRealm.admin().clients()
                .findByClientId(targetRealmName + "-realm").get(0);
        RoleRepresentation manageUsers = masterRealm.admin().clients().get(realmClient.getId())
                .roles().get(AdminRoles.MANAGE_USERS).toRepresentation();
        masterRealm.admin().roles().get("cross-realm-admin").addComposites(List.of(manageUsers));

        ClientRepresentation client = createClient(masterRealm.admin(), "cross-realm-mapper-client",
                toRepresentation(HardcodedRole.create("inject-cross-realm-admin", "cross-realm-admin"))
        );

        try {
            runAs(masterRealm.getName(), client.getClientId(), masterUser.getUsername(), userClient -> {
                AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
                try {
                    AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();
                    AccessToken.Access realmAccess = token.getRealmAccess();

                    if (realmAccess != null && realmAccess.getRoles() != null) {
                        assertFalse(realmAccess.getRoles().contains("cross-realm-admin"),
                                "Composite realm role containing <realm>-realm admin roles should be stripped");
                    }
                } catch (VerificationException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            masterRealm.admin().clients().get(client.getId()).remove();
            masterRealm.admin().roles().get("cross-realm-admin").remove();
        }
    }

    @Test
    public void testCompositeRealmRoleWithAdminRolesNotStrippedWhenGranted() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        testRealm.roles().create(RoleConfigBuilder.create().name("custom-admin").build());
        ClientRepresentation realmMgmt = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation viewClients = testRealm.clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        testRealm.roles().get("custom-admin").addComposites(List.of(viewClients));

        String username = "test-user";
        UserRepresentation user = createUser(testRealm, username);
        grantRealmRole(testRealm, user, "custom-admin");

        ClientRepresentation client = createClient(testRealm, "composite-mapper-client",
                toRepresentation(HardcodedRole.create("redundant-custom-admin", "custom-admin"))
        );

        runAs(realmName, client.getClientId(), username, userClient -> {
            assertFalse(userClient.realm(realmName).clients().findAll().isEmpty());
        });
    }

    @Test
    public void testManageUsersAdminCannotGrantRealmAdmin() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);
        String victimName = "target-user";
        createUser(testRealm, victimName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.VIEW_CLIENTS);

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                grantRealmManagementRole(attackerClient.realm(realmName), victimName, AdminRoles.REALM_ADMIN);
            });
        });
    }

    private ClientRepresentation createClient(RealmResource realm, String clientId, ProtocolMapperRepresentation... mapper) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setProtocolMappers(List.of(mapper));
        try (Response response = realm.clients().create(client)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            client.setId(ApiUtil.getCreatedId(response));
        }
        return client;
    }

    private void grantRealmManagementRole(RealmResource testRealm, GroupRepresentation group, String role) {
        ClientRepresentation realmMgmt = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource realmMgmtResource = testRealm.clients().get(realmMgmt.getId());
        RoleRepresentation adminRole = realmMgmtResource.roles()
                .get(role).toRepresentation();
        testRealm.groups().group(group.getId()).roles()
                .clientLevel(realmMgmt.getId())
                .add(List.of(adminRole));
    }
}
