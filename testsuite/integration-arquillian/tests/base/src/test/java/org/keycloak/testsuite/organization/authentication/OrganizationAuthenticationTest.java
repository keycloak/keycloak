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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;

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
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
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
            oauth.maxAge("1");
            oauth.kcAction(RequiredAction.UPDATE_PASSWORD.name());
            loginPage.open(bc.consumerRealmName());
            loginPage.assertCurrent();
            Matcher<String> expectedInfo = is("Please re-authenticate to continue");
            assertThat(loginPage.getInfoMessage(), expectedInfo);
            loginPage.login(memberPassword);
            updatePasswordPage.updatePasswords(memberPassword, memberPassword);
            appPage.assertCurrent();
        } finally {
            resetTimeOffset();
            oauth.kcAction(null);
            oauth.maxAge(null);
        }
    }

    @Test
    public void testRequiresUserMembership() {
        runOnServer(setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.TRUE.toString()));

        try {
            OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
            UserRepresentation member = addMember(organization);
            organization.members().member(member.getId()).delete().close();
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + organization.toRepresentation().getName()));

            organization.members().addMember(member.getId()).close();
            OrganizationRepresentation orgB = createOrganization("org-b");
            oauth.clientId("broker-app");
            oauth.scope("organization:org-b");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of the organization selected by the client
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + orgB.getName()));
            errorPage.assertTryAnotherWayLinkAvailability(false);
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
        driver.navigate().to(oauth.getLoginFormUrl() + "&" + OIDCLoginProtocol.LOGIN_HINT_PARAM + "=" + expectedUsername);
        assertThat(loginPage.getUsername(), Matchers.equalTo(URLDecoder.decode(expectedUsername, StandardCharsets.UTF_8)));

        // continue authenticating without setting the username
        loginPage.clickSignIn();
        loginPage.login(memberPassword);
        appPage.assertCurrent();
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
