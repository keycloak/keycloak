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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.group.GroupSearchTest.buildSearchQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.runonserver.RunOnServer;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationGroupTest extends AbstractOrganizationTest {

    @Test
    public void testManageOrgGroupsViaDifferentAPIs() {
        // test realm contains some groups initially
        List<GroupRepresentation> getAllBefore = testRealm().groups().groups();
        long countBefore = testRealm().groups().count().get("count");

        List<String> orgIds = new ArrayList<>();
        // create 5 organizations
        for (int i = 0; i < 5; i++) {
            OrganizationRepresentation expected = createOrganization("myorg" + i);
            OrganizationResource organization = testRealm().organizations().get(expected.getId());
            expected.setAttributes(Map.of());
            organization.update(expected).close();
            OrganizationRepresentation existing = organization.toRepresentation();
            orgIds.add(expected.getId());
            assertNotNull(existing);
        }

        // create one top-level group and one subgroup
        GroupRepresentation topGroup = createGroup(testRealm(), "top");
        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2");
        testRealm().groups().group(topGroup.getId()).subGroup(level2Group);

        // check that count queries include org related groups
        assertEquals(countBefore + 7, (long) testRealm().groups().count().get("count"));

        // check that search queries include org related groups but those can't be updated
        assertEquals(getAllBefore.size() + 6, testRealm().groups().groups().size());
        // we need to pull full representation of the group, otherwise org related attributes are lost in the representation
        List<GroupRepresentation> groups = testRealm().groups().query(buildSearchQuery(OrganizationModel.ORGANIZATION_ATTRIBUTE, orgIds.get(0)), false, 0, 10, false);
        assertEquals(1, groups.size());
        GroupRepresentation orgGroupRep = groups.get(0);
        GroupResource group = testRealm().groups().group(orgGroupRep.getId());

        try {
            // group to be updated is organization related group
            group.update(topGroup);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success, the group could not be updated
        }

        try {
            // cannot update a group with the attribute reserved for organization related groups
            testRealm().groups().group(topGroup.getId()).update(orgGroupRep);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success, the group could not be updated
        }

        try {
            // cannot remove organization related group
            group.remove();
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success, the group could not be removed
        }

        try {
            // cannot manage organization related group permissions
            group.setPermissions(new ManagementPermissionRepresentation(true));
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success, the group's permissions cannot be managed
        }

        // try to add subgroup to an org related group
        try (Response response = group.subGroup(topGroup)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // try to add org related group as a subgroup to a group
        try (Response response = testRealm().groups().group(topGroup.getId()).subGroup(orgGroupRep)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().realmLevel().add(null);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().realmLevel().remove(null);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().clientLevel(testRealm().clients().findByClientId("test-app").get(0).getId()).add(null);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().clientLevel(testRealm().clients().findByClientId("test-app").get(0).getId()).remove(null);
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }

        // cannot add top level group with reserved attribute for organizations
        String id = orgGroupRep.getId();
        String name = orgGroupRep.getName();
        orgGroupRep.setId(null);
        orgGroupRep.setName(null);
        try (Response response = testRealm().groups().add(orgGroupRep)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } finally {
            orgGroupRep.setId(id);
            orgGroupRep.setName(name);
        }

        try {
            // cannot add organization related group as a default group
            testRealm().addDefaultGroup(orgGroupRep.getId());
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }

        OrganizationRepresentation org = createOrganization();
        UserRepresentation userRep = addMember(testRealm().organizations().get(org.getId()));

        try {
            // cannot join organization related group
            testRealm().users().get(userRep.getId()).joinGroup(orgGroupRep.getId());
            fail("Expected BadRequestException");
        } catch (BadRequestException ex) {
            // success
        }
    }

    @Test
    public void testManagingOrganizationGroupNotInOrganizationScope() {
        String id = createOrganization().getId();
        String memberId = addMember(testRealm().organizations().get(id)).getId();

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = provider.getById(id);
            RealmModel realm = session.getContext().getRealm();
            GroupModel orgGroup = session.groups().getGroupByName(realm, null, organization.getId());

            try {
                orgGroup.setName("fail");
                fail("can not manage");
            } catch (ModelValidationException ignore) {
            }

            try {
                orgGroup.setSingleAttribute(OrganizationModel.ORGANIZATION_ATTRIBUTE, "fail");
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

    @Override
    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation rep = super.createRepresentation(name, orgDomains);
        rep.setAttributes(Map.of());
        return rep;
    }
}
