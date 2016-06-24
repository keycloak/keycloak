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
package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.util.*;


import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author fkiss
 */
public class TrustStoreEmailTest extends TestRealmKeycloakTest {

    @Page
    protected OIDCLogin testRealmLoginPage;

    @Page
    protected AuthRealm testRealmPage;

    @Page
    protected AccountManagement accountManagement;

    @Page
    private VerifyEmail testRealmVerifyEmailPage;

    private UserRepresentation user;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        log.info("enable verify email and configure smtp server to run with ssl in test realm");

        user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
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
    public void verifyEmailWithSslEnabled() {
        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.PRIVATE_KEY).getFile());
        accountManagement.navigateTo();
        testRealmLoginPage.form().login(user.getUsername(), "password");

        assertEquals("You need to verify your email address to activate your account.",
                testRealmVerifyEmailPage.getFeedbackText());

        String verifyEmailUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, user.getEmail(),
                "Someone has created a Test account with this email address.", true);

        log.info("navigating to url from email: " + verifyEmailUrl);

        driver.navigate().to(verifyEmailUrl);

        assertCurrentUrlStartsWith(accountManagement);
        accountManagement.signOut();
        testRealmLoginPage.form().login(user);
        assertCurrentUrlStartsWith(accountManagement);
    }

    @Test
    public void verifyEmailWithSslWrongCertificate() {
        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.INVALID_KEY).getFile());
        accountManagement.navigateTo();
        loginPage.form().login(user);

        assertEquals("Failed to send email, please try again later.\n" +
                        "« Back to Application",
                testRealmVerifyEmailPage.getErrorMessage());
    }
}