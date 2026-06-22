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
package org.keycloak.tests.client.policies;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminScopeMappingClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void rejectRealmScopeMappingAddAndRemove() throws Exception {
        RoleRepresentation roleToAdd = createRealmRole("scope-add-role");
        RoleRepresentation roleToRemove = createRealmRole("scope-remove-role");
        ClientScopeResource emptyScope = createClientScope("scope-add");
        ClientScopeResource mappedScope = createClientScope("scope-remove");
        mappedScope.getScopeMappings().realmLevel().add(List.of(roleToRemove));

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class,
                () -> emptyScope.getScopeMappings().realmLevel().add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().realmLevel().listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> mappedScope.getScopeMappings().realmLevel().remove(List.of(roleToRemove)));
        assertRolePresent(mappedScope.getScopeMappings().realmLevel().listAll(), roleToRemove.getName());
    }

    @Test
    public void rejectClientScopeMappingAddAndRemove() throws Exception {
        ClientResource client = createClient("scope-role-client");
        String clientUuid = client.toRepresentation().getId();
        RoleRepresentation roleToAdd = createClientRole(client, "client-scope-add-role");
        RoleRepresentation roleToRemove = createClientRole(client, "client-scope-remove-role");
        ClientScopeResource emptyScope = createClientScope("client-scope-add");
        ClientScopeResource mappedScope = createClientScope("client-scope-remove");
        mappedScope.getScopeMappings().clientLevel(clientUuid).add(List.of(roleToRemove));

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class,
                () -> emptyScope.getScopeMappings().clientLevel(clientUuid).add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().clientLevel(clientUuid).listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> mappedScope.getScopeMappings().clientLevel(clientUuid).remove(List.of(roleToRemove)));
        assertRolePresent(mappedScope.getScopeMappings().clientLevel(clientUuid).listAll(), roleToRemove.getName());
    }

    private ClientResource createClient(String clientId) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .publicClient()
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            return realm.admin().clients().get(id);
        }
    }

    private ClientScopeResource createClientScope(String name) {
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(generateSuffixedName(name));
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        try (Response response = realm.admin().clientScopes().create(scope)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            return realm.admin().clientScopes().get(id);
        }
    }

    private RoleRepresentation createRealmRole(String name) {
        RoleRepresentation role = new RoleRepresentation(generateSuffixedName(name), "", false);
        realm.admin().roles().create(role);
        return realm.admin().roles().get(role.getName()).toRepresentation();
    }

    private RoleRepresentation createClientRole(ClientResource client, String name) {
        RoleRepresentation role = new RoleRepresentation(generateSuffixedName(name), "", false);
        client.roles().create(role);
        return client.roles().get(role.getName()).toRepresentation();
    }

    private void assertRolePresent(List<RoleRepresentation> roles, String roleName) {
        Assertions.assertTrue(roles.stream().anyMatch(role -> roleName.equals(role.getName())));
    }

    private void assertRoleAbsent(List<RoleRepresentation> roles, String roleName) {
        Assertions.assertTrue(roles.stream().noneMatch(role -> roleName.equals(role.getName())));
    }

    private void setupRejectingPolicy() throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}
