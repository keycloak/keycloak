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

package org.keycloak.tests.organization.authentication;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.SelectOrganizationPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationAuthenticationTest extends AbstractOrganizationTest {

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginPasswordUpdatePage loginPasswordUpdatePage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    SelectOrganizationPage selectOrganizationPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @BeforeEach
    public void resetOAuthScope() {
        oauth.scope(null);
    }

    @Test
    public void testAuthenticateUnmanagedMember() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org", "Contractor", "User");

        // first try to log in using only the email
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);

        assertTrue(loginPage.isPasswordInputPresent());
        // no idp should be shown because there is only a single idp that is bound to an organization
        assertFalse(loginPage.isSocialButtonPresent(organizationName + "-identity-provider"));

        // the member should be able to log in using the credentials
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();
    }

    @Test
    public void testTryLoginWithUsernameNotAnEmail() {
        realm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testEmptyUserNameValidation() {
        createOrganization();

        oauth.openLoginForm();
        assertFalse(loginPage.isPasswordInputPresent());
        loginUsernamePage.fillLoginWithUsernameOnly("");
        loginUsernamePage.submit();

        assertEquals("Invalid username.", loginUsernamePage.getUsernameInputError());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        realm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAuthenticateUnmanagedMemberWhenProviderDisabled() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org", "Contractor", "User");

        // first try to access login page
        oauth.openLoginForm();
        assertFalse(loginPage.isPasswordInputPresent());
        assertFalse(loginUsernamePage.isSocialButtonPresent(organizationName + "-identity-provider"));

        // disable the organization provider
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        try {
            // access the page again, now it should be present username and password fields
            oauth.openLoginForm();
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/realms/" + realm.getName() + "/"));
            assertTrue(loginPage.isPasswordInputPresent());
            // no idp should be shown because there is only a single idp that is bound to an organization
            assertFalse(loginPage.isSocialButtonPresent(organizationName + "-identity-provider"));

            // the member should be able to log in using the credentials
            loginPage.fillLogin(member.getEmail(), memberPassword);
            loginPage.submit();
            assertLoginSuccess();
        } finally {
            // re-enable organizations before cleanup runs so org deletion works
            RealmRepresentation realmRep = realm.admin().toRepresentation();
            realmRep.setOrganizationsEnabled(true);
            realm.admin().update(realmRep);
        }
    }

    @Test
    public void testForceReAuthenticationBeforeRequiredAction() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, memberEmail, "John", "Doe");

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();

        try {
            timeOffSet.set(10);
            oauth.loginForm().maxAge(1).kcAction(RequiredAction.UPDATE_PASSWORD.name()).open();
            loginPage.assertCurrent();
            assertThat(loginPage.getInfoMessage().orElse(null), is("Please re-authenticate to continue"));
            loginPage.fillPassword(memberPassword);
            loginPage.submit();
            loginPasswordUpdatePage.changePassword(memberPassword, memberPassword);
            assertLoginSuccess();
        } finally {
            timeOffSet.set(0);
        }
    }

    @Test
    public void testRequiresUserMembership() {
        setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.TRUE.toString());

        try {
            OrganizationRepresentation org = createOrganization();
            OrganizationResource organization = realm.admin().organizations().get(org.getId());
            UserRepresentation member = addMember(organization);
            organization.members().member(member.getId()).delete().close();
            oauth.openLoginForm();
            loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
            loginUsernamePage.submit();
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + org.getName()));

            organization.members().addMember(member.getId()).close();
            OrganizationRepresentation orgB = createOrganization("org-b");
            oauth.scope("organization:org-b");
            oauth.openLoginForm();
            loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
            loginUsernamePage.submit();
            // user is not a member of the organization selected by the client
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + orgB.getName()));
            assertFalse(loginPage.isTryAnotherWayPresent());

            organization.members().member(member.getId()).delete().close();
            oauth.scope("organization:*");
            oauth.openLoginForm();
            loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
            loginUsernamePage.submit();
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of any organization"));

            organization.members().addMember(member.getId()).close();
            realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
            oauth.scope("organization");
            oauth.openLoginForm();
            loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
            loginUsernamePage.submit();
            selectOrganizationPage.assertCurrent();
            organization.members().member(member.getId()).delete().close();
            selectOrganizationPage.selectOrganization(org.getAlias());
            // user is not a member of any organization
            assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization " + org.getName()));
        } finally {
            setAuthenticatorConfig(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.FALSE.toString());
        }
    }

    @Test
    public void testLoginHint() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource, memberEmail, "John", "Doe");

        // login hint populates the username field
        String expectedUsername = URLEncoder.encode(member.getEmail(), StandardCharsets.UTF_8);
        oauth.loginForm().loginHint(expectedUsername).open();
        assertThat(loginPage.getAttemptedUsername(), Matchers.equalTo(URLDecoder.decode(expectedUsername, StandardCharsets.UTF_8)));

        // continue authenticating without setting the username
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();
    }

    @Test
    public void testDuplicateEmailsEnabled() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();

        realmRep.setDuplicateEmailsAllowed(true);
        realmRep.setLoginWithEmailAllowed(false);
        realmRep.setRegistrationEmailAsUsername(false);

        realm.admin().update(realmRep);
        realm.cleanup().add(r -> {
            RealmRepresentation rep = r.toRepresentation();
            rep.setDuplicateEmailsAllowed(false);
            rep.setLoginWithEmailAllowed(true);
            rep.setRegistrationEmailAsUsername(false);
            r.update(rep);
        });

        OrganizationRepresentation organization = createOrganization();
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        UserRepresentation member = addMember(organizationResource, memberEmail, "John", "Doe");
        UserRepresentation duplicatedUser = UserBuilder.create()
                .username("duplicated-user")
                .password("duplicated-user")
                .email(member.getEmail())
                .firstName("Duplicate")
                .lastName("User")
                .enabled(true).build();
        try (Response response = realm.admin().users().create(duplicatedUser)) {
            duplicatedUser.setId(ApiUtil.getCreatedId(response));
        }
        String duplicatedUserId = duplicatedUser.getId();
        realm.cleanup().add(r -> r.users().get(duplicatedUserId).remove());

        // user with a unique username can authenticate to his account using a unique username
        oauth.loginForm().open();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getUsername());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();
        realm.admin().users().get(member.getId()).logout();

        // a different account with the same email can also authenticate using a unique username
        oauth.loginForm().open();
        loginUsernamePage.fillLoginWithUsernameOnly(duplicatedUser.getUsername());
        loginUsernamePage.submit();
        loginPage.fillPassword(duplicatedUser.getUsername());
        loginPage.submit();
        assertLoginSuccess();
        realm.admin().users().get(duplicatedUser.getId()).logout();

        // trying to authenticate with the duplicated user using the email will fail because the username is the email of a different account
        oauth.loginForm().open();
        loginUsernamePage.fillLoginWithUsernameOnly(duplicatedUser.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(duplicatedUser.getEmail());
        loginPage.submit();
        assertThat(loginPage.getPasswordInputError().orElse(null), is("Invalid username or password."));

        // trying to authenticate to the account that has the email as username is ok
        oauth.loginForm().open();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();
    }

    @Test
    public void testRestartLogin() {
        realm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());

        loginPage.clickResetLogin();
        assertTrue(loginPage.isUsernameInputPresent());
        assertFalse(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAttemptedUsernameKeptAfterPasswordFailures() {
        realm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());

        for (int i = 0; i < 3; i++) {
            loginPage.fillPassword("wrong-password");
            loginPage.submit();
            assertNotNull(loginPage.getAttemptedUsername());
            assertTrue(loginPage.isPasswordInputPresent());
        }
    }

    @Test
    public void testHideUsernameKeptAfterPasswordFailuresBruteForceEnabled() {
        realm.admin().organizations().get(createOrganization().getId());

        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setBruteForceProtected(true);
        realmRep.setBruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE);
        realmRep.setFailureFactor(1);
        realmRep.setMaxDeltaTimeSeconds(30);
        realmRep.setMaxFailureWaitSeconds(30);
        realmRep.setWaitIncrementSeconds(30);
        realm.admin().update(realmRep);
        realm.cleanup().add(r -> {
            RealmRepresentation rep = r.toRepresentation();
            rep.setBruteForceProtected(false);
            rep.setRegistrationEmailAsUsername(false);
            r.update(rep);
        });

        String email = "existing-user@" + organizationName + ".org";
        UserRepresentation user = UserBuilder.create()
                .username("existing-user")
                .password(memberPassword)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(user)) {
            String userId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.users().get(userId).remove());
        }

        openIdentityFirstLoginPage(email, false, null, false, false);
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());

        loginPage.fillPassword("wrong-password");
        loginPage.submit();
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());
        loginPage.fillPassword("wrong-password");
        loginPage.submit();
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());

        openIdentityFirstLoginPage(email, false, null, false, false);
        realmRep.setRegistrationEmailAsUsername(true);
        realm.admin().update(realmRep);
        loginPage.fillPassword("wrong-password");
        loginPage.submit();
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());
        loginPage.fillPassword("wrong-password");
        loginPage.submit();
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testUsernameExposureWhenEnteringEmail() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        UserRepresentation member = UserBuilder.create()
                .username("secretusername123")  // Different from email
                .email("contractor@contractor.org")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .password(memberPassword)
                .build();

        String memberId;
        try (Response response = realm.admin().users().create(member)) {
            memberId = ApiUtil.getCreatedId(response);
        }
        realm.admin().users().get(memberId).resetPassword(CredentialBuilder.password(memberPassword).build());
        organization.members().addMember(memberId).close();
        String memberIdFinal = memberId;
        realm.cleanup().add(r -> r.users().get(memberIdFinal).remove());

        // Enter the email address in the login form
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);

        // when we enter an email, the attempted username should show the email, not the actual username of the resolved user account
        assertNotNull(loginPage.getAttemptedUsername());
        String displayedUsername = loginPage.getAttemptedUsername();

        assertEquals(member.getEmail(), displayedUsername, "Entering email should not expose actual username");

        // Enter email with different case (should still work with case-insensitive comparison)
        String upperCaseEmail = member.getEmail().toUpperCase();
        openIdentityFirstLoginPage(upperCaseEmail, false, null, false, false);

        assertNotNull(loginPage.getAttemptedUsername());
        String displayedUsernameUpper = loginPage.getAttemptedUsername();
        assertEquals(upperCaseEmail, displayedUsernameUpper, "Should show what user entered (uppercase email)");

        assertTrue(loginPage.isPasswordInputPresent(), "Password input should be present");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSwitchOrganizationDuringLogin() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource, memberEmail, "John", "Doe");
        orgBResource.members().addMember(member.getId()).close();

        // login with "organization" scope (ANY) to trigger org selection
        oauth.scope("organization");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();

        // org selection page should be shown
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));

        // select org A
        selectOrganizationPage.selectOrganization(orgA.getAlias());

        // should be on the password page now
        assertNotNull(loginPage.getAttemptedUsername());
        assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should be available
        assertTrue(loginPage.isSwitchOrganizationPresent());

        // click switch organization
        loginPage.clickSwitchOrganization();

        // org selection page should be shown again
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));

        // select org B this time
        selectOrganizationPage.selectOrganization(orgB.getAlias());

        // should be on the password page again
        assertTrue(loginPage.isPasswordInputPresent());

        // complete login
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();

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
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        UserRepresentation member = addMember(orgResource);

        // login with "organization" scope — single org member should NOT see org selection
        oauth.scope("organization");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();

        // should go directly to password page (no org selection for single-org users)
        assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        assertFalse(loginPage.isSwitchOrganizationPresent());
    }

    @Test
    public void testOrganizationClientScopeDefault() {
        OrganizationRepresentation org = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        UserRepresentation member = addMember(orgResource, memberEmail, "John", "Doe");

        // add organization as default scope to the client
        final String testAppId = oauth.clientResource().toRepresentation().getId();
        final String orgScopeId = AdminApiUtil.findClientScopeByName(realm.admin(), OAuth2Constants.ORGANIZATION).toRepresentation().getId();
        oauth.clientResource().removeOptionalClientScope(orgScopeId);
        oauth.clientResource().addDefaultClientScope(orgScopeId);
        realm.cleanup().add(r -> {
            ClientResource res = r.clients().get(testAppId);
            res.removeDefaultClientScope(orgScopeId);
            res.addOptionalClientScope(orgScopeId);
        });

        // set the organization scope although the client scope is default
        oauth.scope("organization");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getUsername());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertLoginSuccess();
    }

    @Test
    public void testSwitchOrganizationNotAvailableWithSpecificScope() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with specific organization scope — no org selection should be shown
        oauth.scope("organization:" + orgA.getAlias());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();

        // should go directly to password page (specific org requested by client)
        assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        assertFalse(loginPage.isSwitchOrganizationPresent());
    }

    @Test
    public void testSwitchOrganizationNotAvailableWithWildcardScope() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with wildcard organization scope — no org selection should be shown
        oauth.scope("organization:*");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();

        // should go directly to password page (all orgs mapped)
        assertTrue(loginPage.isPasswordInputPresent());

        // switch organization link should NOT be available
        assertFalse(loginPage.isSwitchOrganizationPresent());
    }

    @Test
    public void testUsernamePreservedAfterSwitchOrganization() {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());
        UserRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // login with "organization" scope to trigger org selection
        oauth.scope("organization");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();

        // select org A
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgA.getAlias());

        // verify username is shown on password page
        assertNotNull(loginPage.getAttemptedUsername());
        assertThat(loginPage.getAttemptedUsername(), is(member.getEmail()));

        // switch organization
        loginPage.clickSwitchOrganization();

        // select org B
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());

        // username should still be preserved after switching
        assertNotNull(loginPage.getAttemptedUsername());
        assertThat(loginPage.getAttemptedUsername(), is(member.getEmail()));
    }

    // --- Helper methods ---

    private void openIdentityFirstLoginPage(String username, boolean autoIDPRedirect, String idpAlias, boolean isVisible, boolean clickIdp) {
        oauth.openLoginForm();

        assertTrue(loginPage.isUsernameInputPresent());
        assertNull(loginPage.getUsernameInputError());
        assertFalse(loginPage.isPasswordInputPresent());
        assertFalse(loginPage.isSocialButtonPresent(organizationName + "-identity-provider"));
        if (idpAlias != null) {
            if (isVisible) {
                assertTrue(loginPage.isSocialButtonPresent(idpAlias));
            } else {
                assertFalse(loginPage.isSocialButtonPresent(idpAlias));
            }
        }
        loginUsernamePage.fillLoginWithUsernameOnly(username);
        loginUsernamePage.submit();

        if (clickIdp) {
            assertTrue(loginPage.isPasswordInputPresent());
            assertTrue(loginPage.isSocialButtonPresent(idpAlias));
            loginPage.clickSocial(idpAlias);
        }

        if (autoIDPRedirect) {
            assertThat("Driver should be on the provider realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/realms/"));
        } else {
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/realms/" + realm.getName() + "/"));
        }
    }

    private void assertLoginSuccess() {
        assertNotNull(oauth.parseLoginResponse().getCode());
    }

    private void setAuthenticatorConfig(String key, String value) {
        List<AuthenticationExecutionInfoRepresentation> executions = realm.admin().flows().getExecutions("browser");
        for (AuthenticationExecutionInfoRepresentation execution : executions) {
            if (OrganizationAuthenticatorFactory.ID.equals(execution.getProviderId())) {
                String configId = execution.getAuthenticationConfig();
                if (configId != null) {
                    AuthenticatorConfigRepresentation config = realm.admin().flows().getAuthenticatorConfig(configId);
                    config.getConfig().put(key, value);
                    realm.admin().flows().updateAuthenticatorConfig(configId, config);
                } else {
                    AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
                    config.setAlias(OrganizationAuthenticatorFactory.ID + "-config");
                    config.setConfig(new HashMap<>(Map.of(key, value)));
                    try (Response response = realm.admin().flows().newExecutionConfig(execution.getId(), config)) {
                        // config created
                    }
                }
                return;
            }
        }
    }
}
