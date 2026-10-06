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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
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
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        // we need to be able to list and manage client scopes
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(), onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        // create a realm role to use in scope mapping tests
        RoleRepresentation testRole = new RoleRepresentation();
        testRole.setName("testScopeRole");
        realm.admin().roles().create(testRole);
        testRole = realm.admin().roles().get("testScopeRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("testScopeRole").remove());

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

        // adding a realm-level scope mapping should also fail without MAP_ROLE_CLIENT_SCOPE permission
        try {
            clientScopeResource.getScopeMappings().realmLevel().add(List.of(testRole));
            fail("Expected ForbiddenException when adding realm scope mapping without MAP_ROLE_CLIENT_SCOPE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // grant the permission to map all roles to client scopes
        createAllPermission(adminPermissionsClient, rolesType, onlyMyAdminUserPolicy, Set.of(MAP_ROLE_CLIENT_SCOPE));

        availableRoles = clientScopeResource.getScopeMappings().realmLevel().listAvailable();
        assertThat(availableRoles, not(empty()));

        // adding the scope mapping should now succeed
        clientScopeResource.getScopeMappings().realmLevel().add(List.of(testRole));

        // verify the role was added
        List<RoleRepresentation> mappedRoles = clientScopeResource.getScopeMappings().realmLevel().listAll();
        assertThat(mappedRoles, not(empty()));

        // removing the scope mapping should also succeed
        clientScopeResource.getScopeMappings().realmLevel().remove(List.of(testRole));
    }

    @Test
    public void testMapRoleClientScopeWriteDeniedWithoutPermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(), onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        // create two realm roles
        RoleRepresentation allowedRole = new RoleRepresentation();
        allowedRole.setName("allowedRole");
        realm.admin().roles().create(allowedRole);
        allowedRole = realm.admin().roles().get("allowedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("allowedRole").remove());

        RoleRepresentation deniedRole = new RoleRepresentation();
        deniedRole.setName("deniedRole");
        realm.admin().roles().create(deniedRole);
        deniedRole = realm.admin().roles().get("deniedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("deniedRole").remove());

        // create a client-scope
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            clientScope.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clientScopes().get(clientScope.getId()).remove());
        }

        // grant MAP_ROLE_CLIENT_SCOPE only for a specific role
        createPermission(adminPermissionsClient, allowedRole.getId(), rolesType, Set.of(MAP_ROLE_CLIENT_SCOPE), onlyMyAdminUserPolicy);

        ClientScopeResource clientScopeResource = realmAdminClient.realm(realm.getName()).clientScopes().get(clientScope.getId());

        // adding the allowed role should succeed
        clientScopeResource.getScopeMappings().realmLevel().add(List.of(allowedRole));

        // adding the denied role should fail
        try {
            clientScopeResource.getScopeMappings().realmLevel().add(List.of(deniedRole));
            fail("Expected ForbiddenException when adding scope mapping for a role without MAP_ROLE_CLIENT_SCOPE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // removing the allowed role should succeed
        clientScopeResource.getScopeMappings().realmLevel().remove(List.of(allowedRole));

        // add the denied role as super admin so we can test that a limited admin can't remove it
        realm.admin().clientScopes().get(clientScope.getId()).getScopeMappings().realmLevel().add(List.of(deniedRole));

        // removing the denied role should fail for the limited admin
        try {
            clientScopeResource.getScopeMappings().realmLevel().remove(List.of(deniedRole));
            fail("Expected ForbiddenException when removing scope mapping for a role without MAP_ROLE_CLIENT_SCOPE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testMapRoleClientScopeClientLevelRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(), onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        // create a client role
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("testClientRole");
        clientRole.setClientRole(true);
        realm.admin().clients().get(myclient.getId()).roles().create(clientRole);
        clientRole = realm.admin().clients().get(myclient.getId()).roles().get("testClientRole").toRepresentation();

        // create a client-scope
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope-for-client-roles");
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            clientScope.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clientScopes().get(clientScope.getId()).remove());
        }

        ClientScopeResource clientScopeResource = realmAdminClient.realm(realm.getName()).clientScopes().get(clientScope.getId());

        // adding a client-level scope mapping should fail without MAP_ROLE_CLIENT_SCOPE permission
        try {
            clientScopeResource.getScopeMappings().clientLevel(myclient.getId()).add(List.of(clientRole));
            fail("Expected ForbiddenException when adding client scope mapping without MAP_ROLE_CLIENT_SCOPE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // grant MAP_ROLE_CLIENT_SCOPE for all roles
        createAllPermission(adminPermissionsClient, rolesType, onlyMyAdminUserPolicy, Set.of(MAP_ROLE_CLIENT_SCOPE));

        // adding the client-level scope mapping should now succeed
        clientScopeResource.getScopeMappings().clientLevel(myclient.getId()).add(List.of(clientRole));

        // removing the client-level scope mapping should also succeed
        clientScopeResource.getScopeMappings().clientLevel(myclient.getId()).remove(List.of(clientRole));
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

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, rolesType, onlyMyAdminUserPolicy, Set.of(MAP_ROLE_COMPOSITE));

        realmAdminClient.realm(realm.getName()).roles().get("myRole").addComposites(List.of(subRole));
    }

    @Test
    public void testDeleteCompositeRoleRequiresMapCompositePermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        String myclientId = myclient.getId();

        // create client sub-roles on myclient (using client roles avoids manage-realm bypassing canMapComposite)
        RoleRepresentation allowedSubRole = new RoleRepresentation();
        allowedSubRole.setName("allowedSubRole");
        realm.admin().clients().get(myclientId).roles().create(allowedSubRole);
        allowedSubRole = realm.admin().clients().get(myclientId).roles().get("allowedSubRole").toRepresentation();

        RoleRepresentation restrictedSubRole = new RoleRepresentation();
        restrictedSubRole.setName("restrictedSubRole");
        realm.admin().clients().get(myclientId).roles().create(restrictedSubRole);
        restrictedSubRole = realm.admin().clients().get(myclientId).roles().get("restrictedSubRole").toRepresentation();

        // create parent realm role (for endpoint 1) and parent client role (for endpoints 2 & 3)
        RoleRepresentation parentRealmRole = new RoleRepresentation();
        parentRealmRole.setName("parentRealmRole");
        realm.admin().roles().create(parentRealmRole);
        parentRealmRole = realm.admin().roles().get("parentRealmRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("parentRealmRole").remove());

        RoleRepresentation parentClientRole = new RoleRepresentation();
        parentClientRole.setName("parentClientRole");
        realm.admin().clients().get(myclientId).roles().create(parentClientRole);
        parentClientRole = realm.admin().clients().get(myclientId).roles().get("parentClientRole").toRepresentation();

        // grant myadmin manage-realm (required for realm role endpoint's requireManage(RealmModel))
        String realmMgmtId = realm.admin().clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation manageRealmRole = realm.admin().clients().get(realmMgmtId).roles().get("manage-realm").toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmMgmtId).add(List.of(manageRealmRole));
        realmAdminClient.tokenManager().grantToken();

        // grant FGAP MANAGE on myclient (for client role endpoints' requireManage)
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myclientId, AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(MANAGE), policy);

        // grant MAP_ROLE_COMPOSITE only on allowedSubRole
        createPermission(adminPermissionsClient, allowedSubRole.getId(), rolesType, Set.of(MAP_ROLE_COMPOSITE), policy);

        // as full admin, add both sub-roles as composites of both parent roles
        realm.admin().roles().get("parentRealmRole").addComposites(List.of(allowedSubRole, restrictedSubRole));
        realm.admin().clients().get(myclientId).roles().get("parentClientRole").addComposites(List.of(allowedSubRole, restrictedSubRole));

        // --- Endpoint 1: DELETE /admin/realms/{realm}/roles/{role-name}/composites ---
        try {
            realmAdminClient.realm(realm.getName()).roles().get("parentRealmRole").deleteComposites(List.of(restrictedSubRole));
            fail("Should not be able to delete composite without MAP_ROLE_COMPOSITE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        realmAdminClient.realm(realm.getName()).roles().get("parentRealmRole").deleteComposites(List.of(allowedSubRole));

        // --- Endpoint 3: DELETE /admin/realms/{realm}/clients/{id}/roles/{role-name}/composites ---
        try {
            realmAdminClient.realm(realm.getName()).clients().get(myclientId).roles().get("parentClientRole").deleteComposites(List.of(restrictedSubRole));
            fail("Should not be able to delete composite without MAP_ROLE_COMPOSITE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        realmAdminClient.realm(realm.getName()).clients().get(myclientId).roles().get("parentClientRole").deleteComposites(List.of(allowedSubRole));

        // --- Endpoint 2: DELETE /admin/realms/{realm}/roles-by-id/{role-id}/composites ---
        // re-add allowedSubRole as composite (was removed above)
        realm.admin().clients().get(myclientId).roles().get("parentClientRole").addComposites(List.of(allowedSubRole));

        try {
            realmAdminClient.realm(realm.getName()).rolesById().deleteComposites(parentClientRole.getId(), List.of(restrictedSubRole));
            fail("Should not be able to delete composite without MAP_ROLE_COMPOSITE permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        realmAdminClient.realm(realm.getName()).rolesById().deleteComposites(parentClientRole.getId(), List.of(allowedSubRole));
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
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, role.getId(), rolesType, Set.of(MAP_ROLE), onlyMyAdminUserPolicy);
        createPermission(adminPermissionsClient, myadmin.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);

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
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, onlyMyAdminUserPolicy, Set.of(MAP_ROLES));
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.USERS_RESOURCE_TYPE, onlyMyAdminUserPolicy, Set.of(MAP_ROLES));

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

    @Test
    public void testRoleMappingDeleteRespectsPerRolePermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserRepresentation targetUser = createUser("targetUser");

        RoleRepresentation allowedRole = new RoleRepresentation();
        allowedRole.setName("allowedRole");
        realm.admin().roles().create(allowedRole);
        allowedRole = realm.admin().roles().get("allowedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("allowedRole").remove());

        RoleRepresentation restrictedRole = new RoleRepresentation();
        restrictedRole.setName("restrictedRole");
        realm.admin().roles().create(restrictedRole);
        restrictedRole = realm.admin().roles().get("restrictedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("restrictedRole").remove());

        // assign both roles to the target user (as realm admin)
        realm.admin().users().get(targetUser.getId()).roles().realmLevel().add(List.of(allowedRole, restrictedRole));

        // grant myadmin user-level MAP_ROLES on the target user and role-level MAP_ROLE only for allowedRole
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), policy);
        createPermission(adminPermissionsClient, allowedRole.getId(), rolesType, Set.of(MAP_ROLE), policy);

        RoleMappingResource roleMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles();

        // deleting restrictedRole should be forbidden
        try {
            roleMappings.realmLevel().remove(List.of(restrictedRole));
            fail("Expected ForbiddenException");
        } catch (ForbiddenException expected) {
        }

        // deleting allowedRole should succeed
        roleMappings.realmLevel().remove(List.of(allowedRole));
    }

    @Test
    public void testGroupRoleMappingDeleteRespectsPerRolePermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        GroupRepresentation group = createGroup("testGroup");

        RoleRepresentation allowedRole = new RoleRepresentation();
        allowedRole.setName("allowedRole");
        realm.admin().roles().create(allowedRole);
        allowedRole = realm.admin().roles().get("allowedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("allowedRole").remove());

        RoleRepresentation restrictedRole = new RoleRepresentation();
        restrictedRole.setName("restrictedRole");
        realm.admin().roles().create(restrictedRole);
        restrictedRole = realm.admin().roles().get("restrictedRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("restrictedRole").remove());

        // assign both roles to the group (as realm admin)
        realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(allowedRole, restrictedRole));

        // grant myadmin group-level MANAGE and role-level MAP_ROLE only for allowedRole
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createGroupPermission(group, Set.of(MANAGE), policy);
        createPermission(adminPermissionsClient, allowedRole.getId(), rolesType, Set.of(MAP_ROLE), policy);

        RoleMappingResource roleMappings = realmAdminClient.realm(realm.getName()).groups().group(group.getId()).roles();

        // deleting restrictedRole should be forbidden
        try {
            roleMappings.realmLevel().remove(List.of(restrictedRole));
            fail("Expected ForbiddenException");
        } catch (ForbiddenException expected) {
        }

        // deleting allowedRole should succeed
        roleMappings.realmLevel().remove(List.of(allowedRole));
    }

    @Test
    public void testEffectiveRoleEndpointsFilterHiddenCompositeRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserRepresentation targetUser = createUser("targetUser");

        ClientRepresentation visibleClient = new ClientRepresentation();
        visibleClient.setClientId("visible-client");
        try (Response response = realm.admin().clients().create(visibleClient)) {
            visibleClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation secretClient = new ClientRepresentation();
        secretClient.setClientId("secret-client");
        try (Response response = realm.admin().clients().create(secretClient)) {
            secretClient.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation visibleChild = new RoleRepresentation();
        visibleChild.setName("VISIBLE_CHILD");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleChild);
        visibleChild = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_CHILD").toRepresentation();

        RoleRepresentation secretChild = new RoleRepresentation();
        secretChild.setName("SECRET_CHILD");
        realm.admin().clients().get(secretClient.getId()).roles().create(secretChild);
        secretChild = realm.admin().clients().get(secretClient.getId()).roles().get("SECRET_CHILD").toRepresentation();

        RoleRepresentation visibleParent = new RoleRepresentation();
        visibleParent.setName("VISIBLE_PARENT");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleParent);
        visibleParent = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").toRepresentation();
        realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").addComposites(List.of(visibleChild, secretChild));

        realm.admin().users().get(targetUser.getId()).roles().clientLevel(visibleClient.getId()).add(List.of(visibleParent));
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(secretClient.getId()).add(List.of(secretChild));

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, visibleClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);

        RealmResource realmResource = realmAdminClient.realm(realm.getName());

        // GET /roles-by-id/{id}/composites/composite
        MappingsRepresentation composites = realmResource.rolesById().getCompositeRoleComposites(visibleParent.getId());
        Set<String> roleNames = Stream.concat(
                        composites.getRealmMappings() == null ? Stream.empty() : composites.getRealmMappings().stream(),
                        composites.getClientMappings() == null ? Stream.empty() : composites.getClientMappings().values().stream().flatMap(m -> m.getMappings().stream()))
                .map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat("Visible child role should be present", roleNames, hasItem("VISIBLE_CHILD"));
        assertThat("Secret child role should be filtered out", roleNames, not(hasItem("SECRET_CHILD")));

        // GET /users/{id}/role-mappings/composite
        MappingsRepresentation effective = realmResource.users().get(targetUser.getId()).roles().getAllComposite();
        Map<String, ClientMappingsRepresentation> clientMappings = effective.getClientMappings();
        assertThat("visible-client mapping must be present", clientMappings, not(equalTo(null)));
        roleNames = clientMappings.values().stream()
                .flatMap(m -> m.getMappings().stream())
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertThat("Visible parent role should be present", roleNames, hasItem("VISIBLE_PARENT"));
        assertThat("Visible child role should be present", roleNames, hasItem("VISIBLE_CHILD"));
        assertThat("Directly mapped secret role should be filtered out", roleNames, not(hasItem("SECRET_CHILD")));
        assertThat("Secret client should not be disclosed", clientMappings.containsKey("secret-client"), equalTo(false));
    }

    @Test
    public void testAvailableClientRolesRespectMapRolePermissions() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserRepresentation targetUser = createUser("targetUser");

        ClientRepresentation allowedClient = new ClientRepresentation();
        allowedClient.setClientId("allowed-client");
        try (Response response = realm.admin().clients().create(allowedClient)) {
            allowedClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation otherClient = new ClientRepresentation();
        otherClient.setClientId("other-client");
        try (Response response = realm.admin().clients().create(otherClient)) {
            otherClient.setId(ApiUtil.getCreatedId(response));
        }

        for (String name : List.of("ALLOWED_ONE", "ALLOWED_TWO")) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            realm.admin().clients().get(allowedClient.getId()).roles().create(role);
        }
        RoleRepresentation otherRole = new RoleRepresentation();
        otherRole.setName("OTHER_ROLE");
        realm.admin().clients().get(otherClient.getId()).roles().create(otherRole);
        otherRole = realm.admin().clients().get(otherClient.getId()).roles().get("OTHER_ROLE").toRepresentation();

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW, MAP_ROLES), policy);

        RoleMappingResource roleMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles();

        // no permission to map any role yet
        MappingsRepresentation available = roleMappings.getAllAvailable(null, null, null);
        assertThat(available.getClientMappings(), equalTo(null));

        // map-roles on a client makes all its roles available, roles of other clients stay hidden
        createPermission(adminPermissionsClient, allowedClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(MAP_ROLES), policy);
        available = roleMappings.getAllAvailable(null, null, null);
        assertThat(available.getClientMappings().keySet(), equalTo(Set.of("allowed-client")));
        assertThat(available.getClientMappings().get("allowed-client").getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                equalTo(Set.of("ALLOWED_ONE", "ALLOWED_TWO")));

        // map-role on a single role of another client adds just that role
        createPermission(adminPermissionsClient, otherRole.getId(), rolesType, Set.of(MAP_ROLE), policy);
        available = roleMappings.getAllAvailable(null, null, null);
        assertThat(available.getClientMappings().keySet(), equalTo(Set.of("allowed-client", "other-client")));
        assertThat(available.getClientMappings().get("other-client").getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                equalTo(Set.of("OTHER_ROLE")));

        // search and paging apply on top of the permission filtering
        available = roleMappings.getAllAvailable("ALLOWED", null, null);
        assertThat(available.getClientMappings().keySet(), equalTo(Set.of("allowed-client")));
        available = roleMappings.getAllAvailable(null, 0, 1);
        assertThat(available.getClientMappings().values().stream().mapToInt(m -> m.getMappings().size()).sum(), equalTo(1));

        // mapped roles are no longer available
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(otherClient.getId()).add(List.of(otherRole));
        available = roleMappings.getAllAvailable(null, null, null);
        assertThat(available.getClientMappings().keySet(), equalTo(Set.of("allowed-client")));
    }

    @Test
    public void testAvailableClientRolesHonorRoleSpecificOverrides() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserRepresentation targetUser = createUser("targetUser");
        UserRepresentation otherAdmin = createUser("otherAdmin");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("some-client");
        try (Response response = realm.admin().clients().create(client)) {
            client.setId(ApiUtil.getCreatedId(response));
        }

        for (String name : List.of("GRANTED_ROLE", "OVERRIDDEN_ROLE")) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            realm.admin().clients().get(client.getId()).roles().create(role);
        }
        RoleRepresentation overriddenRole = realm.admin().clients().get(client.getId()).roles().get("OVERRIDDEN_ROLE").toRepresentation();

        UserPolicyRepresentation myAdminPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        UserPolicyRepresentation otherAdminPolicy = createUserPolicy(realm, adminPermissionsClient, "Only Other Admin User Policy", otherAdmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW, MAP_ROLES), myAdminPolicy);

        // myadmin can map all roles, except the one whose resource-specific permission is granted to someone else
        createAllPermission(adminPermissionsClient, rolesType, myAdminPolicy, Set.of(MAP_ROLE));
        createPermission(adminPermissionsClient, overriddenRole.getId(), rolesType, Set.of(MAP_ROLE), otherAdminPolicy);

        RoleMappingResource roleMappings = realmAdminClient.realm(realm.getName()).users().get(targetUser.getId()).roles();
        MappingsRepresentation available = roleMappings.getAllAvailable(null, null, null);
        assertThat(available.getClientMappings().get("some-client").getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                equalTo(Set.of("GRANTED_ROLE")));

        // the listing is consistent with the actual mapping check
        try {
            roleMappings.clientLevel(client.getId()).add(List.of(overriddenRole));
            fail("Expected ForbiddenException");
        } catch (ForbiddenException expected) {
        }
    }

    @Test
    public void testListingClientRolesRespectsViewPermissions() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation viewableClient = new ClientRepresentation();
        viewableClient.setClientId("viewable-client");
        try (Response response = realm.admin().clients().create(viewableClient)) {
            viewableClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation otherClient = new ClientRepresentation();
        otherClient.setClientId("other-client");
        try (Response response = realm.admin().clients().create(otherClient)) {
            otherClient.setId(ApiUtil.getCreatedId(response));
        }

        for (String name : List.of("VIEWABLE_ONE", "VIEWABLE_TWO")) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            realm.admin().clients().get(viewableClient.getId()).roles().create(role);
        }
        for (String name : List.of("OTHER_ONE", "OTHER_TWO")) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            realm.admin().clients().get(otherClient.getId()).roles().create(role);
        }
        RoleRepresentation otherRole = realm.admin().clients().get(otherClient.getId()).roles().get("OTHER_ONE").toRepresentation();

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        ClientsResource clients = realmAdminClient.realm(realm.getName()).clients();

        // query-clients allows calling the endpoint but no role is visible yet
        assertThat(clients.findRoles(null, null, null), empty());

        // view on a client makes all its roles visible
        createPermission(adminPermissionsClient, viewableClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(VIEW), policy);
        List<ClientMappingsRepresentation> roles = clients.findRoles(null, null, null);
        assertThat(roles.stream().map(ClientMappingsRepresentation::getClient).collect(Collectors.toSet()), equalTo(Set.of("viewable-client")));
        assertThat(roles.get(0).getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("VIEWABLE_ONE", "VIEWABLE_TWO")));

        // a role that can be mapped is visible too, the rest of its client stays hidden
        createPermission(adminPermissionsClient, otherRole.getId(), rolesType, Set.of(MAP_ROLE), policy);
        roles = clients.findRoles(null, null, null);
        assertThat(roles.stream().map(ClientMappingsRepresentation::getClient).collect(Collectors.toSet()), equalTo(Set.of("viewable-client", "other-client")));
        assertThat(roles.stream().filter(c -> "other-client".equals(c.getClient())).findFirst().orElseThrow().getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                equalTo(Set.of("OTHER_ONE")));

        // search and paging apply on top of the permission filtering
        roles = clients.findRoles("OTHER", null, null);
        assertThat(roles.stream().map(ClientMappingsRepresentation::getClient).collect(Collectors.toSet()), equalTo(Set.of("other-client")));
        roles = clients.findRoles(null, 0, 1);
        assertThat(roles.stream().mapToInt(c -> c.getMappings().size()).sum(), equalTo(1));
    }

    @Test
    public void testListingScopeMappingsRespectsViewPermissions() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(), policy, Set.of(VIEW));

        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("scopeClientRole");
        realm.admin().clients().get(myclient.getId()).roles().create(clientRole);
        clientRole = realm.admin().clients().get(myclient.getId()).roles().get("scopeClientRole").toRepresentation();

        RoleRepresentation visibleRole = new RoleRepresentation();
        visibleRole.setName("visibleScopeRole");
        realm.admin().roles().create(visibleRole);
        visibleRole = realm.admin().roles().get("visibleScopeRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("visibleScopeRole").remove());

        RoleRepresentation hiddenRole = new RoleRepresentation();
        hiddenRole.setName("hiddenScopeRole");
        realm.admin().roles().create(hiddenRole);
        hiddenRole = realm.admin().roles().get("hiddenScopeRole").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("hiddenScopeRole").remove());

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("scope-mappings-listing");
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            clientScope.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clientScopes().get(clientScope.getId()).remove());
        }

        ClientScopeResource superAdminScope = realm.admin().clientScopes().get(clientScope.getId());
        superAdminScope.getScopeMappings().realmLevel().add(List.of(visibleRole, hiddenRole));
        superAdminScope.getScopeMappings().clientLevel(myclient.getId()).add(List.of(clientRole));

        // only client roles are visible, realm roles need view-realm or map-role
        createPermission(adminPermissionsClient, visibleRole.getId(), rolesType, Set.of(MAP_ROLE), policy);

        ClientScopeResource clientScopeResource = realmAdminClient.realm(realm.getName()).clientScopes().get(clientScope.getId());

        MappingsRepresentation all = clientScopeResource.getScopeMappings().getAll();
        assertThat(all.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("visibleScopeRole")));
        assertThat(all.getClientMappings().get("myclient").getMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("scopeClientRole")));

        assertThat(clientScopeResource.getScopeMappings().realmLevel().listAll().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("visibleScopeRole")));
        assertThat(clientScopeResource.getScopeMappings().clientLevel(myclient.getId()).listAll().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("scopeClientRole")));

        // the super admin keeps seeing everything
        assertThat(superAdminScope.getScopeMappings().realmLevel().listAll().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), equalTo(Set.of("visibleScopeRole", "hiddenScopeRole")));
    }

    @Test
    public void testAdminApiCompositesFilterHiddenClientRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation visibleClient = new ClientRepresentation();
        visibleClient.setClientId("visible-client");
        try (Response response = realm.admin().clients().create(visibleClient)) {
            visibleClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation secretClient = new ClientRepresentation();
        secretClient.setClientId("secret-client");
        try (Response response = realm.admin().clients().create(secretClient)) {
            secretClient.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation visibleChild = new RoleRepresentation();
        visibleChild.setName("VISIBLE_CHILD");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleChild);
        visibleChild = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_CHILD").toRepresentation();

        RoleRepresentation secretChild = new RoleRepresentation();
        secretChild.setName("SECRET_CHILD");
        realm.admin().clients().get(secretClient.getId()).roles().create(secretChild);
        secretChild = realm.admin().clients().get(secretClient.getId()).roles().get("SECRET_CHILD").toRepresentation();

        RoleRepresentation visibleParent = new RoleRepresentation();
        visibleParent.setName("VISIBLE_PARENT");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleParent);
        visibleParent = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").toRepresentation();
        realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").addComposites(List.of(visibleChild, secretChild));

        RoleRepresentation realmParent = new RoleRepresentation();
        realmParent.setName("REALM_PARENT");
        realm.admin().roles().create(realmParent);
        realmParent = realm.admin().roles().get("REALM_PARENT").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("REALM_PARENT").remove());
        realm.admin().roles().get("REALM_PARENT").addComposites(List.of(visibleChild, secretChild));

        String realmMgmtId = realm.admin().clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation manageRealmRole = realm.admin().clients().get(realmMgmtId).roles().get("manage-realm").toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmMgmtId).add(List.of(manageRealmRole));
        realmAdminClient.tokenManager().grantToken();

        UserRepresentation targetUser = createUser("targetUser");
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(visibleClient.getId()).add(List.of(visibleChild));
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(secretClient.getId()).add(List.of(secretChild));

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, visibleClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, targetUser.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);

        Set<String> roleNames;

        // GET /clients/{clientUuid}/roles/{roleName}/composites
        roleNames = realmAdminClient.realm(realm.getName())
                .clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").getRoleComposites()
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(roleNames, hasItem("VISIBLE_CHILD"));
        assertThat(roleNames, not(hasItem("SECRET_CHILD")));

        // GET /clients/{clientUuid}/roles/{roleName}/composites/clients/{hiddenClientUuid}
        assertThat(realmAdminClient.realm(realm.getName())
                .clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT")
                .getClientRoleComposites(secretClient.getId()), empty());

        // GET /roles-by-id/{roleId}/composites
        roleNames = realmAdminClient.realm(realm.getName())
                .rolesById().getRoleComposites(visibleParent.getId())
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(roleNames, hasItem("VISIBLE_CHILD"));
        assertThat(roleNames, not(hasItem("SECRET_CHILD")));

        // GET /roles-by-id/{roleId}/composites/clients/{hiddenClientUuid}
        assertThat(realmAdminClient.realm(realm.getName())
                .rolesById().getClientRoleComposites(visibleParent.getId(), secretClient.getId()), empty());

        // GET /roles/{roleName}/composites
        roleNames = realmAdminClient.realm(realm.getName())
                .roles().get("REALM_PARENT").getRoleComposites()
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(roleNames, hasItem("VISIBLE_CHILD"));
        assertThat(roleNames, not(hasItem("SECRET_CHILD")));

        // GET /users/{userId}/role-mappings — directly mapped roles from hidden clients should be filtered
        MappingsRepresentation mappings = realmAdminClient.realm(realm.getName())
                .users().get(targetUser.getId()).roles().getAll();
        Map<String, ClientMappingsRepresentation> clientMappings = mappings.getClientMappings();
        assertThat("visible-client mapping must be present", clientMappings, not(equalTo(null)));
        Set<String> allClientRoleNames = clientMappings.values().stream()
                .flatMap(m -> m.getMappings().stream())
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertThat(allClientRoleNames, hasItem("VISIBLE_CHILD"));
        assertThat(allClientRoleNames, not(hasItem("SECRET_CHILD")));
        assertThat("secret-client should not appear in client mappings",
                clientMappings.containsKey("secret-client"), equalTo(false));
    }

    /**
     * Regression test for https://github.com/keycloak/keycloak/issues/52399
     *
     * An admin granted VIEW on a specific user (but without view-realm) must not see realm-level
     * role mappings on /role-mappings/realm or /role-mappings/realm/composite, consistently with the
     * combined /role-mappings endpoint.
     */
    @Test
    public void testRealmRoleMappingsFilterHiddenRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("SECRET_REALM_ROLE");
        realm.admin().roles().create(realmRole);
        realmRole = realm.admin().roles().get("SECRET_REALM_ROLE").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("SECRET_REALM_ROLE").remove());

        UserRepresentation targetUser = createUser("targetUserRoleFilter");
        realm.admin().users().get(targetUser.getId()).roles().realmLevel().add(List.of(realmRole));

        // Create a client role on a visible client and assign it too (gives the narrow
        // admin something to see, so the user lookup itself is not forbidden)
        ClientRepresentation visibleClient = new ClientRepresentation();
        visibleClient.setClientId("visible-client-realm-test");
        try (Response response = realm.admin().clients().create(visibleClient)) {
            visibleClient.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clients().get(visibleClient.getId()).remove());
        }
        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("VISIBLE_CLIENT_ROLE");
        realm.admin().clients().get(visibleClient.getId()).roles().create(clientRole);
        clientRole = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_CLIENT_ROLE").toRepresentation();
        realm.admin().users().get(targetUser.getId()).roles().clientLevel(visibleClient.getId()).add(List.of(clientRole));

        // myadmin can view the target user and the visible client, but has no view-realm
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin Policy (realm role filter)", myadmin.getId());
        createPermission(adminPermissionsClient, targetUser.getId(),
                AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, visibleClient.getId(),
                AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // without view-realm the realm role must be hidden by all three endpoints
        assertThat(listRealmRoleMappings(targetUser.getId()), not(hasItem("SECRET_REALM_ROLE")));
        assertThat(listEffectiveRealmRoleMappings(targetUser.getId()), not(hasItem("SECRET_REALM_ROLE")));
        assertThat(listAllRealmRoleMappings(targetUser.getId()), not(hasItem("SECRET_REALM_ROLE")));

        // grant view-realm so that the realm role becomes visible
        String realmMgmtClientId = realm.admin().clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(realmMgmtClientId).roles()
                .get(AdminRoles.VIEW_REALM).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmMgmtClientId).add(List.of(viewRealmRole));
        realm.cleanup().add(r -> r.users().get(myadmin.getId()).roles().clientLevel(realmMgmtClientId).remove(List.of(viewRealmRole)));
        realmAdminClient.tokenManager().grantToken();

        assertThat(listRealmRoleMappings(targetUser.getId()), hasItem("SECRET_REALM_ROLE"));
        assertThat(listEffectiveRealmRoleMappings(targetUser.getId()), hasItem("SECRET_REALM_ROLE"));
        assertThat(listAllRealmRoleMappings(targetUser.getId()), hasItem("SECRET_REALM_ROLE"));
    }

    private Set<String> listRealmRoleMappings(String userId) {
        return realmAdminClient.realm(realm.getName()).users().get(userId).roles().realmLevel().listAll()
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private Set<String> listEffectiveRealmRoleMappings(String userId) {
        return realmAdminClient.realm(realm.getName()).users().get(userId).roles().realmLevel().listEffective()
                .stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private Set<String> listAllRealmRoleMappings(String userId) {
        MappingsRepresentation mappings = realmAdminClient.realm(realm.getName()).users().get(userId).roles().getAll();
        return mappings.getRealmMappings() == null
                ? Set.of()
                : mappings.getRealmMappings().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }
}
