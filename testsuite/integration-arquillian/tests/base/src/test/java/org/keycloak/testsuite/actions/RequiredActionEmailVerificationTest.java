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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.cluster.AuthenticationSessionFailoverClusterTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.UserActionTokenBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.Closeable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
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
                .email("test-user@localhost").build();
        testUserId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }

    /**
     * see KEYCLOAK-4163
     */
    @Test
    public void verifyEmailConfig() throws IOException, MessagingException {

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
    public void verifyEmailExisting() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailRegister() throws IOException, MessagingException {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "email@mail.com", "verifyEmail", "password", "password");

        String userId = events.expectRegister("verifyEmail", "email@mail.com").assertEvent().getUserId();

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).user(userId).detail(Details.USERNAME, "verifyemail").detail("email", "email@mail.com").assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailResend() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail("email", "test-user@localhost")
          .assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail(Details.CODE_ID, mailCodeId)
          .detail("email", "test-user@localhost")
          .assertEvent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();
        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailResendWithRefreshes() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();
        driver.navigate().refresh();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail("email", "test-user@localhost")
          .assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();
        driver.navigate().refresh();

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
          .detail(Details.CODE_ID, mailCodeId)
          .detail("email", "test-user@localhost")
          .assertEvent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();
        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailResendFirstStillValidEvenWithSecond() throws IOException, MessagingException {
        // Email verification can be performed any number of times
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message1 = greenMail.getReceivedMessages()[0];

        String verificationUrl1 = getPasswordResetEmailLink(message1);

        driver.navigate().to(verificationUrl1.trim());

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        MimeMessage message2 = greenMail.getReceivedMessages()[1];

        String verificationUrl2 = getPasswordResetEmailLink(message2);

        driver.navigate().to(verificationUrl2.trim());

        infoPage.assertCurrent();
        Assert.assertEquals("You are already logged in.", infoPage.getInfo());
    }

    @Test
    public void verifyEmailResendFirstAndSecondStillValid() throws IOException, MessagingException {
        // Email verification can be performed any number of times
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.clickResendEmail();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message1 = greenMail.getReceivedMessages()[0];

        String verificationUrl1 = getPasswordResetEmailLink(message1);

        driver.navigate().to(verificationUrl1.trim());

        appPage.assertCurrent();
        accountPage.setAuthRealm(AuthRealm.TEST);
        accountPage.navigateTo();
        accountPage.logOut();

        MimeMessage message2 = greenMail.getReceivedMessages()[1];

        String verificationUrl2 = getPasswordResetEmailLink(message2);

        driver.navigate().to(verificationUrl2.trim());

        proceedPage.assertCurrent();
        proceedPage.clickProceedLink();
        infoPage.assertCurrent();
        assertEquals("Your email address has been verified.", infoPage.getInfo());
    }

    @Test
    public void verifyEmailNewBrowserSession() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailInvalidKeyInVerficationLink() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailExpiredCode() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailExpiredCodedPerActionLifespan() throws IOException, MessagingException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        realmRep.setAttributes(UserActionTokenBuilder.create().verifyEmailLifespan(60).build());
        testRealm().update(realmRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailExpiredCodedPerActionMultipleTimeouts() throws IOException, MessagingException {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        Map<String, String> originalAttributes = Collections.unmodifiableMap(new HashMap<>(realmRep.getAttributes()));

        //Make sure that one attribute settings won't affect the other
        realmRep.setAttributes(UserActionTokenBuilder.create().verifyEmailLifespan(60).resetCredentialsLifespan(300).build());
        testRealm().update(realmRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailExpiredCodeAndExpiredSession() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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


    public static String getPasswordResetEmailLink(MimeMessage message) throws IOException, MessagingException {
        return MailUtils.getPasswordResetEmailLink(message);
    }

    // https://issues.jboss.org/browse/KEYCLOAK-5861
    @Test
    public void verifyEmailNewBrowserSessionWithClientRedirect() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
          .setEmailVerified(false)
          .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(Arrays.asList(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getPasswordResetEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl.trim());
            proceedPage.assertCurrent();
            proceedPage.clickProceedLink();

            infoPage.assertCurrent();
            assertEquals("Your account has been updated.", infoPage.getInfo());

            // Now log into account page
            accountPage.setAuthRealm(testRealm().toRepresentation().getRealm());
            accountPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            accountPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailNewBrowserSessionPreserveClient() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
    public void verifyEmailDuringAuthFlow() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(false)
                .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                .update()) {
            accountPage.setAuthRealm(testRealm().toRepresentation().getRealm());
            accountPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            verifyEmailPage.assertCurrent();

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getPasswordResetEmailLink(message);

            driver.navigate().to(verificationUrl.trim());

            accountPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowFirstClickLink() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(false)
                .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(Arrays.asList(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getPasswordResetEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl);

            accountPage.setAuthRealm(testRealm().toRepresentation().getRealm());
            accountPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            verifyEmailPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailClickLinkRequiredActionsCleared() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .setRequiredActions()
                .update()) {
            testRealm().users().get(testUserId).executeActionsEmail(Arrays.asList(RequiredAction.VERIFY_EMAIL.name()));

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getLastReceivedMessage();

            String verificationUrl = getPasswordResetEmailLink(message);

            driver.manage().deleteAllCookies();

            driver.navigate().to(verificationUrl);

            accountPage.setAuthRealm(testRealm().toRepresentation().getRealm());
            accountPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            accountPage.assertCurrent();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowAfterLogout() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .update()) {
            accountPage.setAuthRealm(testRealm().toRepresentation().getRealm());
            accountPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");

            accountPage.assertCurrent();

            accountPage.logOut();
            loginPage.assertCurrent();

            verifyEmailDuringAuthFlow();
        }
    }

    @Test
    public void verifyEmailDuringAuthFlowAfterRefresh() throws IOException, MessagingException {
        try (Closeable u = new UserAttributeUpdater(testRealm().users().get(testUserId))
                .setEmailVerified(true)
                .update()) {
            final String testRealmName = testRealm().toRepresentation().getRealm();
            accountPage.setAuthRealm(testRealmName);

            // Browser 1: Log in
            accountPage.navigateTo();
            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", "password");
            accountPage.assertCurrent();

            // Browser 2: Log in
            driver2.navigate().to(accountPage.buildUri().toString());

            assertThat(driver2.getTitle(), is("Sign in to " + testRealmName));
            driver2.findElement(By.id("username")).sendKeys("test-user@localhost");
            driver2.findElement(By.id("password")).sendKeys("password");
            driver2.findElement(By.id("password")).submit();

            assertThat(driver2.getCurrentUrl(), Matchers.startsWith(accountPage.buildUri().toString()));

            // Admin: set required action to VERIFY_EMAIL
            try (Closeable u1 = new UserAttributeUpdater(testRealm().users().get(testUserId))
                    .setEmailVerified(false)
                    .setRequiredActions(RequiredAction.VERIFY_EMAIL)
                    .update()) {
                // Browser 2: Refresh window
                driver2.navigate().refresh();
                assertThat(driver2.getCurrentUrl(), Matchers.startsWith(accountPage.buildUri().toString()));

                // Browser 1: Logout
                accountPage.logOut();

                // Browser 1: Go to account page
                accountPage.navigateTo();

                // Browser 1: Log in
                loginPage.assertCurrent();
                loginPage.login("test-user@localhost", "password");

                verifyEmailPage.assertCurrent();

                // Browser 2 [still logged in]: Click the email verification link
                Assert.assertEquals(1, greenMail.getReceivedMessages().length);
                MimeMessage message = greenMail.getLastReceivedMessage();

                String verificationUrl = getPasswordResetEmailLink(message);

                driver2.navigate().to(verificationUrl.trim());

                // Browser 2: Confirm email belongs to the user
                final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
                assertThat(proceedLink, Matchers.notNullValue());
                proceedLink.click();

                // Browser 2: Expect confirmation
                assertThat(driver2.getPageSource(), Matchers.containsString("kc-info-message"));
                assertThat(driver2.getPageSource(), Matchers.containsString("Your email address has been verified."));

                // Browser 1: Expect land back to account after refresh
                driver.navigate().refresh();
                accountPage.assertCurrent();
            }
        }
    }

    @Test
    public void verifyEmailWhileLoggedIn() throws IOException, MessagingException {
        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(true).update();

        final String testRealmName = testRealm().toRepresentation().getRealm();
        accountPage.setAuthRealm(testRealmName);
        oauth.realm(testRealmName).clientId("account").redirectUri(getAuthServerRoot() + "realms/" + testRealmName + "/account");
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        accountPage.assertCurrent();

        userAttributeUpdater.setEmailVerified(false).setRequiredActions(RequiredAction.VERIFY_EMAIL).update();

        // this will result in email verification
        loginPage.open();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

        // confirm
        driver.navigate().to(verificationUrl);

        // back to account, already logged in
        accountPage.assertCurrent();

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        Assert.assertTrue(user.isEmailVerified());
        Assert.assertThat(user.getRequiredActions(), Matchers.empty());
    }

    @Test
    public void verifyEmailViaAuthSessionWhileLoggedIn() throws IOException, MessagingException {
        Assume.assumeTrue("Works only on auth-server-undertow",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT));

        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(false).update();

        final String testRealmName = testRealm().toRepresentation().getRealm();
        accountPage.setAuthRealm(testRealmName);
        oauth.realm(testRealmName).clientId("account").redirectUri(getAuthServerRoot() + "realms/" + testRealmName + "/account");
        loginPage.open();

        String authSessionId = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver);
        String realmId = testRealm().toRepresentation().getId();
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            RootAuthenticationSessionModel ras = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
            assertThat("Expecting single auth session", ras.getAuthenticationSessions().keySet(), Matchers.hasSize(1));
            ras.getAuthenticationSessions().forEach((id, as) -> as.addRequiredAction(RequiredAction.VERIFY_EMAIL));
        });
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

        // confirm
        driver.navigate().to(verificationUrl);

        // back to account, already logged in
        accountPage.assertCurrent();

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        Assert.assertTrue(user.isEmailVerified());
        Assert.assertThat(user.getRequiredActions(), Matchers.empty());
    }

    @Test
    public void verifyEmailInNewBrowserWhileLoggedInFirstBrowser() throws IOException, MessagingException {
        UserAttributeUpdater userAttributeUpdater = new UserAttributeUpdater(testRealm().users().get(testUserId));
        userAttributeUpdater.setEmailVerified(true).update();

        final String testRealmName = testRealm().toRepresentation().getRealm();
        accountPage.setAuthRealm(testRealmName);
        oauth.realm(testRealmName).clientId("account").redirectUri(getAuthServerRoot() + "realms/" + testRealmName + "/account");
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        accountPage.assertCurrent();

        userAttributeUpdater.setEmailVerified(false).setRequiredActions(RequiredAction.VERIFY_EMAIL).update();

        // this will result in email verification
        loginPage.open();
        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

        // confirm in the second browser
        driver2.navigate().to(verificationUrl);

        // follow the link
        final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
        assertThat(proceedLink, Matchers.notNullValue());
        proceedLink.click();

        // confirmation in the second browser
        assertThat(driver2.getPageSource(), Matchers.containsString("kc-info-message"));
        assertThat(driver2.getPageSource(), Matchers.containsString("Your email address has been verified."));

        final WebElement backToApplicationLink = driver2.findElement(By.linkText("« Back to Application"));
        assertThat(backToApplicationLink, Matchers.notNullValue());
        backToApplicationLink.click();

        // login page should be shown in the second browser
        assertThat(driver2.getPageSource(), Matchers.containsString("kc-login"));
        assertThat(driver2.getPageSource(), Matchers.containsString("Sign in"));

        // email should be verified and required actions empty
        UserRepresentation user = testRealm().users().get(testUserId).toRepresentation();
        Assert.assertTrue(user.isEmailVerified());
        Assert.assertThat(user.getRequiredActions(), Matchers.empty());

        // after refresh in the first browser the account console should be shown
        driver.navigate().refresh();
        accountPage.assertCurrent();
    }

    @Test
    public void verifyEmailExpiredRegistration() throws IOException, MessagingException {
        final String COMMON_ATTR = "verifyEmailRegistrationUser";

        String appInitiatedRegisterUrl = oauth.getLoginFormUrl();
        appInitiatedRegisterUrl = appInitiatedRegisterUrl.replace("openid-connect/auth", "openid-connect/registrations");
        driver.navigate().to(appInitiatedRegisterUrl);

        registerPage.assertCurrent();
        registerPage.register(COMMON_ATTR, COMMON_ATTR, COMMON_ATTR + "@" + COMMON_ATTR, COMMON_ATTR, COMMON_ATTR, COMMON_ATTR);

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getLastReceivedMessage();

        String verificationUrl = getPasswordResetEmailLink(message);

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
        String verificationUrl = getPasswordResetEmailLink(message);

        UserResource user = testRealm().users().get(testUserId);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEmail("vmuzikar@redhat.com");
        user.update(userRep);

        driver.navigate().to(verificationUrl.trim());
        errorPage.assertCurrent();
        assertEquals("The link you clicked is an old stale link and is no longer valid. Maybe you have already verified your email.", errorPage.getError());
    }
}
