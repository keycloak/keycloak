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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationRepresentation expected = createOrganization();

        assertEquals(organizationName, expected.getName());
        expected.setName("acme");

        OrganizationResource organization = testRealm().organizations().get(expected.getId());

        try (Response response = organization.update(expected)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationRepresentation existing = organization.toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getName(), existing.getName());
        assertEquals(1, existing.getDomains().size());
    }

    @Test
    public void testGet() {
        OrganizationRepresentation expected = createOrganization();
        OrganizationRepresentation existing = testRealm().organizations().get(expected.getId()).toRepresentation();
        assertNotNull(existing);
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getName(), existing.getName());
    }

    @Test
    public void testGetAll() {
        List<OrganizationRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            expected.add(createOrganization("kc.org." + i));
        }

        List<OrganizationRepresentation> existing = testRealm().organizations().getAll(null);
        assertFalse(existing.isEmpty());
        assertThat(expected, containsInAnyOrder(existing.toArray()));
    }

    @Test
    public void testGetByDomain() {
        // create some organizations with a domain already set.
        for (int i = 0; i < 5; i++) {
            createOrganization("test-org-" + i, "testorg" + i + ".org");
        }

        // search for an organization with an existing domain.
        List<OrganizationRepresentation> existing = testRealm().organizations().getAll("testorg2.org");
        assertEquals(1, existing.size());
        OrganizationRepresentation orgRep = existing.get(0);
        assertEquals("test-org-2", orgRep.getName());
        assertEquals(1, orgRep.getDomains().size());
        OrganizationDomainRepresentation domainRep = orgRep.getDomains().iterator().next();
        assertEquals("testorg2.org", domainRep.getName());
        assertFalse(domainRep.isVerified());

        // search for an organization with an non-existent domain.
        existing = testRealm().organizations().getAll("someother.org");
        assertEquals(0, existing.size());
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
}
