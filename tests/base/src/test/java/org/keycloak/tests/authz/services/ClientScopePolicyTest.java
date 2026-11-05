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

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ClientScopePolicyTest {

    @InjectRealm(config = ClientScopeRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = ResourceServerConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient resourceServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @BeforeEach
    public void configureAuthorization() {
        createResource("Resource A");
        createResource("Resource B");

        createClientScopePolicy("Client Scope foo Policy", "foo", "bar");
        createClientScopePolicyAndLastOneRequired("Client Scope bar Policy", "foo", "bar");

        createResourcePermission("Resource A Permission", "Resource A", "Client Scope foo Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Client Scope bar Policy");
    }

    private void createResource(String name) {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource).close();
    }

    private void createClientScopePolicy(String name, String... clientScopes) {
        ClientScopePolicyRepresentation policy = new ClientScopePolicyRepresentation();

        policy.setName(name);

        for (String clientScope : clientScopes) {
            policy.addClientScope(clientScope);
        }

        resourceServer.admin().authorization().policies().clientScope().create(policy).close();
    }

    private void createClientScopePolicyAndLastOneRequired(String name, String... clientScopes) {
        ClientScopePolicyRepresentation policy = new ClientScopePolicyRepresentation();

        policy.setName(name);

        for (int i = 0; i < clientScopes.length - 1; i++) {
            policy.addClientScope(clientScopes[i]);
        }

        policy.addClientScope(clientScopes[clientScopes.length - 1], true);

        resourceServer.admin().authorization().policies().clientScope().create(policy).close();
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        resourceServer.admin().authorization().permissions().resource().create(permission).close();
    }

    @Test
    public void testRemovingRequiredClientScopeDoesNotGrantAccess() throws Exception {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        PolicyRepresentation negativePolicy = authorization.policies().findByName("Client Scope foo Policy");
        negativePolicy.setLogic(Logic.NEGATIVE);
        ClientScopePolicyRepresentation requiredScopes = new ClientScopePolicyRepresentation();
        requiredScopes.addClientScope("foo", true);
        requiredScopes.addClientScope("bar", true);
        negativePolicy.getConfig().put("clientScopes", JsonSerialization.writeValueAsString(requiredScopes.getClientScopes()));
        authorization.policies().policy(negativePolicy.getId()).update(negativePolicy);

        List<PolicyRepresentation> policies = List.of(
                authorization.policies().findByName("Client Scope bar Policy"),
                authorization.policies().findByName("Client Scope foo Policy"));
        AuthzClient authzClient = getAuthzClient();
        for (PolicyRepresentation policy : policies) {
            boolean positive = policy.getLogic() == Logic.POSITIVE;
            PermissionRequest request = new PermissionRequest(positive ? "Resource B" : "Resource A");
            String ticket = authzClient.protection().permission().create(request).getTicket();
            assertNotNull(authzClient.authorization("marta", "password", positive ? "foo bar" : "baz")
                    .authorize(new AuthorizationRequest(ticket)).getToken());
        }

        ClientScopeRepresentation requiredScope = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("bar"))
                .findFirst().orElseThrow();
        realm.admin().clientScopes().get(requiredScope.getId()).remove();

        // The evaluation endpoint builds its token using the client's default scopes.
        String defaultScopeId = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("foo")).findFirst().orElseThrow().getId();
        resourceServer.admin().removeOptionalClientScope(defaultScopeId);
        resourceServer.admin().addDefaultClientScope(defaultScopeId);

        for (PolicyRepresentation policy : policies) {
            PolicyRepresentation retainedPolicy = authorization.policies().policy(policy.getId()).toRepresentation();
            assertEquals(policy.getConfig().get("clientScopes"), retainedPolicy.getConfig().get("clientScopes"));
            assertEquals(policy.getLogic(), retainedPolicy.getLogic());

            boolean positive = policy.getLogic() == Logic.POSITIVE;
            PermissionRequest request = new PermissionRequest(positive ? "Resource B" : "Resource A");
            String ticket = authzClient.protection().permission().create(request).getTicket();
            assertThrows(AuthorizationDeniedException.class,
                    () -> authzClient.authorization("marta", "password", positive ? "foo" : "baz")
                            .authorize(new AuthorizationRequest(ticket)),
                    "Removing a required client scope must deny access with " + policy.getLogic() + " logic");

            PolicyEvaluationRequest evaluation = new PolicyEvaluationRequest();
            evaluation.setUserId("marta");
            evaluation.addResource(positive ? "Resource B" : "Resource A");
            PolicyEvaluationResponse response = authorization.policies().evaluate(evaluation);
            assertEquals(DecisionEffect.DENY, response.getStatus());
            assertEquals(1, response.getResults().size());
            var result = response.getResults().get(0);
            assertEquals(DecisionEffect.DENY, result.getStatus());
            assertEquals(1, result.getPolicies().size());
            var permissionResult = result.getPolicies().iterator().next();
            assertEquals(DecisionEffect.DENY, permissionResult.getStatus());
            assertEquals(1, permissionResult.getAssociatedPolicies().size());
            assertEquals(DecisionEffect.DENY, permissionResult.getAssociatedPolicies().get(0).getStatus());
        }
    }

    @Test
    public void testRemovingRequiredClientScopeDoesNotGrantAccessThroughNegativeAggregate() {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        PolicyRepresentation policy = authorization.policies().findByName("Client Scope bar Policy");
        policy.setLogic(Logic.NEGATIVE);
        authorization.policies().policy(policy.getId()).update(policy);

        AggregatePolicyRepresentation aggregate = new AggregatePolicyRepresentation();
        aggregate.setName("Negative aggregate");
        aggregate.setLogic(Logic.NEGATIVE);
        aggregate.addPolicy(policy.getName());
        createClientScopePolicy("Valid client scope policy", "baz");
        aggregate.addPolicy("Valid client scope policy");
        authorization.policies().aggregate().create(aggregate).close();
        aggregate = authorization.policies().aggregate().findByName(aggregate.getName());
        createResource("Resource C");
        createResourcePermission("Resource C Permission", "Resource C", aggregate.getName());

        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource C");
        String ticketBeforeDeletion = authzClient.protection().permission().create(request).getTicket();
        assertThrows(AuthorizationDeniedException.class, () -> authzClient.authorization("marta", "password", "baz")
                .authorize(new AuthorizationRequest(ticketBeforeDeletion)));

        ClientScopeRepresentation requiredScope = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("bar"))
                .findFirst().orElseThrow();
        realm.admin().clientScopes().get(requiredScope.getId()).remove();

        for (Logic logic : Logic.values()) {
            for (DecisionStrategy strategy : DecisionStrategy.values()) {
                aggregate.setLogic(logic);
                aggregate.setDecisionStrategy(strategy);
                authorization.policies().aggregate().findById(aggregate.getId()).update(aggregate);

                String ticket = authzClient.protection().permission().create(request).getTicket();
                assertThrows(AuthorizationDeniedException.class, () -> authzClient.authorization("marta", "password", "baz")
                        .authorize(new AuthorizationRequest(ticket)),
                        "An invalid child must invalidate the aggregate with " + logic + " logic and " + strategy + " strategy");
            }
        }

        // Independent policy alternatives still follow the permission's decision strategy.
        ResourcePermissionRepresentation permission = authorization.permissions().resource().findByName("Resource C Permission");
        permission.addPolicy(aggregate.getId(), "Valid client scope policy");
        for (DecisionStrategy strategy : DecisionStrategy.values()) {
            permission.setDecisionStrategy(strategy);
            authorization.permissions().resource().findById(permission.getId()).update(permission);
            String ticket = authzClient.protection().permission().create(request).getTicket();
            AuthorizationRequest authorizationRequest = new AuthorizationRequest(ticket);
            if (strategy == DecisionStrategy.AFFIRMATIVE) {
                assertNotNull(authzClient.authorization("marta", "password", "baz").authorize(authorizationRequest).getToken());
            } else {
                assertThrows(AuthorizationDeniedException.class,
                        () -> authzClient.authorization("marta", "password", "baz").authorize(authorizationRequest),
                        "An invalid policy must count as a denial with " + strategy + " strategy");
            }
        }
    }

    @Test
    public void testMissingClientScopeIsNotCachedAsOrdinaryDenial() {
        AuthorizationResource authorization = resourceServer.admin().authorization();
        PolicyRepresentation policy = authorization.policies().findByName("Client Scope bar Policy");
        policy.setLogic(Logic.NEGATIVE);
        authorization.policies().policy(policy.getId()).update(policy);

        AggregatePolicyRepresentation inner = new AggregatePolicyRepresentation();
        inner.setName("Inner aggregate");
        inner.addPolicy(policy.getName());
        authorization.policies().aggregate().create(inner).close();

        AggregatePolicyRepresentation outer = new AggregatePolicyRepresentation();
        outer.setName("Outer negative aggregate");
        outer.setLogic(Logic.NEGATIVE);
        outer.addPolicy(inner.getName());
        authorization.policies().aggregate().create(outer).close();

        ResourceRepresentation resource = new ResourceRepresentation("Resource C", "read");
        resource.setType("cached-client-scope");
        authorization.resources().create(resource).close();
        createResourcePermission("Direct policy permission", resource.getName(), policy.getName());

        // Resource policies run before type policies, so the latter can reuse the direct policy's decision.
        ResourcePermissionRepresentation typePermission = new ResourcePermissionRepresentation();
        typePermission.setName("Nested aggregate permission");
        typePermission.setResourceType(resource.getType());
        typePermission.addPolicy(outer.getName());
        authorization.permissions().resource().create(typePermission).close();

        ResourceServerRepresentation settings = authorization.getSettings();
        settings.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        authorization.update(settings);

        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest(resource.getName(), "read");
        String ticketWithRequiredScope = authzClient.protection().permission().create(request).getTicket();
        assertNotNull(authzClient.authorization("marta", "password", "bar")
                .authorize(new AuthorizationRequest(ticketWithRequiredScope)).getToken());
        String ticketBeforeDeletion = authzClient.protection().permission().create(request).getTicket();
        assertNotNull(authzClient.authorization("marta", "password", "baz")
                .authorize(new AuthorizationRequest(ticketBeforeDeletion)).getToken());

        ClientScopeRepresentation requiredScope = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("bar"))
                .findFirst().orElseThrow();
        realm.admin().clientScopes().get(requiredScope.getId()).remove();

        String ticketAfterDeletion = authzClient.protection().permission().create(request).getTicket();
        assertThrows(AuthorizationDeniedException.class, () -> authzClient.authorization("marta", "password", "baz")
                .authorize(new AuthorizationRequest(ticketAfterDeletion)));
    }

    @Test
    public void testInvalidClientScopePolicySurvivesExportImportAndCanBeReconfigured() throws Exception {
        ClientScopeRepresentation requiredScope = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("bar"))
                .findFirst().orElseThrow();
        realm.admin().clientScopes().get(requiredScope.getId()).remove();

        ClientScopeRepresentation optionalScope = realm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals("foo"))
                .findFirst().orElseThrow();
        optionalScope.setName(requiredScope.getId());
        realm.admin().clientScopes().get(optionalScope.getId()).update(optionalScope);

        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource B");
        String ticketBeforeImport = authzClient.protection().permission().create(request).getTicket();
        assertThrows(AuthorizationDeniedException.class, () -> authzClient.authorization("marta", "password", optionalScope.getName())
                .authorize(new AuthorizationRequest(ticketBeforeImport)));

        AuthorizationResource authorization = resourceServer.admin().authorization();
        ResourceServerRepresentation settings = authorization.exportSettings();
        authorization.importSettings(settings);

        String ticketAfterImport = authzClient.protection().permission().create(request).getTicket();
        assertThrows(AuthorizationDeniedException.class, () -> authzClient.authorization("marta", "password", optionalScope.getName())
                .authorize(new AuthorizationRequest(ticketAfterImport)));

        PolicyRepresentation policy = authorization.policies().findByName("Client Scope bar Policy");
        assertFalse(policy.getConfig().get("clientScopes").contains("unresolved"), "Export metadata must not be stored in the policy");
        ClientScopePolicyRepresentation repaired = new ClientScopePolicyRepresentation();
        repaired.addClientScope(optionalScope.getId());
        policy.getConfig().put("clientScopes", JsonSerialization.writeValueAsString(repaired.getClientScopes()));
        authorization.policies().policy(policy.getId()).update(policy);

        String ticketAfterReconfiguration = authzClient.protection().permission().create(request).getTicket();
        assertNotNull(authzClient.authorization("marta", "password", optionalScope.getName())
                .authorize(new AuthorizationRequest(ticketAfterReconfiguration)).getToken());
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(new Configuration(keycloakUrls.getBase(), realm.getName(), resourceServer.getClientId(),
                Map.of("secret", resourceServer.getSecret()), httpClient));
    }

    public static class ClientScopeRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .users(UserBuilder.create("marta").name("Marta", "Test").email("marta@example.org").password("password"))
                    .clientScopes(ClientScopeBuilder.create().name("foo").protocol("openid-connect"))
                    .clientScopes(ClientScopeBuilder.create().name("bar").protocol("openid-connect"))
                    .clientScopes(ClientScopeBuilder.create().name("baz").protocol("openid-connect"));
        }
    }

    public static class ResourceServerConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.secret("secret").authorizationServicesEnabled(true)
                    .optionalClientScopes("foo", "bar", "baz").directAccessGrantsEnabled();
        }
    }
}
