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
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationResourceTypeEvaluationTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;
    
    private ClientResource clientResource;

    private String orgAId;
    private String orgBId;

    @BeforeEach
    public void setup() {
        clientResource = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        orgAId = createOrg("orgA", "orga.org");
        orgBId = createOrg("orgB", "orgb.org");
    }

    @Test
    public void testCannotViewOrManageWithoutPermission() {
        try {
            realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
            fail("Expected ForbiddenException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testViewSpecificOrganization() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        OrganizationRepresentation orgA = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        assertNotNull(orgA);
        assertEquals("orgA", orgA.getName());

        try {
            realmAdminClient.realm(realm.getName()).organizations().get(orgBId).toRepresentation();
            fail("Expected ForbiddenException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testManageSpecificOrganization() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        OrganizationResource orgAResource = realmAdminClient.realm(realm.getName()).organizations().get(orgAId);
        OrganizationRepresentation orgARep = orgAResource.toRepresentation();
        orgARep.setName("orgA-updated");
        try (Response response = orgAResource.update(orgARep)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgBRep = new OrganizationRepresentation();
        orgBRep.setName("orgB-updated");
        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgBId).update(orgBRep)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDeleteSpecificOrganization() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).delete()) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgBId).delete()) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testViewAllOrganizations() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(clientResource, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW));

        OrganizationRepresentation orgA = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        assertNotNull(orgA);
        OrganizationRepresentation orgB = realmAdminClient.realm(realm.getName()).organizations().get(orgBId).toRepresentation();
        assertNotNull(orgB);
    }

    @Test
    public void testManageAllOrganizations() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(clientResource, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW, MANAGE));

        OrganizationResource orgAResource = realmAdminClient.realm(realm.getName()).organizations().get(orgAId);
        OrganizationRepresentation orgARep = orgAResource.toRepresentation();
        orgARep.setName("orgA-updated");
        try (Response response = orgAResource.update(orgARep)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationResource orgBResource = realmAdminClient.realm(realm.getName()).organizations().get(orgBId);
        OrganizationRepresentation orgBRep = orgBResource.toRepresentation();
        orgBRep.setName("orgB-updated");
        try (Response response = orgBResource.update(orgBRep)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDenyOverridesAllowAll() {
        UserPolicyRepresentation allowPolicy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(clientResource, ORGANIZATIONS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW, MANAGE));

        OrganizationRepresentation orgARep = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        assertNotNull(orgARep);

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation denyPolicy = PermissionTestUtils.createUserPolicy(Logic.NEGATIVE, realm, clientResource, "Deny Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), denyPolicy);

        try {
            realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
            fail("Expected ForbiddenException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        OrganizationRepresentation orgBRep = realmAdminClient.realm(realm.getName()).organizations().get(orgBId).toRepresentation();
        assertNotNull(orgBRep);
    }

    @Test
    public void testViewDoesNotGrantManage() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        OrganizationRepresentation orgARep = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        assertNotNull(orgARep);

        orgARep.setName("orgA-updated");
        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).update(orgARep)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCreateRequiresGlobalManage() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(clientResource, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW, MANAGE));

        OrganizationRepresentation newOrg = new OrganizationRepresentation();
        newOrg.setName("orgC");
        newOrg.setAlias("orgC");
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName("orgc.org");
        newOrg.addDomain(domain);

        try (Response response = realmAdminClient.realm(realm.getName()).organizations().create(newOrg)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAllResourcePermissionScopeChange() {
        UserPolicyRepresentation policy = createAdminPolicy();
        ScopePermissionRepresentation allPerm = PermissionTestUtils.createAllPermission(clientResource, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW, MANAGE));

        realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        OrganizationRepresentation orgARep = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        orgARep.setName("orgA-updated");
        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).update(orgARep)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        allPerm = clientResource.authorization().permissions().scope().findByName(allPerm.getName());
        allPerm.setScopes(Set.of(VIEW));
        clientResource.authorization().permissions().scope().findById(allPerm.getId()).update(allPerm);

        realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        orgARep = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).toRepresentation();
        orgARep.setName("orgA-updated2");
        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).update(orgARep)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testMemberOrganizationsFilteredByFgapViewPermission() {
        UserPolicyRepresentation policy = createAdminPolicy();
        // grant view on orgA only
        PermissionTestUtils.createPermission(clientResource, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);
        // grant view on all users so auth.users().requireView(member) passes
        PermissionTestUtils.createAllPermission(clientResource, USERS_RESOURCE_TYPE, policy, Set.of(VIEW));

        // create a user who is a member of both orgs
        UserRepresentation userRep = UserBuilder.create()
                .username("multi-org-user")
                .email("multi-org-user@orga.org")
                .build();
        String userId;
        try (Response response = realm.admin().users().create(userRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            userId = ApiUtil.getCreatedId(response);
        }
        try (Response response = realm.admin().organizations().get(orgAId).members().addMember(userId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        try (Response response = realm.admin().organizations().get(orgBId).members().addMember(userId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // myadmin has FGAP view on orgA only — should see only orgA in the member's organizations
        List<OrganizationRepresentation> orgs = realmAdminClient.realm(realm.getName())
                .organizations().get(orgAId).members().member(userId).getOrganizations();

        Set<String> orgNames = orgs.stream().map(OrganizationRepresentation::getName).collect(java.util.stream.Collectors.toSet());
        assertThat(orgNames.contains("orgA"), equalTo(true));
        assertThat(orgNames.contains("orgB"), equalTo(false));
    }

    // -- helpers --

    private String createOrg(String name, String domainName) {
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName(name);
        orgRep.setAlias(name);
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(domainName);
        orgRep.addDomain(domain);

        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }

    private UserPolicyRepresentation createAdminPolicy() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        return PermissionTestUtils.createUserPolicy(realm, clientResource, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
    }
}
