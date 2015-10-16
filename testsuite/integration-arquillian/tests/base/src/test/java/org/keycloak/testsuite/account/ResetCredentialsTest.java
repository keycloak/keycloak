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
import org.keycloak.testsuite.auth.page.login.ResetCredentials;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;


/**
 *
 * @author vramik
 */
public class ResetCredentialsTest extends AbstractAccountManagementTest {

    @Page
    private ResetCredentials testRealmResetCredentialsPage;
    
    private static boolean init = false;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmResetCredentialsPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeResetCredentials() {
        // enable reset credentials and configure smpt server in test realm
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setSmtpServer(suiteContext.getSmtpServer());
        testRealmRep.setResetPasswordAllowed(true);
        testRealmResource().update(testRealmRep);

        if (!init) {
            init = true;
            MailServer.start();
            MailServer.createEmailAccount(testUser.getEmail(), "password");
        }
        
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().forgotPassword();
    }
    
    @AfterClass
    public static void afterClass() {
        MailServer.stop();
    }

    @Test
    public void resetCredentialsWithEmail() {
        testRealmResetCredentialsPage.resetCredentials(testUser.getEmail());
        resetCredentialsAndLoginWithNewPassword();
    }
    
    @Test
    public void resetCredentialsWithUsername() {
        testRealmResetCredentialsPage.resetCredentials(testUser.getUsername());
        resetCredentialsAndLoginWithNewPassword();
    }
    
    private void resetCredentialsAndLoginWithNewPassword() {
        assertEquals("You should receive an email shortly with further instructions.", 
                testRealmResetCredentialsPage.getFeedbackText());
        
        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, testUser.getEmail(), 
                "Someone just requested to change your Test account's credentials.");
        
        log.info("navigating to " + url);
        driver.navigate().to(url);
        //assertCurrentUrlStartsWith(testRealmResetCredentialsPage);
        testRealmResetCredentialsPage.updatePassword("newPassword");
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login("test", "newPassword");
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
}
