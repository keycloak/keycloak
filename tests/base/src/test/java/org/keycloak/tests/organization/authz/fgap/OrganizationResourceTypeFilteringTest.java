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
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationResourceTypeFilteringTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    private String orgAId;
    private String orgBId;
    private String orgCId;

    @BeforeEach
    public void setup() {
        orgAId = createOrg("orgA", "orga.org");
        orgBId = createOrg("orgB", "orgb.org");
        orgCId = createOrg("orgC", "orgc.org");
    }

    @Test
    public void testSearchReturnsEmptyWithNoPermissions() {
        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCountReturnsZeroWithNoPermissions() {
        long count = realmAdminClient.realm(realm.getName()).organizations().count(null);
        assertThat(count, equalTo(0L));
    }

    @Test
    public void testSearchReturnsOnlyPermittedOrganizations() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(orgAId, orgBId), ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(2));
        Set<String> names = result.stream().map(OrganizationRepresentation::getName).collect(Collectors.toSet());
        assertTrue(names.contains("orgA"));
        assertTrue(names.contains("orgB"));
    }

    @Test
    public void testCountReturnsOnlyPermittedOrganizations() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, orgAId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        long count = realmAdminClient.realm(realm.getName()).organizations().count(null);
        assertThat(count, equalTo(1L));
    }

    @Test
    public void testSearchWithAllOrganizationsPermission() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(3));
    }

    @Test
    public void testCountWithAllOrganizationsPermission() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW));

        long count = realmAdminClient.realm(realm.getName()).organizations().count(null);
        assertThat(count, equalTo(3L));
    }

    @Test
    public void testDenyRemovesFromSearchResults() {
        UserPolicyRepresentation allowPolicy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, ORGANIZATIONS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW));

        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(3));

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation denyPolicy = PermissionTestUtils.createUserPolicy(Logic.NEGATIVE, realm, client, "Deny Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createPermission(client, orgBId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), denyPolicy);

        result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(2));
        assertTrue(result.stream().map(OrganizationRepresentation::getId).noneMatch(orgBId::equals));
    }

    @Test
    public void testDenyReducesCount() {
        UserPolicyRepresentation allowPolicy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, ORGANIZATIONS_RESOURCE_TYPE, allowPolicy, Set.of(VIEW));

        assertThat(realmAdminClient.realm(realm.getName()).organizations().count(null), equalTo(3L));

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation denyPolicy = PermissionTestUtils.createUserPolicy(Logic.NEGATIVE, realm, client, "Deny Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createPermission(client, orgBId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), denyPolicy);

        assertThat(realmAdminClient.realm(realm.getName()).organizations().count(null), equalTo(2L));
    }

    @Test
    public void testSearchByName() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(orgAId, orgCId), ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search("orgA", true, -1, -1);
        assertThat(result, hasSize(1));
        assertEquals("orgA", result.get(0).getName());

        result = realmAdminClient.realm(realm.getName()).organizations().search("orgB", true, -1, -1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetByMemberRespectsPermissions() {
        realm.admin().organizations().get(orgAId).members().addMember(userAlice.getId()).close();
        realm.admin().organizations().get(orgBId).members().addMember(userAlice.getId()).close();
        realm.admin().organizations().get(orgCId).members().addMember(userAlice.getId()).close();

        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, userAlice.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        PermissionTestUtils.createPermission(client, Set.of(orgAId, orgCId), ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<OrganizationRepresentation> orgs = realmAdminClient.realm(realm.getName()).organizations().members().getOrganizations(userAlice.getId());
        assertThat(orgs, hasSize(2));
        Set<String> names = orgs.stream().map(OrganizationRepresentation::getName).collect(Collectors.toSet());
        assertTrue(names.contains("orgA"));
        assertTrue(names.contains("orgC"));
    }

    @Test
    public void testGetByMemberWithAllOrganizationsPermission() {
        realm.admin().organizations().get(orgAId).members().addMember(userAlice.getId()).close();
        realm.admin().organizations().get(orgBId).members().addMember(userAlice.getId()).close();

        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, userAlice.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        PermissionTestUtils.createAllPermission(client, ORGANIZATIONS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<OrganizationRepresentation> orgs = realmAdminClient.realm(realm.getName()).organizations().members().getOrganizations(userAlice.getId());
        assertThat(orgs, hasSize(2));
    }

    @Test
    public void testDeleteCleansUpFgapResources() {
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(orgAId, orgBId, orgCId), ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);

        List<OrganizationRepresentation> result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(3));

        try (Response response = realmAdminClient.realm(realm.getName()).organizations().get(orgAId).delete()) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        result = realmAdminClient.realm(realm.getName()).organizations().search(null, null, -1, -1);
        assertThat(result, hasSize(2));
        assertTrue(result.stream().map(OrganizationRepresentation::getId).noneMatch(orgAId::equals));
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
        return PermissionTestUtils.createUserPolicy(realm, client, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
    }
}
