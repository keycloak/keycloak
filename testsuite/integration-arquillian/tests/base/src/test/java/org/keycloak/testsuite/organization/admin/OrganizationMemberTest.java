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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.models.OrganizationModel.ORGANIZATION_ATTRIBUTE;

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationMemberTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);

        expected.setFirstName("f");
        expected.setLastName("l");
        expected.setEmail("some@differentthanorg.com");

        testRealm().users().get(expected.getId()).update(expected);

        UserRepresentation existing = organization.members().member(expected.getId()).toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getUsername(), existing.getUsername());
        assertEquals(expected.getEmail(), existing.getEmail());
        assertEquals(expected.getFirstName(), existing.getFirstName());
        assertEquals(expected.getLastName(), existing.getLastName());
    }

    @Test
    public void testFailSetUserOrganizationAttribute() {
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        testRealm().users().userProfile().update(upConfig);
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        List<String> expectedOrganizations = expected.getAttributes().get(ORGANIZATION_ATTRIBUTE);

        expected.singleAttribute(ORGANIZATION_ATTRIBUTE, "invalid");

        UserResource userResource = testRealm().users().get(expected.getId());

        try {
            userResource.update(expected);
            Assert.fail("The attribute is readonly");
        } catch (BadRequestException bre) {
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals(ORGANIZATION_ATTRIBUTE, error.getField());
            assertEquals("error-user-attribute-read-only", error.getErrorMessage());
        }

        // the attribute is readonly, removing it from the rep does not make any difference
        expected.getAttributes().remove(ORGANIZATION_ATTRIBUTE);
        userResource.update(expected);
        expected = userResource.toRepresentation();
        assertThat(expected.getAttributes().get(ORGANIZATION_ATTRIBUTE), Matchers.containsInAnyOrder(expectedOrganizations.toArray()));

        userResource.update(expected);
        expected = userResource.toRepresentation();
        assertThat(expected.getAttributes().get(ORGANIZATION_ATTRIBUTE), Matchers.containsInAnyOrder(expectedOrganizations.toArray()));
    }

    @Test
    public void testUserAlreadyMemberOfOrganization() {
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        testRealm().users().userProfile().update(upConfig);
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization, KeycloakModelUtils.generateId() + "@user.org");

        try (Response response = organization.members().addMember(expected.getId())) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGet() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        UserRepresentation existing = organization.members().member(expected.getId()).toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getUsername(), existing.getUsername());
        assertEquals(expected.getEmail(), existing.getEmail());
    }

    @Test
    public void testGetMemberOrganization() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization);
        OrganizationRepresentation expected = organization.toRepresentation();
        OrganizationRepresentation actual = organization.members().getOrganization(member.getId());
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void testGetAll() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            expected.add(addMember(organization, "member-" + i + "@neworg.org"));
        }

        List<UserRepresentation> existing = organization.members().getAll();
        assertFalse(existing.isEmpty());
        assertEquals(expected.size(), existing.size());
        for (UserRepresentation expectedRep : expected) {
            UserRepresentation existingRep = existing.stream().filter(member -> member.getId().equals(expectedRep.getId())).findAny().orElse(null);
            assertNotNull(existingRep);
            assertEquals(expectedRep.getId(), existingRep.getId());
            assertEquals(expectedRep.getUsername(), existingRep.getUsername());
            assertEquals(expectedRep.getEmail(), existingRep.getEmail());
            assertEquals(expectedRep.getFirstName(), existingRep.getFirstName());
            assertEquals(expectedRep.getLastName(), existingRep.getLastName());
        }
    }

    @Test
    public void testDelete() {
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        assertNotNull(expected.getAttributes());
        assertTrue(expected.getAttributes().get(ORGANIZATION_ATTRIBUTE).contains(organization.toRepresentation().getId()));
        OrganizationMemberResource member = organization.members().member(expected.getId());

        try (Response response = member.delete()) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // user should exist but no longer an organization member
        expected = testRealm().users().get(expected.getId()).toRepresentation();
        assertNull(expected.getAttributes());
        try {
            member.toRepresentation();
            fail("should not be an organization member");
        } catch (NotFoundException ignore) {

        }
    }

    @Test
    public void testDeleteMembersOnOrganizationRemoval() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            expected.add(addMember(organization, "member-" + i + "@neworg.org"));
        }

        organization.delete().close();

        for (UserRepresentation member : expected) {
            try {
                organization.members().member(member.getId()).toRepresentation();
                fail("should be deleted");
            } catch (NotFoundException ignore) {}
        }

        for (UserRepresentation member : expected) {
            // users should exist as they are not managed by the organization
            testRealm().users().get(member.getId()).toRepresentation();
        }

        for (UserRepresentation member : expected) {
            try {
                // user no longer bound to the organization
                organization.members().getOrganization(member.getId());
                fail("should not be associated with the organization anymore");
            } catch (NotFoundException ignore) {
            }
        }
    }

    @Test
    public void testDeleteGroupOnOrganizationRemoval() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization);

        assertTrue(testRealm().groups().groups().stream().anyMatch(group -> group.getName().startsWith("kc.org.")));

        organization.delete().close();

        assertFalse(testRealm().groups().groups().stream().anyMatch(group -> group.getName().startsWith("kc.org.")));
    }

}