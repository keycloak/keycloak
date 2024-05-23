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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
import java.io.IOException;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;

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
            assertTrue(expectedRep.isEnabled());
        }
    }

    @Test
    public void testGetAllDisabledOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());

        // add some unmanaged members to the organization.
        for (int i = 0; i < 5; i++) {
            addMember(organization, "member-" + i + "@neworg.org");
        }

        // onboard a test user by authenticating using the organization's provider.
        super.assertBrokerRegistration(organization, bc.getUserEmail());

        // disable the organization and check that fetching its representation has it disabled.
        orgRep.setEnabled(false);
        try (Response response = organization.update(orgRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        OrganizationRepresentation existingOrg = organization.toRepresentation();
        assertThat(orgRep.getId(), is(equalTo(existingOrg.getId())));
        assertThat(orgRep.getName(), is(equalTo(existingOrg.getName())));
        assertThat(existingOrg.isEnabled(), is(false));

        // now fetch all users from the org - unmanaged users should still be enabled, but managed ones should not.
        List<UserRepresentation> existing = organization.members().getAll();
        assertThat(existing, not(empty()));
        assertThat(existing, hasSize(6));
        for (UserRepresentation user : existing) {
            if (user.getEmail().equals(bc.getUserEmail())) {
                assertThat(user.isEnabled(), is(false));
            } else {
                assertThat(user.isEnabled(), is(true));
            }
        }

        // fetching users from the users endpoint should have the same result.
        UserRepresentation disabledUser = null;
        existing = testRealm().users().search("*neworg*",0, 10);
        assertThat(existing, not(empty()));
        assertThat(existing, hasSize(6));
        for (UserRepresentation user : existing) {
            if (user.getEmail().equals(bc.getUserEmail())) {
                assertThat(user.isEnabled(), is(false));
                disabledUser = user;
            } else {
                assertThat(user.isEnabled(), is(true));
            }
        }

        assertThat(disabledUser, notNullValue());
        // try to update the disabled user (for example, try to re-enable the user) - should not be possible.
        disabledUser.setEnabled(true);
        try {
            testRealm().users().get(disabledUser.getId()).update(disabledUser);
            fail("Should not be possible to update disabled org user");
        } catch(BadRequestException ignored) {
        }
    }

    @Test
    public void testGetAllDisabledOrganizationProvider() throws IOException {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());

        // add some unmanaged members to the organization.
        for (int i = 0; i < 5; i++) {
            addMember(organization, "member-" + i + "@neworg.org");
        }

        // onboard a test user by authenticating using the organization's provider.
        super.assertBrokerRegistration(organization, bc.getUserEmail());

        // now fetch all users from the realm
        List<UserRepresentation> members = testRealm().users().search("*neworg*", null, null);
        members.stream().forEach(user -> assertThat(user.isEnabled(), is(Boolean.TRUE)));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationEnabled(Boolean.FALSE)
                .update()) {

            // now fetch all members from the realm - unmanaged users should still be enabled, but managed ones should not.
            List<UserRepresentation> existing = testRealm().users().search("*neworg*", null, null);
            assertThat(existing, hasSize(members.size()));
            for (UserRepresentation user : existing) {
                if (user.getEmail().equals(bc.getUserEmail())) {
                    assertThat(user.isEnabled(), is(Boolean.FALSE));

                    // try to update the disabled user (for example, try to re-enable the user) - should not be possible.
                    user.setEnabled(Boolean.TRUE);
                    try {
                        testRealm().users().get(user.getId()).update(user);
                        fail("Should not be possible to update disabled org user");
                    } catch(BadRequestException expected) {}
                } else {
                    assertThat("User " + user.getUsername(), user.isEnabled(), is(true));
                }
            }
        }
    }

    @Test
    public void testDeleteUnmanagedMember() {
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
    public void testUpdateEmailUnmanagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        expected.setEmail("some@unknown.org");
        UserResource userResource = testRealm().users().get(expected.getId());
        userResource.update(expected);
        UserRepresentation actual = userResource.toRepresentation();
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getEmail(), actual.getEmail());

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

        assertTrue(testRealm().groups().groups("", 0, 100, false).stream().anyMatch(group -> group.getAttributes().containsKey("kc.org")));

        organization.delete().close();

        assertFalse(testRealm().groups().groups("", 0, 100, false).stream().anyMatch(group -> group.getAttributes().containsKey("kc.org")));
    }

    @Test
    public void testSearchMembers() {

        // create test users, ordered by username (e-mail).
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();
        expected.add(addMember(organization, "batwoman@neworg.org", "Katherine", "Kane"));
        expected.add(addMember(organization, "brucewayne@neworg.org", "Bruce", "Wayne"));
        expected.add(addMember(organization, "harveydent@neworg.org", "Harvey", "Dent"));
        expected.add(addMember(organization, "marthaw@neworg.org", "Martha", "Wayne"));
        expected.add(addMember(organization, "thejoker@neworg.org", "Jack", "White"));

        // exact search - username/e-mail, first name, last name.
        List<UserRepresentation> existing = organization.members().search("brucewayne@neworg.org", true, null, null);
        assertThat(existing, hasSize(1));
        assertThat(existing.get(0).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(0).getEmail(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Bruce")));
        assertThat(existing.get(0).getLastName(), is(equalTo("Wayne")));

        existing = organization.members().search("Harvey", true, null, null);
        assertThat(existing, hasSize(1));
        assertThat(existing.get(0).getUsername(), is(equalTo("harveydent@neworg.org")));
        assertThat(existing.get(0).getEmail(), is(equalTo("harveydent@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Harvey")));
        assertThat(existing.get(0).getLastName(), is(equalTo("Dent")));

        existing = organization.members().search("Wayne", true, null, null);
        assertThat(existing, hasSize(2));
        assertThat(existing.get(0).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(1).getUsername(), is(equalTo("marthaw@neworg.org")));

        existing = organization.members().search("Gordon", true, null, null);
        assertThat(existing, is(empty()));

        // partial search - partial e-mail should match all users.
        existing = organization.members().search("neworg", false, null, null);
        assertThat(existing, hasSize(5));
        for (int i = 0; i < 5; i++) { // returned entries should also be ordered.
            assertThat(expected.get(i).getId(), is(equalTo(expected.get(i).getId())));
            assertThat(expected.get(i).getUsername(), is(equalTo(expected.get(i).getUsername())));
            assertThat(expected.get(i).getEmail(), is(equalTo(expected.get(i).getEmail())));
            assertThat(expected.get(i).getFirstName(), is(equalTo(expected.get(i).getFirstName())));
            assertThat(expected.get(i).getLastName(), is(equalTo(expected.get(i).getLastName())));
        }

        // partial search using 'th' search string - should match 'Katherine' by name, 'Jack' by username/e-mail
        // and 'Martha' either by username or first name.
        existing = organization.members().search("th", false, null, null);
        assertThat(existing, hasSize(3));
        assertThat(existing.get(0).getUsername(), is(equalTo("batwoman@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Katherine")));
        assertThat(existing.get(1).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getFirstName(), is(equalTo("Martha")));
        assertThat(existing.get(2).getUsername(), is(equalTo("thejoker@neworg.org")));
        assertThat(existing.get(2).getFirstName(), is(equalTo("Jack")));

        // partial search using 'way' - should match both 'Bruce' (either by username or last name) and 'Martha' by last name.
        existing = organization.members().search("way", false, null, null);
        assertThat(existing, hasSize(2));
        assertThat(existing.get(0).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Bruce")));
        assertThat(existing.get(1).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getFirstName(), is(equalTo("Martha")));

        // partial search using with no match - e.g. 'nonexistent'.
        existing = organization.members().search("nonexistent", false, null, null);
        assertThat(existing, is(empty()));

        // paginated search - try to fetch 3 users per page.
        existing = organization.members().search("", false, 0, 3);
        assertThat(existing, hasSize(3));
        assertThat(existing.get(0).getUsername(), is(equalTo("batwoman@neworg.org")));
        assertThat(existing.get(1).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(2).getUsername(), is(equalTo("harveydent@neworg.org")));

        existing = organization.members().search("", false, 3, 3);
        assertThat(existing, hasSize(2));
        assertThat(existing.get(0).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getUsername(), is(equalTo("thejoker@neworg.org")));
    }

    @Test
    public void testAddMemberFromDifferentRealm() {
        String orgId = createOrganization().getId();

        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = provider.getById(orgId);

            RealmModel realm = session.realms().getRealmByName("master");
            session.users().addUser(realm, "master-test-user");
            UserModel user = null;
            try {
                user = session.users().getUserByUsername(realm, "master-test-user");
                assertFalse(provider.addMember(organization, user));
            } finally {
                session.users().removeUser(realm, user);
            }
        });
    }
}