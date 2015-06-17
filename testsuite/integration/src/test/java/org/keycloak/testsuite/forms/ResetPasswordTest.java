/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.forms;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.MailUtil;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.Time;
import org.openqa.selenium.WebDriver;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ResetPasswordTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule((new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().addUser(appRealm, "login-test");
            user.setEmail("login@test.com");
            user.setEnabled(true);

            userId = user.getId();

            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue("password");

            user.updateCredential(creds);
            appRealm.setEventsListeners(Collections.singleton("dummy"));
        }
    }));

    private static String userId;

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected ErrorPage errorPage;

    @WebResource
    protected InfoPage infoPage;

    @WebResource
    protected LoginPasswordResetPage resetPasswordPage;

    @WebResource
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Test
    public void resetPassword() throws IOException, MessagingException {
        resetPassword("login-test");
    }

    @Test
    public void resetPasswordCancel() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        resetPasswordPage.assertCurrent();

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        resetPasswordPage.backToLogin();

        assertTrue(loginPage.isCurrent());

        loginPage.login("login-test", "password");

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];
        
        final String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        events.expect(EventType.RESET_PASSWORD_ERROR).client((String) null).user((String) null).error("invalid_code").clearDetails().assertEvent();

        assertTrue(errorPage.isCurrent());
        assertEquals("An error occurred, please login again through your application.", errorPage.getError());
    }

    @Test
    public void resetPasswordCancelChangeUser() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("test-user@localhost");

        resetPasswordPage.assertCurrent();

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).detail(Details.USERNAME, "test-user@localhost").detail(Details.EMAIL, "test-user@localhost").assertEvent().getSessionId();

        resetPasswordPage.backToLogin();

        assertTrue(loginPage.isCurrent());

        loginPage.login("login@test.com", "password");

        Event loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

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

        resetPasswordPage.assertCurrent();

        String sessionId = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).session(sessionId).detail(Details.USERNAME, username).assertEvent();

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

        resetPasswordPage.assertCurrent();

        String sessionId = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).session(sessionId)
                .detail(Details.USERNAME, username).assertEvent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).detail(Details.USERNAME, username).session(sessionId).assertEvent();

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).session(sessionId).assertEvent();
    }

    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        resetPasswordPage.assertCurrent();

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId)
                .detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        MimeMessage message = greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(password, password);

        assertTrue(updatePasswordPage.isCurrent());
        assertEquals(error, updatePasswordPage.getError());
    }

    @Test
    public void resetPasswordWrongEmail() throws IOException, MessagingException, InterruptedException {
        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("invalid");

        resetPasswordPage.assertCurrent();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        assertEquals(0, greenMail.getReceivedMessages().length);

        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user((String) null).session((String) null).detail(Details.USERNAME, "invalid").removeDetail(Details.EMAIL).removeDetail(Details.CODE_ID).error("user_not_found").assertEvent();
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
        
        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).client((String) null).user((String) null).session((String) null).clearDetails().error("username_missing").assertEvent();
        
    }

    @Test
    public void resetPasswordExpiredCode() throws IOException, MessagingException, InterruptedException {
        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            resetPasswordPage.assertCurrent();

            String sessionId = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

            assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = getPasswordResetEmailLink(message);

            Time.setOffset(350);

            driver.navigate().to(changePasswordUrl.trim());

            errorPage.assertCurrent();

            assertEquals("Login timeout. Please login again.", errorPage.getError());

            events.expectRequiredAction(EventType.RESET_PASSWORD).error("expired_code").client("test-app").user((String) null).session((String) null).clearDetails().assertEvent();
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void resetPasswordDisabledUser() throws IOException, MessagingException, InterruptedException {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                session.users().getUserByUsername("login-test", appRealm).setEnabled(false);
            }
        });

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            resetPasswordPage.assertCurrent();

            assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("user_disabled").assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    session.users().getUserByUsername("login-test", appRealm).setEnabled(true);
                }
            });
        }
    }

    @Test
    public void resetPasswordNoEmail() throws IOException, MessagingException, InterruptedException {
        final String[] email = new String[1];
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                UserModel user = session.users().getUserByUsername("login-test", appRealm);
                email[0] = user.getEmail();
                user.setEmail(null);

            }
        });

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            resetPasswordPage.assertCurrent();

            assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD_ERROR).session((String) null).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error("invalid_email").assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    session.users().getUserByUsername("login-test", appRealm).setEmail(email[0]);
                }
            });
        }
    }

    @Test
    public void resetPasswordWrongSmtp() throws IOException, MessagingException, InterruptedException {
        final String[] host = new String[1];
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                host[0] =  appRealm.getSmtpConfig().get("host");
                appRealm.getSmtpConfig().put("host", "invalid_host");
            }
        });

        try {
            loginPage.open();
            loginPage.resetPassword();

            resetPasswordPage.assertCurrent();

            resetPasswordPage.changePassword("login-test");

            errorPage.assertCurrent();

            assertEquals("Failed to send email, please try again later.", errorPage.getError());

            assertEquals(0, greenMail.getReceivedMessages().length);

            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD_ERROR).user(userId).detail(Details.USERNAME, "login-test").removeDetail(Details.CODE_ID).error(Errors.EMAIL_SEND_FAILED).assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.getSmtpConfig().put("host",host[0]);
                }
            });
        }
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException, MessagingException {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setPasswordPolicy(new PasswordPolicy("length"));
            }
        });

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword("login-test");

        resetPasswordPage.assertCurrent();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        String sessionId = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("invalid", "invalid");

        assertEquals("Invalid password: minimum length 8.", resetPasswordPage.getErrorMessage());

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).session(sessionId).detail(Details.USERNAME, "login-test").assertEvent();

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
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                //Block passwords that are equal to previous passwords. Default value is 3.
                appRealm.setPasswordPolicy(new PasswordPolicy("passwordHistory"));
            }
        });
        
        try {
            Time.setOffset(2000000);
            resetPassword("login-test", "password1");
            
            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");

            Time.setOffset(4000000);
            resetPassword("login-test", "password2");
            
            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");
        
            Time.setOffset(8000000);
            resetPassword("login-test", "password3");
            
            resetPasswordInvalidPassword("login-test", "password1", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password2", "Invalid password: must not be equal to any of last 3 passwords.");
            resetPasswordInvalidPassword("login-test", "password3", "Invalid password: must not be equal to any of last 3 passwords.");

            resetPassword("login-test", "password");
        } finally {
            keycloakRule.update(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });
            Time.setOffset(0);
        }
    }

    @Test
    public void resetPasswordNewBrowserSession() throws IOException, MessagingException {
        String username = "login-test";

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);

        resetPasswordPage.assertCurrent();

        String sessionId = events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).user(userId).detail(Details.USERNAME, username).detail(Details.EMAIL, "login@test.com").assertEvent().getSessionId();

        assertEquals("You should receive an email shortly with further instructions.", resetPasswordPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String changePasswordUrl = getPasswordResetEmailLink(message);

        driver.manage().deleteAllCookies();

        driver.navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).session(sessionId).detail(Details.USERNAME, username).assertEvent();

        assertTrue(infoPage.isCurrent());
        assertEquals("Your password has been updated.", infoPage.getInfo());

        loginPage.open();

        assertTrue(loginPage.isCurrent());
    }
    
    private String getPasswordResetEmailLink(MimeMessage message) throws IOException, MessagingException {
    	Multipart multipart = (Multipart) message.getContent();
    	
        final String textContentType = multipart.getBodyPart(0).getContentType();
        
        assertEquals("text/plain; charset=UTF-8", textContentType);
        
        final String textBody = (String) multipart.getBodyPart(0).getContent();
        final String textChangePwdUrl = MailUtil.getLink(textBody);
        
        final String htmlContentType = multipart.getBodyPart(1).getContentType();
        
        assertEquals("text/html; charset=UTF-8", htmlContentType);
        
        final String htmlBody = (String) multipart.getBodyPart(1).getContent();
        final String htmlChangePwdUrl = MailUtil.getLink(htmlBody);
        
        assertEquals(htmlChangePwdUrl, textChangePwdUrl);

        return htmlChangePwdUrl;
    }

}
