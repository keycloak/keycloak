/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Stan Silvert
 */
public class AppInitiatedActionResetPasswordTest extends AbstractAppInitiatedActionTest {

    @Override
    protected String getAiaAction() {
        return UserModel.RequiredAction.UPDATE_PASSWORD.name();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @After
    public void after() {
        ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);

        // reset password required action max auth age back to default
        Optional<RequiredActionProviderRepresentation> passwordRequiredAction = testRealm().flows().getRequiredActions()
                .stream()
                .filter(requiredAction -> requiredAction.getProviderId().equals(UserModel.RequiredAction.UPDATE_PASSWORD.name()))
                .findFirst();
        if (passwordRequiredAction.isPresent()) {
            passwordRequiredAction.get().getConfig().remove(Constants.MAX_AUTH_AGE_KEY);
            testRealm().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name(), passwordRequiredAction.get());
        }

        // remove all required action from the user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.setRequiredActions(Collections.emptyList());
        user.update(userRepresentation);
    }

    @Test
    public void resetPassword() throws Exception {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(testRealm())
                .addEventsListener(EmailEventListenerProviderFactory.ID)
                .update();
             UserAttributeUpdater userUpdater = new UserAttributeUpdater(ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost"))
                .setEmailVerified(true)
                .update()) {

            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            events.expectLogin().assertEvent();

            doAIA();

            changePasswordPage.assertCurrent();
            assertTrue(changePasswordPage.isCancelDisplayed());

            Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
            assertNotNull(authSessionCookie);
            String authSessionId = authSessionCookie.getValue();
            testingClient.server().run(session -> {
                // ensure that our logic to detect the authentication session works as expected
                RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
                session.getContext().setRealm(realm);
                String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionId);
                assertNotNull(session.authenticationSessions().getRootAuthenticationSession(realm, decodedAuthSessionId));
            });

            changePasswordPage.changePassword("new-password", "new-password");

            testingClient.server().run(session -> {
                // ensure that the authentication session has been terminated
                RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
                session.getContext().setRealm(realm);
                String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionId);
                assertNull(session.authenticationSessions().getRootAuthenticationSession(realm, decodedAuthSessionId));
            });

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            Assert.assertEquals(2, receivedMessages.length);

            Assert.assertEquals("Update password", receivedMessages[0].getSubject());
            Assert.assertEquals("Update credential", receivedMessages[1].getSubject());
            MatcherAssert.assertThat(MailUtils.getBody(receivedMessages[1]).getText(),
                    Matchers.startsWith("Your password credential was changed"));
            MatcherAssert.assertThat(MailUtils.getBody(receivedMessages[1]).getHtml(),
                    Matchers.containsString("Your password credential was changed"));

            assertKcActionStatus(SUCCESS);

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
            oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();

            events.expectLogout(loginEvent.getSessionId()).assertEvent();

            loginPage.open();
            loginPage.login("test-user@localhost", "new-password");

            events.expectLogin().assertEvent();
        }
    }

    @Test
    public void resetPasswordRequiresReAuth() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        setTimeOffset(350);

        // Should prompt for re-authentication
        doAIA();

        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.login("password");

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        assertKcActionStatus(SUCCESS);
    }

    @Test
    public void resetPasswordRequiresReAuthWithIndividualMaxAuthAgeConfig() throws Exception {
        // retrieve the password required action
        RequiredActionProviderRepresentation passwordRequiredAction = testRealm().flows().getRequiredActions()
                .stream()
                .filter(requiredAction -> requiredAction.getProviderId().equals(UserModel.RequiredAction.UPDATE_PASSWORD.name()))
                .findFirst()
                .orElseThrow(() -> new Exception("Required action not found"));

        // override default max auth age to 500 seconds for the password required action
        passwordRequiredAction.getConfig().put(Constants.MAX_AUTH_AGE_KEY, "500");
        testRealm().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name(), passwordRequiredAction);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        setTimeOffset(550);

        // Should prompt for re-authentication
        doAIA();

        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.login("password");


        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        assertKcActionStatus(SUCCESS);
    }

    @Test
    public void resetPasswordRequiresNoReAuthWithIndividualMaxAuthAgeConfig() throws Exception {
        // retrieve the password required action
        RequiredActionProviderRepresentation passwordRequiredAction = testRealm().flows().getRequiredActions()
                .stream()
                .filter(requiredAction -> requiredAction.getProviderId().equals(UserModel.RequiredAction.UPDATE_PASSWORD.name()))
                .findFirst()
                .orElseThrow(() -> new Exception("Required action not found"));

        // override default max auth age to 500 seconds for the password required action
        passwordRequiredAction.getConfig().put(Constants.MAX_AUTH_AGE_KEY, "500");
        testRealm().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name(), passwordRequiredAction);


        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        setTimeOffset(350);

        // Should not prompt for re-authentication
        doAIA();

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        assertKcActionStatus(SUCCESS);
    }


    /**
     * See GH-12943
     */
    @Test
    public void resetPasswordRequiresReAuthWithMaxAuthAgePasswordPolicy() {

        // set password policy
        RealmRepresentation currentTestRealmRep = testRealm().toRepresentation();
        String previousPasswordPolicy = currentTestRealmRep.getPasswordPolicy();
        if (previousPasswordPolicy == null) {
            previousPasswordPolicy = "";
        }
        currentTestRealmRep.setPasswordPolicy("maxAuthAge(0)");
        try {
            testRealm().update(currentTestRealmRep);

            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            events.expectLogin().assertEvent();

            // we need to add some slack to avoid timing issues
            setTimeOffset(1);

            // Should prompt for re-authentication due to maxAuthAge password policy
            doAIA();

            loginPage.assertCurrent();

            Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());

            loginPage.login("password");

            changePasswordPage.assertCurrent();
            assertTrue(changePasswordPage.isCancelDisplayed());

            changePasswordPage.changePassword("new-password", "new-password");

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
            events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
            assertKcActionStatus(SUCCESS);
        } finally {
            // reset password policy to previous state
            currentTestRealmRep.setPasswordPolicy(previousPasswordPolicy);
            testRealm().update(currentTestRealmRep);
        }
    }

    @Test
    public void cancelChangePassword() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        changePasswordPage.assertCurrent();
        changePasswordPage.cancel();

        assertKcActionStatus(CANCELLED);

        events.expect(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                .detail(Details.CUSTOM_REQUIRED_ACTION, UserModel.RequiredAction.UPDATE_PASSWORD.name())
                .error(Errors.REJECTED_BY_USER)
                .assertEvent();
        events.expectLogin().assertEvent();
    }

    @Test
    public void cancelWhenOTPRequiredAction() {
        // Add OTP required action to the user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRep = user.toRepresentation();
        UserBuilder.edit(userRep).requiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        user.update(userRep);

        doAIA();
        loginPage.login("test-user@localhost", "password");

        // Cancel button should not be displayed
        totpPage.assertCurrent();
        Assert.assertFalse(totpPage.isCancelDisplayed());

        // Try to manually send POST request from browser with cancel the AIA
        String actionUrl = URLUtils.getActionUrlFromCurrentPage(driver);
        URLUtils.sendPOSTRequestWithWebDriver(actionUrl, Map.of(LoginActionsService.CANCEL_AIA, "true"));

        // Assert OTP required action still on the user
        Assert.assertThat(user.toRepresentation().getRequiredActions(), contains(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
    }

    @Test
    public void resetPasswordUserHasUpdatePasswordRequiredAction() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        UserResource userResource = testRealm().users().get(findUser("test-user@localhost").getId());
        UserRepresentation userRep = userResource.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        userResource.update(userRep);

        events.expectLogin().assertEvent();

        doAIA();

        changePasswordPage.assertCurrent();
        /*
         * cancel should not be supported, because the action is not only application-initiated, but also required by
         * Keycloak
         */
        assertFalse(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

        assertKcActionStatus(SUCCESS);
    }

    @Test
    public void checkLogoutSessions() {
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        List<UserSessionRepresentation> sessions = testUser.getUserSessions();
        assertEquals(1, sessions.size());
        final String firstSessionId = sessions.get(0).getId();

        oauth2.doLogin("test-user@localhost", "password");
        EventRepresentation event2 = events.expectLogin().assertEvent();
        assertEquals(2, testUser.getUserSessions().size());

        doAIA();

        changePasswordPage.assertCurrent();
        changePasswordPage.checkLogoutSessions();
        changePasswordPage.changePassword("All Right Then, Keep Your Secrets", "All Right Then, Keep Your Secrets");
        events.expectLogout(event2.getSessionId()).detail(Details.LOGOUT_TRIGGERED_BY_REQUIRED_ACTION, UserModel.RequiredAction.UPDATE_PASSWORD.name()).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        assertKcActionStatus(SUCCESS);

        sessions = testUser.getUserSessions();
        assertEquals(1, sessions.size());
        assertEquals("Old session is still valid", firstSessionId, sessions.get(0).getId());
    }

    @Test
    public void uncheckLogoutSessions() {
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        oauth2.doLogin("test-user@localhost", "password");
        events.expectLogin().assertEvent();
        assertEquals(2, testUser.getUserSessions().size());

        doAIA();

        changePasswordPage.assertCurrent();
        assertFalse("Logout other sessions was ticked", changePasswordPage.isLogoutSessionsChecked());
        changePasswordPage.changePassword("All Right Then, Keep Your Secrets", "All Right Then, Keep Your Secrets");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        assertKcActionStatus(SUCCESS);

        assertEquals(2, testUser.getUserSessions().size());
    }

}
