package org.keycloak.tests.admin.authz.rbac;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
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
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAdminAccessTest extends AbstractAdminRBACTest {

    @InjectAdminClientFactory
    AdminClientFactory scopedClientFactory;

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
    public void testCompositeRealmRoleWithAdminSubRolesNotStrippedFromToken() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        testRealm.roles().create(RoleBuilder.create().name("custom-admin").build());
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

                assertTrue(realmAccess != null
                                && realmAccess.getRoles() != null
                                && realmAccess.getRoles().contains("custom-admin"),
                        "Composite realm role with a non-admin name should remain in the token");

                AccessToken.Access realmMgmtAccess = token.getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);
                if (realmMgmtAccess != null && realmMgmtAccess.getRoles() != null) {
                    assertFalse(realmMgmtAccess.getRoles().contains(AdminRoles.VIEW_CLIENTS),
                            "Admin sub-role should not be granted via composite injection");
                }
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, client.getClientId(), username, userClient -> {
                userClient.realm(realmName).clients().findAll();
            });
        });
    }

    @Test
    public void testCompositeRealmRoleWithMasterRealmClientAdminSubRolesNotStrippedFromToken() {
        String targetRealmName = "target-realm";
        createRealm(adminClient, targetRealmName);

        masterRealm.admin().roles().create(RoleBuilder.create().name("cross-realm-admin").build());
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

                    assertTrue(realmAccess != null
                                    && realmAccess.getRoles() != null
                                    && realmAccess.getRoles().contains("cross-realm-admin"),
                            "Composite realm role with a non-admin name should remain in the token");

                    AccessToken.Access realmClientAccess = token.getResourceAccess(targetRealmName + "-realm");
                    if (realmClientAccess != null && realmClientAccess.getRoles() != null) {
                        assertFalse(realmClientAccess.getRoles().contains(AdminRoles.MANAGE_USERS),
                                "Admin sub-role should not be granted via composite injection");
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

        testRealm.roles().create(RoleBuilder.create().name("custom-admin").build());
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
    public void testCustomClientRoleWithAdminRoleNameNotStripped() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String username = "test-user";
        UserRepresentation user = createUser(testRealm, username);

        ClientRepresentation customApp = new ClientRepresentation();
        customApp.setClientId("custom-app");
        customApp.setEnabled(true);
        customApp.setPublicClient(true);
        customApp.setDirectAccessGrantsEnabled(true);
        customApp.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        try (Response response = testRealm.clients().create(customApp)) {
            customApp.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation customManageUsers = new RoleRepresentation();
        customManageUsers.setName(AdminRoles.MANAGE_USERS);
        testRealm.clients().get(customApp.getId()).roles().create(customManageUsers);
        customManageUsers = testRealm.clients().get(customApp.getId())
                .roles().get(AdminRoles.MANAGE_USERS).toRepresentation();

        testRealm.users().get(user.getId()).roles()
                .clientLevel(customApp.getId()).add(List.of(customManageUsers));

        ClientRepresentation tokenClient = createClient(testRealm, "token-client",
                toRepresentation(HardcodedRole.create("inject-custom-manage-users",
                        "custom-app." + AdminRoles.MANAGE_USERS))
        );

        runAs(realmName, tokenClient.getClientId(), username, userClient -> {
            AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
            try {
                AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();
                AccessToken.Access customAppAccess = token.getResourceAccess("custom-app");

                assertTrue(customAppAccess != null
                                && customAppAccess.getRoles() != null
                                && customAppAccess.getRoles().contains(AdminRoles.MANAGE_USERS),
                        "Custom client role named '" + AdminRoles.MANAGE_USERS
                                + "' on non-admin client should not be stripped from the token");

                AccessToken.Access realmMgmtAccess = token.getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);
                if (realmMgmtAccess != null && realmMgmtAccess.getRoles() != null) {
                    assertTrue(realmMgmtAccess.getRoles().stream().noneMatch(AdminRoles.ALL_ROLES::contains),
                            "No admin roles should appear on realm-management for this user");
                }
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testInjectedAdminRoleStrippedDespiteNameCollisionWithCustomClientRole() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String username = "test-user";
        UserRepresentation user = createUser(testRealm, username);

        // Create a custom client with a role named identically to an admin role
        ClientRepresentation customApp = new ClientRepresentation();
        customApp.setClientId("custom-app");
        customApp.setEnabled(true);
        customApp.setPublicClient(true);
        customApp.setDirectAccessGrantsEnabled(true);
        customApp.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        try (Response response = testRealm.clients().create(customApp)) {
            customApp.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation customManageUsers = new RoleRepresentation();
        customManageUsers.setName(AdminRoles.MANAGE_USERS);
        testRealm.clients().get(customApp.getId()).roles().create(customManageUsers);
        customManageUsers = testRealm.clients().get(customApp.getId())
                .roles().get(AdminRoles.MANAGE_USERS).toRepresentation();

        // User has custom-app.manage-users but NOT realm-management.manage-users
        testRealm.users().get(user.getId()).roles()
                .clientLevel(customApp.getId()).add(List.of(customManageUsers));

        // Hardcoded mapper injects the ADMIN version of manage-users on realm-management
        ClientRepresentation tokenClient = createClient(testRealm, "token-client",
                toRepresentation(HardcodedRole.create("inject-admin-manage-users",
                        Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_USERS))
        );

        runAs(realmName, tokenClient.getClientId(), username, userClient -> {
            AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();
            try {
                AccessToken token = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class).getToken();

                // custom-app.manage-users should remain (non-admin client, legitimately granted)
                AccessToken.Access customAppAccess = token.getResourceAccess("custom-app");
                assertTrue(customAppAccess != null
                                && customAppAccess.getRoles() != null
                                && customAppAccess.getRoles().contains(AdminRoles.MANAGE_USERS),
                        "custom-app." + AdminRoles.MANAGE_USERS + " should not be stripped");

                // realm-management.manage-users was injected, not granted — must be stripped
                AccessToken.Access realmMgmtAccess = token.getResourceAccess(Constants.REALM_MANAGEMENT_CLIENT_ID);
                assertTrue(realmMgmtAccess == null
                                || realmMgmtAccess.getRoles() == null
                                || !realmMgmtAccess.getRoles().contains(AdminRoles.MANAGE_USERS),
                        "Injected " + AdminRoles.MANAGE_USERS + " on realm-management should be stripped "
                                + "even when the user has a same-named role on a non-admin client");
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });

        // The injected admin role should not grant actual admin access
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, tokenClient.getClientId(), username, userClient -> {
                userClient.realm(realmName).users().list();
            });
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

    @Test
    public void testClientScopeLimitsRbacAccess() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        createUser(testRealm, "scoped-admin");
        grantRealmManagementRole(testRealm, "scoped-admin", AdminRoles.REALM_ADMIN);

        String realmMgmtUuid = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        ClientRepresentation restrictedClient = createRestrictedScopeClient(testRealm, "restricted-client");
        RoleRepresentation viewUsersRole = testRealm.clients().get(realmMgmtUuid)
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        testRealm.clients().get(restrictedClient.getId()).getScopeMappings()
                .clientLevel(realmMgmtUuid).add(List.of(viewUsersRole));

        runAs(realmName, "restricted-client", "scoped-admin", client -> {
            assertFalse(client.realm(realmName).users().search("").isEmpty());
        });

        runAs(realmName, "restricted-client", "scoped-admin", client -> {
            assertEquals(Status.FORBIDDEN.getStatusCode(), client.realm(realmName).users().create(
                    UserBuilder.create("should-not-be-created").build()).getStatus());
        });
    }

    @Test
    public void testMasterRealmRealmRoleScopeBoundary() {
        grantRealmRole(masterRealm.admin(), masterUser.admin().toRepresentation(), AdminRoles.CREATE_REALM);

        ClientRepresentation restrictedClient = createRestrictedScopeClient(
                masterRealm.admin(), "restricted-client");
        try {
            assertThrows(ForbiddenException.class, () -> {
                runAs("master", "restricted-client", masterUser.getUsername(), client -> {
                    RealmRepresentation r = new RealmRepresentation();
                    r.setRealm("scope-bypass-realm");
                    r.setEnabled(true);
                    client.realms().create(r);
                });
            });
        } finally {
            masterRealm.admin().clients().get(restrictedClient.getId()).remove();
        }
    }

    @Test
    public void testMasterRealmClientScopeBoundary() {
        String targetRealmName = "target-realm";
        createRealm(adminClient, targetRealmName);
        grantMasterRealmManagementRole(targetRealmName, masterUser.getUsername(), AdminRoles.VIEW_USERS);
        grantMasterRealmManagementRole(targetRealmName, masterUser.getUsername(), AdminRoles.MANAGE_USERS);
        grantMasterRealmManagementRole(targetRealmName, masterUser.getUsername(), AdminRoles.QUERY_USERS);

        String masterAdminClientUuid = masterRealm.admin().clients()
                .findByClientId(targetRealmName + "-realm").get(0).getId();
        ClientRepresentation restrictedClient = createRestrictedScopeClient(
                masterRealm.admin(), "restricted-client");
        RoleRepresentation viewUsersRole = masterRealm.admin().clients().get(masterAdminClientUuid)
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        RoleRepresentation queryUsersRole = masterRealm.admin().clients().get(masterAdminClientUuid)
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        masterRealm.admin().clients().get(restrictedClient.getId()).getScopeMappings()
                .clientLevel(masterAdminClientUuid).add(List.of(viewUsersRole, queryUsersRole));

        try {
            // view-users + query-users are in scope — listing users should not throw
            runAs("master", "restricted-client", masterUser.getUsername(), client -> {
                client.realm(targetRealmName).users().search("");
            });

            // manage-users is NOT in scope — creating a user should be denied
            runAs("master", "restricted-client", masterUser.getUsername(), client -> {
                assertEquals(Status.FORBIDDEN.getStatusCode(), client.realm(targetRealmName).users().create(
                        UserBuilder.create("should-not-be-created-from-master").build()).getStatus());
            });
        } finally {
            masterRealm.admin().clients().get(restrictedClient.getId()).remove();
        }
    }

    @Test
    public void testDifferentlyScopedTokensFromSameClientHaveDifferentEffectiveAdminRoles() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        UserRepresentation user = createUser(testRealm, "scoped-admin");

        String realmMgmtUuid = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();

        for (String roleName : List.of(AdminRoles.VIEW_CLIENTS, AdminRoles.QUERY_CLIENTS,
                AdminRoles.VIEW_USERS, AdminRoles.QUERY_USERS)) {
            RoleRepresentation role = testRealm.clients().get(realmMgmtUuid)
                    .roles().get(roleName).toRepresentation();
            testRealm.users().get(user.getId()).roles()
                    .clientLevel(realmMgmtUuid).add(List.of(role));
        }

        // Optional scope that brings view-clients + query-clients into the token
        ClientScopeRepresentation viewClientsScope = new ClientScopeRepresentation();
        viewClientsScope.setName("scope-view-clients");
        viewClientsScope.setProtocol("openid-connect");
        try (Response response = testRealm.clientScopes().create(viewClientsScope)) {
            viewClientsScope.setId(ApiUtil.getCreatedId(response));
        }
        testRealm.clientScopes().get(viewClientsScope.getId()).getScopeMappings()
                .clientLevel(realmMgmtUuid).add(List.of(
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation(),
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation()));

        // Optional scope that brings view-users + query-users into the token
        ClientScopeRepresentation viewUsersScope = new ClientScopeRepresentation();
        viewUsersScope.setName("scope-view-users");
        viewUsersScope.setProtocol("openid-connect");
        try (Response response = testRealm.clientScopes().create(viewUsersScope)) {
            viewUsersScope.setId(ApiUtil.getCreatedId(response));
        }
        testRealm.clientScopes().get(viewUsersScope.getId()).getScopeMappings()
                .clientLevel(realmMgmtUuid).add(List.of(
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.VIEW_USERS).toRepresentation(),
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.QUERY_USERS).toRepresentation()));

        // Non-full-scope client with both optional scopes
        ClientRepresentation tokenClient = createRestrictedScopeClient(testRealm, "test-client");
        testRealm.clients().get(tokenClient.getId()).addOptionalClientScope(viewClientsScope.getId());
        testRealm.clients().get(tokenClient.getId()).addOptionalClientScope(viewUsersScope.getId());

        // Token requested with scope=scope-view-clients: only view-clients roles in the token
        try (Keycloak viewClientsOnly = scopedClientFactory.create()
                .realm(realmName)
                .clientId("test-client")
                .username("scoped-admin")
                .password("password")
                .scope("scope-view-clients")
                .build()) {
            assertFalse(viewClientsOnly.realm(realmName).clients().findAll().isEmpty(),
                    "Token with view-clients scope should be able to list clients");
            assertThrows(ForbiddenException.class, () ->
                            viewClientsOnly.realm(realmName).users().search(""),
                    "Token with only view-clients scope should NOT be able to list users");
        }

        // Token requested with scope=scope-view-users: only view-users roles in the token
        try (Keycloak viewUsersOnly = scopedClientFactory.create()
                .realm(realmName)
                .clientId("test-client")
                .username("scoped-admin")
                .password("password")
                .scope("scope-view-users")
                .build()) {
            assertFalse(viewUsersOnly.realm(realmName).users().search("").isEmpty(),
                    "Token with view-users scope should be able to list users");
            assertThrows(ForbiddenException.class, () ->
                            viewUsersOnly.realm(realmName).clients().findAll(),
                    "Token with only view-users scope should NOT be able to list clients");
        }
    }

    @Test
    public void testDifferentlyScopedLightweightTokensFromSameClientHaveDifferentEffectiveAdminRoles() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        UserRepresentation user = createUser(testRealm, "scoped-admin");

        String realmMgmtUuid = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();

        for (String roleName : List.of(AdminRoles.VIEW_CLIENTS, AdminRoles.QUERY_CLIENTS,
                AdminRoles.VIEW_USERS, AdminRoles.QUERY_USERS)) {
            RoleRepresentation role = testRealm.clients().get(realmMgmtUuid)
                    .roles().get(roleName).toRepresentation();
            testRealm.users().get(user.getId()).roles()
                    .clientLevel(realmMgmtUuid).add(List.of(role));
        }

        ClientScopeRepresentation viewClientsScope = new ClientScopeRepresentation();
        viewClientsScope.setName("scope-view-clients");
        viewClientsScope.setProtocol("openid-connect");
        try (Response response = testRealm.clientScopes().create(viewClientsScope)) {
            viewClientsScope.setId(ApiUtil.getCreatedId(response));
        }
        testRealm.clientScopes().get(viewClientsScope.getId()).getScopeMappings()
                .clientLevel(realmMgmtUuid).add(List.of(
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation(),
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation()));

        ClientScopeRepresentation viewUsersScope = new ClientScopeRepresentation();
        viewUsersScope.setName("scope-view-users");
        viewUsersScope.setProtocol("openid-connect");
        try (Response response = testRealm.clientScopes().create(viewUsersScope)) {
            viewUsersScope.setId(ApiUtil.getCreatedId(response));
        }
        testRealm.clientScopes().get(viewUsersScope.getId()).getScopeMappings()
                .clientLevel(realmMgmtUuid).add(List.of(
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.VIEW_USERS).toRepresentation(),
                        testRealm.clients().get(realmMgmtUuid).roles().get(AdminRoles.QUERY_USERS).toRepresentation()));

        ClientRepresentation tokenClient = ClientBuilder.create("test-client-lightweight")
                .publicClient()
                .directAccessGrantsEnabled()
                .fullScopeEnabled(false)
                .attribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString())
                .build();
        try (Response response = testRealm.clients().create(tokenClient)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            tokenClient.setId(ApiUtil.getCreatedId(response));
        }
        testRealm.clients().get(tokenClient.getId()).addOptionalClientScope(viewClientsScope.getId());
        testRealm.clients().get(tokenClient.getId()).addOptionalClientScope(viewUsersScope.getId());

        try (Keycloak viewClientsOnly = scopedClientFactory.create()
                .realm(realmName)
                .clientId("test-client-lightweight")
                .username("scoped-admin")
                .password("password")
                .scope("scope-view-clients")
                .build()) {
            assertFalse(viewClientsOnly.realm(realmName).clients().findAll().isEmpty(),
                    "Lightweight token with view-clients scope should be able to list clients");
            assertThrows(ForbiddenException.class, () ->
                            viewClientsOnly.realm(realmName).users().search(""),
                    "Lightweight token with only view-clients scope should NOT be able to list users");
        }

        try (Keycloak viewUsersOnly = scopedClientFactory.create()
                .realm(realmName)
                .clientId("test-client-lightweight")
                .username("scoped-admin")
                .password("password")
                .scope("scope-view-users")
                .build()) {
            assertFalse(viewUsersOnly.realm(realmName).users().search("").isEmpty(),
                    "Lightweight token with view-users scope should be able to list users");
            assertThrows(ForbiddenException.class, () ->
                            viewUsersOnly.realm(realmName).clients().findAll(),
                    "Lightweight token with only view-users scope should NOT be able to list clients");
        }
    }

    private ClientRepresentation createRestrictedScopeClient(RealmResource realm, String clientId) {
        ClientRepresentation client = ClientBuilder.create(clientId)
                .publicClient()
                .directAccessGrantsEnabled()
                .fullScopeEnabled(false)
                .build();
        try (Response response = realm.clients().create(client)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            client.setId(ApiUtil.getCreatedId(response));
        }
        return client;
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
