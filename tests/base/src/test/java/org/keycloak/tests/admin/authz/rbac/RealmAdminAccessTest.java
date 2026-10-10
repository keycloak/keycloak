package org.keycloak.tests.admin.authz.rbac;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.common.VerificationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAdminAccessTest extends AbstractAdminRBACTest {

    @InjectAdminClientFactory
    AdminClientFactory scopedClientFactory;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

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
    public void testGroupsInRoleFiltersGroupsCallerCannotView() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        testRealm.roles().create(RoleBuilder.create().name("shared-role").build());

        ClientRepresentation appClient = new ClientRepresentation();
        appClient.setClientId("app1");
        appClient.setEnabled(true);
        String appClientId;
        try (Response response = testRealm.clients().create(appClient)) {
            appClientId = ApiUtil.getCreatedId(response);
        }
        testRealm.clients().get(appClientId).roles().create(RoleBuilder.create().name("app-role").build());

        GroupRepresentation secretGroup = new GroupRepresentation();
        secretGroup.setName("secret-group");
        secretGroup.setAttributes(Map.of("cost-center", List.of("CONFIDENTIAL-4471")));
        String secretGroupId;
        try (Response response = testRealm.groups().add(secretGroup)) {
            secretGroupId = ApiUtil.getCreatedId(response);
        }
        GroupRepresentation visibleGroup = new GroupRepresentation();
        visibleGroup.setName("visible-group");
        String visibleGroupId;
        try (Response response = testRealm.groups().add(visibleGroup)) {
            visibleGroupId = ApiUtil.getCreatedId(response);
        }

        RoleRepresentation sharedRole = testRealm.roles().get("shared-role").toRepresentation();
        RoleRepresentation appRole = testRealm.clients().get(appClientId).roles().get("app-role").toRepresentation();
        testRealm.groups().group(secretGroupId).roles().realmLevel().add(List.of(sharedRole));
        testRealm.groups().group(secretGroupId).roles().clientLevel(appClientId).add(List.of(appRole));
        testRealm.groups().group(visibleGroupId).roles().realmLevel().add(List.of(sharedRole));

        // Delegated admin can search groups and view the role containers, but cannot view any group.
        String username = "limited-admin";
        createUser(testRealm, username);
        grantRealmManagementRole(testRealm, username, AdminRoles.QUERY_GROUPS);
        grantRealmManagementRole(testRealm, username, AdminRoles.VIEW_REALM);
        grantRealmManagementRole(testRealm, username, AdminRoles.VIEW_CLIENTS);

        // A full-access admin still sees every group holding the role (no regression).
        Set<String> groupsForFullAdmin = testRealm.roles().get("shared-role").getRoleGroupMembers().stream()
                .map(GroupRepresentation::getName).collect(Collectors.toSet());
        assertEquals(Set.of("secret-group", "visible-group"), groupsForFullAdmin);

        runAs(realmName, "admin-cli", username, userClient -> {
            RealmResource realm = userClient.realm(realmName);

            // Control: the same caller is denied direct access to the groups.
            assertForbidden("delegated admin must not view a group directly",
                    () -> realm.groups().group(secretGroupId).toRepresentation());

            // The role-groups endpoints must not disclose groups the caller cannot view,
            // including with full representations (attributes and role mappings).
            assertTrue(realm.roles().get("shared-role").getRoleGroupMembers(false, 0, 100).isEmpty(),
                    "realm role groups endpoint disclosed groups the caller cannot view");
            String appId = realm.clients().findByClientId("app1").get(0).getId();
            assertTrue(realm.clients().get(appId).roles().get("app-role").getRoleGroupMembers(false, 0, 100).isEmpty(),
                    "client role groups endpoint disclosed groups the caller cannot view");
        });

        // Once the delegated admin is allowed to view groups, the same endpoints must return them:
        // the filter tightens access without over-restricting an authorized caller.
        grantRealmManagementRole(testRealm, username, AdminRoles.VIEW_USERS);

        runAs(realmName, "admin-cli", username, userClient -> {
            RealmResource realm = userClient.realm(realmName);

            realm.groups().group(secretGroupId).toRepresentation();

            GroupRepresentation secret = realm.roles().get("shared-role").getRoleGroupMembers(false, 0, 100).stream()
                    .filter(g -> "secret-group".equals(g.getName())).findFirst().orElseThrow();
            assertEquals(Set.of("secret-group", "visible-group"),
                    realm.roles().get("shared-role").getRoleGroupMembers(false, 0, 100).stream()
                            .map(GroupRepresentation::getName).collect(Collectors.toSet()));
            // A caller allowed to view the group and the mapped roles sees them in the full representation.
            assertEquals(List.of("shared-role"), secret.getRealmRoles());
            assertEquals(Map.of("app1", List.of("app-role")), secret.getClientRoles());

            String appId = realm.clients().findByClientId("app1").get(0).getId();
            Set<String> clientRoleGroups = realm.clients().get(appId).roles().get("app-role").getRoleGroupMembers(false, 0, 100).stream()
                    .map(GroupRepresentation::getName).collect(Collectors.toSet());
            assertEquals(Set.of("secret-group"), clientRoleGroups);
        });

        // A caller allowed to view the group but not a role mapped to it must not see that role in
        // the representation: the group's app-role (client role) is filtered out because this admin
        // cannot view the client or map the role.
        String roleLimited = "role-limited-admin";
        createUser(testRealm, roleLimited);
        grantRealmManagementRole(testRealm, roleLimited, AdminRoles.VIEW_REALM);
        grantRealmManagementRole(testRealm, roleLimited, AdminRoles.VIEW_USERS);

        runAs(realmName, "admin-cli", roleLimited, userClient -> {
            RealmResource realm = userClient.realm(realmName);

            GroupRepresentation secret = realm.roles().get("shared-role").getRoleGroupMembers(false, 0, 100).stream()
                    .filter(g -> "secret-group".equals(g.getName())).findFirst().orElseThrow();

            assertEquals(List.of("shared-role"), secret.getRealmRoles());
            assertTrue(secret.getClientRoles() == null || secret.getClientRoles().isEmpty(),
                    "client role mapping the caller cannot view must be filtered from the group representation");
        });
    }

    @Test
    public void testScopeMappingsFilterRolesCallerCannotView() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        Map<String, List<String>> parentRoleAttributes = Map.of("cost-center", List.of("CONFIDENTIAL-4471"));
        testRealm.roles().create(RoleBuilder.create().name("child-role").build());
        testRealm.roles().create(RoleBuilder.create().name("parent-role").attributes(parentRoleAttributes).build());
        RoleRepresentation childRole = testRealm.roles().get("child-role").toRepresentation();
        testRealm.roles().get("parent-role").addComposites(List.of(childRole));
        RoleRepresentation parentRole = testRealm.roles().get("parent-role").toRepresentation();

        ClientRepresentation appClient = createClient(testRealm, "app1");
        testRealm.clients().get(appClient.getId()).roles().create(RoleBuilder.create().name("app-role").build());
        RoleRepresentation appRole = testRealm.clients().get(appClient.getId()).roles().get("app-role").toRepresentation();

        ClientRepresentation scopedClient = createRestrictedScopeClient(testRealm, "scoped-client");
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("scoped-client-scope");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        try (Response response = testRealm.clientScopes().create(clientScope)) {
            clientScope.setId(ApiUtil.getCreatedId(response));
        }
        for (RoleMappingResource scopeMappings : scopeMappingEndpoints(testRealm, scopedClient.getId(), clientScope.getId()).values()) {
            scopeMappings.realmLevel().add(List.of(parentRole));
            scopeMappings.clientLevel(appClient.getId()).add(List.of(appRole));
        }

        createDelegatedAdmin(testRealm, "view-clients-admin", AdminRoles.VIEW_CLIENTS);
        createDelegatedAdmin(testRealm, "view-realm-admin", AdminRoles.VIEW_CLIENTS, AdminRoles.VIEW_REALM);
        createDelegatedAdmin(testRealm, "manage-users-admin", AdminRoles.VIEW_CLIENTS, AdminRoles.MANAGE_USERS);

        runAs(realmName, "view-clients-admin", userClient -> {
            RealmResource realm = userClient.realm(realmName);
            assertForbidden("view-clients admin must not view a realm role directly",
                    () -> realm.rolesById().getRole(childRole.getId()));

            scopeMappingEndpoints(realm, scopedClient.getId(), clientScope.getId()).forEach((endpoint, scopeMappings) -> {
                assertEquals(Set.of(), toNames(scopeMappings.realmLevel().listAll()), endpoint);
                assertEquals(Set.of(), toNames(scopeMappings.realmLevel().listEffective()), endpoint);
                assertEquals(Set.of(), toNames(scopeMappings.realmLevel().listEffective(false)), endpoint);
                assertEquals(Set.of("app-role"), toNames(scopeMappings.clientLevel(appClient.getId()).listAll()), endpoint);
                assertEquals(Set.of("app-role"), toNames(scopeMappings.clientLevel(appClient.getId()).listEffective()), endpoint);

                MappingsRepresentation mappings = scopeMappings.getAll();
                assertEquals(Set.of(), toNames(mappings.getRealmMappings()), endpoint);
                assertEquals(Set.of("app1"), mappings.getClientMappings().keySet(), endpoint);
            });
        });

        for (String admin : List.of("view-realm-admin", "manage-users-admin")) {
            runAs(realmName, admin, userClient -> {
                scopeMappingEndpoints(userClient.realm(realmName), scopedClient.getId(), clientScope.getId()).forEach((endpoint, scopeMappings) -> {
                    String message = admin + " on " + endpoint;
                    assertEquals(Set.of("parent-role"), toNames(scopeMappings.realmLevel().listAll()), message);
                    assertEquals(Set.of("parent-role", "child-role"), toNames(scopeMappings.realmLevel().listEffective()), message);
                    assertEquals(Set.of("parent-role"), toNames(scopeMappings.getAll().getRealmMappings()), message);

                    RoleRepresentation parent = scopeMappings.realmLevel().listEffective(false).stream()
                            .filter(role -> "parent-role".equals(role.getName())).findFirst().orElseThrow();
                    assertEquals(parentRoleAttributes, parent.getAttributes(), message);
                });
            });
        }
    }

    @Test
    public void testClientScopeEvaluationFiltersRealmRolesCallerCannotView() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        Map<String, List<String>> attributes = Map.of("classification", List.of("sensitive"));
        testRealm.roles().create(RoleBuilder.create().name("granted-role").attributes(attributes).build());
        testRealm.roles().create(RoleBuilder.create().name("not-granted-role").attributes(attributes).build());
        RoleRepresentation grantedRole = testRealm.roles().get("granted-role").toRepresentation();

        ClientRepresentation evaluatedClient = createRestrictedScopeClient(testRealm, "evaluated-client");
        testRealm.clients().get(evaluatedClient.getId()).getScopeMappings().realmLevel().add(List.of(grantedRole));

        createDelegatedAdmin(testRealm, "view-clients-admin", AdminRoles.VIEW_CLIENTS);
        createDelegatedAdmin(testRealm, "view-realm-admin", AdminRoles.VIEW_CLIENTS, AdminRoles.VIEW_REALM);

        runAs(realmName, "view-clients-admin", userClient -> {
            assertForbidden("view-clients admin must not view a realm role directly",
                    () -> userClient.realm(realmName).rolesById().getRole(grantedRole.getId()));
            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                assertEquals(Set.of(), toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "granted")));
                assertEquals(Set.of(), toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "not-granted")));
            }
        });

        runAs(realmName, "view-realm-admin", userClient -> {
            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                List<RoleRepresentation> granted = evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "granted");
                Set<String> grantedNames = toNames(granted);
                assertTrue(grantedNames.contains("granted-role"));
                assertFalse(grantedNames.contains("not-granted-role"));
                assertNull(granted.stream().filter(role -> "granted-role".equals(role.getName()))
                        .findFirst().orElseThrow().getAttributes());

                List<RoleRepresentation> notGranted = evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "not-granted");
                Set<String> notGrantedNames = toNames(notGranted);
                assertTrue(notGrantedNames.contains("not-granted-role"));
                assertFalse(notGrantedNames.contains("granted-role"));
                assertNull(notGranted.stream().filter(role -> "not-granted-role".equals(role.getName()))
                        .findFirst().orElseThrow().getAttributes());
            }
        });

        ClientRepresentation update = testRealm.clients().get(evaluatedClient.getId()).toRepresentation();
        update.setFullScopeAllowed(true);
        testRealm.clients().get(evaluatedClient.getId()).update(update);

        runAs(realmName, "view-clients-admin", userClient -> {
            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                assertEquals(Set.of(), toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "granted")));
                assertEquals(Set.of(), toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "not-granted")));
            }
        });

        runAs(realmName, "view-realm-admin", userClient -> {
            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                Set<String> grantedNames = toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "granted"));
                assertTrue(grantedNames.contains("granted-role"));
                assertTrue(grantedNames.contains("not-granted-role"));
                assertEquals(Set.of(), toNames(evaluateScopeMappings(httpClient, userClient, realmName,
                        evaluatedClient.getId(), realmName, "not-granted")));
            }
        });
    }

    private void createDelegatedAdmin(RealmResource realm, String username, String... adminRoles) {
        createUser(realm, username);
        for (String adminRole : adminRoles) {
            grantRealmManagementRole(realm, username, adminRole);
        }
    }

    private static Map<String, RoleMappingResource> scopeMappingEndpoints(RealmResource realm, String clientUuid, String clientScopeId) {
        return Map.of("clients/{id}/scope-mappings", realm.clients().get(clientUuid).getScopeMappings(),
                "client-scopes/{id}/scope-mappings", realm.clientScopes().get(clientScopeId).getScopeMappings());
    }

    private static Set<String> toNames(List<RoleRepresentation> roles) {
        return roles == null ? Set.of() : roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private List<RoleRepresentation> evaluateScopeMappings(Client httpClient, Keycloak userClient, String realmName,
                                                            String evaluatedClientId, String roleContainerId, String result) {
        WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                .path("admin").path("realms").path(realmName)
                .path("clients").path(evaluatedClientId)
                .path("evaluate-scopes").path("scope-mappings").path(roleContainerId).path(result)
                .register(new BearerAuthFilter(userClient.tokenManager()));

        try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.readEntity(new GenericType<List<RoleRepresentation>>() {});
        }
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
    public void testIdpManagerCannotEscalateViaIdentityProviderHardcodedRoleMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "idp-manager";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_IDENTITY_PROVIDERS);

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("test-idp");
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setConfig(new java.util.HashMap<>());
        idp.getConfig().put("clientId", "test-client");
        idp.getConfig().put("clientSecret", "test-secret");
        idp.getConfig().put("authorizationUrl", "https://test.example.com/auth");
        idp.getConfig().put("tokenUrl", "https://test.example.com/token");

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName).identityProviders().create(idp)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        });

        IdentityProviderMapperRepresentation mapperRealmAdmin = new IdentityProviderMapperRepresentation();
        mapperRealmAdmin.setName("grant-realm-admin");
        mapperRealmAdmin.setIdentityProviderAlias("test-idp");
        mapperRealmAdmin.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        mapperRealmAdmin.setConfig(new java.util.HashMap<>());
        mapperRealmAdmin.getConfig().put("role", Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.REALM_ADMIN);
        mapperRealmAdmin.getConfig().put("syncMode", "INHERIT");

        // Non-realm-admin cannot create mapper granting admin role (switch is off by default)
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(mapperRealmAdmin)) {
                assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus(),
                        "Creating mapper with realm-admin role should be forbidden when allowAdminRoleMapping is disabled");
            }
        });

        // Non-admin role mappers should always work
        IdentityProviderMapperRepresentation mapperNonAdmin = new IdentityProviderMapperRepresentation();
        mapperNonAdmin.setName("grant-offline-access");
        mapperNonAdmin.setIdentityProviderAlias("test-idp");
        mapperNonAdmin.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        mapperNonAdmin.setConfig(new java.util.HashMap<>());
        mapperNonAdmin.getConfig().put("role", "offline_access");
        mapperNonAdmin.getConfig().put("syncMode", "INHERIT");

        String[] nonAdminMapperIdHolder = new String[1];
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(mapperNonAdmin)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
                nonAdminMapperIdHolder[0] = ApiUtil.getCreatedId(response);
            }
        });
        String nonAdminMapperId = nonAdminMapperIdHolder[0];
        String adminRole = Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.REALM_ADMIN;

        // Updating the benign mapper to grant an admin role must be rejected too (switch is off by default)
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            IdentityProviderMapperRepresentation toEscalate = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").getMapperById(nonAdminMapperId);
            toEscalate.getConfig().put("role", adminRole);
            assertThrows(ForbiddenException.class, () ->
                            attackerClient.realm(realmName).identityProviders().get("test-idp").update(nonAdminMapperId, toEscalate),
                    "Updating a mapper to grant an admin role should be forbidden when allowAdminRoleMapping is disabled");
        });
        assertEquals("offline_access",
                testRealm.identityProviders().get("test-idp").getMapperById(nonAdminMapperId).getConfig().get("role"),
                "A rejected update must not modify the mapper");

        // Realm admin enables the switch
        IdentityProviderRepresentation idpRep = testRealm.identityProviders().get("test-idp").toRepresentation();
        idpRep.getConfig().put(IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING, "true");
        testRealm.identityProviders().get("test-idp").update(idpRep);

        // Now the non-realm-admin can create the admin-role-granting mapper
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(mapperRealmAdmin)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus(),
                        "Creating mapper with realm-admin role should succeed when allowAdminRoleMapping is enabled");
            }
        });

        // ...and can now update the benign mapper to grant the admin role
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            IdentityProviderMapperRepresentation toEscalate = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").getMapperById(nonAdminMapperId);
            toEscalate.getConfig().put("role", adminRole);
            attackerClient.realm(realmName).identityProviders().get("test-idp").update(nonAdminMapperId, toEscalate);
        });
        assertEquals(adminRole,
                testRealm.identityProviders().get("test-idp").getMapperById(nonAdminMapperId).getConfig().get("role"),
                "Updating a mapper to grant an admin role should succeed when allowAdminRoleMapping is enabled");
    }

    @Test
    public void testIdpManagerCannotEscalateViaCompositeRoleMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "idp-manager";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_IDENTITY_PROVIDERS);

        // A composite realm role, set up by a full admin, that transitively grants an admin role.
        testRealm.roles().create(RoleBuilder.create().name("custom-admin").build());
        ClientRepresentation realmMgmt = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmAdmin = testRealm.clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
        testRealm.roles().get("custom-admin").addComposites(List.of(realmAdmin));

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("test-idp");
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setConfig(new java.util.HashMap<>());
        idp.getConfig().put("clientId", "test-client");
        idp.getConfig().put("clientSecret", "test-secret");
        idp.getConfig().put("authorizationUrl", "https://test.example.com/auth");
        idp.getConfig().put("tokenUrl", "https://test.example.com/token");

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName).identityProviders().create(idp)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        });

        // A mapper granting a composite role that hides an admin role must be treated like an admin role mapper.
        IdentityProviderMapperRepresentation compositeMapper = new IdentityProviderMapperRepresentation();
        compositeMapper.setName("grant-custom-admin");
        compositeMapper.setIdentityProviderAlias("test-idp");
        compositeMapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        compositeMapper.setConfig(new java.util.HashMap<>());
        compositeMapper.getConfig().put("role", "custom-admin");
        compositeMapper.getConfig().put("syncMode", "INHERIT");

        // Non-realm-admin cannot create the mapper (switch is off by default)
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(compositeMapper)) {
                assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus(),
                        "Creating mapper with composite role hiding an admin role should be forbidden when allowAdminRoleMapping is disabled");
            }
        });

        // Realm admin enables the switch
        IdentityProviderRepresentation idpRep = testRealm.identityProviders().get("test-idp").toRepresentation();
        idpRep.getConfig().put(IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING, "true");
        testRealm.identityProviders().get("test-idp").update(idpRep);

        // Now the non-realm-admin can create the composite-role-granting mapper
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(compositeMapper)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus(),
                        "Creating mapper with composite role hiding an admin role should succeed when allowAdminRoleMapping is enabled");
            }
        });
    }

    @Test
    public void testIdpManagerCannotEscalateViaIdentityProviderHardcodedGroupMapper() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "idp-manager";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_IDENTITY_PROVIDERS);

        // A group carrying an admin role, set up by a full admin.
        GroupRepresentation adminGroup = new GroupRepresentation();
        adminGroup.setName("admin-group");
        try (Response response = testRealm.groups().add(adminGroup)) {
            adminGroup.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, adminGroup, AdminRoles.REALM_ADMIN);

        // A group with no admin roles.
        GroupRepresentation plainGroup = new GroupRepresentation();
        plainGroup.setName("plain-group");
        try (Response response = testRealm.groups().add(plainGroup)) {
            plainGroup.setId(ApiUtil.getCreatedId(response));
        }

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("test-idp");
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setConfig(new java.util.HashMap<>());
        idp.getConfig().put("clientId", "test-client");
        idp.getConfig().put("clientSecret", "test-secret");
        idp.getConfig().put("authorizationUrl", "https://test.example.com/auth");
        idp.getConfig().put("tokenUrl", "https://test.example.com/token");

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName).identityProviders().create(idp)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        });

        IdentityProviderMapperRepresentation adminGroupMapper = new IdentityProviderMapperRepresentation();
        adminGroupMapper.setName("join-admin-group");
        adminGroupMapper.setIdentityProviderAlias("test-idp");
        adminGroupMapper.setIdentityProviderMapper("oidc-hardcoded-group-idp-mapper");
        adminGroupMapper.setConfig(new java.util.HashMap<>());
        adminGroupMapper.getConfig().put("group", "/admin-group");
        adminGroupMapper.getConfig().put("syncMode", "INHERIT");

        // Non-realm-admin cannot create a mapper joining an admin-role-bearing group (switch off by default)
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(adminGroupMapper)) {
                assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus(),
                        "Creating mapper joining an admin group should be forbidden when allowAdminRoleMapping is disabled");
            }
        });

        // A mapper joining a group without admin roles should always work
        IdentityProviderMapperRepresentation plainGroupMapper = new IdentityProviderMapperRepresentation();
        plainGroupMapper.setName("join-plain-group");
        plainGroupMapper.setIdentityProviderAlias("test-idp");
        plainGroupMapper.setIdentityProviderMapper("oidc-hardcoded-group-idp-mapper");
        plainGroupMapper.setConfig(new java.util.HashMap<>());
        plainGroupMapper.getConfig().put("group", "/plain-group");
        plainGroupMapper.getConfig().put("syncMode", "INHERIT");

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(plainGroupMapper)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        });

        // Realm admin enables the switch
        IdentityProviderRepresentation idpRep = testRealm.identityProviders().get("test-idp").toRepresentation();
        idpRep.getConfig().put(IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING, "true");
        testRealm.identityProviders().get("test-idp").update(idpRep);

        // Now the non-realm-admin can create the admin-group-joining mapper
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").addMapper(adminGroupMapper)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus(),
                        "Creating mapper joining an admin group should succeed when allowAdminRoleMapping is enabled");
            }
        });
    }

    @Test
    public void testNonRealmAdminCannotEnableAllowAdminRoleMapping() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "idp-manager";
        createUser(testRealm, attackerName);

        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_IDENTITY_PROVIDERS);

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("test-idp");
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setConfig(new java.util.HashMap<>());
        idp.getConfig().put("clientId", "test-client");
        idp.getConfig().put("clientSecret", "test-secret");
        idp.getConfig().put("authorizationUrl", "https://test.example.com/auth");
        idp.getConfig().put("tokenUrl", "https://test.example.com/token");

        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            try (Response response = attackerClient.realm(realmName).identityProviders().create(idp)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        });

        // Attacker tries to enable the switch via IdP update — should get 403
        runAs(realmName, "admin-cli", attackerName, attackerClient -> {
            IdentityProviderRepresentation idpRep = attackerClient.realm(realmName)
                    .identityProviders().get("test-idp").toRepresentation();
            idpRep.getConfig().put(IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING, "true");
            assertThrows(ForbiddenException.class, () ->
                    attackerClient.realm(realmName).identityProviders().get("test-idp").update(idpRep),
                    "Non-manage-realm user should not be able to enable allowAdminRoleMapping");
        });
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

    @Test
    public void testManageUsersAdminCannotJoinUserToAdminGroup() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);

        GroupRepresentation adminGroup = new GroupRepresentation();
        adminGroup.setName("realm-admin-group");
        try (Response response = testRealm.groups().add(adminGroup)) {
            adminGroup.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, adminGroup, AdminRoles.REALM_ADMIN);

        UserRepresentation victim = createUser(testRealm, "target-user");

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                attackerClient.realm(realmName).users().get(victim.getId()).joinGroup(adminGroup.getId());
            });
        });
    }

    @Test
    public void testManageUsersAdminCanJoinUserToNonAdminGroup() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String adminName = "limited-admin";
        createUser(testRealm, adminName);
        grantRealmManagementRole(testRealm, adminName, AdminRoles.MANAGE_USERS);

        GroupRepresentation safeGroup = new GroupRepresentation();
        safeGroup.setName("safe-group");
        try (Response response = testRealm.groups().add(safeGroup)) {
            safeGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation user = createUser(testRealm, "normal-user");

        runAs(realmName, "admin-cli", adminName, adminClient -> {
            adminClient.realm(realmName).users().get(user.getId()).joinGroup(safeGroup.getId());
        });

        assertTrue(testRealm.users().get(user.getId()).groups().stream()
                .anyMatch(g -> g.getName().equals("safe-group")));
    }

    @Test
    public void testRealmAdminCanStillJoinUserToAdminGroup() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String realmAdminName = "full-admin";
        createUser(testRealm, realmAdminName);
        grantRealmManagementRole(testRealm, realmAdminName, AdminRoles.REALM_ADMIN);

        GroupRepresentation adminGroup = new GroupRepresentation();
        adminGroup.setName("realm-admin-group");
        try (Response response = testRealm.groups().add(adminGroup)) {
            adminGroup.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, adminGroup, AdminRoles.REALM_ADMIN);

        UserRepresentation user = createUser(testRealm, "normal-user");

        runAs(realmName, "admin-cli", realmAdminName, adminClient -> {
            adminClient.realm(realmName).users().get(user.getId()).joinGroup(adminGroup.getId());
        });

        assertTrue(testRealm.users().get(user.getId()).groups().stream()
                .anyMatch(g -> g.getName().equals("realm-admin-group")));
    }

    @Test
    public void testManageUsersAdminCannotCreateUserWithAdminGroup() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);

        GroupRepresentation adminGroup = new GroupRepresentation();
        adminGroup.setName("realm-admin-group");
        try (Response response = testRealm.groups().add(adminGroup)) {
            adminGroup.setId(ApiUtil.getCreatedId(response));
        }
        grantRealmManagementRole(testRealm, adminGroup, AdminRoles.REALM_ADMIN);

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                UserRepresentation newUser = UserBuilder.create()
                        .username("escalated-user")
                        .email("escalated@keycloak.org")
                        .firstName("First")
                        .lastName("Last")
                        .password("password")
                        .enabled(true)
                        .groups("/realm-admin-group")
                        .build();
                try (Response resp = attackerClient.realm(realmName).users().create(newUser)) {
                    if (resp.getStatus() == Status.FORBIDDEN.getStatusCode()) {
                        throw new ForbiddenException("Forbidden");
                    }
                }
            });
        });
    }

    @Test
    public void testManageUsersAdminCannotJoinUserToGroupWithCompositeAdminRole() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);
        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);

        testRealm.roles().create(RoleBuilder.create().name("sneaky-composite").build());
        ClientRepresentation realmMgmt = testRealm.clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation manageRealm = testRealm.clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        testRealm.roles().get("sneaky-composite").addComposites(List.of(manageRealm));

        GroupRepresentation compositeGroup = new GroupRepresentation();
        compositeGroup.setName("composite-admin-group");
        try (Response response = testRealm.groups().add(compositeGroup)) {
            compositeGroup.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation sneakyRole = testRealm.roles().get("sneaky-composite").toRepresentation();
        testRealm.groups().group(compositeGroup.getId()).roles().realmLevel().add(List.of(sneakyRole));

        UserRepresentation victim = createUser(testRealm, "target-user");

        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                attackerClient.realm(realmName).users().get(victim.getId()).joinGroup(compositeGroup.getId());
            });
        });
    }

    @Test
    public void testManageUsersAdminCannotAddOrgMemberToAdminGroupViaOrgEndpoint() {
        String realmName = "test-realm";
        RealmResource testRealm = createRealm(adminClient, realmName);

        // enable organizations
        RealmRepresentation realmRep = testRealm.toRepresentation();
        realmRep.setOrganizationsEnabled(true);
        testRealm.update(realmRep);

        String attackerName = "limited-admin";
        createUser(testRealm, attackerName);
        grantRealmManagementRole(testRealm, attackerName, AdminRoles.MANAGE_USERS);

        // create org + add member
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName("test-org");
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName("test-org.com");
        org.addDomain(domain);
        String orgId;
        try (Response response = testRealm.organizations().create(org)) {
            orgId = ApiUtil.getCreatedId(response);
        }

        UserRepresentation victim = createUser(testRealm, "target-user");
        testRealm.organizations().get(orgId).members().addMember(victim.getId()).close();

        // create org group and give it admin roles
        GroupRepresentation adminGroup = new GroupRepresentation();
        adminGroup.setName("org-admin-group");
        String groupId;
        try (Response response = testRealm.organizations().get(orgId).groups().addTopLevelGroup(adminGroup)) {
            groupId = ApiUtil.getCreatedId(response);
        }
        adminGroup.setId(groupId);
        // assign admin role via org groups API (realm groups API blocks org-related groups)
        ClientRepresentation realmMgmt = testRealm.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmAdminRole = testRealm.clients().get(realmMgmt.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
        testRealm.organizations().get(orgId).groups().group(groupId).roles().clientLevel(realmMgmt.getId()).add(List.of(realmAdminRole));

        // limited admin should be forbidden from adding member to admin group via org endpoint
        assertThrows(ForbiddenException.class, () -> {
            runAs(realmName, "admin-cli", attackerName, attackerClient -> {
                attackerClient.realm(realmName).organizations().get(orgId).groups().group(groupId).addMember(victim.getId());
            });
        });
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
