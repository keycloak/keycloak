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
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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
public class AdminRoleMappingClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void rejectRealmRoleMappingAddAndRemove() throws Exception {
        UserResource user = createUser("realm-role-user");
        RoleRepresentation roleToAdd = createRealmRole("realm-role-add");
        RoleRepresentation roleToRemove = createRealmRole("realm-role-remove");
        user.roles().realmLevel().add(List.of(roleToRemove));

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class,
                () -> user.roles().realmLevel().add(List.of(roleToAdd)));
        assertRoleAbsent(user.roles().realmLevel().listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> user.roles().realmLevel().remove(List.of(roleToRemove)));
        assertRolePresent(user.roles().realmLevel().listAll(), roleToRemove.getName());
    }

    @Test
    public void rejectClientRoleMappingAddAndRemove() throws Exception {
        UserResource user = createUser("client-role-user");
        ClientResource client = createClient("role-mapping-client");
        String clientId = client.toRepresentation().getId();
        RoleRepresentation roleToAdd = createClientRole(client, "client-role-add");
        RoleRepresentation roleToRemove = createClientRole(client, "client-role-remove");
        user.roles().clientLevel(clientId).add(List.of(roleToRemove));

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class,
                () -> user.roles().clientLevel(clientId).add(List.of(roleToAdd)));
        assertRoleAbsent(user.roles().clientLevel(clientId).listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> user.roles().clientLevel(clientId).remove(List.of(roleToRemove)));
        assertRolePresent(user.roles().clientLevel(clientId).listAll(), roleToRemove.getName());
    }

    private UserResource createUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(generateSuffixedName(username));
        user.setEnabled(true);

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            return realm.admin().users().get(id);
        }
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
