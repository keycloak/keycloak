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

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.UserBuilder;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationBrokerSelfRegistrationTest extends AbstractOrganizationTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AppPage appPage;

    @Test
    public void testBrokerRegistration() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        assertBrokerRegistration(organization);
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        testRealm().organizations().get(createOrganization().getId());
        oauth.clientId("broker-app");

        // login with email only
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("user@noorg.org");

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testLinkExistingAccount() {
        // create a realm user in the consumer realm
        realmsResouce().realm(bc.consumerRealmName()).users()
                .create(UserBuilder.create()
                    .username(bc.getUserLogin())
                    .email(bc.getUserEmail())
                    .password(bc.getUserPassword())
                    .enabled(true).build()
                ).close();

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        oauth.clientId("broker-app");

        // login with email only
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        loginPage.loginUsername(bc.getUserEmail());

        // user automatically redirected to the organization identity provider
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        // account with the same email exists in the realm, execute account linking
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();
        // confirm the link by authenticating
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        assertIsMember(bc.getUserEmail(), organization);
    }

    @Test
    public void testMemberAlreadyExists() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        // add the member for the first time
        assertBrokerRegistration(organization);

        // logout to force the user to authenticate again
        UserRepresentation account = getUserRepresentation(bc.getUserEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();

        // login with email only
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        loginPage.loginUsername(bc.getUserEmail());

        // user automatically redirected to the organization identity provider
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        // login to the organization identity provider and automatically redirects to the app as the account already exists
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        appPage.assertCurrent();
        assertIsMember(bc.getUserEmail(), organization);
    }

    private void assertBrokerRegistration(OrganizationResource organization) {
        // login with email only
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        loginPage.loginUsername(bc.getUserEmail());

        // user automatically redirected to the organization identity provider
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        assertIsMember(bc.getUserEmail(), organization);
    }

    private void assertIsMember(String userEmail, OrganizationResource organization) {
        UserRepresentation account = getUserRepresentation(userEmail);
        UserRepresentation member = organization.members().member(account.getId()).toRepresentation();
        Assert.assertEquals(account.getId(), member.getId());
    }

    private UserRepresentation getUserRepresentation(String userEmail) {
        UsersResource users = adminClient.realm(bc.consumerRealmName()).users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);
        Assert.assertFalse(reps.isEmpty());
        Assert.assertEquals(1, reps.size());
        return reps.get(0);
    }
}
