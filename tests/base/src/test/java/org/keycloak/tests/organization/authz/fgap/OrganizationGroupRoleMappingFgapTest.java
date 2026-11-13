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

package org.keycloak.tests.organization.authz.fgap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationGroupResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ROLES_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that FGAP permissions are correctly enforced for role mappings on organization groups.
 * Uses minimal admin config (QUERY_USERS + QUERY_ORGANIZATIONS) so all access is FGAP-controlled.
 */
@KeycloakIntegrationTest
public class OrganizationGroupRoleMappingFgapTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;
    
    private ClientResource clientResource;

    private String orgId;
    private String groupId;
    private RoleRepresentation testRole;

    @BeforeEach
    public void setup() {
        clientResource = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        // Create org using realm admin
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("testOrg");
        orgRep.setAlias("testOrg");
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName("testorg.org");
        orgRep.addDomain(domain);

        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        // Create org group using realm admin
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("fgap-test-group");
        try (Response response = realm.admin().organizations().get(orgId).groups().addTopLevelGroup(groupRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            groupId = ApiUtil.getCreatedId(response);
        }

        // Create a realm role using realm admin
        testRole = new RoleRepresentation("fgap-test-role", "Test role for FGAP", false);
        realm.admin().roles().create(testRole);
        testRole = realm.admin().roles().get("fgap-test-role").toRepresentation();
    }

    @Test
    public void testNoOrgPermissionDeniesRoleMappingAccess() {
        // myadmin has no FGAP permissions on the org — should get 403
        try {
            getAdminOrgGroup().roles().realmLevel().listAll();
            fail("Expected ForbiddenException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testViewOrgPermissionFiltersHiddenRealmRoleMappings() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // Add role mapping using realm admin
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().realmLevel().add(List.of(testRole));

        // myadmin has VIEW on org but no view-realm — the realm role is hidden
        List<RoleRepresentation> roles = getAdminOrgGroup().roles().realmLevel().listAll();
        assertThat(roles, empty());

        // Grant view-realm — the realm role becomes visible
        String realmMgmtId = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID).toRepresentation().getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(realmMgmtId).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        realm.admin().users().get(myadminId).roles().clientLevel(realmMgmtId).add(List.of(viewRealmRole));

        roles = getAdminOrgGroup().roles().realmLevel().listAll();
        assertThat(roles, hasSize(1));
        assertThat(roles.get(0).getName(), is("fgap-test-role"));
    }

    @Test
    public void testViewOrgPermissionDeniesModifyingRoleMappings() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // myadmin with only VIEW cannot add role mappings
        try {
            getAdminOrgGroup().roles().realmLevel().add(List.of(testRole));
            fail("Expected ForbiddenException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testManageOrgWithoutRoleMapPermissionDenied() {
        UserPolicyRepresentation policy = createAdminPolicy();
        // Grant MANAGE on the org — top-level check passes
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        // myadmin has MANAGE on org but does NOT have MANAGE_USERS or MAP_ROLE permission
        // The per-role check (auth.roles().requireMapRole) should deny the operation
        try {
            getAdminOrgGroup().roles().realmLevel().add(List.of(testRole));
            fail("Expected ForbiddenException — no role map permission");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testManageOrgWithRoleMapPermissionAllowed() {
        UserPolicyRepresentation policy = createAdminPolicy();
        // Grant MANAGE on the org
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);
        // Grant MAP_ROLE on the specific role
        PermissionTestUtils.createPermission(clientResource, testRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

        // Now both checks pass: org-level MANAGE + per-role MAP_ROLE
        getAdminOrgGroup().roles().realmLevel().add(List.of(testRole));

        // Verify role was mapped
        List<RoleRepresentation> roles = getAdminOrgGroup().roles().realmLevel().listAll();
        assertThat(roles, hasSize(1));
        assertThat(roles.get(0).getName(), is("fgap-test-role"));
    }

    @Test
    public void testAvailableRolesFilteredByMapPermission() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        // Create two roles, grant MAP_ROLE only on one
        RoleRepresentation allowedRole = new RoleRepresentation("allowed-role", "", false);
        realm.admin().roles().create(allowedRole);
        allowedRole = realm.admin().roles().get("allowed-role").toRepresentation();

        RoleRepresentation deniedRole = new RoleRepresentation("denied-role", "", false);
        realm.admin().roles().create(deniedRole);

        PermissionTestUtils.createPermission(clientResource, allowedRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

        // Available roles should include only the allowed role (among custom roles)
        List<RoleRepresentation> available = getAdminOrgGroup().roles().realmLevel().listAvailable();
        Set<String> availableNames = available.stream()
                .map(RoleRepresentation::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(availableNames.contains("allowed-role"), is(true));
        assertThat(availableNames.contains("denied-role"), is(false));
    }

    @Test
    public void testRemoveRoleMappingRequiresManageAndMapRole() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);
        PermissionTestUtils.createPermission(clientResource, testRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

        // Add role mapping
        getAdminOrgGroup().roles().realmLevel().add(List.of(testRole));
        assertThat(getAdminOrgGroup().roles().realmLevel().listAll(), hasSize(1));

        // Remove role mapping — should succeed with MANAGE + MAP_ROLE
        getAdminOrgGroup().roles().realmLevel().remove(List.of(testRole));
        assertThat(getAdminOrgGroup().roles().realmLevel().listAll(), hasSize(0));
    }

    @Test
    public void testGroupRepresentationFiltersHiddenClientRoles() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // Create a client with a role and map it to the org group (using realm admin)
        String hiddenClientId = createClientWithRole("hidden-client", "hidden-role");
        RoleRepresentation hiddenRole = realm.admin().clients().get(hiddenClientId).roles().get("hidden-role").toRepresentation();
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().clientLevel(hiddenClientId).add(List.of(hiddenRole));

        // myadmin has VIEW on org but no permission to view the client
        GroupRepresentation rep = getAdminOrgGroup().toRepresentation(false);
        assertClientRoleAbsent(rep, "hidden-client", "hidden-role");

        // Grant VIEW on the client — now the role should appear
        PermissionTestUtils.createPermission(clientResource, hiddenClientId, CLIENTS_RESOURCE_TYPE, Set.of(VIEW), policy);
        rep = getAdminOrgGroup().toRepresentation(false);
        assertClientRolePresent(rep, "hidden-client", "hidden-role");
    }

    @Test
    public void testGroupListFullRepresentationFiltersHiddenClientRoles() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        String hiddenClientId = createClientWithRole("hidden-client-list", "hidden-list-role");
        RoleRepresentation hiddenRole = realm.admin().clients().get(hiddenClientId).roles().get("hidden-list-role").toRepresentation();
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().clientLevel(hiddenClientId).add(List.of(hiddenRole));

        // Full representation list — myadmin cannot view the client
        List<GroupRepresentation> groups = realmAdminClient.realm(realm.getName()).organizations().get(orgId).groups()
                .getAll("fgap-test-group", null, true, null, null, false, false);
        assertThat(groups, hasSize(1));
        assertClientRoleAbsent(groups.get(0), "hidden-client-list", "hidden-list-role");
    }

    @Test
    public void testGroupByPathFiltersHiddenClientRoles() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        String hiddenClientId = createClientWithRole("hidden-client-path", "hidden-path-role");
        RoleRepresentation hiddenRole = realm.admin().clients().get(hiddenClientId).roles().get("hidden-path-role").toRepresentation();
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().clientLevel(hiddenClientId).add(List.of(hiddenRole));

        // group-by-path with full representation — myadmin cannot view the client
        GroupRepresentation rep = realmAdminClient.realm(realm.getName()).organizations().get(orgId).groups()
                .getGroupByPath("fgap-test-group", false, false);
        assertClientRoleAbsent(rep, "hidden-client-path", "hidden-path-role");
    }

    @Test
    public void testGroupRepresentationFiltersHiddenRealmRoles() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // Map the realm role to the org group (using realm admin with MAP_ROLE)
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().realmLevel().add(List.of(testRole));

        // myadmin has VIEW on org but no view-realm — realm roles should be hidden
        GroupRepresentation rep = getAdminOrgGroup().toRepresentation(false);
        assertTrue(rep.getRealmRoles() == null || !rep.getRealmRoles().contains("fgap-test-role"));

        // Grant view-realm — realm roles become visible via canViewRealm()
        String realmMgmtId = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID).toRepresentation().getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(realmMgmtId).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        realm.admin().users().get(myadminId).roles().clientLevel(realmMgmtId).add(List.of(viewRealmRole));

        rep = getAdminOrgGroup().toRepresentation(false);
        assertTrue(rep.getRealmRoles() != null && rep.getRealmRoles().contains("fgap-test-role"));
    }

    // -- helpers --

    private OrganizationGroupResource getAdminOrgGroup() {
        return realmAdminClient.realm(realm.getName()).organizations().get(orgId).groups().group(groupId);
    }

    private UserPolicyRepresentation createAdminPolicy() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        return PermissionTestUtils.createUserPolicy(realm, clientResource, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
    }

    private String createClientWithRole(String clientId, String roleName) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setProtocol("openid-connect");
        try (Response response = realm.admin().clients().create(clientRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.admin().clients().get(id).roles().create(new RoleRepresentation(roleName, "", false));
            return id;
        }
    }

    private void assertClientRoleAbsent(GroupRepresentation rep, String clientId, String roleName) {
        Map<String, List<String>> clientRoles = rep.getClientRoles();
        if (clientRoles == null) return;
        List<String> roles = clientRoles.get(clientId);
        assertTrue(roles == null || !roles.contains(roleName),
                "Client role " + clientId + ":" + roleName + " should not be visible");
    }

    private void assertClientRolePresent(GroupRepresentation rep, String clientId, String roleName) {
        Map<String, List<String>> clientRoles = rep.getClientRoles();
        assertTrue(clientRoles != null && clientRoles.containsKey(clientId)
                && clientRoles.get(clientId).contains(roleName),
                "Client role " + clientId + ":" + roleName + " should be visible");
    }
}
