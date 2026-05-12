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

package org.keycloak.tests.organization.admin;

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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "providerRealm", config = OrganizationRealmConfig.class)
    ManagedRealm secondRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginUsernamePage loginPage;

    @InjectOAuthClient
    OAuthClient oauth;

    @BeforeEach
    public void onBefore() {
        for (OrganizationRepresentation org : realm.admin().organizations().list(null, null)) {
            realm.admin().organizations().get(org.getId()).delete().close();
        }
    }

    @Test
    public void testUpdate() {
        OrganizationRepresentation expected = createOrganization();

        assertEquals(organizationName, expected.getName());
        expected.setName("acme");
        expected.setEnabled(false);
        expected.setDescription("ACME Corporation Organization");

        OrganizationResource organization = realm.admin().organizations().get(expected.getId());

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
        OrganizationResource organization = realm.admin().organizations().get(org1.getId());

        try (Response response = organization.update(org1)) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGet() {
        OrganizationRepresentation expected = createOrganization();
        OrganizationRepresentation existing = realm.admin().organizations().get(expected.getId()).toRepresentation();
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
            realm.admin().organizations().get(organization.getId()).update(organization).close();
        }

        List<OrganizationRepresentation> existing = realm.admin().organizations().list(-1, -1);
        assertFalse(existing.isEmpty());
        assertThat(existing, containsInAnyOrder(expected.toArray()));
        Assertions.assertTrue(existing.stream().map(OrganizationRepresentation::getAttributes).filter(Objects::nonNull).findAny().isEmpty());

        List<OrganizationRepresentation> concatenatedList = Stream.of(
                realm.admin().organizations().list(0, 5),
                realm.admin().organizations().list(5, 5),
                realm.admin().organizations().list(10, 5))
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
        List<OrganizationRepresentation> existing = realm.admin().organizations().search("wayne-industries", true, 0, 10);
        long count = realm.admin().organizations().count("wayne-industries", true);
        assertThat(existing, hasSize(1));
        assertThat(existing, hasSize((int) count));
        OrganizationRepresentation orgRep = existing.get(0);
        assertThat(orgRep.getName(), is(equalTo("wayne-industries")));
        assertThat(orgRep.isEnabled(), is(true));
        assertThat(orgRep.getDomains(), hasSize(2));
        assertThat(orgRep.getDomain("wayneind.com"), not(nullValue()));
        assertThat(orgRep.getDomain("wayneind-gotham.com"), not(nullValue()));
        assertThat(orgRep.getAttributes(), nullValue());

        existing = realm.admin().organizations().search("gtbank.net", true, 0, 10);
        count = realm.admin().organizations().count("gtbank.net", true);

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
        realm.admin().organizations().get(orgRep.getId()).update(orgRep).close();
        existing = realm.admin().organizations().search("gtbank.net", true, 0, 10, false);
        assertThat(existing, hasSize(1));
        orgRep = existing.get(0);
        assertThat(orgRep.getAttributes(), notNullValue());
        assertThat(2, is(orgRep.getAttributes().size()));

        existing = realm.admin().organizations().search("nonexistent.org", true, 0, 10);
        count = realm.admin().organizations().count("nonexistent.org", true);
        assertThat(existing, is(empty()));
        assertThat(count, is(equalTo(0L)));

        // partial search matching name (e.g. 'wa' matching 'wayne-industries', and 'TheWave')
        existing = realm.admin().organizations().search("wa", false, 0, 10);
        count = realm.admin().organizations().count("wa", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        List<String> orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("wayne-industries", "TheWave"));

        // partial search matching domain (e.g. '.net', matching acme and gotham-bank)
        existing = realm.admin().organizations().search(".net", false, 0, 10);
        count = realm.admin().organizations().count(".net", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "acme"));

        // partial search matching both a domain and org name, on two different orgs (e.g. 'gotham' matching 'Gotham-Bank' by name and 'wayne-industries' by domain)
        existing = realm.admin().organizations().search("gotham", false, 0, 10);
        count = realm.admin().organizations().count("gotham", false);
        assertThat(existing, hasSize(2));
        assertThat(existing, hasSize((int) count));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "wayne-industries"));

        // partial search matching no org (e.g. nonexistent)
        existing = realm.admin().organizations().search("nonexistent", false, 0, 10);
        count = realm.admin().organizations().count("nonexistent", false);
        assertThat(existing, is(empty()));
        assertThat(existing, hasSize((int) count));

        // paginated search - create more orgs, try to fetch them all in paginated form.
        for (int i = 0; i < 10; i++) {
            createOrganization("ztest-" + i);
        }
        count = realm.admin().organizations().count("", false);
        assertThat(count, equalTo(14L));

        existing = realm.admin().organizations().search("", false, 0, 10);
        // first page should have 10 results.
        assertThat(existing, hasSize(10));
        orgNames = existing.stream().map(OrganizationRepresentation::getName).collect(Collectors.toList());
        assertThat(orgNames, containsInAnyOrder("Gotham-Bank", "TheWave", "acme", "wayne-industries", "ztest-0",
                "ztest-1", "ztest-2", "ztest-3", "ztest-4", "ztest-5"));

        existing = realm.admin().organizations().search("", false, 10, 10);
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
        try (Response response = realm.admin().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(1);
        orgRep.singleAttribute("attr1", "value1").singleAttribute("attr2", "value2");
        try (Response response = realm.admin().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(2);
        orgRep.singleAttribute("attr1", "value1").singleAttribute("attr3", "value3");
        try (Response response = realm.admin().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        orgRep = expected.get(3);
        orgRep.singleAttribute("attr2", "value2");
        try (Response response = realm.admin().organizations().get(orgRep.getId()).update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        // search for "attr1:value1" - should match testorg.0, testorg.1, and testorg.2
        List<OrganizationRepresentation> fetchedOrgs = realm.admin().organizations().searchByAttribute("attr1:value1");
        long count = realm.admin().organizations().countByAttribute("attr1:value1");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        assertThat(fetchedOrgs, hasSize(3));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(0).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(2).getName(), is(equalTo(expected.get(2).getName())));

        // search for "attr2:value2" - should match testorg.1 and testorg.3
        fetchedOrgs = realm.admin().organizations().searchByAttribute("attr2:value2");
        fetchedOrgs.sort(Comparator.comparing(OrganizationRepresentation::getName));
        count = realm.admin().organizations().countByAttribute("attr2:value2");
        assertThat(fetchedOrgs, hasSize(2));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));
        assertThat(fetchedOrgs.get(1).getName(), is(equalTo(expected.get(3).getName())));

        // search for "attr3:value3" - should match only testorg.2
        fetchedOrgs = realm.admin().organizations().searchByAttribute("attr3:value3");
        count = realm.admin().organizations().countByAttribute("attr3:value3");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(2).getName())));

        // search for both "attr1:value1 attr2:value2" - should match only testorg.1
        fetchedOrgs = realm.admin().organizations().searchByAttribute("attr1:value1 attr2:value2");
        count = realm.admin().organizations().countByAttribute("attr1:value1 attr2:value2");
        assertThat(fetchedOrgs, hasSize(1));
        assertThat(fetchedOrgs, hasSize((int) count));
        assertThat(fetchedOrgs.get(0).getName(), is(equalTo(expected.get(1).getName())));

        // search for both "attr2:value2 attr3:value3" - not org has both of these attributes at the same time.
        fetchedOrgs = realm.admin().organizations().searchByAttribute("attr2:value2 attr3:value3");
        count = realm.admin().organizations().countByAttribute("attr2:value2 attr3:value3");
        assertThat(fetchedOrgs, hasSize(0));
        assertThat(fetchedOrgs, hasSize((int) count));

        // search for "anything:anyvalue" - should again match no org because no org has this attribute.
        fetchedOrgs = realm.admin().organizations().searchByAttribute("anything:anyvalue");
        count = realm.admin().organizations().countByAttribute("anything:anyvalue");
        assertThat(fetchedOrgs, hasSize(0));
        assertThat(fetchedOrgs, hasSize((int) count));
    }

    @Test
    public void testCountEndpoint() {
        createOrganization("testorg.1");
        createOrganization("testorg.2");

        assertThat(realm.admin().organizations().count(null), is(equalTo(2L)));
        assertThat(realm.admin().organizations().count(".1"), is(equalTo(1L)));
    }

    @Test
    public void testDelete() {
        OrganizationRepresentation expected = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(expected.getId());

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

        OrganizationResource organization = realm.admin().organizations().get(org.getId());

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
        OrganizationResource organization = realm.admin().organizations().get(expected.getId());
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
        organization = realm.admin().organizations().get(anotherOrg.getId());
        try (Response response = organization.update(anotherOrg)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // create another org in a different realm with the same internet domain - should be allowed.
        createOrganization(secondRealm.admin(), "testorg", "acme.com");

        // try to remove a domain
        organization = realm.admin().organizations().get(existing.getId());
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
    public void testDomainWithWildcardSubdomains() {
        // Create organization with a domain that has wildcard subdomain matching enabled
        OrganizationRepresentation org = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(org.getId());

        // Add a domain with wildcard subdomain matching using *.domain pattern
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        wildcardDomain.setVerified(true);
        org.addDomain(wildcardDomain);

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Verify the wildcard domain was saved correctly
        OrganizationRepresentation updated = organization.toRepresentation();
        assertEquals(2, updated.getDomains().size());

        OrganizationDomainRepresentation savedDomain = updated.getDomain("*.example.com");
        assertNotNull(savedDomain);
        assertTrue(savedDomain.isVerified());

        // Verify that the original domain without wildcard remains unchanged
        OrganizationDomainRepresentation defaultDomain = updated.getDomain("neworg.org");
        assertNotNull(defaultDomain);

        // Test changing to exact match (removing wildcard prefix)
        org.getDomains().remove(wildcardDomain);
        OrganizationDomainRepresentation exactDomain = new OrganizationDomainRepresentation();
        exactDomain.setName("example.com");
        exactDomain.setVerified(true);
        org.addDomain(exactDomain);

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        updated = organization.toRepresentation();
        assertNotNull(updated.getDomain("example.com"));
        assertNull(updated.getDomain("*.example.com"));

        // Re-add wildcard domain
        org.getDomains().remove(exactDomain);
        org.addDomain(wildcardDomain);
        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        updated = organization.toRepresentation();
        savedDomain = updated.getDomain("*.example.com");
        assertNotNull(savedDomain);
    }

    @Test
    public void testWithoutDomains() {
        // test create organization without any domains
        OrganizationRepresentation orgWithoutDomains = new OrganizationRepresentation();
        orgWithoutDomains.setName("no-domain-org");
        orgWithoutDomains.setAlias("no-domain-org");
        orgWithoutDomains.setDescription("Organization without domains");

        String orgWithoutDomainsId;
        try (Response response = realm.admin().organizations().create(orgWithoutDomains)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgWithoutDomainsId = ApiUtil.getCreatedId(response);
        }

        OrganizationRepresentation created = realm.admin().organizations().get(orgWithoutDomainsId).toRepresentation();
        assertEquals("no-domain-org", created.getName());
        assertEquals("no-domain-org", created.getAlias());
        assertThat(created.getDomains() == null || created.getDomains().isEmpty(), is(true));

        // verify that the organization can be retrieved
        OrganizationRepresentation orgWithDomains = createRepresentation("org-with-domains", "example.com");
        String orgWithDomainsId;
        try (Response response = realm.admin().organizations().create(orgWithDomains)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgWithDomainsId = ApiUtil.getCreatedId(response);
        }

        try {
            List<OrganizationRepresentation> allOrgs = realm.admin().organizations().list(-1, -1);
            assertThat(allOrgs.size(), greaterThanOrEqualTo(2));
            
            Optional<OrganizationRepresentation> foundOrgWithDomains = allOrgs.stream()
                    .filter(org -> org.getId().equals(orgWithDomainsId))
                    .findFirst();
            Optional<OrganizationRepresentation> foundOrgWithoutDomains = allOrgs.stream()
                    .filter(org -> org.getId().equals(orgWithoutDomainsId))
                    .findFirst();
            
            assertTrue(foundOrgWithDomains.isPresent(), "Organization with domains should be in the list");
            assertTrue(foundOrgWithoutDomains.isPresent(), "Organization without domains should be in the list");
            
            assertThat("Organization with domains should have domains", 
                    foundOrgWithDomains.get().getDomains(), is(notNullValue()));
            assertThat("Organization with domains should have at least one domain", 
                    foundOrgWithDomains.get().getDomains().size(), greaterThan(0));
            
            assertThat("Organization without domains should have no domains", 
                    foundOrgWithoutDomains.get().getDomains() == null || 
                    foundOrgWithoutDomains.get().getDomains().isEmpty(), is(true));

            List<OrganizationRepresentation> search = realm.admin().organizations().search("with-domains", false, -1, -1);

            assertThat(search, hasSize(1));

            search = realm.admin().organizations().search("no-domain", false, -1, -1);

            assertThat(search, hasSize(1));
        } finally {
            realm.admin().organizations().get(orgWithDomainsId).delete().close();
            realm.admin().organizations().get(orgWithoutDomainsId).delete().close();
        }
    }

    @Test
    public void testDisabledOrganizationProvider() {
        OrganizationRepresentation existing = createOrganization("acme", "acme.org", "acme.net");
        // disable the organization provider and try to access REST endpoints
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        OrganizationRepresentation org = createRepresentation("some", "some.com");

        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        try {
            realm.admin().organizations().list(-1, -1);
            fail("Expected NotFoundException");
        } catch (NotFoundException expected) {
        }
        try {
            realm.admin().organizations().search("*");
            fail("Expected NotFoundException");
        } catch (NotFoundException expected) {
        }
        try {
            realm.admin().organizations().get(existing.getId()).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException expected) {
        }
    }

    @Test
    public void testDeleteRealm() {
        RealmRepresentation realmRep = RealmBuilder.create()
                .name(KeycloakModelUtils.generateId())
                .organizationsEnabled(true)
                .build();
        RealmResource realmRes = adminClient.realms().realm(realmRep.getRealm());

        try {
            realmRep.setEnabled(true);
            adminClient.realms().create(realmRep);
            realmRes = adminClient.realms().realm(realmRep.getRealm());
            realmRes.toRepresentation();

            createOrganization(realmRes, "test-org", "test.org");

            List<OrganizationRepresentation> orgs = realmRes.organizations().list(-1, -1);
            assertThat(orgs, hasSize(1));

            IdentityProviderRepresentation broker = createOrgBroker(KeycloakModelUtils.generateId());
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

        String firstOrgId = orgIds.get(0);
        runOnServer.run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            assertEquals(10, orgProvider.count());

            OrganizationModel org = orgProvider.getById(firstOrgId);
            orgProvider.remove(org);

            assertEquals(9, orgProvider.count());
        });
    }

    @Test
    public void testFailUpdateAlias() {
        OrganizationRepresentation rep = createOrganization();

        rep.setAlias("changed");

        OrganizationsResource organizations = realm.admin().organizations();
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
        OrganizationsResource organizations = realm.admin().organizations();

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
        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // blank alias will be replaced with org name, which is valid
        org.setAlias("");
        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.organizations().get(id).delete().close());
        }

        org.setAlias(" ");
        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        org.setAlias("\n");
        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // when alias is empty, name is used as alias
        org.setName("acme@");
        org.setAlias("");
        try (Response response = realm.admin().organizations().create(org)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testInvalidRedirectUri() {
        OrganizationRepresentation expected = createOrganization();
        expected.setRedirectUrl("http://valid.url:8080/");

        OrganizationResource organization = realm.admin().organizations().get(expected.getId());

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

    @Test
    public void testDomainConflictExactDuplicate() {
        // Org A owns "example.com"; Org B must not be able to claim the same domain.
        createOrganization("org-a", "example.com");

        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationDomainRepresentation duplicateDomain = new OrganizationDomainRepresentation();
        duplicateDomain.setName("example.com");
        orgB.addDomain(duplicateDomain);

        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgC = createRepresentation("org-c", "example.com");
        try (Response response = realm.admin().organizations().create(orgC)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDomainAllowExactAndWildcardDomainWithSameBaseDomain() {
        createOrganization("org-a", "example.com");

        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgB.addDomain(wildcardDomain);

        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAllowSubDomainsAndWildcardDomainInSeparateOrgs() {
        OrganizationRepresentation orgA = createOrganization("org-a", "acme.org");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgA.addDomain(wildcardDomain);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationDomainRepresentation subDomain = new OrganizationDomainRepresentation();
        subDomain.setName("sub.example.com");
        orgB.addDomain(subDomain);

        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        orgB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        OrganizationDomainRepresentation deepSubDomain = new OrganizationDomainRepresentation();
        deepSubDomain.setName("deep.sub.example.com");
        orgB.addDomain(deepSubDomain);

        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgC = createOrganization("org-c");
        subDomain = new OrganizationDomainRepresentation();
        subDomain.setName("*.deep.sub.example.com");
        orgC.addDomain(subDomain);

        try (Response response = realm.admin().organizations().get(orgC.getId()).update(orgC)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgD = createOrganization("org-d");
        subDomain = new OrganizationDomainRepresentation();
        subDomain.setName("some.deep.sub.example.com");
        orgD.addDomain(subDomain);

        try (Response response = realm.admin().organizations().get(orgD.getId()).update(orgD)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAddingOrgWithWildCardDomainWhenExactDomainOrgExist() {
        createOrganization("org-a", "sub.example.com");
        createOrganization("org-b", "test.sub.example.com");

        OrganizationRepresentation orgC = createOrganization("org-c");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgC.addDomain(wildcardDomain);

        try (Response response = realm.admin().organizations().get(orgC.getId()).update(orgC)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgD = createOrganization("org-d");
        orgD.addDomain(new OrganizationDomainRepresentation("*.sub.example.com"));
        try (Response response = realm.admin().organizations().get(orgD.getId()).update(orgD)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgE = createOrganization("org-e");
        orgE.addDomain(new OrganizationDomainRepresentation("sub.example.com"));
        try (Response response = realm.admin().organizations().get(orgE.getId()).update(orgE)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDomainConflictDuplicateWildcard() {
        // Org A owns "*.example.com". Org B must not also claim "*.example.com".
        OrganizationRepresentation orgA = createOrganization("org-a", "acme.org");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgA.addDomain(wildcardDomain);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationDomainRepresentation duplicateWildcard = new OrganizationDomainRepresentation();
        duplicateWildcard.setName("*.example.com");
        orgB.addDomain(duplicateWildcard);

        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDomainNoConflictDifferentBaseDomains() {
        // Org A owns "*.example.com" and Org B owns "*.acme.com" — no overlap, both should succeed.
        OrganizationRepresentation orgA = createOrganization("org-a", "other-a.org");
        OrganizationDomainRepresentation wildcardA = new OrganizationDomainRepresentation();
        wildcardA.setName("*.example.com");
        orgA.addDomain(wildcardA);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation orgB = createOrganization("org-b", "other-b.org");
        OrganizationDomainRepresentation wildcardB = new OrganizationDomainRepresentation();
        wildcardB.setName("*.acme.com");
        orgB.addDomain(wildcardB);
        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation updatedA = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        assertNotNull(updatedA.getDomain("*.example.com"));

        OrganizationRepresentation updatedB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        assertNotNull(updatedB.getDomain("*.acme.com"));
    }

    @Test
    public void testDomainNoConflictUnrelatedExactDomains() {
        // Org A owns "example.com", Org B owns "acme.com" — no conflict.
        OrganizationRepresentation orgA = createOrganization("org-a", "example.com");
        OrganizationRepresentation orgB = createOrganization("org-b", "acme.com");

        OrganizationRepresentation updatedA = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        assertNotNull(updatedA.getDomain("example.com"));

        OrganizationRepresentation updatedB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        assertNotNull(updatedB.getDomain("acme.com"));
    }

    @Test
    public void testDomainConflictWildcardOnCreate() {
        // Org A owns "*.example.com". Creating Org B with "sub.example.com" must fail.
        OrganizationRepresentation orgA = createOrganization("org-a", "acme.org");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgA.addDomain(wildcardDomain);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Creating org with subdomain of the wildcard should fail
        OrganizationRepresentation orgB = createRepresentation("org-b", "sub.example.com");
        try (Response response = realm.admin().organizations().create(orgB)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Creating org with the exact base domain should also fail
        OrganizationRepresentation orgC = createRepresentation("org-c", "example.com");
        try (Response response = realm.admin().organizations().create(orgC)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Creating org with the same wildcard should fail
        OrganizationRepresentation orgD = createRepresentation("org-d", "*.example.com");
        try (Response response = realm.admin().organizations().create(orgD)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testResolveOrganizationByDomain() {
        OrganizationRepresentation orgA = createOrganization("org-a", "sub.example.com");
        OrganizationResource organization = realm.admin().organizations().get(orgA.getId());
        String brokerAliasA = orgA.getAlias() + "-identity-provider";
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(brokerAliasA);
        IdentityProviderRepresentation brokerRepOrgA = broker.toRepresentation();
        brokerRepOrgA.setHideOnLogin(false);
        brokerRepOrgA.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        realm.admin().identityProviders().get(brokerRepOrgA.getAlias()).update(brokerRepOrgA);

        oauth.openLoginForm();
        loginPage.fillLoginWithUsernameOnly("user@sub.example.com");
        loginPage.submit();
        assertTrue(loginPage.isSocialButtonPresent(brokerRepOrgA.getAlias()));

        OrganizationRepresentation orgB = createOrganization("org-b", "example.com");
        organization = realm.admin().organizations().get(orgB.getId());
        String brokerAliasB = orgB.getAlias() + "-identity-provider";
        broker = organization.identityProviders().get(brokerAliasB);
        IdentityProviderRepresentation brokerRepOrgB = broker.toRepresentation();
        brokerRepOrgB.setHideOnLogin(false);
        brokerRepOrgB.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        realm.admin().identityProviders().get(brokerRepOrgB.getAlias()).update(brokerRepOrgB);
        oauth.openLoginForm();
        loginPage.fillLoginWithUsernameOnly("user@example.com");
        loginPage.submit();
        assertTrue(loginPage.isSocialButtonPresent(brokerRepOrgB.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(brokerRepOrgA.getAlias()));

        OrganizationRepresentation orgC = createOrganization("org-c", "*.deep.sub.example.com");
        organization = realm.admin().organizations().get(orgC.getId());
        String brokerAliasC = orgC.getAlias() + "-identity-provider";
        broker = organization.identityProviders().get(brokerAliasC);
        IdentityProviderRepresentation brokerRepOrgC = broker.toRepresentation();
        brokerRepOrgC.setHideOnLogin(false);
        brokerRepOrgC.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        realm.admin().identityProviders().get(brokerRepOrgC.getAlias()).update(brokerRepOrgC);
        oauth.openLoginForm();
        loginPage.fillLoginWithUsernameOnly("user@deep.sub.example.com");
        loginPage.submit();
        assertTrue(loginPage.isSocialButtonPresent(brokerRepOrgC.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(brokerRepOrgA.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(brokerRepOrgB.getAlias()));
    }

    @Test
    public void testDomainConflictAfterDeletingOrganization() {
        OrganizationRepresentation orgA = createOrganization("org-a", "acme.org");
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgA.addDomain(wildcardDomain);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Org B cannot claim "example.com" while Org A exists
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationDomainRepresentation exactDomain = new OrganizationDomainRepresentation();
        exactDomain.setName("example.com");
        orgB.addDomain(exactDomain);
        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Delete Org A
        try (Response response = realm.admin().organizations().get(orgA.getId()).delete()) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Now Org B should be able to claim "example.com"
        orgB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        exactDomain = new OrganizationDomainRepresentation();
        exactDomain.setName("example.com");
        orgB.addDomain(exactDomain);
        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation updatedB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        assertNotNull(updatedB.getDomain("example.com"));
    }

    @Test
    public void testDomainConflictAfterRemovingDomain() {
        // Org A owns "example.com". After removing that domain, Org B should be able to claim it.
        OrganizationRepresentation orgA = createOrganization("org-a", "example.com");
        OrganizationRepresentation orgB = createOrganization("org-b");

        // Org B cannot claim "example.com" while Org A has it
        OrganizationDomainRepresentation exactDomain = new OrganizationDomainRepresentation();
        exactDomain.setName("example.com");
        orgB.addDomain(exactDomain);
        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // Remove "example.com" from Org A
        orgA = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        OrganizationDomainRepresentation domainToRemove = orgA.getDomain("example.com");
        assertNotNull(domainToRemove);
        orgA.removeDomain(domainToRemove);
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Now Org B should be able to claim "example.com"
        orgB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        exactDomain = new OrganizationDomainRepresentation();
        exactDomain.setName("example.com");
        orgB.addDomain(exactDomain);
        try (Response response = realm.admin().organizations().get(orgB.getId()).update(orgB)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation updatedB = realm.admin().organizations().get(orgB.getId()).toRepresentation();
        assertNotNull(updatedB.getDomain("example.com"));
    }

    @Test
    public void testDomainSameOrgCanUpdateOwnDomains() {
        // An organization should be able to update its own domains without self-conflict.
        OrganizationRepresentation orgA = createOrganization("org-a", "example.com");
        OrganizationResource organization = realm.admin().organizations().get(orgA.getId());

        // Re-submit the same domains — should succeed (no self-conflict)
        orgA = organization.toRepresentation();
        try (Response response = organization.update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Add a wildcard domain to the same org — should succeed
        OrganizationDomainRepresentation wildcardDomain = new OrganizationDomainRepresentation();
        wildcardDomain.setName("*.example.com");
        orgA.addDomain(wildcardDomain);
        try (Response response = organization.update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation updated = organization.toRepresentation();
        assertNotNull(updated.getDomain("example.com"));
        assertNotNull(updated.getDomain("*.example.com"));
    }

    @Test
    public void testRejectDomainWithTooManyParts() {
        // 11-part domain (10 dots) must be rejected by Organizations.validateDomainParts.
        String tooLong = "a.b.c.d.e.f.g.h.i.j.com";
        OrganizationRepresentation orgA = createOrganization("org-a");
        orgA.addDomain(new OrganizationDomainRepresentation(tooLong));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // the wildcard form of the same over-long domain must also be rejected
        orgA = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        orgA.addDomain(new OrganizationDomainRepresentation("*." + tooLong));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // a 10-part domain (9 dots) is still allowed
        String atLimit = "*.a.b.c.d.e.f.g.h.i.jay";
        orgA = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        orgA.addDomain(new OrganizationDomainRepresentation(atLimit));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(orgA)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testRejectSinglePartDomains() {
        OrganizationRepresentation orgA = createOrganization("org-a");

        // a wildcard over a bare TLD must also be rejected
        for (String wildcardTld : List.of("*.com", "*.org", "*.net", "*.io")) {
            OrganizationRepresentation attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
            attempt.addDomain(new OrganizationDomainRepresentation(wildcardTld));
            try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
                assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            }
        }

        // the minimum valid values (2-part base) must be accepted
        for (String valid : List.of("example.com", "*.example.com")) {
            OrganizationRepresentation attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
            attempt.addDomain(new OrganizationDomainRepresentation(valid));
            try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
                assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
        }
    }

    @Test
    public void testRejectInvalidWildcardPatterns() {
        OrganizationRepresentation orgA = createOrganization("org-a", "example.com");

        // bare "*." (no base domain) is rejected
        OrganizationRepresentation attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation("*."));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // wildcard in the middle of the pattern is rejected
        attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation("sub.*.example.com"));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // multiple wildcards are rejected
        attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation("*.*.example.com"));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // an empty domain name is rejected
        attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation(""));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // a syntactically invalid base domain is rejected
        attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation("*.not valid"));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // a wildcard over a bare TLD is rejected (base domain has only 1 part)
        attempt = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        attempt.addDomain(new OrganizationDomainRepresentation("*.com"));
        try (Response response = realm.admin().organizations().get(orgA.getId()).update(attempt)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // sanity: none of the rejected values were persisted
        OrganizationRepresentation reloaded = realm.admin().organizations().get(orgA.getId()).toRepresentation();
        assertEquals(1, reloaded.getDomains().size());
        assertNotNull(reloaded.getDomain("example.com"));
    }
}
