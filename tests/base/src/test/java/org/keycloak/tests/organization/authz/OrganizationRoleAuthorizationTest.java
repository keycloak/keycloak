/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.organization.authz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.provider.permission.UMAPolicyProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationRoleAuthorizationTest {

    private static final String CLIENT_ID = "organization-role-resource-server";
    private static final String USERNAME = "organization-role-policy-user";

    @InjectRealm(config = OrganizationRoleAuthorizationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testPoliciesEvaluateAndExportRolesByType() {
        String[] identifiers = runOnServer.fetch(OrganizationRoleAuthorizationTest::setUpAuthorization,
                String[].class);
        String resourceServerId = identifiers[0];
        String userId = identifiers[1];
        String organizationRoleId = identifiers[2];

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            AuthorizationProviderFactory factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(AuthorizationProvider.class);
            DefaultEvaluation evaluation = new DefaultEvaluation(null, null, decision -> {
            }, factory.create(session, realm));
            assertTrue(evaluation.getRealm().isUserInRealmRole(userId, "realm-role"));
            assertFalse(evaluation.getRealm().isUserInRealmRole(userId, "organization-role"));
            assertTrue(evaluation.getRealm().isUserInClientRole(userId, CLIENT_ID, "client-role"));
            List<String> realmRoles = evaluation.getRealm().getUserRealmRoles(userId);
            assertTrue(realmRoles.contains("realm-role"));
            assertFalse(realmRoles.contains("organization-role"));
            assertEquals(List.of("client-role"), evaluation.getRealm().getUserClientRoles(userId, CLIENT_ID));
        });

        assertEvaluation(resourceServerId, userId, "realm-resource", DecisionEffect.PERMIT);
        assertEvaluation(resourceServerId, userId, "client-resource", DecisionEffect.PERMIT);
        assertEvaluation(resourceServerId, userId, "organization-required-resource", DecisionEffect.DENY);
        assertEvaluation(resourceServerId, userId, "organization-required-fetch-resource", DecisionEffect.DENY);
        assertEvaluation(resourceServerId, userId, "organization-optional-resource", DecisionEffect.DENY);
        assertEvaluation(resourceServerId, userId, "mixed-optional-resource", DecisionEffect.PERMIT);
        assertEvaluation(resourceServerId, userId, "mixed-optional-fetch-resource", DecisionEffect.PERMIT);
        assertEvaluation(resourceServerId, userId, "mixed-required-resource", DecisionEffect.DENY);

        ResourceServerRepresentation exported = realm.admin().clients().get(resourceServerId)
                .authorization().exportSettings();
        assertExportedRole(exported, "realm-policy", "realm-role", true);
        assertExportedRole(exported, "client-policy", CLIENT_ID + "/client-role", true);
        assertExportedRole(exported, "organization-required-policy", organizationRoleId, true);
        assertExportedRole(exported, "organization-required-fetch-policy", organizationRoleId, true);
        assertExportedRole(exported, "organization-optional-policy", organizationRoleId, false);
        assertEquals("true", getExportedPolicy(exported, "organization-required-fetch-policy")
                .getConfig().get("fetchRoles"));
        assertEquals(String.join(",", CLIENT_ID + "/client-role", "realm-role"), identifiers[3]);
    }

    @Test
    public void testOrganizationRolesRejectedOnCreateUpdateAndImportWithoutPartialConfigChanges() {
        String[] identifiers = runOnServer.fetch(OrganizationRoleAuthorizationTest::setUpPolicyValidation,
                String[].class);
        AuthorizationResource authorization = realm.admin().clients().get(identifiers[0]).authorization();
        RolePolicyRepresentation invalid = new RolePolicyRepresentation();
        invalid.setName("invalid-organization-role-policy");
        invalid.setFetchRoles(false);
        invalid.addRole(identifiers[1], true);
        invalid.addRole(identifiers[2], false);

        try (Response response = authorization.policies().role().create(invalid)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Organization roles cannot be used in role policies",
                    response.readEntity(OAuth2ErrorRepresentation.class).getError());
        }
        assertTrue(authorization.policies().role().findByName(invalid.getName()) == null);

        RolePolicyRepresentation valid = new RolePolicyRepresentation();
        valid.setName("valid-realm-role-policy");
        valid.setFetchRoles(true);
        valid.addRole(identifiers[1], true);
        String validPolicyId;
        try (Response response = authorization.policies().role().create(valid)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            validPolicyId = response.readEntity(RolePolicyRepresentation.class).getId();
        }

        RolePolicyRepresentation update = authorization.policies().role().findById(validPolicyId).toRepresentation();
        update.setFetchRoles(false);
        update.addRole(identifiers[2], false);
        BadRequestException updateFailure = assertThrows(BadRequestException.class, () -> authorization.policies().role().findById(validPolicyId).update(update));
        assertEquals("Organization roles cannot be used in role policies",
                updateFailure.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());

        RolePolicyRepresentation afterUpdate = authorization.policies().role().findById(validPolicyId).toRepresentation();
        assertEquals(true, afterUpdate.isFetchRoles());
        assertEquals(Set.of(identifiers[1]), afterUpdate.getRoles().stream()
                .map(RolePolicyRepresentation.RoleDefinition::getId).collect(Collectors.toSet()));

        ResourceServerRepresentation imported = authorization.exportSettings();
        PolicyRepresentation invalidImport = new PolicyRepresentation();
        invalidImport.setName("invalid-imported-organization-role-policy");
        invalidImport.setType("role");
        invalidImport.setConfig(Map.of(
                "roles", "[{\"id\":\"" + identifiers[2] + "\",\"required\":true}]",
                "fetchRoles", "false"));
        imported.getPolicies().add(invalidImport);

        BadRequestException importFailure = assertThrows(BadRequestException.class, () -> authorization.importSettings(imported));
        assertEquals("Organization roles cannot be used in role policies",
                importFailure.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());

        RolePolicyRepresentation afterImport = authorization.policies().role().findById(validPolicyId).toRepresentation();
        assertEquals(true, afterImport.isFetchRoles());
        assertEquals(Set.of(identifiers[1]), afterImport.getRoles().stream()
                .map(RolePolicyRepresentation.RoleDefinition::getId).collect(Collectors.toSet()));
        assertTrue(authorization.policies().role().findByName(invalidImport.getName()) == null);
    }

    private void assertEvaluation(String resourceServerId, String userId, String resource, DecisionEffect expected) {
        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(userId);
        request.setClientId(resourceServerId);
        request.addResource(resource, resource + "-scope");

        PolicyEvaluationResponse response = realm.admin().clients().get(resourceServerId)
                .authorization().policies().evaluate(request);
        assertEquals(expected, response.getStatus());
    }

    private void assertExportedRole(ResourceServerRepresentation exported, String policyName, String expectedRole,
            boolean required) {
        PolicyRepresentation policy = getExportedPolicy(exported, policyName);
        RolePolicyRepresentation.RoleDefinition[] roles = org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> JsonSerialization.readValue(policy.getConfig().get("roles"),
                        RolePolicyRepresentation.RoleDefinition[].class));
        assertEquals(1, roles.length);
        assertEquals(expectedRole, roles[0].getId());
        assertEquals(required, roles[0].isRequired());
    }

    private PolicyRepresentation getExportedPolicy(ResourceServerRepresentation exported, String policyName) {
        return exported.getPolicies().stream()
                .filter(candidate -> policyName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static String[] setUpAuthorization(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().addClient(realm, CLIENT_ID);
        RoleModel clientRole = session.roles().addClientRole(client, "client-role");
        RoleModel realmRole = session.roles().addRealmRole(realm, "realm-role");
        OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
        OrganizationModel organization = organizations.create("organization-role-authz", "Organization Role Authz",
                "organization-role-authz");
        RoleModel organizationRole = organization.addRole("organization-role");
        UserModel user = session.users().addUser(realm, USERNAME);
        organizations.addMember(organization, user);
        user.grantRole(realmRole);
        user.grantRole(clientRole);
        user.grantRole(organizationRole);

        AuthorizationProviderFactory factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorization = factory.create(session, realm);
        ResourceServer resourceServer = authorization.getStoreFactory().getResourceServerStore().create(client);
        createProtectedResource(authorization, resourceServer, "realm", realmRole);
        createProtectedResource(authorization, resourceServer, "client", clientRole);
        createLegacyProtectedResource(authorization, resourceServer, "organization-required", false, realmRole,
                Map.of(organizationRole, true));
        createLegacyProtectedResource(authorization, resourceServer, "organization-required-fetch", true, realmRole,
                Map.of(organizationRole, true));
        createLegacyProtectedResource(authorization, resourceServer, "organization-optional", false, realmRole,
                Map.of(organizationRole, false));
        createLegacyProtectedResource(authorization, resourceServer, "mixed-optional", false, realmRole,
                Map.of(organizationRole, false, realmRole, true));
        createLegacyProtectedResource(authorization, resourceServer, "mixed-optional-fetch", true, realmRole,
                Map.of(organizationRole, false, realmRole, true));
        createLegacyProtectedResource(authorization, resourceServer, "mixed-required", false, realmRole,
                Map.of(organizationRole, true, realmRole, true));
        Policy umaRolePolicy = createLegacyRolePolicy(authorization, resourceServer, "uma-role-policy", null,
                realmRole, Map.of(realmRole, true, clientRole, true, organizationRole, true));
        PolicyRepresentation umaPolicy = new PolicyRepresentation();
        umaPolicy.setName("uma-policy");
        umaPolicy.setType("uma");
        umaPolicy.addPolicy(umaRolePolicy.getName());
        Policy persistedUmaPolicy = authorization.getStoreFactory().getPolicyStore().create(resourceServer, umaPolicy);
        UmaPermissionRepresentation exportedUmaPolicy = new UMAPolicyProviderFactory()
                .toRepresentation(persistedUmaPolicy, authorization);

        return new String[] { client.getId(), user.getId(), organizationRole.getId(),
                exportedUmaPolicy.getRoles().stream().sorted().collect(Collectors.joining(",")) };
    }

    private static String[] setUpPolicyValidation(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().addClient(realm, "organization-role-policy-validation");
        RoleModel realmRole = session.roles().addRealmRole(realm, "policy-validation-realm-role");
        OrganizationModel organization = session.getProvider(OrganizationProvider.class)
                .create("organization-role-policy-validation", "Organization role policy validation",
                        "organization-role-policy-validation");
        RoleModel organizationRole = organization.addRole("policy-validation-organization-role");
        AuthorizationProviderFactory factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorization = factory.create(session, realm);

        authorization.getStoreFactory().getResourceServerStore().create(client);
        return new String[] { client.getId(), realmRole.getId(), organizationRole.getId() };
    }

    private static void createProtectedResource(AuthorizationProvider authorization, ResourceServer resourceServer,
            String prefix, RoleModel role) {
        Policy policy = createRolePolicy(authorization, resourceServer, prefix + "-policy", role);
        createProtectedResource(authorization, resourceServer, prefix, policy);
    }

    private static void createLegacyProtectedResource(AuthorizationProvider authorization,
            ResourceServer resourceServer, String prefix, Boolean fetchRoles, RoleModel fallbackRole,
            Map<RoleModel, Boolean> roles) {
        Policy policy = createLegacyRolePolicy(authorization, resourceServer, prefix + "-policy", fetchRoles,
                fallbackRole, roles);
        createProtectedResource(authorization, resourceServer, prefix, policy);
    }

    private static void createProtectedResource(AuthorizationProvider authorization, ResourceServer resourceServer,
            String prefix, Policy policy) {
        Scope scope = authorization.getStoreFactory().getScopeStore().create(resourceServer, prefix + "-resource-scope");
        Resource resource = authorization.getStoreFactory().getResourceStore().create(resourceServer,
                prefix + "-resource", resourceServer.getClientId());
        resource.updateScopes(Set.of(scope));

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(prefix + "-permission");
        permission.setType("scope");
        permission.addResource(resource.getName());
        permission.addScope(scope.getName());
        permission.addPolicy(policy.getName());
        permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        permission.setLogic(Logic.POSITIVE);
        authorization.getStoreFactory().getPolicyStore().create(resourceServer, permission);
    }

    private static Policy createRolePolicy(AuthorizationProvider authorization, ResourceServer resourceServer,
            String name, RoleModel... roles) {
        return createRolePolicy(authorization, resourceServer, name, null,
                java.util.Arrays.stream(roles).collect(Collectors.toMap(role -> role, role -> true)));
    }

    private static Policy createRolePolicy(AuthorizationProvider authorization, ResourceServer resourceServer,
            String name, Boolean fetchRoles, Map<RoleModel, Boolean> roles) {
        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setName(name);
        representation.setType("role");
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        Map<String, String> config = new HashMap<>();
        config.put("roles", roles.entrySet().stream()
                .map(entry -> "{\"id\":\"" + entry.getKey().getId() + "\",\"required\":" + entry.getValue() + "}")
                .collect(Collectors.joining(",", "[", "]")));
        if (fetchRoles != null) {
            config.put("fetchRoles", String.valueOf(fetchRoles));
        }
        representation.setConfig(config);
        return authorization.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    private static Policy createLegacyRolePolicy(AuthorizationProvider authorization, ResourceServer resourceServer,
            String name, Boolean fetchRoles, RoleModel fallbackRole, Map<RoleModel, Boolean> roles) {
        Policy policy = createRolePolicy(authorization, resourceServer, name, fetchRoles, Map.of(fallbackRole, true));
        Map<String, String> config = new HashMap<>(policy.getConfig());
        config.put("roles", roles.entrySet().stream()
                .map(entry -> "{\"id\":\"" + entry.getKey().getId() + "\",\"required\":" + entry.getValue() + "}")
                .collect(Collectors.joining(",", "[", "]")));
        policy.setConfig(config);
        return policy;
    }

    public static final class OrganizationRoleAuthorizationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
