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

package org.keycloak.tests.organization.authz;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationMembersResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that FGAP V2 user permissions are correctly applied when searching/counting organization members.
 * An admin with manage-organizations + query-users should only see org members they have FGAP permission to view.
 */
@KeycloakIntegrationTest
public class OrganizationMemberFgapTest {

    @InjectRealm(config = OrganizationAdminPermissionsConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @InjectUser(ref = "charlie")
    ManagedUser userCharlie;

    private String orgId;

    @BeforeEach
    public void setup() {
        // create the organization
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

        // add users as org members
        addMember(userAlice.getId());
        addMember(userBob.getId());
        addMember(userCharlie.getId());
    }

    @Test
    public void testSearchReturnsOnlyPermittedMembers() {
        // grant myadmin permission to view only alice and bob
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(userAlice.getId(), userBob.getId()), AdminPermissionsSchema.USERS.getType(), Set.of(VIEW), policy);

        List<MemberRepresentation> result = getOrgMembers().list(-1, -1);

        // should only see alice and bob, not charlie
        assertThat(result, hasSize(2));
        Set<String> returnedUsernames = result.stream().map(MemberRepresentation::getUsername).collect(Collectors.toSet());
        assertTrue(returnedUsernames.contains(userAlice.getUsername()));
        assertTrue(returnedUsernames.contains(userBob.getUsername()));
    }

    @Test
    public void testCountReturnsOnlyPermittedMembers() {
        // grant myadmin permission to view only alice
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(userAlice.getId()), AdminPermissionsSchema.USERS.getType(), Set.of(VIEW), policy);

        assertThat(getOrgMembers().count(), equalTo(1L));
    }

    @Test
    public void testSearchWithNoPermissionsReturnsEmpty() {
        // no user view permissions granted - myadmin has only query-users + manage-organizations
        assertTrue(getOrgMembers().list(null, null).isEmpty());
    }

    @Test
    public void testCountWithNoPermissionsReturnsZero() {
        // no user view permissions granted
        assertThat(getOrgMembers().count(), equalTo(0L));
    }

    @Test
    public void testSearchWithAllUsersPermission() {
        // grant myadmin permission to view all users
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, AdminPermissionsSchema.USERS.getType(), policy, Set.of(VIEW));

        // should see all three members
        assertThat(getOrgMembers().list(null, null), hasSize(3));
    }

    @Test
    public void testCountWithAllUsersPermission() {
        // grant myadmin permission to view all users
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createAllPermission(client, AdminPermissionsSchema.USERS.getType(), policy, Set.of(VIEW));

        assertThat(getOrgMembers().count(), equalTo(3L));
    }

    @Test
    public void testSearchWithGroupBasedPermission() {
        // create a realm group and add alice and bob to it
        GroupRepresentation group = new GroupRepresentation();
        group.setName("viewable-group");
        try (Response response = realm.admin().groups().add(group)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            group.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(userAlice.getId()).joinGroup(group.getId());
        realm.admin().users().get(userBob.getId()).joinGroup(group.getId());

        // grant myadmin VIEW_MEMBERS on the group - this implicitly allows viewing users in that group
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(group.getId()), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), policy);

        List<MemberRepresentation> result = getOrgMembers().list(-1, -1);

        // should see alice and bob (in the group), not charlie
        assertThat(result, hasSize(2));
        Set<String> returnedUsernames = result.stream().map(MemberRepresentation::getUsername).collect(Collectors.toSet());
        assertTrue(returnedUsernames.contains(userAlice.getUsername()));
        assertTrue(returnedUsernames.contains(userBob.getUsername()));
    }

    @Test
    public void testCountWithGroupBasedPermission() {
        // create a realm group and add only alice to it
        GroupRepresentation group = new GroupRepresentation();
        group.setName("viewable-group");
        try (Response response = realm.admin().groups().add(group)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            group.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(userAlice.getId()).joinGroup(group.getId());

        // grant myadmin VIEW_MEMBERS on the group
        UserPolicyRepresentation policy = createAdminPolicy();
        PermissionTestUtils.createPermission(client, Set.of(group.getId()), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), policy);

        Long count = getOrgMembers().count();

        // should count only alice
        assertThat(count, equalTo(1L));
    }

    // -- helpers --

    private OrganizationMembersResource getOrgMembers() {
        return realmAdminClient.realm(realm.getName()).organizations().get(orgId).members();
    }

    private void addMember(String userId) {
        try (Response response = realm.admin().organizations().get(orgId).members().addMember(userId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private UserPolicyRepresentation createAdminPolicy() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        return PermissionTestUtils.createUserPolicy(realm, client, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
    }
}
