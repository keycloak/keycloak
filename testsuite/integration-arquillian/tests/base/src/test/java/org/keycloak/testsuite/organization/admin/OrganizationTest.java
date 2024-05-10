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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.group.GroupSearchTest.buildSearchQuery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.OrganizationModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationRepresentation expected = createOrganization();

        assertEquals(organizationName, expected.getName());
        expected.setName("acme");
        expected.setEnabled(false);
        expected.setDescription("ACME Corporation Organization");

        OrganizationResource organization = testRealm().organizations().get(expected.getId());

        try (Response response = organization.update(expected)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation existing = organization.toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getName(), existing.getName());
        assertEquals(1, existing.getDomains().size());
        assertThat(existing.isEnabled(), is(false));
        assertThat(existing.getDescription(), notNullValue());
        assertThat(expected.getDescription(), is(equalTo(existing.getDescription())));
    }

    @Test
    public void testGet() {
        OrganizationRepresentation expected = createOrganization();
        OrganizationRepresentation existing = testRealm().organizations().get(expected.getId()).toRepresentation();
        assertNotNull(existing);
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getName(), existing.getName());
        assertThat(expected.isEnabled(), is(true));
    }

    @Test
    public void testGetAll() {
        List<OrganizationRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            expected.add(createOrganization("kc.org." + i));
        }

        List<OrganizationRepresentation> existing = testRealm().organizations().getAll();
        assertFalse(existing.isEmpty());
        assertThat(expected, containsInAnyOrder(existing.toArray()));
    }

    @Test
    public void testSearch() {
        // create some organizations with different names and domains.
        createOrganization("acme", "acme.org", "acme.net");
        createOrganization("Gotham-Bank", "gtbank.com", "gtbank.net");
        createOrganization("wayne-industries", "wayneind.com", "wayneind-gotham.com");
        createOrganization("TheWave", "the-wave.br");

        // test exact search by name (e.g. 'wayne-industries'), e-mail (e.g. 'gtbank.net'), and no result (e.g. 'nonexistent.com')
        List<OrganizationRepresentation> existing = testRealm().organizations().search("wayne-industries", true, 0, 10);
        assertThat(existing, hasSize(1));
        OrganizationRepresentation orgRep = existing.get(0);
        assertThat(orgRep.getName(), is(equalTo("wayne-industries")));
        assertThat(orgRep.isEnabled(), is(true));
        assertThat(orgRep.getDomains(), hasSize(2));
        assertThat(orgRep.getDomain("wayneind.com"), not(nullValue()));
        assertThat(orgRep.getDomain("wayneind-gotham.com"), not(nullValue()));

        existing = testRealm().organizations().search("gtbank.net", true, 0, 10);
        assertThat(existing, hasSize(1));
        orgRep = existing.get(0);
        assertThat(orgRep.getName(), is(equalTo("Gotham-Bank")));
        assertThat(orgRep.isEnabled(), is(true));
        assertThat(orgRep.getDomains(), hasSize(2));
        assertThat(orgRep.getDomain("gtbank.com"), not(nullValue()));
        assertThat(orgRep.getDomain("gtbank.net"), not(nullValue()));

        existing = testRealm().organizations().search("nonexistent.org", true, 0, 10);
        assertThat(existing, is(empty()));

        // partial search matching name (e.g. 'wa' matching 'wayne-industries', and 'TheWave')
        existing = testRealm().organizations().search("wa", false, 0, 10);
        assertThat(existing, hasSize(2));
        List<String> orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("wayne-industries", "TheWave"));

        // partial search matching domain (e.g. '.net', matching acme and gotham-bank)
        existing = testRealm().organizations().search(".net", false, 0, 10);
        assertThat(existing, hasSize(2));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "acme"));

        // partial search matching both a domain and org name, on two different orgs (e.g. 'gotham' matching 'Gotham-Bank' by name and 'wayne-industries' by domain)
        existing = testRealm().organizations().search("gotham", false, 0, 10);
        assertThat(existing, hasSize(2));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "wayne-industries"));

        // partial search matching no org (e.g. nonexistent)
        existing = testRealm().organizations().search("nonexistent", false, 0, 10);
        assertThat(existing, is(empty()));

        // paginated search - create more orgs, try to fetch them all in paginated form.
        for (int i = 0; i < 10; i++) {
            createOrganization("ztest-" + i);
        }
        existing = testRealm().organizations().search("", false, 0, 10);
        // first page should have 10 results.
        assertThat(existing, hasSize(10));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "TheWave", "acme", "wayne-industries", "ztest-0",
                "ztest-1", "ztest-2", "ztest-3", "ztest-4", "ztest-5"));

        existing = testRealm().organizations().search("", false, 10, 10);
        // second page should have 4 results.
        assertThat(existing, hasSize(4));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("ztest-6", "ztest-7", "ztest-8", "ztest-9"));
    }

    @Test
    public void testSearchByAttributes() {
        List<OrganizationRepresentation> expected = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            expected.add(createOrganization("testorg." + i));
        }

        // set attributes to the orgs.
        OrganizationRepresentation orgRep = expected.get(0);
        orgRep.singleAttribute("attr1", "value1");
        try (Response response = testRealm().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(1);
        orgRep.singleAttribute("attr1", "value1").singleAttribute("attr2", "value2");
        try (Response response = testRealm().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(2);
        orgRep.singleAttribute("attr1", "value1").singleAttribute("attr3", "value3");
        try (Response response = testRealm().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(3);
        orgRep.singleAttribute("attr2", "value2");
        try (Response response = testRealm().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        // search for "attr1:value1" - should match testorg.0, testorg.1, and testorg.2
        List<OrganizationRepresentation> fetchedOrgs = testRealm().organizations().searchByAttribute("attr1:value1");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        assertThat(fetchedOrgs, hasSize(3));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(0).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(2).getName(), is(equalTo(expected.get(2).getName())));

        // search for "attr2:value2" - should match testorg.1 and testorg.3
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr2:value2");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        assertThat(fetchedOrgs, hasSize(2));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(3).getName())));

        // search for "attr3:value3" - should match only testorg.2
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr3:value3");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(2).getName())));

        // search for both "attr1:value1 attr2:value2" - should match only testorg.1
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr1:value1 attr2:value2");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));

        // search for both "attr2:value2 attr3:value3" - not org has both of these attributes at the same time.
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr2:value2 attr3:value3");
        assertThat(fetchedOrgs, hasSize(0));

        // search for "anything:anyvalue" - should again match no org because no org has this attribute.
        fetchedOrgs = testRealm().organizations().searchByAttribute("anything:anyvalue");
        assertThat(fetchedOrgs, hasSize(0));
    }

    @Test
    public void testDelete() {
        OrganizationRepresentation expected = createOrganization();
        OrganizationResource organization = testRealm().organizations().get(expected.getId());

        try (Response response = organization.delete()) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try {
            organization.toRepresentation();
            fail("should be deleted");
        } catch (NotFoundException ignore) {}
    }

    @Test
    public void testAttributes() {
        OrganizationRepresentation org = createOrganization();
        org = org.singleAttribute("key", "value");

        OrganizationResource organization = testRealm().organizations().get(org.getId());

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation updated = organization.toRepresentation();
        assertEquals(org.getAttributes().get("key"), updated.getAttributes().get("key"));

        HashMap<String, List<String>> attributes = new HashMap<>();
        attributes.put("attr1", List.of("val11", "val12"));
        attributes.put("attr2", List.of("val21", "val22"));
        org.setAttributes(attributes);

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        updated = organization.toRepresentation();
        assertNull(updated.getAttributes().get("key"));
        assertEquals(2, updated.getAttributes().size());
        assertThat(org.getAttributes().get("attr1"), containsInAnyOrder(updated.getAttributes().get("attr1").toArray()));
        assertThat(org.getAttributes().get("attr2"), containsInAnyOrder(updated.getAttributes().get("attr2").toArray()));

        attributes.clear();
        org.setAttributes(attributes);

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        updated = organization.toRepresentation();
        assertEquals(0, updated.getAttributes().size());

    }

    @Test
    public void testDomains() {
        // test create org with default domain settings
        OrganizationRepresentation expected = createOrganization();
        OrganizationDomainRepresentation expectedNewOrgDomain = expected.getDomains().iterator().next();
        OrganizationResource organization = testRealm().organizations().get(expected.getId());
        OrganizationRepresentation existing = organization.toRepresentation();
        assertEquals(1, existing.getDomains().size());
        OrganizationDomainRepresentation existingNewOrgDomain = existing.getDomain("neworg.org");
        assertEquals(expectedNewOrgDomain.getName(), existingNewOrgDomain.getName());
        assertFalse(existingNewOrgDomain.isVerified());

        // create a second domain with verified true
        OrganizationDomainRepresentation expectedNewOrgBrDomain = new OrganizationDomainRepresentation();
        expectedNewOrgBrDomain.setName("neworg.org.br");
        expectedNewOrgBrDomain.setVerified(true);
        expected.addDomain(expectedNewOrgBrDomain);
        try (Response response = organization.update(expected)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        existing = organization.toRepresentation();
        assertEquals(2, existing.getDomains().size());
        OrganizationDomainRepresentation existingNewOrgBrDomain = existing.getDomain("neworg.org.br");
        assertEquals(expectedNewOrgBrDomain.getName(), existingNewOrgBrDomain.getName());
        assertEquals(expectedNewOrgBrDomain.isVerified(), existingNewOrgBrDomain.isVerified());

        // now test updating an existing internet domain (change verified to false and check the model was updated).
        expectedNewOrgDomain.setVerified(true);
        try (Response response = organization.update(expected)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        existing = organization.toRepresentation();
        existingNewOrgDomain = existing.getDomain("neworg.org");
        assertEquals(expectedNewOrgDomain.isVerified(), existingNewOrgDomain.isVerified());
        existingNewOrgBrDomain = existing.getDomain("neworg.org.br");
        assertNotNull(existingNewOrgBrDomain);
        assertEquals(expectedNewOrgBrDomain.isVerified(), existingNewOrgBrDomain.isVerified());

        // now replace the internet domain for a different one.
        expectedNewOrgBrDomain.setName("acme.com");
        expectedNewOrgBrDomain.setVerified(false);
        try (Response response = organization.update(expected)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        existing = organization.toRepresentation();
        assertEquals(2, existing.getDomains().size());
        existingNewOrgBrDomain = existing.getDomain("acme.com");
        assertNotNull(existingNewOrgBrDomain);
        assertEquals(expectedNewOrgBrDomain.getName(), existingNewOrgBrDomain.getName());
        assertEquals(expectedNewOrgBrDomain.isVerified(), existingNewOrgBrDomain.isVerified());

        // attempt to set the internet domain to an invalid domain.
        expectedNewOrgBrDomain.setName("_invalid.domain.3com");
        try (Response response = organization.update(expected)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        expectedNewOrgBrDomain.setName("acme.com");

        // create another org and attempt to set the same internet domain during update - should not be possible.
        OrganizationRepresentation anotherOrg = createOrganization("another-org");
        anotherOrg.addDomain(expectedNewOrgDomain);
        organization = testRealm().organizations().get(anotherOrg.getId());
        try (Response response = organization.update(anotherOrg)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // try to remove a domain
        organization = testRealm().organizations().get(existing.getId());
        existing.removeDomain(existingNewOrgDomain);
        try (Response response = organization.update(existing)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        existing = organization.toRepresentation();
        assertFalse(existing.getDomains().isEmpty());
        assertEquals(1, existing.getDomains().size());
        assertNotNull(existing.getDomain("acme.com"));
    }

    @Test
    public void testManageOrgGroupsViaDifferentAPIs() {
        // test realm contains some groups initially
        List<GroupRepresentation> getAllBefore = testRealm().groups().groups();
        long countBefore = testRealm().groups().count().get("count");

        List<String> orgIds = new ArrayList<>();
        // create 5 organizations
        for (int i = 0; i < 5; i++) {
            OrganizationRepresentation expected = createOrganization("myorg" + i);
            OrganizationRepresentation existing = testRealm().organizations().get(expected.getId()).toRepresentation();
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
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success, the group could not be updated
        }

        try {
            // cannot update a group with the attribute reserved for organization related groups
            testRealm().groups().group(topGroup.getId()).update(orgGroupRep);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success, the group could not be updated
        }

        try {
            // cannot remove organization related group
            group.remove();
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success, the group could not be removed
        }

        try {
            // cannot manage organization related group permissions
            group.setPermissions(new ManagementPermissionRepresentation(true));
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success, the group's permissions cannot be managed
        }

        // try to add subgroup to an org related group
        try (Response response = group.subGroup(topGroup)) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // try to add org related group as a subgroup to a group
        try (Response response = testRealm().groups().group(topGroup.getId()).subGroup(orgGroupRep)) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().realmLevel().add(null);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().realmLevel().remove(null);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().clientLevel(testRealm().clients().findByClientId("test-app").get(0).getId()).add(null);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        try {
            // cannot manage organization related group role mappers
            group.roles().clientLevel(testRealm().clients().findByClientId("test-app").get(0).getId()).remove(null);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        // cannot add top level group with reserved attribute for organizations
        try (Response response = testRealm().groups().add(orgGroupRep)) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        try {
            // cannot add organization related group as a default group
            testRealm().addDefaultGroup(orgGroupRep.getId());
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        try {
            // cannot remove organization related group as a default group
            testRealm().removeDefaultGroup(orgGroupRep.getId());
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        OrganizationRepresentation org = createOrganization();
        UserRepresentation userRep = addMember(testRealm().organizations().get(org.getId()));

        try {
            // cannot join organization related group
            testRealm().users().get(userRep.getId()).joinGroup(orgGroupRep.getId());
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }

        try {
            // cannot leave organization related group
            testRealm().users().get(userRep.getId()).leaveGroup(orgGroupRep.getId());
            fail("Expected ForbiddenException");
        } catch (ForbiddenException ex) {
            // success
        }
    }
}
