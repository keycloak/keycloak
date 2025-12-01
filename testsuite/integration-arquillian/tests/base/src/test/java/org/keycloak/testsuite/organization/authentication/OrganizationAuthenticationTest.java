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

package org.keycloak.testsuite.organization.authentication;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UserBuilder;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class OrganizationAuthenticationTest extends AbstractOrganizationTest {

    @Test
    public void testAuthenticateUnmanagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to log in using only the email
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);

        Assert.assertTrue(loginPage.isPasswordInputPresent());
        // no idp should be shown because there is only a single idp that is bound to an organization
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // the member should be able to log in using the credentials
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testTryLoginWithUsernameNotAnEmail() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testEmptyUserNameValidation() {
        createOrganization();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("");

        assertEquals("Invalid username.", loginPage.getUsernameInputError());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAuthenticateUnmanagedMemberWhenProviderDisabled() throws IOException {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to access login page
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationsEnabled(Boolean.FALSE)
                .update()) {

            // access the page again, now it should be present username and password fields
            loginPage.open(bc.consumerRealmName());

            waitForPage(driver, "sign in to", true);
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.consumerRealmName() + "/"));
            Assert.assertTrue(loginPage.isPasswordInputPresent());
            // no idp should be shown because there is only a single idp that is bound to an organization
            Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

            // the member should be able to log in using the credentials
            loginPage.login(member.getEmail(), memberPassword);
            appPage.assertCurrent();
        }
    }

    @Test
    public void testForceReAuthenticationBeforeRequiredAction() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        appPage.assertCurrent();

        try {
            setTimeOffset(10);
            oauth.realm(bc.consumerRealmName());
            oauth.loginForm().maxAge(1).kcAction(RequiredAction.UPDATE_PASSWORD.name()).open();
            loginPage.assertCurrent();
            Matcher<String> expectedInfo = is("Please re-authenticate to continue");
            assertThat(loginPage.getInfoMessage(), expectedInfo);
            loginPage.login(memberPassword);
            updatePasswordPage.updatePasswords(memberPassword, memberPassword);
            appPage.assertCurrent();
        } finally {
            resetTimeOffset();
        }
    }

    @Test
    public void testRequiresUserMembership() {
        runOnServer(setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.TRUE.toString()));

        try {
            OrganizationRepresentation org = createOrganization();
            OrganizationResource organization = testRealm().organizations().get(org.getId());
            UserRepresentation member = addMember(organization);
            organization.members().member(member.getId()).delete().close();
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + org.getName()));

            organization.members().addMember(member.getId()).close();
            OrganizationRepresentation orgB = createOrganization("org-b");
            oauth.clientId("broker-app");
            oauth.scope("organization:org-b");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of the organization selected by the client
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + orgB.getName()));
            errorPage.assertTryAnotherWayLinkAvailability(false);

            organization.members().member(member.getId()).delete().close();
            oauth.clientId("broker-app");
            oauth.scope("organization:*");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of any organization"));

            organization.members().addMember(member.getId()).close();
            testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
            oauth.clientId("broker-app");
            oauth.scope("organization");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            selectOrganizationPage.assertCurrent();
            organization.members().member(member.getId()).delete().close();
            selectOrganizationPage.selectOrganization(org.getAlias());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + org.getName()));
        } finally {
            runOnServer(setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.FALSE.toString()));
        }
    }

    @Test
    public void testLoginHint() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationResource organizationResource = testRealm().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource);

        // login hint populates the username field
        oauth.clientId("broker-app");
        String expectedUsername = URLEncoder.encode(member.getEmail(), StandardCharsets.UTF_8);
        oauth.realm(bc.consumerRealmName());
        oauth.loginForm().loginHint(expectedUsername).open();
        assertThat(loginPage.getAttemptedUsername(), Matchers.equalTo(URLDecoder.decode(expectedUsername, StandardCharsets.UTF_8)));

        // continue authenticating without setting the username
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testDuplicateEmailsEnabled() {
        RealmRepresentation realm = testRealm().toRepresentation();

        realm.setDuplicateEmailsAllowed(true);
        realm.setLoginWithEmailAllowed(false);
        realm.setRegistrationEmailAsUsername(false);

        testRealm().update(realm);

        OrganizationRepresentation organization = createOrganization();
        OrganizationResource organizationResource = testRealm().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource);
        UserRepresentation duplicatedUser = UserBuilder.create()
                .username("duplicated-user")
                .password("duplicated-user")
                .email(member.getEmail())
                .enabled(true).build();
        try (Response response = testRealm().users().create(duplicatedUser)) {
            duplicatedUser.setId(ApiUtil.getCreatedId(response));
        }

        // user with a unique username can authenticate to his account using a unique username
        oauth.clientId("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.loginForm().open();
        loginPage.loginUsername(member.getUsername());
        loginPage.clickSignIn();
        loginPage.login(memberPassword);
        appPage.assertCurrent();
        testRealm().users().get(member.getId()).logout();

        // a different account with the same email can also authenticate using a unique username
        oauth.loginForm().open();
        loginPage.loginUsername(duplicatedUser.getUsername());
        loginPage.clickSignIn();
        loginPage.login(duplicatedUser.getUsername());
        appPage.assertCurrent();
        testRealm().users().get(duplicatedUser.getId()).logout();

        // trying to authenticate with the duplicated user using the email will fail because the username is the email of a different account
        oauth.loginForm().open();
        loginPage.loginUsername(duplicatedUser.getEmail());
        loginPage.clickSignIn();
        loginPage.login(duplicatedUser.getEmail());
        assertThat(loginPage.getInputError(), is("Invalid password."));

        // trying to authenticate to the account that has the email as username is ok
        oauth.loginForm().open();
        loginPage.loginUsername(member.getEmail());
        loginPage.clickSignIn();
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testRestartLogin() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());

        loginPage.clickResetLogin();
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAttemptedUsernameKeptAfterPasswordFailures() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());

        for (int i = 0; i < 3; i++) {
            loginPage.login("wrong-password");
            loginPage.assertAttemptedUsernameAvailability(true);
            Assert.assertTrue(loginPage.isPasswordInputPresent());
        }
    }

    private void runOnServer(RunOnServer function) {
        testingClient.server(bc.consumerRealmName()).run(function);
    }

    public static RunOnServer setAuthenticatorConfig(String key, String value) {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            FlowUtil.setAuthenticatorConfig(session, realm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW).getId(), OrganizationAuthenticatorFactory.ID, key, value);
        };
    }
}
