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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.Constants;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
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
public class OrganizationMemberTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "provider", config = AbstractOrganizationTest.ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AliceUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginUpdateProfilePage loginUpdateProfilePage;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testUserProfileAttributePermissions() {
        OrganizationRepresentation org = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        MemberRepresentation member = addMember(orgResource, memberEmail, "Test", "User");

        UserProfileResource upResource = realm.admin().users().userProfile();
        UPConfig originalCfg = upResource.getConfiguration();

        try {
            // Restrict email and firstName to user-role only: admins (USER_API context) cannot view them
            UPConfig cfg = upResource.getConfiguration();

            UPAttribute emailAttr = cfg.getAttribute(UserModel.EMAIL);
            if (emailAttr == null) {
                emailAttr = new UPAttribute(UserModel.EMAIL);
            }
            emailAttr.setPermissions(new UPAttributePermissions(Set.of("user"), Set.of("user")));
            cfg.addOrReplaceAttribute(emailAttr);

            UPAttribute firstNameAttr = cfg.getAttribute(UserModel.FIRST_NAME);
            if (firstNameAttr == null) {
                firstNameAttr = new UPAttribute(UserModel.FIRST_NAME);
            }
            firstNameAttr.setPermissions(new UPAttributePermissions(Set.of("user"), Set.of("user")));
            cfg.addOrReplaceAttribute(firstNameAttr);

            upResource.update(cfg);

            // List endpoint: email and firstName must be filtered by user profile permissions
            List<MemberRepresentation> members = orgResource.members().search(memberEmail, true, 0, 10);
            assertEquals(1, members.size());
            assertNull(members.get(0).getEmail());
            assertNull(members.get(0).getFirstName());
            assertNull(members.get(0).getUserProfileMetadata());

            // Single member endpoint: same filtering must apply
            MemberRepresentation fetched = orgResource.members().member(member.getId()).toRepresentation();
            assertNull(fetched.getEmail());
            assertNull(fetched.getFirstName());
            assertNull(fetched.getUserProfileMetadata());

        } finally {
            upResource.update(originalCfg);
        }
    }

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
    public void testGetAllDisabledOrganization() {
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(organizationName + "-identity-provider"), organizationName + ".org");
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());

        // add some unmanaged members to the organization.
        for (int i = 0; i < 5; i++) {
            addMember(organization, "member-" + i + "@neworg.org");
        }

        // onboard a test user by authenticating using the organization's provider.
        loginViaBroker(aliceFromProviderRealm.getEmail(), aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());

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
        List<MemberRepresentation> existing = organization.members().list(-1, -1);
        assertThat(existing, not(empty()));
        assertThat(existing, hasSize(6));
        for (UserRepresentation user : existing) {
            if (user.getEmail().equals(aliceFromProviderRealm.getEmail())) {
                assertThat(user.isEnabled(), is(false));
            } else {
                assertThat(user.isEnabled(), is(true));
            }
        }

        // fetching users from the users endpoint should have the same result.
        UserRepresentation disabledUser = null;
        List<UserRepresentation> existingUsers = realm.admin().users().search("*neworg*",0, 10);
        assertThat(existingUsers, not(empty()));
        assertThat(existingUsers, hasSize(6));
        for (UserRepresentation user : existingUsers) {
            if (user.getEmail().equals(aliceFromProviderRealm.getEmail())) {
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
            realm.admin().users().get(disabledUser.getId()).update(disabledUser);
            fail("Should not be possible to update disabled org user");
        } catch(BadRequestException ignored) {
        }
    }

    @Test
    public void testGetAllDisabledOrganizationProvider() throws IOException {
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(organizationName + "-identity-provider"), organizationName + ".org");
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());

        // add some unmanaged members to the organization.
        for (int i = 0; i < 5; i++) {
            addMember(organization, "member-" + i + "@neworg.org");
        }

        // onboard a test user by authenticating using the organization's provider.
        loginViaBroker(aliceFromProviderRealm.getEmail(), aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());

        // now fetch all users from the realm
        List<UserRepresentation> members = realm.admin().users().search("*neworg*", null, null);
        members.forEach(user -> assertThat(user.isEnabled(), is(Boolean.TRUE)));

        // disable the organization provider (cannot use updateWithCleanup, because it would prevent org cleanup - created in the beginning of the test as the org provider would be disabled)
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setOrganizationsEnabled(Boolean.FALSE);
        realm.admin().update(realmRep);

        try {
            // now fetch all members from the realm - unmanaged users should still be enabled, but managed ones should not.
            List<UserRepresentation> existing = realm.admin().users().search("*neworg*", null, null);
            assertThat(existing, hasSize(members.size()));
            for (UserRepresentation user : existing) {
                if (user.getEmail().equals(aliceFromProviderRealm.getEmail())) {
                    assertThat(user.isEnabled(), is(Boolean.FALSE));

                    // try to update the disabled user (for example, try to re-enable the user) - should not be possible.
                    user.setEnabled(Boolean.TRUE);
                    try {
                        realm.admin().users().get(user.getId()).update(user);
                        fail("Should not be possible to update disabled org user");
                    } catch (BadRequestException expected) {
                    }
                } else {
                    assertThat("User " + user.getUsername(), user.isEnabled(), is(true));
                }
            }
        } finally {
            realmRep.setOrganizationsEnabled(Boolean.TRUE);
            realm.admin().update(realmRep);
        }
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

        // user should exist but no longer an organization member
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
            } catch (NotFoundException ignore) {}
        }

        for (MemberRepresentation member : expected) {
            // users should exist as they are not managed by the organization
            realm.admin().users().get(member.getId()).toRepresentation();
        }

        for (MemberRepresentation member : expected) {
            try {
                // user no longer bound to the organization
                organization.members().member(member.getId()).getOrganizations(true);
                fail("should not be associated with the organization anymore");
            } catch (NotFoundException ignore) {
            }
        }
    }

    @Test
    @DatabaseTest
    public void testSearchMembers() {

        // create test users, ordered by username (e-mail).
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        List<UserRepresentation> expected = new ArrayList<>();
        expected.add(addMember(organization, "batwoman@neworg.org", "Katherine", "Kane"));
        expected.add(addMember(organization, "brucewayne@neworg.org", "Bruce", "Wayne"));
        expected.add(addMember(organization, "harveydent@neworg.org", "Harvey", "Dent"));
        expected.add(addMember(organization, "marthaw@neworg.org", "Martha", "Wayne"));
        expected.add(addMember(organization, "thejoker@neworg.org", "Jack", "White"));

        // exact search - username/e-mail, first name, last name.
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

        existing = organization.members().search(null, null, MembershipType.MANAGED, -1, -1);
        assertTrue(existing.isEmpty());
        existing = organization.members().search(null, null, MembershipType.UNMANAGED, -1, -1);
        assertThat(existing, hasSize(5));
    }

    @Test
    @DatabaseTest
    public void testSearchMembersWithSqlWildcards() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        // Create members with SQL wildcard characters in various fields
        UserRepresentation user1 = addMember(organization, "john_doe@test.org", "John", "Doe");
        UserRepresentation user2 = addMember(organization, "johnadoe@test.org", "Johna", "Doe");
        UserRepresentation user3 = addMember(organization, "johnbdoe@test.org", "Johnb", "Doe");

        // Search with underscore in username - should match literally, not as wildcard
        List<MemberRepresentation> members = organization.members().search("john_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(equalTo("john_doe@test.org")));

        // Search with percent character - should match literally, not as wildcard
        UserRepresentation user4 = addMember(organization, "fifty", "50%@test.org", "Fifty", "Percent", true);
        UserRepresentation user5 = addMember(organization, "500@test.org", "Five", "Hundred");
        UserRepresentation user6 = addMember(organization, "50abc@test.org", "Fiftyabc", "Test");

        members = organization.members().search("50%", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%@test.org")));

        // Test exact search with SQL wildcards
        members = organization.members().search("john_doe@test.org", true, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(equalTo("john_doe@test.org")));

        members = organization.members().search("50%@test.org", true, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%@test.org")));

        // Test search by email with underscore
        UserRepresentation user7 = addMember(organization, "testfn", "test_fn@test.org", "TestName", "LastName", true);
        UserRepresentation user8 = addMember(organization, "testafn", "testafn@test.org", "TestaName", "LastName", true);

        members = organization.members().search("test_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("test_fn@test.org")));

        // Test search by email with both percent and underscore
        UserRepresentation user9 = addMember(organization, "testpercent", "50%_test@test.org", "FirstName", "Last", true);
        UserRepresentation user10 = addMember(organization, "testatest", "50atest@test.org", "FirstName", "Last", true);

        members = organization.members().search("50%_", false, null, null);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getEmail(), is(equalTo("50%_test@test.org")));
    }

    @Test
    public void testAddMemberFromDifferentRealm() {
        String orgId = createOrganization().getId();
        String providerRealmName = providerRealm.getName();

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = provider.getById(orgId);

            RealmModel realm = session.realms().getRealmByName(providerRealmName);
            session.users().addUser(realm, "provider-test-user");
            UserModel user = null;
            try {
                user = session.users().getUserByUsername(realm, "provider-test-user");
                assertFalse(provider.addMember(organization, user));
            } finally {
                session.users().removeUser(realm, user);
            }
        });
    }

    @Test
    public void testUserFederatedBeforeTheIDPBoundWithAnOrgIsNotMember() {
        // create non-org idp in a realm with real provider endpoints
        String idpAlias = "former-non-org-identity-provider";
        IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
        idpRep.setAlias(idpAlias);
        idpRep.setProviderId("keycloak-oidc");
        idpRep.setEnabled(true);
        String providerBaseUrl = providerRealm.getBaseUrl();
        idpRep.setConfig(new HashMap<>(Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth",
                "tokenUrl", providerBaseUrl + "/protocol/openid-connect/token",
                "userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo",
                "defaultScope", "email profile",
                "syncMode", "IMPORT"
        )));
        try (Response response = realm.admin().identityProviders().create(idpRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> r.identityProviders().get(idpAlias).remove());
        }

        loginViaNonOrgIdP(idpAlias);

        List<UserRepresentation> search = realm.admin().users().search(aliceFromProviderRealm.getUsername(), Boolean.TRUE);
        assertThat(search, hasSize(1));

        // create org
        String orgDomain = organizationName + ".org";
        OrganizationRepresentation orgRep = createRepresentation(organizationName, orgDomain);
        String id;

        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.organizations().get(id).delete().close());
        }

        // assign IdP to the org
        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomain);
        idpRep.getConfig().put(OrganizationModel.IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        realm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        try (Response response = realm.admin().organizations().get(id).identityProviders().addIdentityProvider(idpAlias)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        //check the federated user is not a member
        assertThat(realm.admin().organizations().get(id).members().list(-1, -1), hasSize(0));

        // test again this time assigning any org domain to the identity provider

        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        realm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);
        assertThat(realm.admin().organizations().get(id).members().list(-1, -1), hasSize(0));
    }

    @Test
    public void testMemberInMultipleOrganizations() {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assertions.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assertions.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        String orgbId = orgb.toRepresentation().getId();
        String orgaId = orga.toRepresentation().getId();
        List<String> memberOfOrgs = orga.members().member(member.getId()).getOrganizations(true).stream().map(OrganizationRepresentation::getId).toList();
        assertTrue(memberOfOrgs.contains(orgaId));
        assertTrue(memberOfOrgs.contains(orgbId));
    }

    @Test
    public void testManagedMemberOnlyRemovedFromHomeOrganization() {
        OrganizationResource orga = realm.admin().organizations().get(
                createOrganization(realm, "org-a", createRealOrgBroker("org-a-identity-provider"), "org-a.org").getId());
        loginViaBroker("alice@org-a.org", aliceFromProviderRealm.getUsername(),
                aliceFromProviderRealm.getPassword(), "managed-org-a@org-a.org");
        UserRepresentation memberOrgA = orga.members().list(-1, -1).get(0);
        realm.admin().users().get(memberOrgA.getId()).logout();
        providerRealm.admin().logoutAll();

        OrganizationResource orgb = realm.admin().organizations().get(
                createOrganization(realm, "org-b", createRealOrgBroker("org-b-identity-provider"), "org-b.org").getId());
        UserRepresentation memberOrgB = UserBuilder.create()
                .username("managed-org-b")
                .password("password")
                .enabled(true)
                .build();
        try (Response response = providerRealm.admin().users().create(memberOrgB)) {
            memberOrgB.setId(ApiUtil.getCreatedId(response));
        }
        String memberOrgBProviderId = memberOrgB.getId();
        providerRealm.cleanup().add(r -> r.users().get(memberOrgBProviderId).remove());

        loginViaBroker("managed-org-b@org-b.org", memberOrgB.getUsername(),
                "password", "managed-org-b@org-b.org");
        memberOrgB = orgb.members().list(-1, -1).get(0);

        orga.members().addMember(memberOrgB.getId()).close();
        assertThat(orga.members().list(-1, -1).size(), is(2));
        OrganizationMemberResource memberOrgBInOrgA = orga.members().member(memberOrgB.getId());
        memberOrgB = memberOrgBInOrgA.toRepresentation();
        memberOrgBInOrgA.delete().close();
        assertThat(orga.members().list(-1, -1).size(), is(1));
        assertThat(orga.members().list(-1, -1).get(0).getId(), is(memberOrgA.getId()));
        assertThat(orgb.members().list(-1, -1).size(), is(1));

        orgb.members().member(memberOrgB.getId()).delete().close();
        assertThat(orga.members().list(-1, -1).size(), is(1));
        assertThat(orga.members().list(-1, -1).get(0).getId(), is(memberOrgA.getId()));
        assertThat(orgb.members().list(-1, -1).size(), is(0));
    }

    @Test
    public void testMembersCount() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        for (int i = 0; i < 10; i++) {
            addMember(organization, "user" + i +"@neworg.org", "First" + i, "Last" + i);
        }

        assertEquals(10, (long) organization.members().count());
    }

    @Test
    public void testNonMemberCanUnsetEmailThatMatchesOrg() {
        // create a test org with a domain "neworg.org"
        OrganizationRepresentation orgRep = createOrganization();
        assertThat(orgRep.getDomains(), hasSize(1));
        assertThat(orgRep.getDomains().iterator().next().getName(), equalTo("neworg.org"));

        // create a user whose e-mail matches the org
        UserRepresentation user = new UserRepresentation();
        user.setUsername("brucewayne");
        user.setFirstName("Bruce");
        user.setLastName("Wayne");
        user.setEmail("bwayne@neworg.org");

        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        realm.cleanup().add(r -> r.users().get(user.getId()).remove());

        // now update the user, unsetting the e-mail
        user.setEmail("");
        realm.admin().users().get(user.getId()).update(user);

        UserRepresentation updatedUser = realm.admin().users().get(user.getId()).toRepresentation();
        assertThat(updatedUser.getEmail(), is(nullValue()));
    }

    @Test
    public void testGetMemberOrganizationsBriefVsFullRepresentation() {
        // Create an organization with attributes
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation orgRep = organization.toRepresentation();
        orgRep.singleAttribute("testAttribute", "testValue");
        organization.update(orgRep).close();
        
        UserRepresentation member = addMember(organization);
        
        // Test brief representation (default, briefRepresentation=true)
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
        
        // Test full representation (briefRepresentation=false)
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
        
        // Test the global members API endpoint as well
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
    public void testGetMemberOrganizationsForbiddenForNonAdminUser() throws Exception {
        // create 2 orgs
        OrganizationRepresentation orgA = createOrganization("orga");
        OrganizationRepresentation orgB = createOrganization("orgb");

        // create userA and add as member of both orgs
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation userA = addMember(orgAResource, "usera@orga.org");
        orgBResource.members().addMember(userA.getId()).close();

        // create userB (non-admin user)
        UserRepresentation userB = UserBuilder.create()
                .username("userb")
                .firstName("userb")
                .lastName("userb")
                .password("password")
                .email("userb@test.org")
                .emailVerified(true)
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(userB)) {
            userB.setId(ApiUtil.getCreatedId(response));
        }
        realm.cleanup().add(r -> r.users().get(userB.getId()).remove());

        // send request as userB to OrganizationsResource.getOrganizations with member-id = userA
        try (Keycloak userBClient = adminClientFactory.create()
                .realm(realm.getName()).username("userb").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            userBClient.realm(realm.getName()).organizations().members().getOrganizations(userA.getId(), true);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException expected) {
        }
    }

    @Test
    public void testSearchMembersBriefVsFullRepresentation() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        realm.admin().users().userProfile().update(upConfig);

        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "member@neworg.org");

        // set a custom attribute on the member so we can verify it's absent in brief representation
        UserResource userResource = realm.admin().users().get(member.getId());
        member = userResource.toRepresentation();
        member.singleAttribute("testAttr", "testValue");
        userResource.update(member);

        // default search returns brief representation — attributes should be null
        List<MemberRepresentation> briefMembers = organization.members().search("member@neworg.org", true, null, null);
        assertThat(briefMembers, hasSize(1));
        MemberRepresentation briefMember = briefMembers.get(0);
        assertNotNull(briefMember.getId());
        assertNotNull(briefMember.getUsername());
        assertNotNull(briefMember.getEmail());
        assertNotNull(briefMember.getMembershipType());
        assertNull(briefMember.getAttributes(), "Brief representation should not include attributes");

        // explicit briefRepresentation=false returns full representation — attributes should be present
        List<MemberRepresentation> fullMembers = organization.members().search("member@neworg.org", true, null, null, false);
        assertThat(fullMembers, hasSize(1));
        MemberRepresentation fullMember = fullMembers.get(0);
        assertNotNull(fullMember.getId());
        assertNotNull(fullMember.getUsername());
        assertNotNull(fullMember.getEmail());
        assertNotNull(fullMember.getMembershipType());
        assertNotNull(fullMember.getAttributes(), "Full representation should include attributes");
        assertTrue(fullMember.getAttributes().containsKey("testAttr"),
                "Full representation should include the test attribute");
        assertEquals("testValue", fullMember.getAttributes().get("testAttr").get(0));

        // explicit briefRepresentation=true also returns brief
        List<MemberRepresentation> explicitBriefMembers = organization.members().search("member@neworg.org", true, null, null, true);
        assertThat(explicitBriefMembers, hasSize(1));
        assertNull(explicitBriefMembers.get(0).getAttributes(), "Explicit brief representation should not include attributes");

        // single-member GET should still return full representation
        MemberRepresentation singleMember = organization.members().member(briefMember.getId()).toRepresentation();
        assertNotNull(singleMember.getAttributes(), "Single member GET should return full representation");
        assertTrue(singleMember.getAttributes().containsKey("testAttr"));
    }

    private void loginViaBroker(String email, String username, String password) {
        loginViaBroker(email, username, password, null);
    }

    private void loginViaBroker(String email, String username, String password, String updateEmail) {
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(email);
        loginUsernamePage.submit();

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page");

        loginPage.fillLogin(username, password);
        loginPage.submit();

        if (updateEmail != null) {
            loginUpdateProfilePage.update("Firstname", "Lastname", updateEmail);
        }

        List<UserRepresentation> users = realm.admin().users().search(username);
        assertEquals(1, users.size(), "Federated user should be created in consumer realm");

        String userId = users.get(0).getId();
        realm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (NotFoundException ignored) {}
        });
    }

    private void loginViaNonOrgIdP(String idpAlias) {
        oauth.openLoginForm();

        assertTrue(loginPage.isSocialButtonPresent(idpAlias));
        loginPage.clickSocial(idpAlias);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page");

        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();
    }

    private IdentityProviderRepresentation createRealOrgBroker(String alias) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId("keycloak-oidc");
        idp.setEnabled(true);
        idp.setTrustEmail(true);
        String providerBaseUrl = providerRealm.getBaseUrl();
        idp.setConfig(new HashMap<>(Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth",
                "tokenUrl", providerBaseUrl + "/protocol/openid-connect/token",
                "userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo",
                "defaultScope", "email profile",
                "syncMode", "IMPORT"
        )));
        return idp;
    }

    private UserRepresentation getUserRepFromMemberRep(MemberRepresentation member) {
        return new UserRepresentation(member);
    }

    static class AliceUserConf implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder builder) {
            return builder.username("alice")
                    .password("password")
                    .email("alice@neworg.org")
                    .emailVerified(true)
                    .name("Alice", "Org");
        }
    }
}
