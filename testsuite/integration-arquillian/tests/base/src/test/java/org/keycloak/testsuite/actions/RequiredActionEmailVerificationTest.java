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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.UserBuilder;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionEmailVerificationTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

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
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setVerifyEmail(Boolean.TRUE);
        ActionUtil.findUserInRealmRep(testRealm, "test-user@localhost").setEmailVerified(Boolean.FALSE);
    }

    @Before
    public void before() {
        oauth.state("mystate"); // have to set this as keycloak validates that state is sent


        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost").build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }

    /**
     * see KEYCLOAK-4163
     */
    @Test
    public void verifyEmailConfig() throws IOException, MessagingException {

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

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

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getPasswordResetEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        EventRepresentation sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        Assert.assertEquals(mailCodeId, verificationUrl.split("code=")[1].split("\\&")[0].split("\\.")[1]);

        driver.navigate().to(verificationUrl.trim());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).detail(Details.CODE_ID, mailCodeId).assertEvent();
    }

    @Test
    public void verifyEmailRegister() throws IOException, MessagingException {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "email@mail.com", "verifyEmail", "password", "password");

        String userId = events.expectRegister("verifyEmail", "email@mail.com").assertEvent().getUserId();

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).user(userId).detail("username", "verifyemail").detail("email", "email@mail.com").assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        String verificationUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).user(userId).session(sessionId).detail("username", "verifyemail").detail("email", "email@mail.com").detail(Details.CODE_ID, mailCodeId).assertEvent();

        events.expectLogin().user(userId).session(sessionId).detail("username", "verifyemail").detail(Details.CODE_ID, mailCodeId).assertEvent();
    }

    @Test
    public void verifyEmailResend() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost").assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        verifyEmailPage.clickResendEmail();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[1];

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").assertEvent(sendEvent);

        String verificationUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        events.expectLogin().session(sessionId).assertEvent();
    }

    @Test
    public void verifyEmailResendFirstInvalidSecondStillValid() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.clickResendEmail();

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        MimeMessage message1 = greenMail.getReceivedMessages()[0];

        String verificationUrl1 = getPasswordResetEmailLink(message1);

        driver.navigate().to(verificationUrl1.trim());

        assertTrue(errorPage.isCurrent());
        assertEquals("The link you clicked is a old stale link and is no longer valid. Maybe you have already verified your email?", errorPage.getError());

        MimeMessage message2 = greenMail.getReceivedMessages()[1];

        String verificationUrl2 = getPasswordResetEmailLink(message2);

        driver.navigate().to(verificationUrl2.trim());

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void verifyEmailNewBrowserSession() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getPasswordResetEmailLink(message);

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        EventRepresentation sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();

        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        Assert.assertEquals(mailCodeId, verificationUrl.split("code=")[1].split("\\&")[0].split("\\.")[1]);

        driver.manage().deleteAllCookies();

        driver.navigate().to(verificationUrl.trim());

        events.expectRequiredAction(EventType.VERIFY_EMAIL).session(sessionId).detail("email", "test-user@localhost").detail(Details.CODE_ID, mailCodeId).assertEvent();

        assertTrue(infoPage.isCurrent());
        assertEquals("Your email address has been verified.", infoPage.getInfo());

        loginPage.open();

        assertTrue(loginPage.isCurrent());
    }


    @Test
    public void verifyInvalidKeyOrCode() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());
        String resendEmailLink = verifyEmailPage.getResendEmailLink();
        String keyInsteadCodeURL = resendEmailLink.replace("code=", "key=");

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost").assertEvent();

        driver.navigate().to(keyInsteadCodeURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();

        String badKeyURL = KeycloakUriBuilder.fromUri(resendEmailLink).replaceQueryParam("key", "foo").build().toString();
        driver.navigate().to(badKeyURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();
    }

    @Test
    public void verifyEmailBadCode() throws IOException, MessagingException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(verifyEmailPage.isCurrent());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String verificationUrl = getPasswordResetEmailLink(message);

        verificationUrl = KeycloakUriBuilder.fromUri(verificationUrl).replaceQueryParam("code", "foo").build().toString();

        events.poll();

        driver.navigate().to(verificationUrl.trim());

        assertEquals("The link you clicked is a old stale link and is no longer valid. Maybe you have already verified your email?", errorPage.getError());

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();
    }


    public static String getPasswordResetEmailLink(MimeMessage message) throws IOException, MessagingException {
    	Multipart multipart = (Multipart) message.getContent();

        final String textContentType = multipart.getBodyPart(0).getContentType();

        assertEquals("text/plain; charset=UTF-8", textContentType);

        final String textBody = (String) multipart.getBodyPart(0).getContent();
        final String textChangePwdUrl = MailUtils.getLink(textBody);

        final String htmlContentType = multipart.getBodyPart(1).getContentType();

        assertEquals("text/html; charset=UTF-8", htmlContentType);

        final String htmlBody = (String) multipart.getBodyPart(1).getContent();
        final String htmlChangePwdUrl = MailUtils.getLink(htmlBody);

        assertEquals(htmlChangePwdUrl, textChangePwdUrl);

        return htmlChangePwdUrl;
    }

}
