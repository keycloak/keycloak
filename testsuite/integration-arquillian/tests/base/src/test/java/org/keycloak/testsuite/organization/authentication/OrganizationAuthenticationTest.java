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
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationAuthenticationTest extends AbstractOrganizationTest {

    @Test
    public void testAuthenticateUnmanagedMember() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to log in using only the email
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);

        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        // no idp should be shown because there is only a single idp that is bound to an organization
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // the member should be able to log in using the credentials
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testTryLoginWithUsernameNotAnEmail() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testEmptyUserNameValidation() {
        createOrganization();

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("");

        assertEquals("Invalid username.", loginPage.getUsernameInputError());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAuthenticateUnmanagedMemberWhenProviderDisabled() throws IOException {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to access login page
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(managedRealm.admin())
                .setOrganizationsEnabled(Boolean.FALSE)
                .update()) {

            // access the page again, now it should be present username and password fields
            loginPage.open(bc.consumerRealmName());

            waitForPage(driver, "sign in to", true);
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.consumerRealmName() + "/"));
            Assertions.assertTrue(loginPage.isPasswordInputPresent());
            // no idp should be shown because there is only a single idp that is bound to an organization
            Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

            // the member should be able to log in using the credentials
            loginPage.login(member.getEmail(), memberPassword);
            appPage.assertCurrent();
        }
    }

    @Test
    public void testForceReAuthenticationBeforeRequiredAction() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization);

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        appPage.assertCurrent();

        try {
            timeOffSet.set(10);
            oauth.realm(bc.consumerRealmName());
            oauth.loginForm().maxAge(1).kcAction(RequiredAction.UPDATE_PASSWORD.name()).open();
            loginPage.assertCurrent();
            Matcher<String> expectedInfo = is("Please re-authenticate to continue");
            assertThat(loginPage.getInfoMessage(), expectedInfo);
            loginPage.login(memberPassword);
            updatePasswordPage.updatePasswords(memberPassword, memberPassword);
            appPage.assertCurrent();
        } finally {
            timeOffSet.set(0);
        }
    }

    @Test
    public void testRequiresUserMembership() {
        runOnServer(setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.TRUE.toString()));

        try {
            OrganizationRepresentation org = createOrganization();
            OrganizationResource organization = managedRealm.admin().organizations().get(org.getId());
            UserRepresentation member = addMember(organization);
            organization.members().member(member.getId()).delete().close();
            oauth.client("broker-app");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + org.getName()));

            organization.members().addMember(member.getId()).close();
            OrganizationRepresentation orgB = createOrganization("org-b");
            oauth.client("broker-app");
            oauth.scope("organization:org-b");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of the organization selected by the client
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + orgB.getName()));
            errorPage.assertTryAnotherWayLinkAvailability(false);

            organization.members().member(member.getId()).delete().close();
            oauth.client("broker-app");
            oauth.scope("organization:*");
            loginPage.open(bc.consumerRealmName());
            loginPage.loginUsername(member.getEmail());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of any organization"));

            organization.members().addMember(member.getId()).close();
            managedRealm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
            oauth.client("broker-app");
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
        OrganizationResource organizationResource = managedRealm.admin().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource);

        // login hint populates the username field
        oauth.client("broker-app");
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
        RealmRepresentation realm = managedRealm.admin().toRepresentation();

        realm.setDuplicateEmailsAllowed(true);
        realm.setLoginWithEmailAllowed(false);
        realm.setRegistrationEmailAsUsername(false);

        managedRealm.admin().update(realm);

        OrganizationRepresentation organization = createOrganization();
        OrganizationResource organizationResource = managedRealm.admin().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource);
        UserRepresentation duplicatedUser = UserBuilder.create()
                .username("duplicated-user")
                .password("duplicated-user")
                .email(member.getEmail())
                .enabled(true).build();
        try (Response response = managedRealm.admin().users().create(duplicatedUser)) {
            duplicatedUser.setId(ApiUtil.getCreatedId(response));
        }

        // user with a unique username can authenticate to his account using a unique username
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.loginForm().open();
        loginPage.loginUsername(member.getUsername());
        loginPage.clickSignIn();
        loginPage.login(memberPassword);
        appPage.assertCurrent();
        managedRealm.admin().users().get(member.getId()).logout();

        // a different account with the same email can also authenticate using a unique username
        oauth.loginForm().open();
        loginPage.loginUsername(duplicatedUser.getUsername());
        loginPage.clickSignIn();
        loginPage.login(duplicatedUser.getUsername());
        appPage.assertCurrent();
        managedRealm.admin().users().get(duplicatedUser.getId()).logout();

        // trying to authenticate with the duplicated user using the email will fail because the username is the email of a different account
        oauth.loginForm().open();
        loginPage.loginUsername(duplicatedUser.getEmail());
        loginPage.clickSignIn();
        loginPage.login(duplicatedUser.getEmail());
        assertThat(loginPage.getInputError(), is("Invalid username or password."));

        // trying to authenticate to the account that has the email as username is ok
        oauth.loginForm().open();
        loginPage.loginUsername(member.getEmail());
        loginPage.clickSignIn();
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testRestartLogin() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        loginPage.clickResetLogin();
        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAttemptedUsernameKeptAfterPasswordFailures() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        for (int i = 0; i < 3; i++) {
            loginPage.login("wrong-password");
            loginPage.assertAttemptedUsernameAvailability(true);
            Assertions.assertFalse(loginPage.isEmailInputPresent());
            Assertions.assertTrue(loginPage.isPasswordInputPresent());
        }
    }

    @Test
    public void testHideUsernameKeptAfterPasswordFailuresBruteForceEnabled() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBruteForceProtected(true);
        realm.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE);
        realm.setFailureFactor(1);
        realm.setMaxDeltaTimeSeconds(30);
        realm.setMaxFailureWaitSeconds(30);
        realm.setWaitIncrementSeconds(30);
        managedRealm.admin().update(realm);
        getCleanup().addCleanup(() -> {
            RealmRepresentation r = managedRealm.admin().toRepresentation();
            r.setBruteForceProtected(false);
            managedRealm.admin().update(r);
        });

        String email = "existing-user@" + organizationName + ".org";
        createUser(realm.getRealm(), "existing-user", memberPassword, "John", "Doe", email);
        openIdentityFirstLoginPage(email, false, null, false, false);
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        loginPage.login("wrong-password");
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        loginPage.login("wrong-password");
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        openIdentityFirstLoginPage(email, false, null, false, false);
        realm.setRegistrationEmailAsUsername(true);
        managedRealm.admin().update(realm);
        loginPage.login("wrong-password");
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertFalse(loginPage.isEmailInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        loginPage.login("wrong-password");
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertFalse(loginPage.isEmailInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testUsernameExposureWhenEnteringEmail() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());

        UserRepresentation member = UserBuilder.create()
                .username("secretusername123")  // Different from email
                .email("contractor@contractor.org")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .password(memberPassword)
                .build();
        
        String memberId = AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), member, memberPassword);
        organization.members().addMember(memberId).close();
        
        // Enter the email address in the login form
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);
        
        // when we enter an email, the attempted username should show the email, not the actual username of the resolved user account
        loginPage.assertAttemptedUsernameAvailability(true);
        String displayedUsername = loginPage.getAttemptedUsername();

        assertEquals(member.getEmail(), displayedUsername, "Entering email should not expose actual username");

        // Enter email with different case (should still work with case-insensitive comparison)
        String upperCaseEmail = member.getEmail().toUpperCase();
        openIdentityFirstLoginPage(upperCaseEmail, false, null, false, false);

        loginPage.assertAttemptedUsernameAvailability(true);
        String displayedUsernameUpper = loginPage.getAttemptedUsername();
        assertEquals(upperCaseEmail, displayedUsernameUpper, "Should show what user entered (uppercase email)");
        
        Assertions.assertTrue(loginPage.isPasswordInputPresent(), "Password input should be present");
        
        // Clean up
        managedRealm.admin().users().get(memberId).remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSwitchOrganizationDuringLogin() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = managedRealm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = managedRealm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with "organization" scope (ANY) to trigger org selection
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        oauth.scope("organization");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());

        // org selection page should be shown
        selectOrganizationPage.assertCurrent();
        Assertions.assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        Assertions.assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));

        // select org A
        selectOrganizationPage.selectOrganization(orgA.getAlias());

        // should be on the password page now
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should be available
        loginPage.assertSwitchOrganizationLinkAvailability(true);

        // click switch organization
        loginPage.clickSwitchOrganizationLink();

        // org selection page should be shown again
        selectOrganizationPage.assertCurrent();
        Assertions.assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        Assertions.assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));

        // select org B this time
        selectOrganizationPage.selectOrganization(orgB.getAlias());

        // should be on the password page again
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        // complete login
        loginPage.login(memberPassword);
        appPage.assertCurrent();

        // verify the token contains org B (the final selection), not org A
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, hasItem(orgB.getAlias()));
        assertThat(organizations, not(hasItem(orgA.getAlias())));
    }

    @Test
    public void testSwitchOrganizationNotAvailableForSingleOrgUser() {
        OrganizationRepresentation org = createOrganization();
        OrganizationResource orgResource = managedRealm.admin().organizations().get(org.getId());
        UserRepresentation member = addMember(orgResource);

        // login with "organization" scope — single org member should NOT see org selection
        oauth.client("broker-app");
        oauth.scope("organization");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());

        // should go directly to password page (no org selection for single-org users)
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        loginPage.assertSwitchOrganizationLinkAvailability(false);
    }

    @Test
    public void testSwitchOrganizationNotAvailableWithSpecificScope() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = managedRealm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = managedRealm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with specific organization scope — no org selection should be shown
        oauth.client("broker-app");
        oauth.scope("organization:" + orgA.getAlias());
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());

        // should go directly to password page (specific org requested by client)
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        loginPage.assertSwitchOrganizationLinkAvailability(false);
    }

    @Test
    public void testSwitchOrganizationNotAvailableWithWildcardScope() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = managedRealm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = managedRealm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with wildcard organization scope — no org selection should be shown
        oauth.client("broker-app");
        oauth.scope("organization:*");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());

        // should go directly to password page (all orgs mapped)
        Assertions.assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        loginPage.assertSwitchOrganizationLinkAvailability(false);
    }

    @Test
    public void testUsernamePreservedAfterSwitchOrganization() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = managedRealm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = managedRealm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with "organization" scope to trigger org selection
        oauth.client("broker-app");
        oauth.scope("organization");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());

        // select org A
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgA.getAlias());

        // verify username is shown on password page
        loginPage.assertAttemptedUsernameAvailability(true);
        assertThat(loginPage.getAttemptedUsername(), is(member.getEmail()));

        // switch organization
        loginPage.clickSwitchOrganizationLink();

        // select org B
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());

        // username should still be preserved after switching
        loginPage.assertAttemptedUsernameAvailability(true);
        assertThat(loginPage.getAttemptedUsername(), is(member.getEmail()));
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
