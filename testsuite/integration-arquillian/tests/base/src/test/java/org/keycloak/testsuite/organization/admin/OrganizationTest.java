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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.RealmBuilder;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertEquals(expected.getAlias(), existing.getAlias());
        assertEquals(1, existing.getDomains().size());
        assertThat(existing.isEnabled(), is(false));
        assertThat(existing.getDescription(), notNullValue());
        assertThat(expected.getDescription(), is(equalTo(existing.getDescription())));
    }

    @Test
    public void testUpdateConflict() {
        OrganizationRepresentation org1 = createOrganization();
        OrganizationRepresentation org2 = createOrganization("orga");

        org1.setName(org2.getName());
        OrganizationResource organization = testRealm().organizations().get(org1.getId());

        try (Response response = organization.update(org1)) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
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

        for (int i = 0; i < 15; i++) {
            OrganizationRepresentation organization = createOrganization("kc.org." + i);
            expected.add(organization);
            organization.setAttributes(Map.of("foo", List.of("foo")));
            testRealm().organizations().get(organization.getId()).update(organization).close();
        }

        List<OrganizationRepresentation> existing = testRealm().organizations().list(-1, -1);
        assertFalse(existing.isEmpty());
        assertThat(existing, containsInAnyOrder(expected.toArray()));
        Assert.assertTrue(existing.stream().map(OrganizationRepresentation::getAttributes).filter(Objects::nonNull).findAny().isEmpty());

        List<OrganizationRepresentation> concatenatedList = Stream.of(
                testRealm().organizations().list(0, 5),
                testRealm().organizations().list(5, 5),
                testRealm().organizations().list(10, 5))
                .flatMap(Collection::stream).toList();

        assertThat(concatenatedList, containsInAnyOrder(expected.toArray()));
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
        long count = testRealm().organizations().count("wayne-industries", true);
        assertThat(existing, hasSize(1));
        assertThat(existing, hasSize((int) count));
        OrganizationRepresentation orgRep = existing.get(0);
        assertThat(orgRep.getName(), is(equalTo("wayne-industries")));
        assertThat(orgRep.isEnabled(), is(true));
        assertThat(orgRep.getDomains(), hasSize(2));
        assertThat(orgRep.getDomain("wayneind.com"), not(nullValue()));
        assertThat(orgRep.getDomain("wayneind-gotham.com"), not(nullValue()));
        assertThat(orgRep.getAttributes(), nullValue());

        existing = testRealm().organizations().search("gtbank.net", true, 0, 10);
        count = testRealm().organizations().count("gtbank.net", true);

        assertThat(existing, hasSize(1));
        assertThat(existing, hasSize((int) count));
        orgRep = existing.get(0);
        assertThat(orgRep.getName(), is(equalTo("Gotham-Bank")));
        assertThat(orgRep.isEnabled(), is(true));
        assertThat(orgRep.getDomains(), hasSize(2));
        assertThat(orgRep.getDomain("gtbank.com"), not(nullValue()));
        assertThat(orgRep.getDomain("gtbank.net"), not(nullValue()));
        assertThat(orgRep.getAttributes(), nullValue());

        orgRep.singleAttribute("foo", "bar");
        orgRep.singleAttribute("bar", "foo");
        testRealm().organizations().get(orgRep.getId()).update(orgRep).close();
        existing = testRealm().organizations().search("gtbank.net", true, 0, 10, false);
        assertThat(existing, hasSize(1));
        orgRep = existing.get(0);
        assertThat(orgRep.getAttributes(), notNullValue());
        assertThat(2, is(orgRep.getAttributes().size()));

        existing = testRealm().organizations().search("nonexistent.org", true, 0, 10);
        count = testRealm().organizations().count("nonexistent.org", true);
        assertThat(existing, is(empty()));
        assertThat(count, is(equalTo(0L)));

        // partial search matching name (e.g. 'wa' matching 'wayne-industries', and 'TheWave')
        existing = testRealm().organizations().search("wa", false, 0, 10);
        count = testRealm().organizations().count("wa", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        List<String> orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("wayne-industries", "TheWave"));

        // partial search matching domain (e.g. '.net', matching acme and gotham-bank)
        existing = testRealm().organizations().search(".net", false, 0, 10);
        count = testRealm().organizations().count(".net", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "acme"));

        // partial search matching both a domain and org name, on two different orgs (e.g. 'gotham' matching 'Gotham-Bank' by name and 'wayne-industries' by domain)
        existing = testRealm().organizations().search("gotham", false, 0, 10);
        count = testRealm().organizations().count("gotham", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "wayne-industries"));

        // partial search matching no org (e.g. nonexistent)
        existing = testRealm().organizations().search("nonexistent", false, 0, 10);
        count = testRealm().organizations().count("nonexistent", false);
        assertThat(existing, is(empty()));
        assertThat(existing, hasSize((int) count));

        // paginated search - create more orgs, try to fetch them all in paginated form.
        for (int i = 0; i < 10; i++) {
            createOrganization("ztest-" + i);
        }
        count = testRealm().organizations().count("", false);
        assertThat(count, equalTo(14L));

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
        long count = testRealm().organizations().countByAttribute("attr1:value1");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        assertThat(fetchedOrgs, hasSize(3));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(0).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(2).getName(), is(equalTo(expected.get(2).getName())));

        // search for "attr2:value2" - should match testorg.1 and testorg.3
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr2:value2");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        count = testRealm().organizations().countByAttribute("attr2:value2");
        assertThat(fetchedOrgs, hasSize(2));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(3).getName())));

        // search for "attr3:value3" - should match only testorg.2
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr3:value3");
        count = testRealm().organizations().countByAttribute("attr3:value3");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(2).getName())));

        // search for both "attr1:value1 attr2:value2" - should match only testorg.1
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr1:value1 attr2:value2");
        count = testRealm().organizations().countByAttribute("attr1:value1 attr2:value2");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));

        // search for both "attr2:value2 attr3:value3" - not org has both of these attributes at the same time.
        fetchedOrgs = testRealm().organizations().searchByAttribute("attr2:value2 attr3:value3");
        count = testRealm().organizations().countByAttribute("attr2:value2 attr3:value3");
        assertThat(fetchedOrgs, hasSize(0));
        assertThat(fetchedOrgs, hasSize((int) count));

        // search for "anything:anyvalue" - should again match no org because no org has this attribute.
        fetchedOrgs = testRealm().organizations().searchByAttribute("anything:anyvalue");
        count = testRealm().organizations().countByAttribute("anything:anyvalue");
        assertThat(fetchedOrgs, hasSize(0));
        assertThat(fetchedOrgs, hasSize((int) count));
    }

    @Test
    public void testCountEndpoint() {
        createOrganization("testorg.1");
        createOrganization("testorg.2");

        assertThat(testRealm().organizations().count(null), is(equalTo(2L)));
        assertThat(testRealm().organizations().count(".1"), is(equalTo(1L)));
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
        } catch (NotFoundException ignore) {
        }
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

        // create another org in the same realm and attempt to set the same internet domain during update - should not be possible.
        OrganizationRepresentation anotherOrg = createOrganization("another-org");
        anotherOrg.addDomain(expectedNewOrgDomain);
        organization = testRealm().organizations().get(anotherOrg.getId());
        try (Response response = organization.update(anotherOrg)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // create another org in a different realm with the same internet domain - should be allowed.
        createOrganization(adminClient.realm(bc.providerRealmName()), "testorg", "acme.com");

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
    public void testWithoutDomains() {
        // test create organization without any domains
        OrganizationRepresentation orgWithoutDomains = new OrganizationRepresentation();
        orgWithoutDomains.setName("no-domain-org");
        orgWithoutDomains.setAlias("no-domain-org");
        orgWithoutDomains.setDescription("Organization without domains");

        String orgWithoutDomainsId;
        try (Response response = testRealm().organizations().create(orgWithoutDomains)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgWithoutDomainsId = ApiUtil.getCreatedId(response);
        }

        OrganizationRepresentation created = testRealm().organizations().get(orgWithoutDomainsId).toRepresentation();
        assertEquals("no-domain-org", created.getName());
        assertEquals("no-domain-org", created.getAlias());
        assertThat(created.getDomains() == null || created.getDomains().isEmpty(), is(true));

        // verify that the organization can be retrieved
        OrganizationRepresentation orgWithDomains = createRepresentation("org-with-domains", "example.com");
        String orgWithDomainsId;
        try (Response response = testRealm().organizations().create(orgWithDomains)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgWithDomainsId = ApiUtil.getCreatedId(response);
        }

        try {
            List<OrganizationRepresentation> allOrgs = testRealm().organizations().list(-1, -1);
            assertThat(allOrgs.size(), greaterThanOrEqualTo(2));
            
            Optional<OrganizationRepresentation> foundOrgWithDomains = allOrgs.stream()
                    .filter(org -> org.getId().equals(orgWithDomainsId))
                    .findFirst();
            Optional<OrganizationRepresentation> foundOrgWithoutDomains = allOrgs.stream()
                    .filter(org -> org.getId().equals(orgWithoutDomainsId))
                    .findFirst();
            
            assertTrue("Organization with domains should be in the list", foundOrgWithDomains.isPresent());
            assertTrue("Organization without domains should be in the list", foundOrgWithoutDomains.isPresent());
            
            assertThat("Organization with domains should have domains", 
                    foundOrgWithDomains.get().getDomains(), is(notNullValue()));
            assertThat("Organization with domains should have at least one domain", 
                    foundOrgWithDomains.get().getDomains().size(), greaterThan(0));
            
            assertThat("Organization without domains should have no domains", 
                    foundOrgWithoutDomains.get().getDomains() == null || 
                    foundOrgWithoutDomains.get().getDomains().isEmpty(), is(true));

            List<OrganizationRepresentation> search = testRealm().organizations().search("with-domains", false, -1, -1);

            assertThat(search, hasSize(1));

            search = testRealm().organizations().search("no-domain", false, -1, -1);

            assertThat(search, hasSize(1));
        } finally {
            testRealm().organizations().get(orgWithDomainsId).delete().close();
            testRealm().organizations().get(orgWithoutDomainsId).delete().close();
        }
    }

    @Test
    public void testFilterEmptyDomain() {
        //org should be created with only one domain
        assertThat(createOrganization("singleValidDomainOrg", "validDomain.com", "", null).getDomains(), hasSize(1));
    }

    @Test
    public void testDisabledOrganizationProvider() throws IOException {
        OrganizationRepresentation existing = createOrganization("acme", "acme.org", "acme.net");
        // disable the organization provider and try to access REST endpoints
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationsEnabled(Boolean.FALSE)
                .update()) {
            OrganizationRepresentation org = createRepresentation("some", "some.com");

            try (Response response = testRealm().organizations().create(org)) {
                assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            }
            try {
                testRealm().organizations().list(-1, -1);
                fail("Expected NotFoundException");
            } catch (NotFoundException expected) {
            }
            try {
                testRealm().organizations().search("*");
                fail("Expected NotFoundException");
            } catch (NotFoundException expected) {
            }
            try {
                testRealm().organizations().get(existing.getId()).toRepresentation();
                fail("Expected NotFoundException");
            } catch (NotFoundException expected) {
            }
        }
    }

    @Test
    public void testDeleteRealm() {
        RealmRepresentation realmRep = RealmBuilder.create()
                .name(KeycloakModelUtils.generateId())
                .organizationEnabled(true)
                .build();
        RealmResource realmRes = realmsResouce().realm(realmRep.getRealm());

        try {
            realmRep.setEnabled(true);
            realmsResouce().create(realmRep);
            realmRes = realmsResouce().realm(realmRep.getRealm());
            realmRes.toRepresentation();

            createOrganization(realmRes, "test-org", "test.org");

            List<OrganizationRepresentation> orgs = realmRes.organizations().list(-1, -1);
            assertThat(orgs, hasSize(1));

            IdentityProviderRepresentation broker = bc.setUpIdentityProvider();
            broker.setAlias(KeycloakModelUtils.generateId());
            try (Response response = realmRes.identityProviders().create(broker)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
            try (Response response = realmRes.organizations().get(orgs.get(0).getId()).identityProviders().addIdentityProvider(broker.getAlias())) {
                assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
        } finally {
            realmRes.remove();
        }
    }

    @Test
    public void testCount() {
        List<String> orgIds = IntStream.range(0, 10)
                .mapToObj(i -> createOrganization("kc.org." + i).getId())
                .collect(Collectors.toList());

        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            assertEquals(10, orgProvider.count());

            OrganizationModel org = orgProvider.getById(orgIds.get(0));
            orgProvider.remove(org);

            assertEquals(9, orgProvider.count());
        });
    }

    @Test
    public void testFailUpdateAlias() {
        OrganizationRepresentation rep = createOrganization();

        rep.setAlias("changed");

        OrganizationsResource organizations = testRealm().organizations();
        OrganizationResource organization = organizations.get(rep.getId());

        try (Response response = organization.update(rep)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("Cannot change the alias", error.getErrorMessage());
        }

        rep.setAlias(rep.getName());

        try (Response response = organization.update(rep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testFailDuplicatedAlias() {
        OrganizationRepresentation rep = createOrganization();
        OrganizationsResource organizations = testRealm().organizations();

        rep.setId(null);
        rep.getDomains().clear();
        rep.addDomain(new OrganizationDomainRepresentation("acme-2"));
        rep.setName("acme-2");

        try (Response response = organizations.create(rep)) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("A organization with the same alias already exists", error.getErrorMessage());
        }
    }

    @Test
    public void testSpecialCharsAlias() {
        OrganizationRepresentation org = createRepresentation("acme");
        OrganizationDomainRepresentation orgDomain = new OrganizationDomainRepresentation();
        orgDomain.setName("acme.com");
        org.addDomain(orgDomain);

        org.setAlias("acme&@#!");
        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // blank alias will be replaced with org name, which is valid
        org.setAlias("");
        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            getCleanup().addCleanup(() -> testRealm().organizations().get(id).delete().close());
        }

        org.setAlias(" ");
        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        org.setAlias("\n");
        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // when alias is empty, name is used as alias
        org.setName("acme@");
        org.setAlias("");
        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testInvalidRedirectUri() {
        OrganizationRepresentation expected = createOrganization();
        expected.setRedirectUrl("http://valid.url:8080/");

        OrganizationResource organization = testRealm().organizations().get(expected.getId());

        try (Response response = organization.update(expected)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            assertThat(organization.toRepresentation().getRedirectUrl(), equalTo("http://valid.url:8080/"));
        }

        expected.setRedirectUrl("");
        try (Response response = organization.update(expected)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            assertThat(organization.toRepresentation().getRedirectUrl(), nullValue());
        }

        expected.setRedirectUrl(" ");
        try (Response response = organization.update(expected)) {
            assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
            assertThat(organization.toRepresentation().getRedirectUrl(), nullValue());
        }

        expected.setRedirectUrl("invalid");
        try (Response response = organization.update(expected)) {
            assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
            assertThat(organization.toRepresentation().getRedirectUrl(), nullValue());
        }

        expected.setRedirectUrl("https://\ninvalid");
        try (Response response = organization.update(expected)) {
            assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
            assertThat(organization.toRepresentation().getRedirectUrl(), nullValue());
        }
    }
}
