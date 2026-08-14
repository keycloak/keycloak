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

package org.keycloak.tests.organization.mapper;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.HardcodedGroupMapper;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationGroupOidcIdpMapperTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "provider", config = ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AliceUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginUpdateProfilePage loginUpdateProfilePage;

    @BeforeEach
    public void onBefore() {
        for (OrganizationRepresentation org : realm.admin().organizations().list(null, null)) {
            realm.admin().organizations().get(org.getId()).delete().close();
        }
        realm.admin().identityProviders().findAll().forEach(idp -> realm.admin().identityProviders().get(idp.getAlias()).remove());
        realm.admin().users().list().stream()
                .filter(u -> !"admin".equals(u.getUsername()))
                .forEach(u -> realm.admin().users().get(u.getId()).remove());
        createTestClients();
    }

    @Test
    public void testAdvancedClaimToGroupMapperWithOrganizationGroup() {
        // Create organization with IdP
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());
        String idpAlias = organizationName + "-identity-provider";

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
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("org-group-mapper");
        mapper.setIdentityProviderMapper(AdvancedClaimToGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString(),
                ConfigConstants.GROUP, groupPath,
                AdvancedClaimToGroupMapper.CLAIM, "organization",
                AdvancedClaimToGroupMapper.CLAIM_VALUE, orgRep.getName()));

        String mapperId;
        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        // Verify mapper was created
        IdentityProviderMapperRepresentation createdMapper = realm.admin().identityProviders()
                .get(idp.getAlias())
                .getMapperById(mapperId);

        assertNotNull(createdMapper, "Mapper should be created");
        assertEquals(groupPath, createdMapper.getConfig().get(ConfigConstants.GROUP), "Mapper should reference org group");
    }

    @Test
    public void testCreateMapperWithOrganizationSubgroup() {
        // Create organization with IdP
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());
        String idpAlias = organizationName + "-identity-provider";

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
        assertNotNull(children, "Parent should have subgroups");
        assertThat("Parent should have 1 subgroup", children.size(), is(1));

        String childGroupPath = children.get(0).getPath();

        // Create mapper with child subgroup
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("subgroup-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString(),
                ConfigConstants.GROUP, childGroupPath));

        String mapperId;
        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        // Verify mapper was created with subgroup path
        IdentityProviderMapperRepresentation createdMapper = realm.admin().identityProviders()
                .get(idp.getAlias())
                .getMapperById(mapperId);

        assertNotNull(createdMapper, "Mapper should be created");
        assertEquals(childGroupPath, createdMapper.getConfig().get(ConfigConstants.GROUP), "Mapper should reference org subgroup");
    }

    @Test
    public void testGetGroupsEndpointForNonOrganizationIdp() {
        // Create IdP NOT linked to organization
        IdentityProviderRepresentation nonOrgIdp = createOrgBroker("non-org");
        nonOrgIdp.setAlias("non-org-idp");
        try (Response response = realm.admin().identityProviders().create(nonOrgIdp)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
        realm.cleanup().add(r -> r.identityProviders().get("non-org-idp").remove());

        // Create organization with groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-org-group");
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Try to get groups for non-org IdP - should return NOT_FOUND
        try {
            realm.admin().organizations().get(orgRep.getId())
                    .identityProviders().get("non-org-idp").getGroups(null, null, false, null, null, true, false);
            fail("Should have failed with NotFoundException");
        } catch (jakarta.ws.rs.NotFoundException e) {
            // Expected
        }
    }

    @Test
    public void testUserAddedToOrganizationGroupViaMapper() {
        // Create organization with real broker
        String idpAlias = organizationName + "-identity-provider";
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(idpAlias, providerRealm), organizationName + ".org");
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("mapper-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // Add hardcoded group mapper to the organization IdP
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("org-group-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString(),
                ConfigConstants.GROUP, groupPath,
                ConfigConstants.GROUP_TYPE, GroupModel.Type.ORGANIZATION.name()));

        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Authenticate via IdP - user should be added to org group
        assertBrokerRegistration(orgResource, aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getEmail(),
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        // Verify user is member of the organization group
        UserRepresentation user = getUserRepresentation(aliceFromProviderRealm.getEmail());
        assertNotNull(user);

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));
        assertThat(groupMembers.get(0).getId(), is(user.getId()));
    }

    @Test
    public void testUserNotAddedToGroupAfterIdpUnlinkedFromOrganization() {
        String idpAlias = organizationName + "-identity-provider";
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(idpAlias, providerRealm), organizationName + ".org");
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("unlink-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // Add a HardcodedGroupMapper pointing to the org group
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("unlink-test-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString(),
                ConfigConstants.GROUP, groupPath,
                ConfigConstants.GROUP_TYPE, GroupModel.Type.ORGANIZATION.name()));

        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // First login: user IS added to the org group while IdP is still linked
        assertBrokerRegistration(orgResource, aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getEmail(),
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));

        // Log out from both realms
        UserRepresentation user = getUserRepresentation(aliceFromProviderRealm.getEmail());
        realm.admin().users().get(user.getId()).logout();
        providerRealm.admin().logoutAll();

        // Unlink IdP from organization - the IdP still exists in the realm but is no longer org-linked
        try (Response response = orgResource.identityProviders().get(idpAlias).delete()) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Remove the user from the group so the second login can prove the mapper does not re-add them
        orgResource.groups().group(groupId).removeMember(user.getId());
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(0));

        // Second login: bypass the org identity-first page (which hides the unlinked IdP) by
        // navigating directly with kc_idp_hint. The IdP still exists in the realm so login succeeds,
        // but the mapper cannot resolve the org group and the user is NOT re-added to it.
        oauth.openLoginForm();
        oauth.getDriver().navigate().to(oauth.getDriver().getCurrentUrl() + "&kc_idp_hint=" + idpAlias);
        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();

        // Verify user was not re-added to the org group
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(0));
    }

    @Test
    public void testRealmGroupAllowedWithOrganizationIdp() {
        String idpAlias = organizationName + "-identity-provider";
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(idpAlias, providerRealm), organizationName + ".org");
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create REALM group in the consumer realm
        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("realm-test-group");
        try (Response response = realm.admin().groups().add(realmGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        String groupPath = realm.admin().getGroupByPath("/realm-test-group").getPath();

        // Add mapper with REALM group to organization IdP
        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("realm-group-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.toString(),
                ConfigConstants.GROUP, groupPath));

        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Authenticate via IdP - realm groups are always allowed
        assertBrokerRegistration(orgResource, aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getEmail(),
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        // Verify user is member of the realm group
        UserRepresentation user = getUserRepresentation(aliceFromProviderRealm.getEmail());
        assertNotNull(user);

        List<GroupRepresentation> userGroups = realm.admin().users().get(user.getId()).groups();
        assertThat(userGroups, hasSize(1));
        assertThat(userGroups.get(0).getPath(), is(groupPath));
    }

    @Test
    public void testHardcodedGroupMapperDoesNotAssignOrganizationGroupMembershipWhenOrganizationIsDisabled() {
        String idpAlias = organizationName + "-identity-provider";
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(idpAlias, providerRealm), organizationName + ".org");
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("disabled-org-test-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        IdentityProviderRepresentation idp = orgResource.identityProviders().get(idpAlias).toRepresentation();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("disabled-org-test-mapper");
        mapper.setIdentityProviderMapper(HardcodedGroupMapper.PROVIDER_ID);
        mapper.setIdentityProviderAlias(idp.getAlias());
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString(),
                ConfigConstants.GROUP, groupPath,
                ConfigConstants.GROUP_TYPE, GroupModel.Type.ORGANIZATION.name()));

        try (Response response = realm.admin().identityProviders().get(idp.getAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // First login: org is enabled, user IS added to org group
        assertBrokerRegistration(orgResource, aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getEmail(),
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));

        // When org is disabled, the IdP appears disabled, blocking further broker logins.
        orgRep.setEnabled(false);
        try (Response ignored = orgResource.update(orgRep)) {
            assertThat(ignored.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify the org-linked IdP now appears disabled (org-aware wrapper)
        IdentityProviderRepresentation updatedIdp = realm.admin().identityProviders().get(idpAlias).toRepresentation();
        assertThat("IdP should appear disabled when org is disabled", updatedIdp.isEnabled(), is(false));

        // Group membership assigned while the org was enabled is unaffected by the org being disabled
        groupMembers = orgResource.groups().group(groupId).getMembers(null, null, false);
        assertThat(groupMembers, hasSize(1));
    }
}
