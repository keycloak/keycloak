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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import java.io.IOException;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionEmailVerificationTest extends TestRealmKeycloakTest {

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

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setVerifyEmail(Boolean.TRUE);
        ActionUtil.findUserInRealmRep(testRealm, "test-user@localhost").setEmailVerified(Boolean.FALSE);
    }

    @Before
    public void before() {
        oauth.state("mystate"); // have to set this as keycloak validates that state is sent
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

        AssertEvents.ExpectedEvent emailEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL).detail("email", "test-user@localhost");
        EventRepresentation sendEvent = emailEvent.assertEvent();
        String sessionId = sendEvent.getSessionId();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        driver.navigate().to(keyInsteadCodeURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_CODE)
                .client((String)null)
                .user((String)null)
                .session((String)null)
                .clearDetails()
                .assertEvent();

        String badKeyURL = KeycloakUriBuilder.fromUri(resendEmailLink).queryParam("key", "foo").build().toString();
        driver.navigate().to(badKeyURL);

        events.expectRequiredAction(EventType.VERIFY_EMAIL_ERROR)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .session(sessionId)
                .detail("email", "test-user@localhost")
                .detail(Details.CODE_ID, mailCodeId)
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
