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

package org.keycloak.testsuite.organization.mapper;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedGroupMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class OrganizationGroupOidcIdpMapperTest extends AbstractOrganizationTest {

    @Test
    public void testAdvancedClaimToGroupMapperWithOrganizationGroup() {
        // Create organization with IdP
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create organization group
        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-org-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation createdGroup = orgResource.groups().group(groupId).toRepresentation(false);
        String groupPath = createdGroup.getPath();

        // Create AdvancedClaimToGroupMapper with organization group
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("org-group-mapper");
        mapper.setIdentityProviderMapper(AdvancedClaimToGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .put(AdvancedClaimToGroupMapper.CLAIM, "organization")
                .put(AdvancedClaimToGroupMapper.CLAIM_VALUE, orgRep.getName())
                .build());

        String mapperId;
        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        // Verify mapper was created
        IdentityProviderMapperRepresentation createdMapper = testRealm().identityProviders()
                .get(idp.getAlias())
                .getMapperById(mapperId);

        assertNotNull("Mapper should be created", createdMapper);
        assertEquals("Mapper should reference org group", groupPath, createdMapper.getConfig().get(ConfigConstants.GROUP));
    }

    @Test
    public void testCreateMapperWithOrganizationSubgroup() {
        // Create organization with IdP
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create parent organization group
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName("parent-group");
        String parentId;
        try (Response response = orgResource.groups().addTopLevelGroup(parentGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            parentId = ApiUtil.getCreatedId(response);
        }

        // Create child subgroup
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-group");
        try (Response response = orgResource.groups().group(parentId).addSubGroup(childGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Get the subgroup from the parent's children
        List<GroupRepresentation> children = orgResource.groups().group(parentId).getSubGroups(null, null, null, null);
        assertNotNull("Parent should have subgroups", children);
        assertThat("Parent should have 1 subgroup", children.size(), is(1));

        String childGroupPath = children.get(0).getPath();

        // Create mapper with child subgroup
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("subgroup-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString())
                .put(ConfigConstants.GROUP, childGroupPath)
                .build());

        String mapperId;
        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        // Verify mapper was created with subgroup path
        IdentityProviderMapperRepresentation createdMapper = testRealm().identityProviders()
                .get(idp.getAlias())
                .getMapperById(mapperId);

        assertNotNull("Mapper should be created", createdMapper);
        assertEquals("Mapper should reference org subgroup", childGroupPath, createdMapper.getConfig().get(ConfigConstants.GROUP));
    }

    @Test
    public void testGetGroupsEndpointForNonOrganizationIdp() {
        // Create IdP NOT linked to organization
        IdentityProviderRepresentation nonOrgIdp = bc.setUpIdentityProvider();
        nonOrgIdp.setAlias("non-org-idp");
        try (Response response = testRealm().identityProviders().create(nonOrgIdp)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
        getCleanup().addCleanup(() -> testRealm().identityProviders().get("non-org-idp").remove());

        // Create organization with groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-org-group");
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Try to get groups for non-org IdP - should return NOT_FOUND
        try {
            testRealm().organizations().get(orgRep.getId())
                    .identityProviders().get("non-org-idp").getGroups(null, null, false, null, null, true, false);
            fail("Should have failed with NotFoundException");
        } catch (jakarta.ws.rs.NotFoundException e) {
            // Expected
        }
    }

    @Test
    public void testUserAddedToOrganizationGroupViaMapper() {
        // Create organization with group
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("mapper-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // Add hardcoded group mapper to the organization IdP
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("org-group-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Authenticate via IdP - user should be added to org group
        assertBrokerRegistration(orgResource, bc.getUserLogin(), bc.getUserEmail());

        // Verify user is member of the organization group
        UserRepresentation user = getUserRepresentation(bc.getUserEmail());
        assertNotNull(user);

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));
        assertThat(groupMembers.get(0).getId(), is(user.getId()));
    }

    @Test
    public void testUserNotAddedToGroupAfterIdpUnlinkedFromOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("unlink-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // Add a HardcodedGroupMapper pointing to the org group
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("unlink-test-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // First login: user IS added to the org group while IdP is still linked
        assertBrokerRegistration(orgResource, bc.getUserLogin(), bc.getUserEmail());

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));

        // Log out from both realms
        UserRepresentation user = getUserRepresentation(bc.getUserEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(user.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        // Unlink IdP from organization - the IdP still exists in the realm but is no longer org-linked
        try (Response response = orgResource.identityProviders().get(bc.getIDPAlias()).delete()) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Remove the user from the group so the second login can prove the mapper does not re-add them
        orgResource.groups().group(groupId).removeMember(user.getId());
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(0));

        // Second login: bypass the org identity-first page (which hides the unlinked IdP) by
        // navigating directly with kc_idp_hint. The IdP still exists in the realm so login succeeds,
        // but the mapper cannot resolve the org group and the user is NOT re-added to it.
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        driver.navigate().to(driver.getCurrentUrl() + "&kc_idp_hint=" + bc.getIDPAlias());
        loginOrgIdp(bc.getUserLogin(), bc.getUserEmail(), false, true);

        // Verify user was not re-added to the org group
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(0));
    }

    @Test
    public void testRealmGroupAllowedWithOrganizationIdp() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create REALM group in the consumer realm
        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("realm-test-group");
        try (Response response = realmsResouce().realm(bc.consumerRealmName()).groups().add(realmGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        String groupPath = realmsResouce().realm(bc.consumerRealmName()).getGroupByPath("/realm-test-group").getPath();

        // Add mapper with REALM group to organization IdP
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("realm-group-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Authenticate via IdP - realm groups are always allowed
        assertBrokerRegistration(orgResource, bc.getUserLogin(), bc.getUserEmail());

        // Verify user is member of the realm group
        UserRepresentation user = getUserRepresentation(bc.getUserEmail());
        assertNotNull(user);

        List<GroupRepresentation> userGroups = realmsResouce().realm(bc.consumerRealmName()).users().get(user.getId()).groups();
        assertThat(userGroups, hasSize(1));
        assertThat(userGroups.get(0).getPath(), is(groupPath));
    }

    @Test
    public void testHardcodedGroupMapperDoesNotAssignOrganizationGroupMembershipWhenOrganizationIsDisabled() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("disabled-org-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        IdentityProviderRepresentation idp = orgResource.identityProviders().get(bc.getIDPAlias()).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("disabled-org-test-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put(ConfigConstants.GROUP, groupPath)
                .build());

        try (Response response = testRealm().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // First login: org is enabled, user IS added to org group
        assertBrokerRegistration(orgResource, bc.getUserLogin(), bc.getUserEmail());

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));

        // When org is disabled, the IdP appears disabled, blocking further broker logins.
        orgRep.setEnabled(false);
        try (Response ignored = orgResource.update(orgRep)) {
            assertThat(ignored.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify the org-linked IdP now appears disabled (org-aware wrapper)
        IdentityProviderRepresentation updatedIdp = testRealm().identityProviders().get(bc.getIDPAlias()).toRepresentation();
        assertThat("IdP should appear disabled when org is disabled", updatedIdp.isEnabled(), is(false));

        // Group membership assigned while the org was enabled is unaffected by the org being disabled
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));
    }
}
