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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest
public class AllEffectiveRoleMappingResourceTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void rolesOfOtherClientsGrantedByClientScopeAreListed() {
        RealmResource realm = managedRealm.admin();
        RoleRepresentation realmRole = createRealmRole(realm, "test");
        String clientUuid = createClient(realm, "evaluated-client", false);
        String otherUuid = createClient(realm, "other-client", true);
        RoleRepresentation ownRole = createClientRole(realm, clientUuid, "own-role");
        RoleRepresentation otherRole = createClientRole(realm, otherUuid, "other-role");

        String scopeId = createClientScope(realm, "default-scope");
        realm.clientScopes().get(scopeId).getScopeMappings().realmLevel().add(List.of(realmRole));
        realm.clientScopes().get(scopeId).getScopeMappings().clientLevel(clientUuid).add(List.of(ownRole));
        realm.clientScopes().get(scopeId).getScopeMappings().clientLevel(otherUuid).add(List.of(otherRole));
        realm.clients().get(clientUuid).addDefaultClientScope(scopeId);

        assertThat(evaluate(clientUuid, "openid"),
                equalTo(Set.of("realm:test", "evaluated-client:own-role", "other-client:other-role")));
    }

    @Test
    public void optionalClientScopeIsOnlyAppliedWhenRequested() {
        RealmResource realm = managedRealm.admin();
        String clientUuid = createClient(realm, "evaluated-client", false);
        String otherUuid = createClient(realm, "other-client", true);
        RoleRepresentation otherRole = createClientRole(realm, otherUuid, "other-role");

        String scopeId = createClientScope(realm, "optional-scope");
        realm.clientScopes().get(scopeId).getScopeMappings().clientLevel(otherUuid).add(List.of(otherRole));
        realm.clients().get(clientUuid).addOptionalClientScope(scopeId);

        assertThat(evaluate(clientUuid, "openid"), not(hasItems("other-client:other-role")));
        assertThat(evaluate(clientUuid, "openid optional-scope"), hasItems("other-client:other-role"));
    }

    @Test
    public void compositeRolesAreExpanded() {
        RealmResource realm = managedRealm.admin();
        String clientUuid = createClient(realm, "evaluated-client", false);
        String otherUuid = createClient(realm, "other-client", true);
        RoleRepresentation otherRole = createClientRole(realm, otherUuid, "other-role");
        RoleRepresentation composite = createRealmRole(realm, "composite-role");
        realm.roles().get(composite.getName()).addComposites(List.of(otherRole));

        String scopeId = createClientScope(realm, "composite-scope");
        realm.clientScopes().get(scopeId).getScopeMappings().realmLevel().add(List.of(composite));
        realm.clients().get(clientUuid).addDefaultClientScope(scopeId);

        assertThat(evaluate(clientUuid, "openid"), hasItems("realm:composite-role", "other-client:other-role"));
    }

    @Test
    public void fullScopeAllowedListsRolesOfAllClients() {
        RealmResource realm = managedRealm.admin();
        createRealmRole(realm, "test");
        String clientUuid = createClient(realm, "evaluated-client", true);
        String otherUuid = createClient(realm, "other-client", true);
        createClientRole(realm, clientUuid, "own-role");
        createClientRole(realm, otherUuid, "other-role");

        assertThat(evaluate(clientUuid, "openid"),
                hasItems("realm:test", "evaluated-client:own-role", "other-client:other-role", "account:manage-account"));
    }

    private RoleRepresentation createRealmRole(RealmResource realm, String name) {
        realm.roles().create(RoleBuilder.create().name(name).build());
        return realm.roles().get(name).toRepresentation();
    }

    private RoleRepresentation createClientRole(RealmResource realm, String clientUuid, String name) {
        realm.clients().get(clientUuid).roles().create(RoleBuilder.create().name(name).build());
        return realm.clients().get(clientUuid).roles().get(name).toRepresentation();
    }

    private String createClient(RealmResource realm, String clientId, boolean fullScopeAllowed) {
        ClientRepresentation client = ClientBuilder.create().clientId(clientId).build();
        client.setFullScopeAllowed(fullScopeAllowed);
        try (Response response = realm.clients().create(client)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private String createClientScope(RealmResource realm, String name) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(name);
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.clientScopes().create(clientScope)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    /**
     * Calls the evaluate endpoint and returns the granted roles as "realm:roleName" or "clientId:roleName".
     */
    private Set<String> evaluate(String clientUuid, String scope) {
        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(managedRealm.getName())
                    .path("ui-ext").path("effective-roles-all").path("clients").path(clientUuid).path("evaluate")
                    .queryParam("scope", scope)
                    .register(new BearerAuthFilter(adminClient.tokenManager()));

            try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
                assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
                List<Map<String, Object>> roles = JsonSerialization.readValue(response.readEntity(String.class),
                        new TypeReference<>() {});
                return roles.stream()
                        .map(role -> (Boolean.TRUE.equals(role.get("clientRole")) ? role.get("client") : "realm") + ":" + role.get("name"))
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
