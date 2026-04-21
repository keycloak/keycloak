/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.group;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.jpa.JpaOrganizationProviderFactory;
import org.keycloak.organization.jpa.OrganizationAdapter;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationInternalGroupTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectUser(config = NonOrgUserConfig.class)
    ManagedUser nonOrgUser;

    @Test
    public void testManagingOrgInternalGroupNotInOrganizationScope() {
        String id = createOrganization().getId();
        String memberId = addMember(realm.admin().organizations().get(id)).getId();

        String nonOrgUserId = nonOrgUser.getId();
        String realmName = realm.getName();
        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter organization = (OrganizationAdapter) provider.getById(id);
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel orgGroup = session.groups().getGroupById(realm, organization.getGroupId());
            assertNotNull(orgGroup);

            try {
                orgGroup.setSingleAttribute(OrganizationModel.ORGANIZATION_ATTRIBUTE, "fail");
                fail("can not manage");
            } catch (ModelValidationException ignore) {
                try {
                    orgGroup.setSingleAttribute(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());
                } catch (ModelValidationException ignore2) {}
            }

            try {
                orgGroup.setSingleAttribute("something", "fail");
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            GroupModel child = realm.createGroup("child");

            try {
                orgGroup.addChild(child);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            GroupModel parent = realm.createGroup("parent");

            try {
                realm.moveGroup(orgGroup, parent);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            try {
                realm.removeGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            try {
                realm.addDefaultGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            UserModel user = session.users().getUserById(realm, nonOrgUserId);
            assertNotNull(user);
            try {
                user.joinGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            UserModel member = session.users().getUserById(realm, memberId);
            assertNotNull(member);
            try {
                member.leaveGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }
        });
    }

    @Test
    public void testOrgInternalGroupNotAvailableFromGroupAPI() {
        Set<String> orgIds = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            orgIds.add(createOrganization("org-" + i).getId());
        }

        assertEquals(orgIds.size(), realm.admin().organizations().list(-1, -1).size());
        assertTrue(realm.admin().groups().groups().stream().map(GroupRepresentation::getId).noneMatch(orgIds::contains));
    }

    @Test
    public void testOrgInternalGroupNotAvailableFromUserAPI() {
        OrganizationRepresentation organization = createOrganization();
        UserRepresentation member = addMember(realm.admin().organizations().get(organization.getId()));
        UserResource userResource = realm.admin().users().get(member.getId());
        assertTrue(userResource.groups().isEmpty());
        assertEquals(0, userResource.groupsCount(null).get("count").intValue());
        assertEquals(0, userResource.groupsCount(organization.getId()).get("count").intValue());
    }

    @Test
    public void testDeleteInternalGroupOnOrganizationRemoval() {
        String id = createOrganization().getId();

        String realmName = realm.getName();
        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter organization = (OrganizationAdapter) provider.getById(id);
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel group = session.groups().getGroupById(realm, organization.getGroupId());
            assertNotNull(group);
            provider.remove(organization);
            group = session.groups().getGroupById(realm, organization.getId());
            assertNull(group);
        });
    }

    @Test
    public void testCannotRemoveInternalOrgMembershipViaUserAPI() {
        String orgId = createOrganization().getId();
        String memberId = addMember(realm.admin().organizations().get(orgId)).getId();

        assertThat(memberId, notNullValue());

        String realmName = realm.getName();
        String groupId = runOnServer.fetch(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter org = (OrganizationAdapter) provider.getById(orgId);
            return org.getGroupId();
        }, String.class);

        assertThat(groupId, notNullValue());

        // Try to remove user from organization group via User API - should fail with BAD_REQUEST
        try {
            realm.admin().users().get(memberId).leaveGroup(groupId);
            fail("Should not be able to remove user from organization group via User API");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Bad Request"));
        }
    }

    @Test
    public void testCannotGetInternalGroupViaRealmAPI() {
        String orgId = createOrganization().getId();

        // Try to get organization group by path via Realm API - should fail with NOT_FOUND (the search is limited to REALM groups)
        // internal group's name equals to orgId
        try {
            realm.admin().getGroupByPath("/" + orgId);
            fail("Should not be able to get organization group via Realm API getGroupByPath");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Not Found"));
        }
    }

    @Test
    public void testCannotMoveInternalGroupViaGroupAPI() {
        String orgId = createOrganization().getId();
        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("realm_group");
        String realmGroupId;
        try (Response response = realm.admin().groups().add(realmGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realmGroupId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation groupRep = runOnServer.fetch(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter org = (OrganizationAdapter) provider.getById(orgId);
            String groupId = org.getGroupId();
            RealmModel realm = session.getContext().getRealm();
            GroupModel group = session.groups().getGroupById(realm, groupId);
            return ModelToRepresentation.groupToBriefRepresentation(group);
        }, GroupRepresentation.class);

        groupRep.setParentId(realmGroupId);

        // Try to add organization group as top-level group via Groups API - should fail with BAD_REQUEST
        try (Response response = realm.admin().groups().add(groupRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Override
    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation rep = super.createRepresentation(name, orgDomains);
        rep.setAttributes(Map.of());
        return rep;
    }

    static class NonOrgUserConfig implements UserConfig {
        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("non-org-user").email("non-org-user@example.com");
        }
    }
}
