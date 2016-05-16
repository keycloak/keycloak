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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SslMailServer;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author fkiss
 */
public class TrustStoreEmailTest extends AbstractAccountManagementTest {

    @Page
    private VerifyEmail testRealmVerifyEmailPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmVerifyEmailPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeTrustStoreEmailTest() {
        log.info("enable verify email and configure smtp server to run with ssl in test realm");
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setSmtpServer(SslMailServer.getServerConfiguration());
        testRealmRep.setVerifyEmail(true);
        System.out.println(testRealmRep.getSmtpServer());
        testRealmResource().update(testRealmRep);
    }

    @After
    public void afterTrustStoreEmailTest() {
        SslMailServer.stop();
    }


    @Test
    public void verifyEmailWithSslEnabled() {
        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.PRIVATE_KEY).getFile());
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

        assertEquals("You need to verify your email address to activate your account.",
                testRealmVerifyEmailPage.getFeedbackText());

        String verifyEmailUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, testUser.getEmail(),
                "Someone has created a Test account with this email address.", true);

        log.info("navigating to url from email: " + verifyEmailUrl);
        driver.navigate().to(verifyEmailUrl);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }

    @Test
    public void verifyEmailWithSslWrongCertificate() {
        SslMailServer.startWithSsl(this.getClass().getClassLoader().getResource(SslMailServer.INVALID_KEY).getFile());
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

        assertEquals("Failed to send email, please try again later.\n" +
                        "« Back to Application",
                testRealmVerifyEmailPage.getErrorMessage());
    }
}