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

package org.keycloak.testsuite.organization.admin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.jpa.JpaOrganizationProviderFactory;
import org.keycloak.organization.jpa.OrganizationAdapter;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.runonserver.RunOnServer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OrganizationGroupTest extends AbstractOrganizationTest {

    @Test
    public void testManagingOrganizationGroupNotInOrganizationScope() {
        String id = createOrganization().getId();
        String memberId = addMember(testRealm().organizations().get(id)).getId();

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter organization = (OrganizationAdapter) provider.getById(id);
            RealmModel realm = session.getContext().getRealm();
            GroupModel orgGroup = session.groups().getGroupById(realm, organization.getGroupId());
            assertNotNull(orgGroup);

            try {
                orgGroup.setName("fail");
                fail("can not manage");
            } catch (ModelValidationException ignore) {
                try {
                    orgGroup.setName(organization.getId());
                } catch (ModelValidationException ignore2) {}
            }

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

            UserModel user = session.users().getUserByUsername(realm, "john-doh@localhost");
            assertNotNull(user);
            try {
                user.joinGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            UserModel member = session.users().getUserById(realm, memberId);
            assertNotNull(user);
            try {
                member.leaveGroup(orgGroup);
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }
        });
    }

    @Test
    public void testOrganizationGroupsNotAvailableFromGroupAPI() {
        Set<String> orgIds = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            orgIds.add(createOrganization("org-" + i).getId());
        }

        assertEquals(orgIds.size(), testRealm().organizations().list(-1, -1).size());
        assertTrue(testRealm().groups().groups().stream().map(GroupRepresentation::getId).noneMatch(orgIds::contains));
    }

    @Test
    public void testOrganizationGroupsNotAvailableFromUserAPI() {
        OrganizationRepresentation organization = createOrganization();
        UserRepresentation member = addMember(testRealm().organizations().get(organization.getId()));
        UserResource userResource = testRealm().users().get(member.getId());
        assertTrue(userResource.groups().isEmpty());
        assertEquals(0, userResource.groupsCount(null).get("count").intValue());
        assertEquals(0, userResource.groupsCount(organization.getId()).get("count").intValue());
    }

    @Test
    public void testDeleteGroupOnOrganizationRemoval() {
        String id = createOrganization().getId();

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class, JpaOrganizationProviderFactory.ID);
            OrganizationAdapter organization = (OrganizationAdapter) provider.getById(id);
            RealmModel realm = session.getContext().getRealm();
            GroupModel group = session.groups().getGroupById(realm, organization.getGroupId());
            assertNotNull(group);
            provider.remove(organization);
            group = session.groups().getGroupById(realm, organization.getId());
            assertNull(group);
        });
    }

    @Override
    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation rep = super.createRepresentation(name, orgDomains);
        rep.setAttributes(Map.of());
        return rep;
    }
}
