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

import org.junit.*;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.testsuite.AssertEvents;
//import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ResetPasswordTest extends TestRealmKeycloakTest {

    static int lifespan = 0;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                                             .username("login-test")
                                             .email("login@test.com")
                                             .enabled(true)
                                             .password("password")
                                             .build();
        testRealm.getUsers().add(user);
    }

    @Before
    public void setAccessCodeLifespanUserAction() {
        // Must do this with adminClient because default is not set until after testRealm.json is loaded.
        lifespan = testRealm().toRepresentation().getAccessCodeLifespanUserAction();
    }

    @Before
    public void setUserId() {
        userId = findUser("login-test").getId();
    }


    private static String userId;

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

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Test
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

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        String sessionId = events.expectRequiredAction(EventType.UPDATE_PASSWORD)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client("account")
                .user(userId).detail(Details.USERNAME, username).assertEvent().getSessionId();

        events.expectLogin().user(userId).detail(Details.USERNAME, username)
                .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                .client("account")
                .session(sessionId).assertEvent();

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

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
    public void resetPasswordCancelChangeUser() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("test-user@localhost");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

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

    private void resetPassword(String username) throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                .user(userId)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, "login@test.com")
                .session((String)null)
                .assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        String sessionId = events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, username).assertEvent().getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, username).session(sessionId).assertEvent();

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        loginPage.open();

        loginPage.login("login-test", "resetPassword");

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    private void resetPassword(String username, String password) throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).session((String)null)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent();

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        String sessionId = events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId)
                .detail(Details.USERNAME, username).assertEvent().getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, username).assertEvent();

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();
    }

    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).session((String)null)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent();

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        assertTrue(updatePasswordPage.isCurrent());
        assertEquals(error, updatePasswordPage.getError());
        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }

    @Test
    public void resetPasswordWrongEmail() throws IOException, MessagingException, InterruptedException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("invalid");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

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

        assertEquals("Please specify username.", resetPasswordPage.getErrorMessage());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).user((String) null).session((String) null).clearDetails().error("username_missing").assertEvent();

    }

    @Test
    public void resetPasswordExpiredCode() throws IOException, MessagingException, InterruptedException {
        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = getPasswordResetEmailLink(message);

            setTimeOffset(1800 + 23);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("You took too long to login. Login process starting from beginning.", loginPage.getError());

            events.expectRequiredAction(EventType.RESET_PASSWORD).error("expired_code").client("test-app").user((String) null).session((String) null).clearDetails().assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordExpiredCodeShort() throws IOException, MessagingException, InterruptedException {
        final AtomicInteger originalValue = new AtomicInteger();

        RealmRepresentation realmRep = testRealm().toRepresentation();
        originalValue.set(realmRep.getAccessCodeLifespan());
        realmRep.setAccessCodeLifespanUserAction(60);
        testRealm().update(realmRep);

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .session((String)null)
                    .user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = getPasswordResetEmailLink(message);

            setTimeOffset(70);

            driver.navigate().to(changePasswordUrl.trim());

            loginPage.assertCurrent();

            assertEquals("You took too long to login. Login process starting from beginning.", loginPage.getError());

            events.expectRequiredAction(EventType.RESET_PASSWORD).error("expired_code").client("test-app").user((String) null).session((String) null).clearDetails().assertEvent();
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void resetPasswordDisabledUser() throws IOException, MessagingException, InterruptedException {
        UserRepresentation user = findUser("login-test");
        user.setEnabled(false);
        updateUser(user);

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("user_disabled").assertEvent();
    }

    @Test
    public void resetPasswordNoEmail() throws IOException, MessagingException, InterruptedException {
        final String[] email = new String[1];

        UserRepresentation user = findUser("login-test");
        email[0] = user.getEmail();
        user.setEmail("");
        updateUser(user);

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.RESET_PASSWORD_ERROR).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("invalid_email").assertEvent();
    }

    @Test
    public void resetPasswordWrongSmtp() throws IOException, MessagingException, InterruptedException {
        final String[] host = new String[1];

        Map<String, String> smtpConfig = new HashMap<>();
        smtpConfig.putAll(testRealm().toRepresentation().getSmtpServer());
        host[0] =  smtpConfig.get("host");
        smtpConfig.put("host", "invalid_host");
        RealmRepresentation realmRep = testRealm().toRepresentation();
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
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        testRealm().update(realmRep);
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException, MessagingException {
        setPasswordPolicy("length");

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).session((String)null).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent();

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("invalid", "invalid");

        assertEquals("Invalid password: minimum length 8.", resetPasswordPage.getErrorMessage());

        events.expectRequiredAction(EventType.UPDATE_PASSWORD_ERROR).error(Errors.PASSWORD_REJECTED).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        String sessionId = events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").session(sessionId).assertEvent();

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();

        loginPage.open();

        loginPage.login("login-test", "resetPasswordWithPasswordPolicy");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void resetPasswordWithPasswordHisoryPolicy() throws IOException, MessagingException {
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

            setTimeOffset(8000000);
            resetPassword("login-test", "password3");

            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password3", "Invalid password: must not be equal to any of last 3 passwords.");

            resetPassword("login-test", "password");
        } finally {
            setTimeOffset(0);
        }
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
