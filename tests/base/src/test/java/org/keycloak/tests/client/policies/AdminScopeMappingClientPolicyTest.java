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
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

@KeycloakIntegrationTest
public class AdminScopeMappingClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm(config = ScopeMappingRealmConfig.class)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @Test
    public void rejectRealmScopeMappingAddAndRemove() throws Exception {
        RoleRepresentation roleToAdd = createRealmRole("scope-add-role");
        RoleRepresentation roleToRemove = createRealmRole("scope-remove-role");
        ClientScopeResource emptyScope = createClientScope("scope-add");
        ClientScopeResource mappedScope = createClientScope("scope-remove");
        mappedScope.getScopeMappings().realmLevel().add(List.of(roleToRemove));

        realmAdminClient.tokenManager().getAccessToken();
        setupRejectingPolicy();
        grantClientScopeManagePermission();

        ClientScopeResource limitedEmptyScope = realmAdminClient.realm(realm.getName()).clientScopes()
                .get(emptyScope.toRepresentation().getId());
        Assertions.assertThrows(ForbiddenException.class,
                () -> limitedEmptyScope.getScopeMappings().realmLevel().add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().realmLevel().listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> emptyScope.getScopeMappings().realmLevel().add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().realmLevel().listAll(), roleToAdd.getName());

        RoleRepresentation missingRole = new RoleRepresentation(generateSuffixedName("missing-scope-add-role"), "", false);
        missingRole.setId("missing-scope-add-role-id");
        Assertions.assertThrows(NotFoundException.class,
                () -> emptyScope.getScopeMappings().realmLevel().add(List.of(missingRole)));

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

        realmAdminClient.tokenManager().getAccessToken();
        setupRejectingPolicy();
        grantClientScopeManagePermission();

        ClientScopeResource limitedEmptyScope = realmAdminClient.realm(realm.getName()).clientScopes()
                .get(emptyScope.toRepresentation().getId());
        Assertions.assertThrows(ForbiddenException.class,
                () -> limitedEmptyScope.getScopeMappings().clientLevel(clientUuid).add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().clientLevel(clientUuid).listAll(), roleToAdd.getName());

        Assertions.assertThrows(BadRequestException.class,
                () -> emptyScope.getScopeMappings().clientLevel(clientUuid).add(List.of(roleToAdd)));
        assertRoleAbsent(emptyScope.getScopeMappings().clientLevel(clientUuid).listAll(), roleToAdd.getName());

        RoleRepresentation missingRole = new RoleRepresentation(generateSuffixedName("missing-client-scope-add-role"), "", false);
        Assertions.assertThrows(NotFoundException.class,
                () -> emptyScope.getScopeMappings().clientLevel(clientUuid).add(List.of(missingRole)));

        Assertions.assertThrows(BadRequestException.class,
                () -> mappedScope.getScopeMappings().clientLevel(clientUuid).remove(List.of(roleToRemove)));
        assertRolePresent(mappedScope.getScopeMappings().clientLevel(clientUuid).listAll(), roleToRemove.getName());
    }

    @Test
    public void applyTargetClientProtocolConditionWithClientScopeSemantics() throws Exception {
        RoleRepresentation role = createRealmRole("target-scope-role");
        ClientResource oidcClient = createClient("target-scope-oidc-client", OIDCLoginProtocol.LOGIN_PROTOCOL);
        ClientResource samlClient = createClient("target-scope-saml-client", SamlProtocol.LOGIN_PROTOCOL);
        ClientScopeResource clientScope = createClientScope("target-scope-client-scope");

        setupRejectingProtocolPolicy(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Assertions.assertThrows(BadRequestException.class,
                () -> oidcClient.getScopeMappings().realmLevel().add(List.of(role)));
        assertRoleAbsent(oidcClient.getScopeMappings().realmLevel().listAll(), role.getName());

        samlClient.getScopeMappings().realmLevel().add(List.of(role));
        assertRolePresent(samlClient.getScopeMappings().realmLevel().listAll(), role.getName());

        clientScope.getScopeMappings().realmLevel().add(List.of(role));
        assertRolePresent(clientScope.getScopeMappings().realmLevel().listAll(), role.getName());
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

    private ClientScopeResource createClientScope(String name) {
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(generateSuffixedName(name));
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        try (Response response = realm.admin().clientScopes().create(scope)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clientScopes().get(id).remove());
            return realm.admin().clientScopes().get(id);
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

    private void grantClientScopeManagePermission() {
        ClientResource adminPermissionsClient = AdminApiUtil.findClientByClientId(realm.admin(),
                Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation onlyMyAdminUserPolicy = PermissionTestUtils.createUserPolicy(realm, adminPermissionsClient,
                generateSuffixedName("scope-mapping-admin"), myadmin.getId());

        PermissionTestUtils.createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(),
                onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));
    }

    public static class ScopeMappingRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true)
                    .users(UserBuilder.create("myadmin")
                            .name("My", "Admin")
                            .email("myadmin@localhost")
                            .emailVerified(true)
                            .password("password")
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_CLIENTS))
                    .clients(ClientBuilder.create("myclient")
                            .secret("mysecret")
                            .directAccessGrantsEnabled(true));
        }
    }
}
