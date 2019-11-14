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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
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
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SslMailServer;

import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author fkiss
 */
public class TrustStoreEmailTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected OIDCLogin testRealmLoginPage;

    @Page
    protected AuthRealm testRealmPage;

    @Page
    protected AccountManagement accountManagement;

    @Page
    private VerifyEmail testRealmVerifyEmailPage;

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
        accountManagement.setAuthRealm(testRealmPage);
        testRealmLoginPage.setAuthRealm(testRealmPage);
    }

    @After
    public void afterTrustStoreEmailTest() {
        SslMailServer.stop();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void verifyEmailWithSslEnabled() {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.PRIVATE_KEY).getFile());
        accountManagement.navigateTo();
        testRealmLoginPage.form().login(user.getUsername(), "password");

        EventRepresentation sendEvent = events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL)
                .user(user.getId())
                .client("account")
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
                .client("account")
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.EMAIL, "test-user@localhost")
                .detail(Details.CODE_ID, mailCodeId)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        events.expectLogin()
                .client("account")
                .user(user.getId())
                .session(mailCodeId)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        assertCurrentUrlStartsWith(accountManagement);
        accountManagement.signOut();
        testRealmLoginPage.form().login(user.getUsername(), "password");
        assertCurrentUrlStartsWith(accountManagement);
    }

    @Test
    public void verifyEmailWithSslWrongCertificate() throws Exception {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost");

        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.INVALID_KEY).getFile());
        accountManagement.navigateTo();
        loginPage.form().login(user.getUsername(), "password");

        events.expectRequiredAction(EventType.SEND_VERIFY_EMAIL_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .user(user.getId())
                .client("account")
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.EMAIL, "test-user@localhost")
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        // Email wasn't send
        Assert.assertNull(SslMailServer.getLastReceivedMessage());

        // Email wasn't send, but we won't notify end user about that. Admin is aware due to the error in the logs and the SEND_VERIFY_EMAIL_ERROR event.
        assertEquals("You need to verify your email address to activate your account.",
                testRealmVerifyEmailPage.feedbackMessage().getText());
    }
}
