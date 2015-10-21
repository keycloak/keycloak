/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author vramik
 */
public class VerifyEmailTest extends AbstractAccountManagementTest {

    @Page
    private VerifyEmail testRealmVerifyEmailPage;
    
    private static boolean init = false;
   
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmVerifyEmailPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeVerifyEmail() {
        log.info("enable verify email and configure smpt server in test realm");
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setSmtpServer(suiteContext.getSmtpServer());
        testRealmRep.setVerifyEmail(true);
        testRealmResource().update(testRealmRep);

        if (!init) {
            init = true;
            MailServer.start();
            MailServer.createEmailAccount(testUser.getEmail(), "password");
        }
    }
    
    @AfterClass
    public static void afterClass() {
        MailServer.stop();
    }

    @Test
    public void verifyEmail() {
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        
        assertEquals("You need to verify your email address to activate your account.", 
                testRealmVerifyEmailPage.getFeedbackText());
        
        String verifyEmailUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, testUser.getEmail(), 
                "Someone has created a Test account with this email address.");
        
        log.info("navigating to url from email: " + verifyEmailUrl);
        driver.navigate().to(verifyEmailUrl);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
}
