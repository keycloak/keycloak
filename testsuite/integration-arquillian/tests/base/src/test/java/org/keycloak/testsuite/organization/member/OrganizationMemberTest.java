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

package org.keycloak.testsuite.organization.member;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserResource;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.UserBuilder;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OrganizationMemberTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation expected = getUserRepFromMemberRep(addMember(organization));

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
        OrganizationRepresentation orgB = createOrganization("orgb");
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        OrganizationRepresentation expected = organization.toRepresentation();
        List<OrganizationRepresentation> actual = organization.members().member(member.getId()).getOrganizations(true);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(expected.getId()::equals));
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(orgB.getId()::equals));

        actual = testRealm().organizations().members().getOrganizations(member.getId(), true);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(expected.getId()::equals));
        assertTrue(actual.stream().map(OrganizationRepresentation::getId).anyMatch(orgB.getId()::equals));
    }

    @Test
    public void testGetAll() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
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
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());

        // add some unmanaged members to the organization.
        for (int i = 0; i < 5; i++) {
            addMember(organization, "member-" + i + "@neworg.org");
        }

        // onboard a test user by authenticating using the organization's provider.
        super.assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());

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
            if (user.getEmail().equals(bc.getUserEmail())) {
                assertThat(user.isEnabled(), is(false));
            } else {
                assertThat(user.isEnabled(), is(true));
            }
        }

        // fetching users from the users endpoint should have the same result.
        UserRepresentation disabledUser = null;
        List<UserRepresentation> existingUsers = testRealm().users().search("*neworg*",0, 10);
        assertThat(existingUsers, not(empty()));
        assertThat(existingUsers, hasSize(6));
        for (UserRepresentation user : existingUsers) {
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
        super.assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());

        // now fetch all users from the realm
        List<UserRepresentation> members = testRealm().users().search("*neworg*", null, null);
        members.stream().forEach(user -> assertThat(user.isEnabled(), is(Boolean.TRUE)));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationsEnabled(Boolean.FALSE)
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
        assertNull(expected.getAttributes());
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
        UserRepresentation expected = getUserRepFromMemberRep(addMember(organization));
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
            testRealm().users().get(member.getId()).toRepresentation();
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

    @Test
    public void testUserFederatedBeforeTheIDPBoundWithAnOrgIsNotMember() {
        // create non-org idp in a realm
        String idpAlias = "former-non-org-identity-provider";
        IdentityProviderRepresentation idpRep = brokerConfigFunction.apply("former-non-org").setUpIdentityProvider();
        idpRep.setHideOnLogin(false);
        try (Response response = testRealm().identityProviders().create(idpRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            getCleanup().addCleanup(testRealm().identityProviders().get(bc.getIDPAlias())::remove);
        }

        loginViaNonOrgIdP(idpAlias);

        List<UserRepresentation> search = testRealm().users().search(bc.getUserLogin(), Boolean.TRUE);
        assertThat(search, hasSize(1));

        // create org
        String orgDomain = organizationName + ".org";
        OrganizationRepresentation orgRep = createRepresentation(organizationName, orgDomain);
        String id;

        try (Response response = testRealm().organizations().create(orgRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            id = ApiUtil.getCreatedId(response);
            getCleanup().addCleanup(() -> testRealm().organizations().get(id).delete().close());
        }

        // assign IdP to the org
        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomain);
        idpRep.getConfig().put(OrganizationModel.IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        try (Response response = testRealm().organizations().get(id).identityProviders().addIdentityProvider(idpAlias)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        //check the federated user is not a member
        assertThat(testRealm().organizations().get(id).members().list(-1, -1), hasSize(0));

        // test again this time assigning any org domain to the identity provider

        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);
        assertThat(testRealm().organizations().get(id).members().list(-1, -1), hasSize(0));
    }

    @Test
    public void testMemberInMultipleOrganizations() {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assert.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assert.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        String orgbId = orgb.toRepresentation().getId();
        String orgaId = orga.toRepresentation().getId();
        List<String> memberOfOrgs = orga.members().member(member.getId()).getOrganizations(true).stream().map(OrganizationRepresentation::getId).toList();
        assertTrue(memberOfOrgs.contains(orgaId));
        assertTrue(memberOfOrgs.contains(orgbId));
    }

    @Test
    public void testManagedMemberOnlyRemovedFromHomeOrganization() {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        assertBrokerRegistration(orga, bc.getUserEmail(), "managed-org-a@org-a.org");
        UserRepresentation memberOrgA = orga.members().list(-1, -1).get(0);
        realmsResouce().realm(bc.consumerRealmName()).users().get(memberOrgA.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());
        UserRepresentation memberOrgB = UserBuilder.create()
                .username("managed-org-b")
                .password("password")
                .enabled(true)
                .build();
        realmsResouce().realm(bc.providerRealmName()).users().create(memberOrgB).close();
        assertBrokerRegistration(orgb, memberOrgB.getUsername(), "managed-org-b@org-b.org");
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
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

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

        try (Response response = testRealm().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        getCleanup().addCleanup(() -> testRealm().users().get(user.getId()).remove());

        // now update the user, unsetting the e-mail
        user.setEmail("");
        testRealm().users().get(user.getId()).update(user);

        UserRepresentation updatedUser = testRealm().users().get(user.getId()).toRepresentation();
        assertThat(updatedUser.getEmail(), is(nullValue()));
    }

    @Test
    public void testGetMemberOrganizationsBriefVsFullRepresentation() {
        // Create an organization with attributes
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
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
        assertNull("Brief representation should not include attributes", briefRep.getAttributes());
        
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
        assertNotNull("Full representation should include attributes", fullRep.getAttributes());
        assertTrue("Full representation should include the test attribute", 
                fullRep.getAttributes().containsKey("testAttribute"));
        assertEquals("testValue", fullRep.getAttributes().get("testAttribute").get(0));
        
        // Test the global members API endpoint as well
        List<OrganizationRepresentation> briefOrgsGlobal = testRealm().organizations().members().getOrganizations(member.getId(), true);
        assertNotNull(briefOrgsGlobal);
        assertEquals(1, briefOrgsGlobal.size());
        assertNull("Brief representation should not include attributes", briefOrgsGlobal.get(0).getAttributes());
        
        List<OrganizationRepresentation> fullOrgsGlobal = testRealm().organizations().members().getOrganizations(member.getId(), false);
        assertNotNull(fullOrgsGlobal);
        assertEquals(1, fullOrgsGlobal.size());
        assertNotNull("Full representation should include attributes", fullOrgsGlobal.get(0).getAttributes());
        assertTrue("Full representation should include the test attribute", 
                fullOrgsGlobal.get(0).getAttributes().containsKey("testAttribute"));
    }

    private void loginViaNonOrgIdP(String idpAlias) {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        assertTrue(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(idpAlias));
        loginPage.clickSocial(idpAlias);

        waitForPage(driver, "sign in to", true);

        // user automatically redirected to the identity provider
        assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.providerRealmName() + "/"));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        appPage.assertCurrent();
        assertThat(appPage.getRequestType(), equalTo(AppPage.RequestType.AUTH_RESPONSE));
    }

    private UserRepresentation getUserRepFromMemberRep(MemberRepresentation member) {
        return new UserRepresentation(member);
    }
}
