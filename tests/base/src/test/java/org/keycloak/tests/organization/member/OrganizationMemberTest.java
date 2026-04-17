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

package org.keycloak.tests.organization.member;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.Constants;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationMemberTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testUpdate() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation expected = getUserRepFromMemberRep(addMember(organization));

        expected.setFirstName("f");
        expected.setLastName("l");
        expected.setEmail("some@differentthanorg.com");

        realm.admin().users().get(expected.getId()).update(expected);

        UserRepresentation existing = organization.members().member(expected.getId()).toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getUsername(), existing.getUsername());
        assertEquals(expected.getEmail(), existing.getEmail());
        assertEquals(expected.getFirstName(), existing.getFirstName());
        assertEquals(expected.getLastName(), existing.getLastName());
    }

    @Test
    public void testUserAlreadyMemberOfOrganization() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        realm.admin().users().userProfile().update(upConfig);
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization, KeycloakModelUtils.generateId() + "@user.org");

        try (Response response = organization.members().addMember(expected.getId())) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGet() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        UserRepresentation existing = organization.members().member(expected.getId()).toRepresentation();
        assertEquals(expected.getId(), existing.getId());
        assertEquals(expected.getUsername(), existing.getUsername());
        assertEquals(expected.getEmail(), existing.getEmail());
    }

    @Test
    public void testGetMemberOrganization() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization);
        OrganizationRepresentation orgB = createOrganization("orgb");
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        OrganizationRepresentation expected = organization.toRepresentation();
        List<OrganizationRepresentation> actual = organization.members().member(member.getId()).getOrganizations(true);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(expected.getId()::equals));
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(orgB.getId()::equals));

        actual = realm.admin().organizations().members().getOrganizations(member.getId(), true);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(expected.getId()::equals));
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(orgB.getId()::equals));
    }

    @Test
    public void testGetMemberOrganizationRequiresMembershipInCurrentOrganization() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation orgB = createOrganization("orgb");
        OrganizationResource organizationB = realm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(organizationB);

        try {
            organization.members().member(member.getId()).getOrganizations(true);
            fail("should not resolve organizations for a user that is not a member of the current organization");
        } catch (NotFoundException expected) {
        }

        List<OrganizationRepresentation> actual = realm.admin().organizations().members().getOrganizations(member.getId(), true);
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(orgB.getId(), actual.get(0).getId());
    }

    @Test
    public void testGetAll() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            expected.add(addMember(organization, "member-" + i + "@neworg.org"));
        }

        List<MemberRepresentation> existing = organization.members().list(-1, -1);
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

        List<String> concatenatedList = Stream.of(
                        organization.members().list(0, 5).stream().map(AbstractUserRepresentation::getId).toList(),
                        organization.members().list(5, 5).stream().map(AbstractUserRepresentation::getId).toList(),
                        organization.members().list(10, 5).stream().map(AbstractUserRepresentation::getId).toList())
                .flatMap(Collection::stream).toList();

        assertThat(concatenatedList, containsInAnyOrder(expected.stream().map(AbstractUserRepresentation::getId).toArray()));
    }

    @Test
    public void testDeleteUnmanagedMember() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation expected = addMember(organization);
        assertNull(expected.getAttributes());
        OrganizationMemberResource member = organization.members().member(expected.getId());

        try (Response response = member.delete()) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        expected = realm.admin().users().get(expected.getId()).toRepresentation();
        assertNull(expected.getAttributes());
        try {
            member.toRepresentation();
            fail("should not be an organization member");
        } catch (NotFoundException ignore) {
        }
    }

    @Test
    public void testUpdateEmailUnmanagedMember() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation expected = getUserRepFromMemberRep(addMember(organization));
        expected.setEmail("some@unknown.org");
        UserResource userResource = realm.admin().users().get(expected.getId());
        userResource.update(expected);
        UserRepresentation actual = userResource.toRepresentation();
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getEmail(), actual.getEmail());
    }

    @Test
    public void testDeleteMembersOnOrganizationRemoval() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        List<MemberRepresentation> expected = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            expected.add(addMember(organization, "member-" + i + "@neworg.org"));
        }

        organization.delete().close();

        for (MemberRepresentation member : expected) {
            try {
                organization.members().member(member.getId()).toRepresentation();
                fail("should be deleted");
            } catch (NotFoundException ignore) {
            }
        }

        for (MemberRepresentation member : expected) {
            // users should exist as they are not managed by the organization
            realm.admin().users().get(member.getId()).toRepresentation();
        }

        for (MemberRepresentation member : expected) {
            try {
                organization.members().member(member.getId()).getOrganizations(true);
                fail("should not be associated with the organization anymore");
            } catch (NotFoundException ignore) {
            }
        }
    }

    @Test
    public void testSearchMembers() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();
        expected.add(addMember(organization, "batwoman@neworg.org", "Katherine", "Kane"));
        expected.add(addMember(organization, "brucewayne@neworg.org", "Bruce", "Wayne"));
        expected.add(addMember(organization, "harveydent@neworg.org", "Harvey", "Dent"));
        expected.add(addMember(organization, "marthaw@neworg.org", "Martha", "Wayne"));
        expected.add(addMember(organization, "thejoker@neworg.org", "Jack", "White"));

        List<MemberRepresentation> existing = organization.members().search("brucewayne@neworg.org", true, null, null);
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

        existing = organization.members().search("neworg", false, null, null);
        assertThat(existing, hasSize(5));
        for (int i = 0; i < 5; i++) {
            assertThat(expected.get(i).getId(), is(equalTo(expected.get(i).getId())));
            assertThat(expected.get(i).getUsername(), is(equalTo(expected.get(i).getUsername())));
            assertThat(expected.get(i).getEmail(), is(equalTo(expected.get(i).getEmail())));
            assertThat(expected.get(i).getFirstName(), is(equalTo(expected.get(i).getFirstName())));
            assertThat(expected.get(i).getLastName(), is(equalTo(expected.get(i).getLastName())));
        }

        existing = organization.members().search("th", false, null, null);
        assertThat(existing, hasSize(3));
        assertThat(existing.get(0).getUsername(), is(equalTo("batwoman@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Katherine")));
        assertThat(existing.get(1).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getFirstName(), is(equalTo("Martha")));
        assertThat(existing.get(2).getUsername(), is(equalTo("thejoker@neworg.org")));
        assertThat(existing.get(2).getFirstName(), is(equalTo("Jack")));

        existing = organization.members().search("way", false, null, null);
        assertThat(existing, hasSize(2));
        assertThat(existing.get(0).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(0).getFirstName(), is(equalTo("Bruce")));
        assertThat(existing.get(1).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getFirstName(), is(equalTo("Martha")));

        existing = organization.members().search("nonexistent", false, null, null);
        assertThat(existing, is(empty()));

        existing = organization.members().search("", false, 0, 3);
        assertThat(existing, hasSize(3));
        assertThat(existing.get(0).getUsername(), is(equalTo("batwoman@neworg.org")));
        assertThat(existing.get(1).getUsername(), is(equalTo("brucewayne@neworg.org")));
        assertThat(existing.get(2).getUsername(), is(equalTo("harveydent@neworg.org")));

        existing = organization.members().search("", false, 3, 3);
        assertThat(existing, hasSize(2));
        assertThat(existing.get(0).getUsername(), is(equalTo("marthaw@neworg.org")));
        assertThat(existing.get(1).getUsername(), is(equalTo("thejoker@neworg.org")));

        existing = organization.members().search(null, null, MembershipType.MANAGED, -1, -1);
        assertTrue(existing.isEmpty());
        existing = organization.members().search(null, null, MembershipType.UNMANAGED, -1, -1);
        assertThat(existing, hasSize(5));
    }

    @Test
    public void testSearchMembersWithSqlWildcards() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        addMember(organization, "john_doe@test.org", "John", "Doe");
        addMember(organization, "johnadoe@test.org", "Johna", "Doe");
        addMember(organization, "johnbdoe@test.org", "Johnb", "Doe");

        List<MemberRepresentation> members = organization.members().search("john_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(equalTo("john_doe@test.org")));

        addMember(organization, "fifty", "50%@test.org", "Fifty", "Percent", true);
        addMember(organization, "500@test.org", "Five", "Hundred");
        addMember(organization, "50abc@test.org", "Fiftyabc", "Test");

        members = organization.members().search("50%", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%@test.org")));

        members = organization.members().search("john_doe@test.org", true, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(equalTo("john_doe@test.org")));

        members = organization.members().search("50%@test.org", true, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%@test.org")));

        addMember(organization, "testfn", "test_fn@test.org", "TestName", "LastName", true);
        addMember(organization, "testafn", "testafn@test.org", "TestaName", "LastName", true);

        members = organization.members().search("test_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("test_fn@test.org")));

        addMember(organization, "testpercent", "50%_test@test.org", "FirstName", "Last", true);
        addMember(organization, "testatest", "50atest@test.org", "FirstName", "Last", true);

        members = organization.members().search("50%_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%_test@test.org")));
    }

    @Test
    public void testAddMemberFromDifferentRealm() {
        String orgId = createOrganization().getId();

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = provider.getById(orgId);

            RealmModel master = session.realms().getRealmByName("master");
            session.users().addUser(master, "master-test-user");
            UserModel user = null;
            try {
                user = session.users().getUserByUsername(master, "master-test-user");
                assertFalse(provider.addMember(organization, user));
            } finally {
                if (user != null) {
                    session.users().removeUser(master, user);
                }
            }
        });
    }

    @Test
    public void testMemberInMultipleOrganizations() {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        String orgbId = orgb.toRepresentation().getId();
        String orgaId = orga.toRepresentation().getId();
        List<String> memberOfOrgs = orga.members().member(member.getId()).getOrganizations(true).stream().map(OrganizationRepresentation::getId).toList();
        assertTrue(memberOfOrgs.contains(orgaId));
        assertTrue(memberOfOrgs.contains(orgbId));
    }

    @Test
    public void testMembersCount() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        for (int i = 0; i < 10; i++) {
            addMember(organization, "user" + i + "@neworg.org", "First" + i, "Last" + i);
        }

        assertEquals(10, (long) organization.members().count());
    }

    @Test
    public void testNonMemberCanUnsetEmailThatMatchesOrg() {
        OrganizationRepresentation orgRep = createOrganization();
        assertThat(orgRep.getDomains(), hasSize(1));
        assertThat(orgRep.getDomains().iterator().next().getName(), equalTo("neworg.org"));

        UserRepresentation user = new UserRepresentation();
        user.setUsername("brucewayne");
        user.setFirstName("Bruce");
        user.setLastName("Wayne");
        user.setEmail("bwayne@neworg.org");

        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        String userId = user.getId();
        realm.cleanup().add(r -> r.users().get(userId).remove());

        user.setEmail("");
        realm.admin().users().get(userId).update(user);

        UserRepresentation updatedUser = realm.admin().users().get(userId).toRepresentation();
        assertThat(updatedUser.getEmail(), is(nullValue()));
    }

    @Test
    public void testGetMemberOrganizationsBriefVsFullRepresentation() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation orgRep = organization.toRepresentation();
        orgRep.singleAttribute("testAttribute", "testValue");
        organization.update(orgRep).close();

        UserRepresentation member = addMember(organization);

        List<OrganizationRepresentation> briefOrgs = organization.members().member(member.getId()).getOrganizations(true);
        assertNotNull(briefOrgs);
        assertEquals(1, briefOrgs.size());
        OrganizationRepresentation briefRep = briefOrgs.get(0);
        assertEquals(orgRep.getId(), briefRep.getId());
        assertEquals(orgRep.getName(), briefRep.getName());
        assertEquals(orgRep.getAlias(), briefRep.getAlias());
        assertEquals(orgRep.getDescription(), briefRep.getDescription());
        assertEquals(orgRep.getRedirectUrl(), briefRep.getRedirectUrl());
        assertEquals(orgRep.isEnabled(), briefRep.isEnabled());
        assertNull(briefRep.getAttributes(), "Brief representation should not include attributes");

        List<OrganizationRepresentation> fullOrgs = organization.members().member(member.getId()).getOrganizations(false);
        assertNotNull(fullOrgs);
        assertEquals(1, fullOrgs.size());
        OrganizationRepresentation fullRep = fullOrgs.get(0);
        assertEquals(orgRep.getId(), fullRep.getId());
        assertEquals(orgRep.getName(), fullRep.getName());
        assertEquals(orgRep.getAlias(), fullRep.getAlias());
        assertEquals(orgRep.getDescription(), fullRep.getDescription());
        assertEquals(orgRep.getRedirectUrl(), fullRep.getRedirectUrl());
        assertEquals(orgRep.isEnabled(), fullRep.isEnabled());
        assertNotNull(fullRep.getAttributes(), "Full representation should include attributes");
        assertTrue(fullRep.getAttributes().containsKey("testAttribute"),
                "Full representation should include the test attribute");
        assertEquals("testValue", fullRep.getAttributes().get("testAttribute").get(0));

        List<OrganizationRepresentation> briefOrgsGlobal = realm.admin().organizations().members().getOrganizations(member.getId(), true);
        assertNotNull(briefOrgsGlobal);
        assertEquals(1, briefOrgsGlobal.size());
        assertNull(briefOrgsGlobal.get(0).getAttributes(), "Brief representation should not include attributes");

        List<OrganizationRepresentation> fullOrgsGlobal = realm.admin().organizations().members().getOrganizations(member.getId(), false);
        assertNotNull(fullOrgsGlobal);
        assertEquals(1, fullOrgsGlobal.size());
        assertNotNull(fullOrgsGlobal.get(0).getAttributes(), "Full representation should include attributes");
        assertTrue(fullOrgsGlobal.get(0).getAttributes().containsKey("testAttribute"),
                "Full representation should include the test attribute");
    }

    @Test
    public void testGetMemberOrganizationsForbiddenForNonAdminUser() {
        OrganizationRepresentation orgA = createOrganization("orga");
        OrganizationRepresentation orgB = createOrganization("orgb");

        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation userA = addMember(orgAResource, "usera@orga.org");
        orgBResource.members().addMember(userA.getId()).close();

        UserRepresentation userB = UserConfigBuilder.create()
                .username("userb")
                .password("password")
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(userB)) {
            userB.setId(ApiUtil.getCreatedId(response));
        }
        String userBId = userB.getId();
        realm.cleanup().add(r -> r.users().get(userBId).remove());

        try (Keycloak userBClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .username("userb")
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .clientSecret("")
                .build()) {
            userBClient.realm(realm.getName()).organizations().members().getOrganizations(userA.getId(), true);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException expected) {
        }
    }

    private UserRepresentation getUserRepFromMemberRep(MemberRepresentation member) {
        return new UserRepresentation(member);
    }
}
