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
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.OrganizationModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OrganizationMemberTest extends AbstractOrganizationTest {

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

}
