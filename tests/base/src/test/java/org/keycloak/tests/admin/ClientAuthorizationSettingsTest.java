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
package org.keycloak.tests.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = ClientAuthorizationSettingsTest.AuthorizationServerConfig.class)
public class ClientAuthorizationSettingsTest {

    @InjectRealm(config = AuthorizationRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = ConfidentialClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient client;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = true)
    public void testEnableAuthorizationWithSettings(Boolean authorizationServicesEnabled) {
        assertThrows(NotFoundException.class, () -> client.admin().authorization().getSettings());

        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setAuthorizationServicesEnabled(authorizationServicesEnabled);
        rep.setAuthorizationSettings(createSettings());
        adminEvents.skipAll();
        client.admin().update(rep);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.AUTHORIZATION_RESOURCE_SERVER)
                .resourcePath("clients", client.getId());
        assertAuthorizationImportEvents(rep.getAuthorizationSettings());

        assertThat(client.admin().toRepresentation().getAuthorizationServicesEnabled(), equalTo(true));
        assertSettings(client.admin().authorization().exportSettings());
    }

    @Test
    public void testUpdateExistingAuthorizationSettings() {
        enableAuthorization();
        AuthorizationResource authorization = client.admin().authorization();
        authorization.importSettings(createSettings());

        try (Response response = authorization.resources().create(new ResourceRepresentation("Unrelated resource"))) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }

        String resourceId = authorization.resources().findByName("Test resource").get(0).getId();
        String scopeId = authorization.scopes().findByName("Test scope").getId();
        String policyId = authorization.policies().role().findByName("Test policy").getId();
        String permissionId = authorization.permissions().resource().findByName("Test permission").getId();

        ResourceServerRepresentation settings = createSettings();
        settings.setAllowRemoteResourceManagement(true);
        settings.setPolicyEnforcementMode(PolicyEnforcementMode.DISABLED);
        settings.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        settings.getResources().get(0).setName("Additional resource");
        settings.getResources().get(0).setUris(Set.of("/additional"));
        settings.getScopes().get(0).setName("Additional scope");
        settings.getPolicies().get(0).setName("Additional policy");
        settings.getPolicies().get(1).setName("Additional permission");
        settings.getPolicies().get(1).setConfig(Map.of("resources", "[\"Additional resource\"]", "applyPolicies", "[\"Additional policy\"]"));

        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setAuthorizationSettings(settings);
        adminEvents.skipAll();
        client.admin().update(rep);
        assertAuthorizationImportEvents(settings);
        client.admin().update(rep);
        assertAuthorizationImportEvents(settings);

        ResourceServerRepresentation stored = authorization.exportSettings();
        assertThat(stored.isAllowRemoteResourceManagement(), equalTo(true));
        assertThat(stored.getPolicyEnforcementMode(), equalTo(PolicyEnforcementMode.DISABLED));
        assertThat(stored.getDecisionStrategy(), equalTo(DecisionStrategy.UNANIMOUS));
        assertThat(stored.getResources(), hasSize(3));
        assertThat(stored.getScopes(), hasSize(2));
        assertThat(stored.getPolicies(), hasSize(4));

        ResourceRepresentation resource = authorization.resources().findByName("Test resource").get(0);
        assertThat(resource.getId(), equalTo(resourceId));
        assertThat(resource.getUris(), contains("/test"));
        ScopeRepresentation scope = authorization.scopes().findByName("Test scope");
        assertThat(scope.getId(), equalTo(scopeId));
        assertThat(scope.getIconUri(), equalTo("https://example.org/scope"));
        assertThat(authorization.policies().role().findByName("Test policy").getId(), equalTo(policyId));
        assertThat(authorization.permissions().resource().findByName("Test permission").getId(), equalTo(permissionId));
        assertThat(authorization.resources().findByName("Unrelated resource"), hasSize(1));
        assertThat(authorization.resources().findByName("Additional resource").get(0).getUris(), contains("/additional"));
        assertThat(authorization.scopes().findByName("Additional scope").getName(), equalTo("Additional scope"));
        assertThat(authorization.policies().role().findByName("Additional policy").getName(), equalTo("Additional policy"));
        assertThat(authorization.permissions().resource().findByName("Additional permission").getName(), equalTo("Additional permission"));
    }

    @Test
    public void testUpdateClientWithoutAuthorizationSettings() {
        enableAuthorization();
        client.admin().authorization().importSettings(createSettings());

        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setName("Updated client");
        client.admin().update(rep);

        assertThat(client.admin().toRepresentation().getName(), equalTo("Updated client"));
        assertSettings(client.admin().authorization().exportSettings());
    }

    @Test
    public void testInvalidClientUpdateRollsBackAuthorizationSettings() {
        ClientRepresentation original = client.admin().toRepresentation();
        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setName("Invalid update");
        rep.setRedirectUris(List.of("https://example.org/callback#fragment"));
        rep.setAuthorizationSettings(createSettings());

        assertThrows(BadRequestException.class, () -> client.admin().update(rep));

        assertThat(client.admin().toRepresentation().getName(), equalTo(original.getName()));
        assertThat(client.admin().toRepresentation().getAuthorizationServicesEnabled(), equalTo(original.getAuthorizationServicesEnabled()));
        assertThrows(NotFoundException.class, () -> client.admin().authorization().getSettings());
    }

    private void enableAuthorization() {
        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setAuthorizationServicesEnabled(true);
        client.admin().update(rep);
    }

    private void assertAuthorizationImportEvents(ResourceServerRepresentation submittedSettings) {
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHORIZATION_RESOURCE_SERVER)
                .resourcePath("clients", client.getId());
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.CLIENT)
                .resourcePath("clients", client.getId())
                .representation(Map.of("clientId", client.getClientId(),
                        "authorizationSettings", JsonSerialization.mapper.convertValue(submittedSettings, Map.class)));
    }

    private ResourceServerRepresentation createSettings() {
        ScopeRepresentation scope = new ScopeRepresentation("Test scope", "https://example.org/scope");
        ResourceRepresentation resource = new ResourceRepresentation("Test resource", Set.of(scope), Set.of("/test"), "test-resource");

        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName("Test policy");
        policy.setType("role");
        policy.setConfig(Map.of("roles", "[{\"id\":\"test-role\"}]"));

        PolicyRepresentation permission = new PolicyRepresentation();
        permission.setName("Test permission");
        permission.setType("resource");
        permission.setConfig(Map.of("resources", "[\"Test resource\"]", "applyPolicies", "[\"Test policy\"]"));

        ResourceServerRepresentation settings = new ResourceServerRepresentation();
        settings.setAllowRemoteResourceManagement(false);
        settings.setPolicyEnforcementMode(PolicyEnforcementMode.PERMISSIVE);
        settings.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        settings.setScopes(List.of(scope));
        settings.setResources(List.of(resource));
        settings.setPolicies(List.of(policy, permission));
        return settings;
    }

    private void assertSettings(ResourceServerRepresentation settings) {
        assertThat(settings.isAllowRemoteResourceManagement(), equalTo(false));
        assertThat(settings.getPolicyEnforcementMode(), equalTo(PolicyEnforcementMode.PERMISSIVE));
        assertThat(settings.getDecisionStrategy(), equalTo(DecisionStrategy.AFFIRMATIVE));
        assertThat(settings.getResources(), hasSize(1));
        assertThat(settings.getResources().get(0).getName(), equalTo("Test resource"));
        assertThat(settings.getResources().get(0).getUris(), contains("/test"));
        assertThat(settings.getResources().get(0).getScopes().stream().map(ScopeRepresentation::getName).toList(), contains("Test scope"));
        assertThat(settings.getScopes(), hasSize(1));
        assertThat(settings.getScopes().get(0).getName(), equalTo("Test scope"));
        assertThat(settings.getScopes().get(0).getIconUri(), equalTo("https://example.org/scope"));
        assertThat(settings.getPolicies(), hasSize(2));
        PolicyRepresentation permission = settings.getPolicies().stream().filter(p -> "resource".equals(p.getType())).findFirst().orElseThrow();
        assertThat(permission.getName(), equalTo("Test permission"));
        assertThat(permission.getConfig().get("resources"), equalTo("[\"Test resource\"]"));
        assertThat(permission.getConfig().get("applyPolicies"), equalTo("[\"Test policy\"]"));
    }

    public static class ConfidentialClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.authorizationServicesEnabled(false).serviceAccountsEnabled(true);
        }
    }

    public static class AuthorizationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.roles("test-role");
        }
    }

    public static class AuthorizationServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.features(Profile.Feature.AUTHORIZATION);
        }
    }
}
