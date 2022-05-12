/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.forms;

import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.federation.kerberos.AbstractKerberosTest;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.BrowserTabUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.UserActionTokenBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ResetPasswordTest extends AbstractTestRealmKeycloakTest {

    private String userId;
    private UserRepresentation defaultUser;

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RealmBuilder.edit(testRealm)
                .client(org.keycloak.testsuite.util.ClientBuilder.create().clientId("client-user").serviceAccount());
    }

    @Before
    public void setup() {
        log.info("Adding login-test user");
        defaultUser = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .build();

        userId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), defaultUser, "password");
        defaultUser.setId(userId);
        expectedMessagesCount = 0;
        getCleanup().addUserId(userId);
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected AccountUpdateProfilePage account1ProfilePage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private int expectedMessagesCount;

    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void resetPasswordLink() throws IOException, MessagingException {
        String username = "login-test";
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri);

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client("account")
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD)
                .detail(Details.REDIRECT_URI, oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client("account")
                .user(userId).detail(Details.USERNAME, username).assertEvent();

        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, username)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client("account")
                .assertEvent();
        String sessionId = loginEvent.getSessionId();

        account1ProfilePage.assertCurrent();
        account1ProfilePage.logout();

        events.expectLogout(sessionId).user(userId).removeDetail(Details.REDIRECT_URI).assertEvent();

        loginPage.open();

        loginPage.login("login-test", "resetPassword");

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void resetPassword() throws IOException, MessagingException {
        resetPassword("login-test");
    }

    @Test
    public void resetPasswordTwice() throws IOException, MessagingException {
        String changePasswordUrl = resetPassword("login-test");
        events.clear();

        assertSecondPasswordResetFails(changePasswordUrl, oauth.getClientId()); // KC_RESTART doesn't exist, it was deleted after first successful reset-password flow was finished
    }

    @Test
    public void resetPasswordTwiceInNewBrowser() throws IOException, MessagingException {
        String changePasswordUrl = resetPassword("login-test");
        events.clear();

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
        driver.manage().deleteAllCookies();

        assertSecondPasswordResetFails(changePasswordUrl, oauth.getClientId());
    }

    public void assertSecondPasswordResetFails(String changePasswordUrl, String clientId) {
        driver.navigate().to(changePasswordUrl.trim());

        errorPage.assertCurrent();
        assertEquals("Action expired. Please continue with login now.", errorPage.getError());

        events.expect(EventType.RESET_PASSWORD)
          .client(clientId)
          .session((String) null)
          .user(userId)
          .error(Errors.EXPIRED_CODE)
          .assertEvent();
    }

    @Test
    public void resetPasswordWithSpacesInUsername() throws IOException, MessagingException {
        resetPassword(" login-test ");
    }

    @Test
    public void resetPasswordCancelChangeUser() throws IOException, MessagingException {
        initiateResetPasswordFromResetPasswordPage("test-user@localhost");

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).detail(Details.USERNAME, "test-user@localhost")
                .session((String) null)
                .detail(Details.EMAIL, "test-user@localhost").assertEvent();

        loginPage.login("login@test.com", "password");

        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login@test.com").assertEvent();

        String code = oauth.getCurrentQuery().get("code");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, tokenResponse.getStatusCode());
        assertEquals(userId, oauth.verifyToken(tokenResponse.getAccessToken()).getSubject());

        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId()).user(userId).assertEvent();
    }

    @Test
    public void resetPasswordByEmail() throws IOException, MessagingException {
        resetPassword("login@test.com");
    }

    private String resetPassword(String username) throws IOException, MessagingException {
        return resetPassword(username, "resetPassword");
    }

    private String resetPassword(String username, String password) throws IOException, MessagingException {
        initiateResetPasswordFromResetPasswordPage(username);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.USERNAME, username.trim())
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(expectedMessagesCount, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        assertEquals("You need to change your password.", updatePasswordPage.getFeedbackMessage());

        updatePasswordPage.changePassword(password, password);

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, username.trim()).assertEvent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, username.trim()).assertEvent();
        String sessionId = loginEvent.getSessionId();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        loginPage.open();

        loginPage.login("login-test", password);

        loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        sessionId = loginEvent.getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        return changePasswordUrl;
    }

    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException, MessagingException {

        initiateResetPasswordFromResetPasswordPage(username);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).session((String) null)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent();

        assertEquals(expectedMessagesCount, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());


        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        updatePasswordPage.assertCurrent();
        assertEquals(error, updatePasswordPage.getError());
        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }

    private void initiateResetPasswordFromResetPasswordPage(String username) {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();
        
        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
        expectedMessagesCount++;
    }

    @Test
    public void resetPasswordWrongEmail() throws IOException, MessagingException, InterruptedException {
        initiateResetPasswordFromResetPasswordPage("invalid");

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).user((String) null).session((String) null).detail(Details.USERNAME, "invalid").removeDetail(Details.EMAIL).removeDetail(Details.CODE_ID).error("user_not_found").assertEvent();
    }

    @Test
    public void resetPasswordMissingUsername() throws IOException, MessagingException, InterruptedException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("");

        resetPasswordPage.assertCurrent();

        assertEquals("Please specify username.", resetPasswordPage.getUsernameError());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).user((String) null).session((String) null).clearDetails().error("username_missing").assertEvent();

    }

    @Test
    public void resetPasswordExpiredCode() throws IOException, MessagingException, InterruptedException {
        initiateResetPasswordFromResetPasswordPage("login-test");

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .session((String)null)
                .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        try {
            setTimeOffset(360);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShort() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());

        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    // KEYCLOAK-4016
    @Test
    public void resetPasswordExpiredCodeAndAuthSession() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials"); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            log.debug("Removing cookies.");
            driver.manage().deleteAllCookies();
            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials"); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            log.debug("Removing cookies.");
            driver.manage().deleteAllCookies();
            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());
        testRealm().update(realmRep);

        try {
            initiateResetPasswordFromResetPasswordPage("login-test");

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message).replace("&amp;", "&");

            setTimeOffset(70);

            log.debug("Going to reset password URI.");
            driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials"); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
            log.debug("Removing cookies.");
            driver.manage().deleteAllCookies();
            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();
            Assert.assertEquals("Action expired.", errorPage.getError());
            String backToAppLink = errorPage.getBackToApplicationLink();
            Assert.assertTrue(backToAppLink.endsWith("/app/auth"));

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    // KEYCLOAK-5061
    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlow() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getActionTokenGeneratedByUserLifespan());
        realmRep.setActionTokenGeneratedByUserLifespan(60);
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.getLoginFormUrl();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setActionTokenGeneratedByUserLifespan(originalValue.get());
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionLifespan() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).build());
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.getLoginFormUrl();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionMultipleTimeouts() throws IOException, MessagingException, InterruptedException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().resetCredentialsLifespan(60).verifyEmailLifespan(300).build());
        testRealm().update(realmRep);

        try {
            // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
            String loginUrl = oauth.getLoginFormUrl();
            String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

            driver.navigate().to(forgotPasswordUrl);
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            expectedMessagesCount++;

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            resetPasswordPage.assertCurrent();

            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR).error("expired_code").client((String) null).user(userId).session((String) null).clearDetails().detail(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE).assertEvent();
        } finally {
            setTimeOffset(0);

            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void resetPasswordDisabledUser() throws IOException, MessagingException, InterruptedException {
        UserRepresentation user = findUser("login-test");
        try {
            user.setEnabled(false);
            updateUser(user);

            initiateResetPasswordFromResetPasswordPage("login-test");

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.RESET_PASSWORD).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("user_disabled").assertEvent();
        } finally {
            user.setEnabled(true);
            updateUser(user);
        }
    }

    @Test
    public void resetPasswordNoEmail() throws IOException, MessagingException, InterruptedException {
        final String email;

        UserRepresentation user = findUser("login-test");
        email = user.getEmail();

        try {
            user.setEmail("");
            updateUser(user);

            initiateResetPasswordFromResetPasswordPage("login-test");

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.RESET_PASSWORD_ERROR).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("invalid_email").assertEvent();
        } finally {
            user.setEmail(email);
            updateUser(user);
        }
    }

    @Test
    public void resetPasswordWrongSmtp() throws IOException, MessagingException, InterruptedException {
        final String[] host = new String[1];

        Map<String, String> smtpConfig = new HashMap<>();
        smtpConfig.putAll(testRealm().toRepresentation().getSmtpServer());
        host[0] =  smtpConfig.get("host");
        smtpConfig.put("host", "invalid_host");
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> oldSmtp = realmRep.getSmtpServer();

        try {
            realmRep.setSmtpServer(smtpConfig);
            testRealm().update(realmRep);

            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            errorPage.assertCurrent();

            assertEquals("Failed to send email, please try again later.", errorPage.getError());

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD_ERROR).user(userId)
                    .session((String)null)
                    .detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error(Errors.EMAIL_SEND_FAILED).assertEvent();
        } finally {
            // Revert SMTP back
            realmRep.setSmtpServer(oldSmtp);
            testRealm().update(realmRep);
        }
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        testRealm().update(realmRep);
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException, MessagingException {
        setPasswordPolicy("length");

        initiateResetPasswordFromResetPasswordPage("login-test");

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).session((String)null).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("invalid", "invalid");

        assertEquals("Invalid password: minimum length 8.", resetPasswordPage.getErrorMessage());

        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());


        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        String sessionId = loginEvent.getSessionId();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        loginPage.open();

        loginPage.login("login-test", "resetPasswordWithPasswordPolicy");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void resetPasswordWithPasswordHistoryPolicy() throws IOException, MessagingException {
        //Block passwords that are equal to previous passwords. Default value is 3.
        setPasswordPolicy("passwordHistory");

        try {
            setTimeOffset(2000000);
            resetPassword("login-test", "password1");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(4000000);
            resetPassword("login-test", "password2");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(6000000);
            resetPassword("login-test", "password3");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password3", "Invalid password: must not be equal to any of last 3 passwords.");

            setTimeOffset(8000000);
            resetPassword("login-test", "password");
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordLinkOpenedInNewBrowser() throws IOException, MessagingException {
        resetPasswordLinkOpenedInNewBrowser(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    }


    private void resetPasswordLinkOpenedInNewBrowser(String expectedSystemClientId) throws IOException, MessagingException {
        String username = "login-test";
        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri);

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        log.info("Should be at login page again.");
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client(expectedSystemClientId)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        log.debug("Going to reset password URI.");
        driver.navigate().to(resetUri); // This is necessary to delete KC_RESTART cookie that is restricted to /auth/realms/test path
        log.debug("Removing cookies.");
        driver.manage().deleteAllCookies();
        log.debug("Going to URI from e-mail.");
        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());
    }


    // KEYCLOAK-5982
    @Test
    public void resetPasswordLinkOpenedInNewBrowserAndAccountClientRenamed() throws IOException, MessagingException {
        // Temporarily rename client "account" . Revert it back after the test
        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .setClientId("account-changed")
                .update()) {

            // Assert resetPassword link opened in new browser works even if client "account" not available
            resetPasswordLinkOpenedInNewBrowser(SystemClientUtil.SYSTEM_CLIENT_ID);

        }
    }

    @Test
    public void resetPasswordLinkNewBrowserSessionPreserveClient() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver2.navigate().to(changePasswordUrl.trim());

        changePasswordOnUpdatePage(driver2);

        assertThat(driver2.getCurrentUrl(), Matchers.containsString("client_id=test-app"));

        assertThat(driver2.getPageSource(), Matchers.containsString("Your account has been updated."));
    }


    // KEYCLOAK-15239
    @Test
    public void resetPasswordWithSpnegoEnabled() throws IOException, MessagingException {
        // Just switch SPNEGO authenticator requirement to alternative. No real usage of SPNEGO needed for this test
        AuthenticationExecutionModel.Requirement origRequirement = AbstractKerberosTest.updateKerberosAuthExecutionRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE, testRealm());

        try {
            resetPassword("login-test");
        } finally {
            // Revert
            AbstractKerberosTest.updateKerberosAuthExecutionRequirement(origRequirement, testRealm());
        }
    }


    @Test
    public void failResetPasswordServiceAccount() {
        String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "client-user";
        UserRepresentation serviceAccount = testRealm().users()
                .search(username).get(0);

        serviceAccount.toString();

        UserResource serviceAccount1 = testRealm().users().get(serviceAccount.getId());

        serviceAccount = serviceAccount1.toRepresentation();
        serviceAccount.setEmail("client-user@test.com");
        serviceAccount1.update(serviceAccount);

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
        driver.navigate().to(resetUri);

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("Invalid username or password.", errorPage.getError());
    }

    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void resetPasswordLinkNewTabAndProperRedirectAccount() throws IOException {
        final String REQUIRED_URI = OAuthClient.AUTH_SERVER_ROOT + "/realms/test/account/applications";
        final String REDIRECT_URI = getAccountRedirectUrl() + "?path=applications";
        final String CLIENT_ID = "account";
        final String ACCOUNT_MANAGEMENT_TITLE = getProjectName() + " Account Management";

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));

            driver.navigate().to(REQUIRED_URI);
            resetPasswordTwiceInNewTab(defaultUser, CLIENT_ID, false, REDIRECT_URI, REQUIRED_URI);
            assertThat(driver.getTitle(), Matchers.equalTo(ACCOUNT_MANAGEMENT_TITLE));

            account1ProfilePage.assertCurrent();
            account1ProfilePage.logout();

            driver.navigate().to(REQUIRED_URI);
            resetPasswordTwiceInNewTab(defaultUser, CLIENT_ID, true, REDIRECT_URI, REQUIRED_URI);
            assertThat(driver.getTitle(), Matchers.equalTo(ACCOUNT_MANAGEMENT_TITLE));
        }
    }

    @Test
    public void resetPasswordLinkNewTabAndProperRedirectClient() throws IOException {
        final String REDIRECT_URI = getAuthServerRoot() + "realms/master/app/auth";
        final String CLIENT_ID = "test-app";

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver);
             ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(getAdminClient(), TEST_REALM_NAME, CLIENT_ID)
                     .filterRedirectUris(uri -> uri.contains(REDIRECT_URI))
                     .update()) {

            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));

            loginPage.open();
            resetPasswordTwiceInNewTab(defaultUser, CLIENT_ID, false, REDIRECT_URI);
            assertThat(driver.getCurrentUrl(), Matchers.containsString(REDIRECT_URI));

            String logoutUrl = oauth.getLogoutUrl().build();
            driver.navigate().to(logoutUrl);
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            loginPage.open();
            resetPasswordTwiceInNewTab(defaultUser, CLIENT_ID, true, REDIRECT_URI);
            assertThat(driver.getCurrentUrl(), Matchers.containsString(REDIRECT_URI));
        }
    }

    @Test
    public void resetPasswordInfoMessageWithDuplicateEmailsAllowed() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Boolean originalLoginWithEmailAllowed = realmRep.isLoginWithEmailAllowed();
        Boolean originalDuplicateEmailsAllowed = realmRep.isDuplicateEmailsAllowed();

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            assertEquals("Enter your username or email address and we will send you instructions on how to create a new password.", resetPasswordPage.getInfoMessage());

            realmRep.setLoginWithEmailAllowed(false);
            realmRep.setDuplicateEmailsAllowed(true);
            testRealm().update(realmRep);

            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            assertEquals("Enter your username and we will send you instructions on how to create a new password.", resetPasswordPage.getInfoMessage());
        } finally {
            realmRep.setLoginWithEmailAllowed(originalLoginWithEmailAllowed);
            realmRep.setDuplicateEmailsAllowed(originalDuplicateEmailsAllowed);
            testRealm().update(realmRep);
        }

    }

    // KEYCLOAK-15170
    @Test
    public void changeEmailAddressAfterSendingEmail() throws IOException {
        initiateResetPasswordFromResetPasswordPage(defaultUser.getUsername());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        UserResource user = testRealm().users().get(defaultUser.getId());
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEmail("vmuzikar@redhat.com");
        user.update(userRep);

        driver.navigate().to(changePasswordUrl.trim());
        errorPage.assertCurrent();
        assertEquals("Invalid email address.", errorPage.getError());
    }

    @Test
    public void resetPasswordWithLoginHint() throws IOException {
        // Redirect directly to KC "forgot password" endpoint instead of "authenticate" endpoint
        String loginUrl = oauth.getLoginFormUrl();
        String forgotPasswordUrl = loginUrl.replace("/auth?", "/forgot-credentials?"); // Workaround, but works

        driver.navigate().to(forgotPasswordUrl + "&login_hint=test");
        resetPasswordPage.assertCurrent();

        assertEquals("test", resetPasswordPage.getUsername());
    }

    private void changePasswordOnUpdatePage(WebDriver driver) {
        assertThat(driver.getPageSource(), Matchers.containsString("You need to change your password."));

        final WebElement newPassword = driver.findElement(By.id("password-new"));
        newPassword.sendKeys("resetPassword");
        final WebElement confirmPassword = driver.findElement(By.id("password-confirm"));
        confirmPassword.sendKeys("resetPassword");
        final WebElement submit = driver.findElement(By.cssSelector("input[type=\"submit\"]"));

        submit.click();
    }

    private void resetPasswordTwiceInNewTab(UserRepresentation user, String clientId, boolean shouldLogOut, String redirectUri) throws IOException {
        resetPasswordTwiceInNewTab(user, clientId, shouldLogOut, redirectUri, redirectUri);
    }

    private void resetPasswordTwiceInNewTab(UserRepresentation user, String clientId, boolean shouldLogOut, String redirectUri, String requiredUri) throws IOException {
        events.clear();
        updateForgottenPassword(user, clientId, redirectUri, requiredUri);

        if (shouldLogOut) {
            String sessionId = events.expectLogin().user(user.getId()).detail(Details.USERNAME, user.getUsername())
                    .detail(Details.REDIRECT_URI, redirectUri)
                    .client(clientId)
                    .assertEvent().getSessionId();

            String logoutUrl = oauth.getLogoutUrl().build();
            driver.navigate().to(logoutUrl);
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            events.expectLogout(sessionId).user(user.getId()).removeDetail(Details.REDIRECT_URI).assertEvent();
        }

        BrowserTabUtil util = BrowserTabUtil.getInstanceAndSetEnv(driver);
        assertThat(util.getCountOfTabs(), Matchers.equalTo(2));
        util.closeTab(1);
        assertThat(util.getCountOfTabs(), Matchers.equalTo(1));

        if (shouldLogOut) {
            final ClientRepresentation client = testRealm().clients()
                    .findByClientId(clientId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            assertThat(client, Matchers.notNullValue());
            updateForgottenPassword(user, clientId, getValidRedirectUriWithRootUrl(client.getRootUrl(), client.getRedirectUris()));
        } else {
            doForgotPassword(user.getUsername());
        }
    }

    private void updateForgottenPassword(UserRepresentation user, String clientId, String redirectUri) throws IOException {
        updateForgottenPassword(user, clientId, redirectUri, redirectUri);
    }

    private void updateForgottenPassword(UserRepresentation user, String clientId, String redirectUri, String requiredUri) throws IOException {
        final int emailCount = greenMail.getReceivedMessages().length;

        doForgotPassword(user.getUsername());
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(user.getId())
                .client(clientId)
                .detail(Details.REDIRECT_URI, redirectUri)
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.EMAIL, user.getEmail())
                .session((String) null)
                .assertEvent();

        assertEquals(emailCount + 1, greenMail.getReceivedMessages().length);

        final MimeMessage message = greenMail.getReceivedMessages()[emailCount];
        final String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        BrowserTabUtil util = BrowserTabUtil.getInstanceAndSetEnv(driver);
        util.newTab(changePasswordUrl.trim());

        changePasswordOnUpdatePage(driver);

        events.expectRequiredAction(EventType.UPDATE_PASSWORD)
                .detail(Details.REDIRECT_URI, redirectUri)
                .client(clientId)
                .user(user.getId()).detail(Details.USERNAME, user.getUsername()).assertEvent();

        assertThat(driver.getCurrentUrl(), Matchers.containsString(requiredUri));
    }

    private void doForgotPassword(String username) {
        loginPage.assertCurrent();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);
        WaitUtils.waitForPageToLoad();
    }

    private String getValidRedirectUriWithRootUrl(String rootUrl, Collection<String> redirectUris) {
        final boolean isRootUrlValid = isValidUrl(rootUrl);

        return redirectUris.stream()
                .map(uri -> isRootUrlValid && uri.startsWith("/") ? rootUrl + uri : uri)
                .map(uri -> uri.startsWith("/") ? OAuthClient.AUTH_SERVER_ROOT + uri : uri)
                .map(RedirectUtils::validateRedirectUriWildcard)
                .findFirst()
                .orElse(null);
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
