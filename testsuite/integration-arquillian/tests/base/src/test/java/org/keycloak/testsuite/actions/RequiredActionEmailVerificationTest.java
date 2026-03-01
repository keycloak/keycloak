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
package org.keycloak.testsuite.actions;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.cluster.AuthenticationSessionFailoverClusterTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.pages.VerifyProfilePage;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.UserActionTokenBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.keycloak.authentication.requiredactions.VerifyEmail.EMAIL_RESEND_COOLDOWN_KEY_PREFIX;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionEmailVerificationTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected VerifyProfilePage verifyProfilePage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected ProceedPage proceedPage;

    @Page
    protected ErrorPage errorPage;

    private String testUserId;

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setVerifyEmail(Boolean.TRUE);
        ActionUtil.findUserInRealmRep(testRealm, "test-user@localhost").setEmailVerified(Boolean.FALSE);
    }

    @Before
    public void before() {
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .firstName("test-user")
                .lastName("test-user")
                .emailVerified(false)
                .email("test-user@localhost").build();
        testUserId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

        clearCooldownForUser();
    }

    private void clearCooldownForUser() {
        String cooldownKey = EMAIL_RESEND_COOLDOWN_KEY_PREFIX + testUserId;
        testingClient.server().run(session -> session.singleUseObjects().remove(cooldownKey));
    }

    protected boolean removeVerifyProfileAtImport() {
        // in this test verify profile is enabled
        return false;
    }

    /**
     * see KEYCLOAK-4163
     */
    @Test
    public void verifyEmailConfig() throws MessagingException {

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        // see testsuite/integration-arquillian/tests/base/src/test/resources/testrealm.json
        Assert.assertEquals("<auto+bounces@keycloak.org>", message.getHeader("Return-Path")[0]);
        // displayname <email@example.org>
        Assert.assertEquals("Keycloak SSO <auto@keycloak.org>", message.getHeader("From")[0]);
        Assert.assertEquals("Keycloak no-reply <reply-to@keycloak.org>", message.getHeader("Reply-To")[0]);
    }

    @Test
    public void verifyEmailExisting() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        EventRepresentation sendEvent = emailEvent.assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        driver.navigate().to(verificationUrl.trim());

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
          .user(testUserId)
          .detail(Details.USERNAME, "test-user@localhost")
          .detail(Details.EMAIL, "test-user@localhost")
          .detail(Details.CODE_ID, mailCodeId)
          .assertEvent();

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(testUserId).session(mailCodeId).detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void verifyEmailRegister() throws IOException {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "email@mail.com", "verifyEmail", "password", "password");

        String userId = events.expectRegister("verifyEmail", "email@mail.com").assertEvent().getUserId();

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).user(userId).detail(Details.USERNAME, "verifyemail").detail("email", "email@mail.com").assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        String verificationUrl = getEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
          .user(userId)
          .detail(Details.USERNAME, "verifyemail")
          .detail(Details.EMAIL, "email@mail.com")
          .detail(Details.CODE_ID, mailCodeId)
          .assertEvent();

        events.expectLogin().user(userId).session(mailCodeId).detail(Details.USERNAME, "verifyemail").assertEvent();
    }

    @Test
    public void verifyEmailRegisterSetLocale() throws IOException {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setInternationalizationEnabled(true);
        realm.setSupportedLocales(Set.of("en", "pt"));
        testRealm().update(realm);
        loginPage.open();
        loginPage.clickRegister();
        loginPage.openLanguage("Português");
        registerPage.register("firstName", "lastName", "locale@mail.com", "locale", "password", "password");

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String verificationUrl = getEmailLink(message);

        driver.manage().deleteAllCookies();
        driver.navigate().to(verificationUrl.trim());
        assertTrue(driver.getPageSource().contains("Confirme a validade do endereço"));
    }

    @Test
    public void verifyEmailFromAnotherAccountWhenUserIsAuthenticated() throws Exception {
        loginPage.open();
        loginPage.clickRegister();
        String username1 = KeycloakModelUtils.generateId();
        registerPage.register("firstName", "lastName", username1 + "@mail.com", username1, "password", "password");
        verifyEmailPage.assertCurrent();
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String verificationLink1 = getEmailLink(message);

        loginPage.open();
        loginPage.clickRegister();
        String username2 = KeycloakModelUtils.generateId();
        registerPage.register("firstName", "lastName", username2 + "@mail.com", username2, "password", "password");
        verifyEmailPage.assertCurrent();
        Assert.assertEquals(2, greenMail.getReceivedMessages().length);
        message = greenMail.getReceivedMessages()[1];
        String verificationLink2 = getEmailLink(message);
        driver.navigate().to(verificationLink2.trim());
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        driver.navigate().to(verificationLink1.trim());
        assertTrue(errorPage.getError().contains("You are already authenticated as different user"));
        UserRepresentation user1 = testRealm().users().search(username1).get(0);
        UserRepresentation user2 = testRealm().users().search(username2).get(0);
        assertFalse(user1.isEmailVerified());
        assertTrue(user2.isEmailVerified());
    }

    @Test
    public void verifyEmailFromAnotherAccountAfterEmalIsVerified() throws Exception {
        loginPage.open();
        loginPage.clickRegister();
        String username1 = KeycloakModelUtils.generateId();
        registerPage.register("firstName", "lastName", username1 + "@mail.com", username1, "password", "password");
        verifyEmailPage.assertCurrent();
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String verificationLink1 = getEmailLink(message);

        loginPage.open();
        loginPage.clickRegister();
        String username2 = KeycloakModelUtils.generateId();
        registerPage.register("firstName", "lastName", username2 + "@mail.com", username2, "password", "password");
        verifyEmailPage.assertCurrent();
        Assert.assertEquals(2, greenMail.getReceivedMessages().length);
        message = greenMail.getReceivedMessages()[1];
        String verificationLink2 = getEmailLink(message);

        driver.navigate().to(verificationLink1.trim());
        driver.navigate().to(verificationLink2.trim());
        assertTrue(errorPage.getError().contains("You are already authenticated as different user"));
    }

    @Test
    public void verifyEmailResend() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail("email", "test-user@localhost")
          .assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        clearCooldownForUser();
        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail(Details.CODE_ID, mailCodeId)
          .detail("email", "test-user@localhost")
          .assertEvent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();
        String verificationUrl = getEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
          .user(testUserId)
          .detail(Details.USERNAME, "test-user@localhost")
          .detail(Details.EMAIL, "test-user@localhost")
          .detail(Details.CODE_ID, mailCodeId)
          .assertEvent();

        events.expectLogin().user(testUserId).session(mailCodeId).detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void verifyEmailResendTooFast() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        verifyEmailPage.clickResendEmail();
        assertThat(verifyEmailPage.getFeedbackText(), Matchers.containsString("You must wait"));
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        try {
            setTimeOffset(40);
            verifyEmailPage.clickResendEmail();
            Assert.assertEquals(2, greenMail.getReceivedMessages().length);
        } finally {
            setTimeOffset(0);
        }

    }

    @Test
    public void verifyEmailResendWithRefreshes() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();
        driver.navigate().refresh();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail("email", "test-user@localhost")
          .assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        clearCooldownForUser();
        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();
        driver.navigate().refresh();

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail(Details.CODE_ID, mailCodeId)
          .detail("email", "test-user@localhost")
          .assertEvent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();
        String verificationUrl = getEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
          .user(testUserId)
          .detail(Details.USERNAME, "test-user@localhost")
          .detail(Details.EMAIL, "test-user@localhost")
          .detail(Details.CODE_ID, mailCodeId)
          .assertEvent();

        events.expectLogin().user(testUserId).session(mailCodeId).detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void verifyEmailResendFirstStillValidEvenWithSecond() throws IOException {
        // Email verification can be performed any number of times
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        clearCooldownForUser();
        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message1 = greenMail.getReceivedMessages()[0];

        String verificationUrl1 = getEmailLink(message1);

        driver.navigate().to(verificationUrl1.trim());

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        MimeMessage message2 = greenMail.getReceivedMessages()[1];

        String verificationUrl2 = getEmailLink(message2);

        events.clear();
        driver.navigate().to(verificationUrl2.trim());
        events.expectRequiredAction(EventType.VERIFY_EMAIL)
                .error(Errors.EMAIL_ALREADY_VERIFIED)
                .detail(Details.REDIRECT_URI, Matchers.any(String.class))
                .assertEvent();
        infoPage.assertCurrent();
        Assert.assertEquals("Your email address has been verified already.", infoPage.getInfo());
    }

    @Test
    public void verifyEmailResendFirstAndSecondStillValid() throws IOException {
        // Email verification can be performed any number of times
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        clearCooldownForUser();
        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message1 = greenMail.getReceivedMessages()[0];

        String verificationUrl1 = getEmailLink(message1);

        driver.navigate().to(verificationUrl1.trim());

        AccountHelper.logout(testRealm(), "test-user@localhost");

        MimeMessage message2 = greenMail.getReceivedMessages()[1];

        String verificationUrl2 = getEmailLink(message2);

        driver.navigate().to(verificationUrl2.trim());

        assertEquals("Your email address has been verified already.", infoPage.getInfo());
    }

    @Test
    public void verifyEmailResendAndVerifyWithLatestLink() throws IOException {
        // Email verification can be performed any number of times
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        clearCooldownForUser();
        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();
        Assert.assertEquals(2, greenMail.getReceivedMessages().length);
        MimeMessage message1 = greenMail.getReceivedMessages()[0];
        String verificationUrl1 = getEmailLink(message1);

        MimeMessage message2 = greenMail.getReceivedMessages()[1];
        String verificationUrl2 = getEmailLink(message2);
        driver.navigate().to(verificationUrl2.trim());
        appPage.assertCurrent();

        driver.navigate().to(verificationUrl1.trim());
        assertEquals("Your email address has been verified already.", infoPage.getInfo());
    }

    @Test
    public void verifyEmailNewBrowserSession() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        EventRepresentation sendEvent = emailEvent.assertEvent();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        driver.manage().deleteAllCookies();

        driver.navigate().to(verificationUrl.trim());
        proceedPage.assertCurrent();
        proceedPage.clickProceedLink();
        infoPage.assertCurrent();

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
          .user(testUserId)
          .detail(Details.CODE_ID, Matchers.not(Matchers.is(mailCodeId)))
          .client(oauth.getClientId())   // the "test-app" client specified in loginPage.open() is expected
          .detail(Details.REDIRECT_URI, Matchers.any(String.class))
          .assertEvent();

        infoPage.assertCurrent();
        assertEquals("Your email address has been verified.", infoPage.getInfo());

        loginPage.open();
        loginPage.assertCurrent();
    }

    @Test
    public void verifyEmailInvalidKeyInVerficationLink() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        verificationUrl = KeycloakUriBuilder.fromUri(verificationUrl).replaceQueryParam(Constants.KEY, "foo").build().toString();

        events.poll();

        driver.navigate().to(verificationUrl.trim());

        errorPage.assertCurrent();
        assertEquals("An error occurred, please login again through your application.", errorPage.getError());

        events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();
    }

    @Test
    public void verifyEmailExpiredCode() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        events.poll();

        try {
            setTimeOffset(360);

            driver.navigate().to(verificationUrl.trim());

            loginPage.assertCurrent();
            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                    .error(Errors.EXPIRED_CODE)
                    .client((String)null)
                    .user(testUserId)
                    .session((String)null)
                    .clearDetails()
                    .detail(Details.ACTION, VerifyEmailActionToken.TOKEN_TYPE)
                    .assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void verifyEmailExpiredCodedPerActionLifespan() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Map.copyOf(realmRep.getAttributes());

        realmRep.setAttributes(UserActionTokenBuilder.create().verifyEmailLifespan(60).build());
        testRealm().update(realmRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        events.poll();

        try {
            setTimeOffset(70);

            driver.navigate().to(verificationUrl.trim());

            loginPage.assertCurrent();
            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                    .error(Errors.EXPIRED_CODE)
                    .client((String)null)
                    .user(testUserId)
                    .session((String)null)
                    .clearDetails()
                    .detail(Details.ACTION, VerifyEmailActionToken.TOKEN_TYPE)
                    .assertEvent();
        } finally {
            setTimeOffset(0);
            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void verifyEmailExpiredCodedPerActionMultipleTimeouts() throws IOException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Map.copyOf(realmRep.getAttributes());

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().verifyEmailLifespan(60).resetCredentialsLifespan(300).build());
        testRealm().update(realmRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        events.poll();

        try {
            setTimeOffset(70);

            driver.navigate().to(verificationUrl.trim());

            loginPage.assertCurrent();
            assertEquals("Action expired. Please start again.", loginPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                    .error(Errors.EXPIRED_CODE)
                    .client((String)null)
                    .user(testUserId)
                    .session((String)null)
                    .clearDetails()
                    .detail(Details.ACTION, VerifyEmailActionToken.TOKEN_TYPE)
                    .assertEvent();
        } finally {
            setTimeOffset(0);
            realmRep.setAttributes(originalAttributes);
            testRealm().update(realmRep);
        }
    }

    @Test
    public void verifyEmailExpiredCodeAndExpiredSession() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        events.poll();

        try {
            setTimeOffset(3600);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl.trim());

            errorPage.assertCurrent();
            assertEquals("Action expired.", errorPage.getError());

            events.expectRequiredAction(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                    .error(Errors.EXPIRED_CODE)
                    .client((String)null)
                    .user(testUserId)
                    .session((String)null)
                    .clearDetails()
                    .detail(Details.ACTION, VerifyEmailActionToken.TOKEN_TYPE)
                    .assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }


    public static String getEmailLink(MimeMessage message) throws IOException {
        return MailUtils.getPasswordResetEmailLink(message);
    }

    // https://issues.jboss.org/browse/KEYCLOAK-5861
    @Test
    public void verifyEmailNewBrowserSessionWithClientRedirect() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
          .setEmailVerified(false)
          .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(List.of(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl.trim());
            proceedPage.assertCurrent();
            proceedPage.clickProceedLink();

            infoPage.assertCurrent();
            assertEquals("Your account has been updated.", infoPage.getInfo());

            // Now login to app
            TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
            testAppHelper.login("test-user@localhost", "password");
            appPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailNewBrowserSessionPreserveClient() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        // open link in the second browser without the session
        driver2.navigate().to(verificationUrl.trim());

        // follow the link
        final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
        assertThat(proceedLink, Matchers.notNullValue());

        // check if the initial client is preserved
        String link = proceedLink.getAttribute("href");
        assertThat(link, Matchers.containsString("client_id=test-app"));
        proceedLink.click();

        // confirmation in the second browser
        assertThat(driver2.getPageSource(), Matchers.containsString("kc-info-message"));
        assertThat(driver2.getPageSource(), Matchers.containsString("Your email address has been verified."));

        final WebElement backToApplicationLink = driver2.findElement(By.linkText("« Back to Application"));
        assertThat(backToApplicationLink, Matchers.notNullValue());
    }

    @Test
    public void verifyEmailDuringAuthFlow() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(false)
                .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                .update()) {

            loginPage.open();
            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            verifyEmailPage.assertCurrent();

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getEmailLink(message);

            driver.navigate().to(verificationUrl.trim());

            appPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowFirstClickLink() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(false)
                .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(List.of(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl);

            loginPage.open();
            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            verifyEmailPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailClickLinkRequiredActionsCleared() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .setRequiredActions()
                .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(List.of(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl);

            loginPage.open();
            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            appPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowAfterLogout() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .update()) {

            TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
            testAppHelper.login("test-user@localhost", "password");
            appPage.assertCurrent();

            testAppHelper.logout();
            appPage.assertCurrent();

            verifyEmailDuringAuthFlow();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowAfterRefresh() throws IOException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .update()) {
            final String testRealmName = testRealm().toRepresentation().getRealm();

            // Browser 1: Log in
            TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
            testAppHelper.login("test-user@localhost", "password");
            appPage.assertCurrent();

            // Browser 2: Log in
            driver2.navigate().to(oauth.loginForm().build());

            assertThat(driver2.getTitle(), is("Sign in to " + testRealmName));
            driver2.findElement(By.id("username")).sendKeys("test-user@localhost");
            driver2.findElement(By.id("password")).sendKeys("password");
            driver2.findElement(By.id("password")).submit();

            assertThat(driver2.getCurrentUrl(), Matchers.startsWith(OAuthClient.APP_AUTH_ROOT));

            // Admin: set required action to VERIFY_EMAIL
            try (Closeable u1 = new UserAttributeUpdater(testRealm().users().get(testUserId))
                    .setEmailVerified(false)
                    .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                    .update()) {
                // Browser 2: Refresh window
                driver2.navigate().refresh();
                assertThat(driver2.getCurrentUrl(), Matchers.startsWith(OAuthClient.APP_AUTH_ROOT));

                // Browser 1: Logout
                testAppHelper.logout();
                appPage.assertCurrent();


                // Browser 1: Log in
                testAppHelper.login("test-user@localhost", "password");

                verifyEmailPage.assertCurrent();

                // Browser 2 [still logged in]: Click the email verification link
                Assert.assertEquals(1, greenMail.getReceivedMessages().length);
                MimeMessage message = greenMail.getLastReceivedMessage();

                String verificationUrl = getEmailLink(message);

                driver2.navigate().to(verificationUrl.trim());

                // Browser 2: Confirm email belongs to the user
                final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
                assertThat(proceedLink, Matchers.notNullValue());
                proceedLink.click();

                // Browser 2: Expect confirmation
                assertThat(driver2.getPageSource(), Matchers.containsString("kc-info-message"));
                assertThat(driver2.getPageSource(), Matchers.containsString("Your email address has been verified."));

                // Browser 1: Expect land back to app after refresh
                driver.navigate().refresh();
                appPage.assertCurrent();
            }
        }
    }

    @Test
    public void verifyEmailWhileLoggedIn() throws IOException {
        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(true).update();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        userAttributeUpdater.setEmailVerified(false).setRequiredActions(RequiredAction.VERIFY_EMAIL).update();

        // this will result in email verification
        loginPage.open();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        // confirm
        driver.navigate().to(verificationUrl);

        // back to app, already logged in
        appPage.assertCurrent();

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        assertTrue(user.isEmailVerified());
        assertThat(user.getRequiredActions(), Matchers.empty());
    }

    @Test
    public void verifyEmailViaAuthSessionWhileLoggedIn() throws IOException {
        Assume.assumeTrue("Works only on auth-server-undertow",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT));

        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(false).update();

        loginPage.open();

        String authSessionId = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);
        String realmId = testRealm().toRepresentation().getId();
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            RootAuthenticationSessionModel ras = session.authenticationSessions()
                    .getRootAuthenticationSession(realm, new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionId));
            assertThat("Expecting single auth session", ras.getAuthenticationSessions().keySet(), Matchers.hasSize(1));
            ras.getAuthenticationSessions().forEach((id, as) -> as.addRequiredAction(RequiredAction.VERIFY_EMAIL));
        });
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        // confirm
        driver.navigate().to(verificationUrl);

        // back to app, already logged in
        appPage.assertCurrent();

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        assertTrue(user.isEmailVerified());
        assertThat(user.getRequiredActions(), Matchers.empty());
    }

    @Test
    public void verifyEmailInNewBrowserWhileLoggedInFirstBrowser() throws IOException {
        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(true).update();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        userAttributeUpdater.setEmailVerified(false).setRequiredActions(RequiredAction.VERIFY_EMAIL).update();

        // this will result in email verification
        loginPage.open();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        // confirm in the second browser
        driver2.navigate().to(verificationUrl);

        // follow the link
        final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
        assertThat(proceedLink, Matchers.notNullValue());
        proceedLink.click();

        // confirmation in the second browser
        assertThat(driver2.getPageSource(), Matchers.containsString("kc-info-message"));
        assertThat(driver2.getPageSource(), Matchers.containsString("Your email address has been verified."));

        driver2.navigate().to(oauth.loginForm().build());

        // login page should be shown in the second browser
        assertThat(driver2.getPageSource(), Matchers.containsString("kc-login"));
        assertThat(driver2.getPageSource(), Matchers.containsString("Sign in"));

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        assertTrue(user.isEmailVerified());
        assertThat(user.getRequiredActions(), Matchers.empty());

        // after refresh in the first browser the app should be shown
        driver.navigate().refresh();
        appPage.assertCurrent();
    }

    @Test
    public void verifyEmailExpiredRegistration() throws IOException {
        final String COMMON_ATTR = "verifyEmailRegistrationUser";

        driver.navigate().to(oauth.registrationForm().build());

        registerPage.assertCurrent();
        registerPage.register(COMMON_ATTR, COMMON_ATTR, COMMON_ATTR + "@" + COMMON_ATTR, COMMON_ATTR, COMMON_ATTR, COMMON_ATTR);

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getEmailLink(message);

        try {
            setTimeOffset(360);

            driver.navigate().to(verificationUrl.trim());

            loginPage.assertCurrent();
            assertEquals("Action expired. Please start again.", loginPage.getError());
        } finally {
            setTimeOffset(0);
        }
    }

    // KEYCLOAK-15170
    @Test
    public void changeEmailAddressAfterSendingEmail() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];
        String verificationUrl = getEmailLink(message);

        UserResource user = testRealm().users().get(testUserId);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEmail("vmuzikar@redhat.com");
        user.update(userRep);

        driver.navigate().to(verificationUrl.trim());
        errorPage.assertCurrent();
        assertEquals("The link you clicked is an old stale link and is no longer valid. Maybe you have already verified your email.", errorPage.getError());
    }

    @Test
    public void actionTokenWithInvalidRequiredActions() throws IOException {
        // Send email with required action
        testRealm().users().get(testUserId).executeActionsEmail(List.of(RequiredAction.UPDATE_EMAIL.name()));

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        MailUtils.EmailBody body = MailUtils.getBody(message);
        assertThat(body, notNullValue());

        final String link = MailUtils.getLink(body.getText());
        assertThat(link, notNullValue());

        // Disable feature and the required action UPDATE_EMAIL provider is not present
        testingClient.disableFeature(Profile.Feature.UPDATE_EMAIL);

        driver.navigate().to(link);

        errorPage.assertCurrent();

        // Required action included in the action token is not valid anymore, because we don't know the provider for it
        assertThat(errorPage.getError(), is("Required actions included in the link are not valid"));
    }

    @Test
    public void testVerifyEmailWithNoEmailAndVerifyProfile() throws Exception {
        UserResource user = testRealm().users().get(testUserId);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEmail("");
        user.update(userRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        // verify profile should be presented first as the verify email is ignored without email
        verifyProfilePage.assertCurrent();
        events.expectRequiredAction(EventType.VERIFY_PROFILE)
                .user(testUserId)
                .detail(Details.FIELDS_TO_UPDATE, UserModel.EMAIL)
                .assertEvent();

        verifyProfilePage.updateEmail("test-user@localhost", "test-user", "test-user");

        verifyEmailPage.assertCurrent();

        events.expectRequiredAction(EventType.UPDATE_PROFILE)
                .user(testUserId)
                .detail(Details.UPDATED_EMAIL, "test-user@localhost")
                .assertEvent();

        // verify email is presented now
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        final MimeMessage message = greenMail.getLastReceivedMessage();

        final String verificationUrl = getEmailLink(message);

        // confirm
        driver.navigate().to(verificationUrl);

        // back to app, already logged in
        appPage.assertCurrent();

        // email should be verified and required actions empty
        userRep = user.toRepresentation();
        assertTrue(userRep.isEmailVerified());
        assertThat(userRep.getRequiredActions(), Matchers.empty());
    }
}
