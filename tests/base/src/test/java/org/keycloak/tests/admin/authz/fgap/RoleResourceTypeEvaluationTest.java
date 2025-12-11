/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class RoleResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private final String rolesType = AdminPermissionsSchema.ROLES.getType();

    @Test
    public void testMapRoleClientScopeAllRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        // we need to be able to list client scopes
        createAllPermission(client, AdminPermissionsSchema.CLIENTS.getType(), onlyMyAdminUserPolicy, Set.of(VIEW));

        // create a client-scope
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("my-client-scope");
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            clientScope.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clientScopes().get(clientScope.getId()).remove());
        }

        // we don't have permissions to map roles to a client scope so the list of available roles should be empty
        ClientScopeResource clientScopeResource = realmAdminClient.realm(realm.getName()).clientScopes().get(clientScope.getId());
        List<RoleRepresentation> availableRoles = clientScopeResource.getScopeMappings().realmLevel().listAvailable();
        assertThat(availableRoles, empty());

        // grant the permission to map all roles to client scopes
        createAllPermission(client, rolesType, onlyMyAdminUserPolicy, Set.of(MAP_ROLE_CLIENT_SCOPE));

        availableRoles = clientScopeResource.getScopeMappings().realmLevel().listAvailable();
        assertThat(availableRoles, not(empty()));
    }

    @Test
    public void testMapCompositeRoleAllRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        // create a role and sub-role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("myRole");
        realm.admin().roles().create(role);
        realm.cleanup().add(r -> r.roles().get("myRole").remove());

        RoleRepresentation subRole = new RoleRepresentation();
        subRole.setName("mySubRole");
        realm.admin().roles().create(subRole);
        subRole = realm.admin().roles().get("mySubRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("mySubRole").remove());

        // the following operation should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).roles().get("myRole").addComposites(List.of(subRole));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        String clientId = realm.admin().clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation manageRealmRole = realm.admin().clients().get(clientId).roles().get("manage-realm").toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(clientId).add(List.of(manageRealmRole));
        realmAdminClient.tokenManager().grantToken();

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, rolesType, onlyMyAdminUserPolicy, Set.of(MAP_ROLE_COMPOSITE));

        realmAdminClient.realm(realm.getName()).roles().get("myRole").addComposites(List.of(subRole));
    }

    @Test
    public void testMapRoleOnlySpecificRole() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        // create some roles
        RoleRepresentation role = new RoleRepresentation();
        role.setName("myRole");
        realm.admin().roles().create(role);
        role = realm.admin().roles().get("myRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("myRole").remove());

        RoleRepresentation otherRole = new RoleRepresentation();
        otherRole.setName("otherRole");
        realm.admin().roles().create(otherRole);
        otherRole = realm.admin().roles().get("otherRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("otherRole").remove());

        // the following operation should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().realmLevel().add(List.of(role));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // create required permissions
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createPermission(client, role.getId(), rolesType, Set.of(MAP_ROLE), onlyMyAdminUserPolicy);
        createPermission(client, myadmin.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);

        // should pass
        realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().realmLevel().add(List.of(role));

        // the following operation should fail as there is no permission for "otherRole"
        try {
            realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().realmLevel().add(List.of(otherRole));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testMappingAdminRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId("realm-management").get(0);
        RoleRepresentation createClientRole = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.CREATE_CLIENT).toRepresentation();

        // create permission to map roles from all clients and to all users
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, onlyMyAdminUserPolicy, Set.of(MAP_ROLES));
        createAllPermission(client, AdminPermissionsSchema.USERS_RESOURCE_TYPE, onlyMyAdminUserPolicy, Set.of(MAP_ROLES));

        // create a role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("myRole");
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        realm.admin().clients().get(myclient.getId()).roles().create(role);
        role = realm.admin().clients().get(myclient.getId()).roles().get("myRole").toRepresentation();

        // should pass
        realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(myclient.getId()).add(List.of(role));

        // should fail as it is admin role and myadmin does not have master realm admin role assigned
        try {
            realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId())
                    .add(List.of(createClientRole));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        RoleRepresentation realmAdminRole = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(realmAdminRole));
        // should pass, user is a realm admin
        realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId())
                .add(List.of(createClientRole));
    }
}
