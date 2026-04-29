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
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationGroupResource;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ROLES_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that FGAP permissions are correctly enforced for role mappings on organization groups.
 * Uses minimal admin config (QUERY_USERS + QUERY_ORGANIZATIONS) so all access is FGAP-controlled.
 */
@KeycloakIntegrationTest
public class OrganizationGroupRoleMappingFgapTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private String orgId;
    private String groupId;
    private RoleRepresentation testRole;

    @BeforeEach
    public void setup() {
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
    public void testViewOrgPermissionAllowsListingRoleMappings() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        // Add role mapping using realm admin
        realm.admin().organizations().get(orgId).groups().group(groupId).roles().realmLevel().add(List.of(testRole));

        // myadmin with VIEW can list role mappings
        List<RoleRepresentation> roles = getAdminOrgGroup().roles().realmLevel().listAll();
        assertThat(roles, hasSize(1));
        assertThat(roles.get(0).getName(), is("fgap-test-role"));
    }

    @Test
    public void testViewOrgPermissionDeniesModifyingRoleMappings() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

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
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

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
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);
        // Grant MAP_ROLE on the specific role
        PermissionTestUtils.createPermission(client, testRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

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
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        // Create two roles, grant MAP_ROLE only on one
        RoleRepresentation allowedRole = new RoleRepresentation("allowed-role", "", false);
        realm.admin().roles().create(allowedRole);
        allowedRole = realm.admin().roles().get("allowed-role").toRepresentation();

        RoleRepresentation deniedRole = new RoleRepresentation("denied-role", "", false);
        realm.admin().roles().create(deniedRole);

        PermissionTestUtils.createPermission(client, allowedRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

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
        PermissionTestUtils.createPermission(client, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);
        PermissionTestUtils.createPermission(client, testRole.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);

        // Add role mapping
        getAdminOrgGroup().roles().realmLevel().add(List.of(testRole));
        assertThat(getAdminOrgGroup().roles().realmLevel().listAll(), hasSize(1));

        // Remove role mapping — should succeed with MANAGE + MAP_ROLE
        getAdminOrgGroup().roles().realmLevel().remove(List.of(testRole));
        assertThat(getAdminOrgGroup().roles().realmLevel().listAll(), hasSize(0));
    }

    // -- helpers --

    private OrganizationGroupResource getAdminOrgGroup() {
        return realmAdminClient.realm(realm.getName()).organizations().get(orgId).groups().group(groupId);
    }

    private UserPolicyRepresentation createAdminPolicy() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        return PermissionTestUtils.createUserPolicy(realm, client, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
    }
}
