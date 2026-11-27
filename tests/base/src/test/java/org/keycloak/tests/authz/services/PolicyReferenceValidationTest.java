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
package org.keycloak.tests.authz.services;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.admin.PolicyEvaluationService.EvaluationDecisionCollector;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider.Outcome;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation.GroupDefinition;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class PolicyReferenceValidationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectClient(config = ResourceServerConfig.class)
    ManagedClient client;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.authz.services")
    RunOnServerClient runOnServer;

    @Test
    public void missingRequiredRoleInvalidatesPolicy() {
        String clientId = client.getId();
        runOnServer.run(session -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = resourceServer(authorization, clientId);
            RolePolicyRepresentation representation = new RolePolicyRepresentation();
            representation.setName("required role");
            PolicyStore policies = authorization.getStoreFactory().getPolicyStore();
            Policy policy = policies.create(resourceServer, representation);
            policy.putConfig("roles", JsonSerialization.writeValueAsString(Set.of(new RoleDefinition("missing-role", true))));

            PolicyProvider provider = authorization.getProvider(policy.getType());
            assertFalse(provider.isValid(policy, authorization));
            assertRuntimeInvalid(authorization, policy);

            PolicyRepresentation exported = ModelToRepresentation.toRepresentation(policy, authorization, true, true);
            exported.setId(null);
            exported.setName("imported required role");
            assertFalse(provider.isValid(policies.create(resourceServer, exported), authorization));
        });
    }

    @Test
    public void missingGroupInvalidatesNegativePolicy() {
        String clientId = client.getId();
        runOnServer.run(session -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = resourceServer(authorization, clientId);
            GroupPolicyRepresentation representation = new GroupPolicyRepresentation();
            representation.setName("negative group");
            representation.setLogic(Logic.NEGATIVE);
            representation.setGroups(Set.of());
            Policy policy = authorization.getStoreFactory().getPolicyStore().create(resourceServer, representation);
            policy.putConfig("groups", JsonSerialization.writeValueAsString(Set.of(new GroupDefinition("missing-group"))));

            assertFalse(authorization.getProvider(policy.getType()).isValid(policy, authorization));
            assertRuntimeInvalid(authorization, policy);
        });
    }

    @Test
    public void missingClientAndUserReferencesSurviveUpdatesAndInvalidateNegativePolicies() {
        String clientId = client.getId();
        String subjectName = KeycloakModelUtils.generateId();
        ReferenceCase[] cases = runOnServer.fetch(session -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer server = resourceServer(authorization, clientId);
            PolicyStore policies = authorization.getStoreFactory().getPolicyStore();
            session.users().addUser(authorization.getRealm(), subjectName);
            UserModel liveUser = session.users().addUser(authorization.getRealm(), "live-policy-user");
            List<ReferenceCase> fixtures = new ArrayList<>();

            for (boolean mixed : List.of(true, false)) {
                String scenario = mixed ? "mixed references" : "only missing references";
                ClientPolicyRepresentation clients = new ClientPolicyRepresentation();
                clients.setName("client " + scenario);
                clients.setClients(Set.of(clientId));
                Policy clientPolicy = policies.create(server, clients);
                fixtures.add(createReferenceCase(authorization, clientPolicy, "clients",
                        mixed ? Set.of(clientId, "missing-client") : Set.of("missing-client")));

                UserPolicyRepresentation users = new UserPolicyRepresentation();
                users.setName("user " + scenario);
                Policy userPolicy = policies.create(server, users);
                fixtures.add(createReferenceCase(authorization, userPolicy, "users",
                        mixed ? Set.of(liveUser.getId(), "missing-user") : Set.of("missing-user")));
            }
            return fixtures.toArray(ReferenceCase[]::new);
        }, ReferenceCase[].class);

        runOnServer.run(session -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer server = resourceServer(authorization, clientId);
            UserModel subject = session.users().getUserByUsername(authorization.getRealm(), subjectName);
            DefaultEvaluationContext context = new DefaultEvaluationContext(new UserModelIdentity(authorization.getRealm(), subject), session);
            for (ReferenceCase fixture : cases) {
                Policy permission = authorization.getStoreFactory().getPolicyStore().findById(server, fixture.permissionId());
                Policy aggregate = permission.getAssociatedPolicies().iterator().next();
                Resource resource = permission.getResources().iterator().next();
                ResourcePermission requested = new ResourcePermission(resource, Set.of(), server);
                EvaluationDecisionCollector decision = new EvaluationDecisionCollector(authorization, server, new AuthorizationRequest());
                authorization.evaluators().from(List.of(requested), server, context).evaluate(decision);
                assertEquals(Effect.DENY, decision.getResults().iterator().next().getPolicy(permission).getEffect(), permission.getName());
                PartialEvaluationPolicyProvider partial = (PartialEvaluationPolicyProvider) authorization.getProvider(aggregate.getType());
                assertEquals(Outcome.INVALID, partial.evaluateOutcome(session, aggregate, subject), permission.getName());

                Policy policy = aggregate.getAssociatedPolicies().iterator().next();
                assertEquals("generic update", policy.getDescription());
                assertEquals(fixture.expectedIds(), Set.of(JsonSerialization.readValue(policy.getConfig().get(fixture.configKey()), String[].class)));
            }
        });
    }

    private record ReferenceCase(String permissionId, String configKey, Set<String> expectedIds) implements Serializable {}

    private static ReferenceCase createReferenceCase(AuthorizationProvider authorization, Policy policy, String configKey, Set<String> ids) throws IOException {
        policy.putConfig(configKey, JsonSerialization.writeValueAsString(ids));
        applyGenericUpdate(authorization, policy);
        policy.setLogic(Logic.NEGATIVE);
        ResourceServer server = policy.getResourceServer();
        PolicyStore policies = authorization.getStoreFactory().getPolicyStore();
        AggregatePolicyRepresentation aggregate = new AggregatePolicyRepresentation();
        aggregate.setName(policy.getName() + " aggregate");
        aggregate.addPolicy(policy.getId());
        Policy dependent = policies.create(server, aggregate);
        Resource resource = authorization.getStoreFactory().getResourceStore().create(server, policy.getName(), server.getClientId());
        Policy permission = policies.create(server, ResourcePermissionRepresentation.create().name(policy.getName() + " permission")
                .resources(Set.of(resource.getId())).policies(Set.of(dependent.getId())).build());
        return new ReferenceCase(permission.getId(), configKey, ids);
    }

    @Test
    public void removingLastOptionalClientScopePreservesPolicyAssociation() {
        String clientId = client.getId();
        runOnServer.run(session -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = resourceServer(authorization, clientId);
            PolicyStore policies = authorization.getStoreFactory().getPolicyStore();
            RealmModel realm = authorization.getRealm();
            ClientScopeModel scope = realm.addClientScope("optional policy scope");

            ClientScopePolicyRepresentation scopePolicy = new ClientScopePolicyRepresentation();
            scopePolicy.setName("optional client scope");
            scopePolicy.addClientScope(scope.getId());
            String scopePolicyId = policies.create(resourceServer, scopePolicy).getId();

            AggregatePolicyRepresentation aggregate = new AggregatePolicyRepresentation();
            aggregate.setName("dependent aggregate");
            aggregate.addPolicy(scopePolicyId);
            String aggregateId = policies.create(resourceServer, aggregate).getId();

            realm.removeClientScope(scope.getId());

            Policy dependent = policies.findById(resourceServer, aggregateId);
            assertNotNull(dependent);
            assertEquals(Set.of(scopePolicyId), dependent.getAssociatedPolicies().stream().map(Policy::getId).collect(Collectors.toSet()));
            Policy storedPolicy = policies.findById(resourceServer, scopePolicyId);
            assertRuntimeInvalid(authorization, storedPolicy);

            ClientScopePolicyRepresentation update = ModelToRepresentation.toRepresentation(storedPolicy, authorization, false, false);
            update.setDescription("typed update");
            RepresentationToModel.toModel(update, authorization, storedPolicy);
            assertEquals("typed update", storedPolicy.getDescription());
            assertRuntimeInvalid(authorization, storedPolicy);
            assertGenericUpdatePreservesConfig(authorization, storedPolicy, "clientScopes");

            update.addClientScope("new-missing-scope");
            assertThrows(RuntimeException.class, () -> RepresentationToModel.toModel(update, authorization, storedPolicy));
        });
    }

    private static void assertGenericUpdatePreservesConfig(AuthorizationProvider authorization, Policy policy, String configKey) {
        String storedReferences = policy.getConfig().get(configKey);
        applyGenericUpdate(authorization, policy);
        assertEquals("generic update", policy.getDescription());
        assertEquals(storedReferences, policy.getConfig().get(configKey));
    }

    private static void applyGenericUpdate(AuthorizationProvider authorization, Policy policy) {
        PolicyRepresentation update = ModelToRepresentation.toRepresentation(policy, authorization, true, false);
        update.setDescription("generic update");
        RepresentationToModel.updatePolicy(update, authorization, policy);
    }

    private static void assertRuntimeInvalid(AuthorizationProvider authorization, Policy policy) {
        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();
        UserModel user = session.users().addUser(realm, KeycloakModelUtils.generateId());
        DefaultEvaluationContext context = new DefaultEvaluationContext(new UserModelIdentity(realm, user), session);
        ResourcePermission permission = new ResourcePermission(null, Set.of(), policy.getResourceServer());
        DefaultEvaluation evaluation = new DefaultEvaluation(permission, context, null, policy, ignored -> {}, authorization, Map.of());

        authorization.getProvider(policy.getType()).evaluate(evaluation);

        assertEquals(Effect.INVALID, evaluation.getEffect());
    }

    private static ResourceServer resourceServer(AuthorizationProvider authorization, String clientId) {
        return authorization.getStoreFactory().getResourceServerStore()
                .findByClient(authorization.getRealm().getClientById(clientId));
    }

    public static class ResourceServerConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.secret("secret").authorizationServicesEnabled(true);
        }
    }
}
