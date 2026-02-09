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
package org.keycloak.testsuite.ssl;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SslMailServer;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author fkiss
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrustStoreEmailTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected OIDCLogin testRealmLoginPage;

    @Page
    protected AuthRealm testRealmPage;

    @Page
    private VerifyEmail testRealmVerifyEmailPage;

    @Page
    private ErrorPage errorPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        log.info("enable verify email and configure smtp server to run with ssl in test realm");

        testRealm.setSmtpServer(SslMailServer.getServerConfiguration());
        testRealm.setVerifyEmail(true);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm("test");
        testRealmVerifyEmailPage.setAuthRealm(testRealmPage);
        testRealmLoginPage.setAuthRealm(testRealmPage);
    }

    @After
    public void afterTrustStoreEmailTest() {
        SslMailServer.stop();
    }

    public void verifyEmailWithSslEnabled(Boolean opportunistic) {
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.setEmailVerified(false);
        userResource.update(user);

        String privateKey = this.getClass().getClassLoader().getResource(opportunistic ? SslMailServer.INVALID_KEY : SslMailServer.PRIVATE_KEY).getFile();

        if (opportunistic) {
            SslMailServer.startWithOpportunisticSsl(privateKey);
        } else {
            SslMailServer.startWithSsl(privateKey);
        }

        oauth.openLoginForm();
        testRealmLoginPage.form().login(user.getUsername(), "password");

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
                .user(user.getId())
                .client("test-app")
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.EMAIL, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();
        String mailCodeId = sendEvent.getDetails().get(Details.CODE_ID);

        assertEquals("You need to verify your email address to activate your account.",
                testRealmVerifyEmailPage.feedbackMessage().getText());

        String verifyEmailUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, user.getEmail(),
                "Someone has created a Test account with this email address.", true);

        log.info("navigating to url from email: " + verifyEmailUrl);

        driver.navigate().to(verifyEmailUrl);

        events.expectRequiredAction(EventType.VERIFY_EMAIL)
                .user(user.getId())
                .client("test-app")
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.EMAIL, "test-user@localhost")
                .detail(Details.CODE_ID, mailCodeId)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        events.expectLogin()
                .client("test-app")
                .user(user.getId())
                .session(mailCodeId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        assertCurrentUrlStartsWith(OAuthClient.APP_AUTH_ROOT);
        AccountHelper.logout(testRealm(), user.getUsername());
        oauth.openLoginForm();
        testRealmLoginPage.form().login(user.getUsername(), "password");
        assertCurrentUrlStartsWith(OAuthClient.APP_AUTH_ROOT);
    }

    @Test
    public void test01VerifyEmailWithSslWrongCertificate() throws Exception {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.INVALID_KEY).getFile());
        oauth.openLoginForm();
        loginPage.form().login(user.getUsername(), "password");

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .user(user.getId())
                .client("test-app")
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.EMAIL, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        // Email wasn't send
        Assert.assertNull(SslMailServer.getLastReceivedMessage());

        // Email wasn't sent, and we notify end user about that.
        assertEquals("Failed to send email, please try again later.",
                errorPage.getError());
    }

    @Test
    public void test02VerifyEmailWithSslWrongCertificateAndAnyHostnamePolicy() throws Exception {
        testingClient.testing().modifyTruststoreSpiHostnamePolicy(HostnameVerificationPolicy.ANY);
        try {
            test01VerifyEmailWithSslWrongCertificate();
        } finally {
            testingClient.testing().reenableTruststoreSpi();
        }
    }

    @Test
    public void test03erifyEmailWithSslWrongHostname() throws Exception {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm())
                .setSmtpServer("host", "localhost.localdomain")
                .update()) {
            SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.PRIVATE_KEY).getFile());
            oauth.openLoginForm();
            loginPage.form().login(user.getUsername(), "password");

            events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL_ERROR)
                    .error(Errors.EMAIL_SEND_FAILED)
                    .user(user.getId())
                    .client("test-app")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.EMAIL, "test-user@localhost")
                    .removeDetail(Details.REDIRECT_URI)
                    .assertEvent();

            // Email wasn't send
            Assert.assertNull(SslMailServer.getLastReceivedMessage());

            // Email wasn't sent, and we notify end user about that.
            assertEquals("Failed to send email, please try again later.",
                    errorPage.getError());
        }
    }

    @Test
    public void test04VerifyEmailWithSslEnabled() {
        verifyEmailWithSslEnabled(false);
    }

    @Test
    public void test05VerifyEmailWithSslWrongHostnameButAnyHostnamePolicy() throws Exception {
        testingClient.testing().modifyTruststoreSpiHostnamePolicy(HostnameVerificationPolicy.ANY);
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm())
                .setSmtpServer("host", "localhost.localdomain")
                .update()) {
            test04VerifyEmailWithSslEnabled();
        } finally {
            testingClient.testing().reenableTruststoreSpi();
        }
    }

    @Test
    public void test06VerifyEmailOpportunisticEncryptionWithAnyHostnamePolicy() throws Exception {
        testingClient.testing().modifyTruststoreSpiHostnamePolicy(HostnameVerificationPolicy.ANY);
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm())
                .setSmtpServer("host", "localhost.localdomain")
                .setSmtpServer("auth", "true")
                .setSmtpServer("ssl", "false")
                .setSmtpServer("starttls", "false")
                .setSmtpServer("user", "user")
                .setSmtpServer("password", "password")
                .update()) {
            verifyEmailWithSslEnabled(true);
        } finally {
            testingClient.testing().reenableTruststoreSpi();
        }
    }

}
