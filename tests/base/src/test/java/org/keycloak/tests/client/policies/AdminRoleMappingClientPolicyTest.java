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
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminRoleMappingClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

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

    @Test
    public void rejectBodylessRealmAndClientRoleMappingDelete() throws Exception {
        UserResource user = createUser("bodyless-role-user");
        ClientResource client = createClient("bodyless-role-client");
        String clientId = client.toRepresentation().getId();
        RoleRepresentation realmRole = createRealmRole("bodyless-realm-role");
        RoleRepresentation clientRole = createClientRole(client, "bodyless-client-role");
        user.roles().realmLevel().add(List.of(realmRole));
        user.roles().clientLevel(clientId).add(List.of(clientRole));

        setupRejectingPolicy();

        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                bodylessDelete(user.toRepresentation().getId(), "realm"));
        assertRolePresent(user.roles().realmLevel().listAll(), realmRole.getName());

        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                bodylessDelete(user.toRepresentation().getId(), "clients", clientId));
        assertRolePresent(user.roles().clientLevel(clientId).listAll(), clientRole.getName());
    }

    @Test
    public void applyTargetClientProtocolConditionWithRealmRoleSemantics() throws Exception {
        UserResource user = createUser("target-role-user");
        ClientResource oidcClient = createClient("target-role-oidc-client", OIDCLoginProtocol.LOGIN_PROTOCOL);
        ClientResource samlClient = createClient("target-role-saml-client", SamlProtocol.LOGIN_PROTOCOL);
        String oidcClientId = oidcClient.toRepresentation().getId();
        String samlClientId = samlClient.toRepresentation().getId();
        RoleRepresentation oidcRole = createClientRole(oidcClient, "target-oidc-role");
        RoleRepresentation samlRole = createClientRole(samlClient, "target-saml-role");
        RoleRepresentation realmRole = createRealmRole("target-realm-role");

        setupRejectingProtocolPolicy(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Assertions.assertThrows(BadRequestException.class,
                () -> user.roles().clientLevel(oidcClientId).add(List.of(oidcRole)));
        assertRoleAbsent(user.roles().clientLevel(oidcClientId).listAll(), oidcRole.getName());

        user.roles().clientLevel(samlClientId).add(List.of(samlRole));
        assertRolePresent(user.roles().clientLevel(samlClientId).listAll(), samlRole.getName());

        user.roles().realmLevel().add(List.of(realmRole));
        assertRolePresent(user.roles().realmLevel().listAll(), realmRole.getName());
    }

    private UserResource createUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(generateSuffixedName(username));
        user.setEnabled(true);

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.users().get(id).remove());
            return realm.admin().users().get(id);
        }
    }

    private ClientResource createClient(String clientId) {
        return createClient(clientId, OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private ClientResource createClient(String clientId, String protocol) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(protocol)
                .publicClient()
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().delete(id));
            return realm.admin().clients().get(id);
        }
    }

    private RoleRepresentation createRealmRole(String name) {
        RoleRepresentation role = new RoleRepresentation(generateSuffixedName(name), "", false);
        realm.admin().roles().create(role);
        realm.cleanup().add(r -> r.roles().get(role.getName()).remove());
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
        ClientUpdaterContextCondition.Configuration configuration = new ClientUpdaterContextCondition.Configuration();
        configuration.setUpdateClientSource(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER));
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientUpdaterContextConditionFactory.PROVIDER_ID, configuration);
    }

    private void setupRejectingProtocolPolicy(String protocol) throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientProtocolConditionFactory.PROVIDER_ID, new ClientProtocolCondition.Configuration(protocol));
    }

    private int bodylessDelete(String userId, String... roleMappingPath) {
        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(realm.getName())
                    .path("users").path(userId).path("role-mappings")
                    .register(new BearerAuthFilter(adminClient.tokenManager()));
            for (String segment : roleMappingPath) {
                target = target.path(segment);
            }
            try (Response response = target.request(MediaType.APPLICATION_JSON).delete()) {
                return response.getStatus();
            }
        }
    }
}
