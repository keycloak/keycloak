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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
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

        List<OrganizationRepresentation> existing = testRealm().organizations().getAll();
        assertFalse(existing.isEmpty());
        MatcherAssert.assertThat(expected, Matchers.containsInAnyOrder(existing.toArray()));
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
        assertEquals(org.getAttributes().get("attr1"), updated.getAttributes().get("attr1"));
        assertEquals(org.getAttributes().get("attr2"), updated.getAttributes().get("attr2"));

        attributes.clear();
        org.setAttributes(attributes);

        try (Response response = organization.update(org)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        updated = organization.toRepresentation();
        assertEquals(0, updated.getAttributes().size());
    }
}
