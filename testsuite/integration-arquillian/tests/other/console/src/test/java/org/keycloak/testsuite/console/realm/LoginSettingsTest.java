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
package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.Registration;
import org.keycloak.testsuite.auth.page.login.ResetCredentials;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.keycloak.testsuite.console.page.realm.LoginSettings.RequireSSLOption;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.Cookie;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 *
 * @author tkyjovsk
 */
public class LoginSettingsTest extends AbstractRealmTest {

    private static final String NEW_USERNAME = "newUsername";
    
    @Page
    private LoginSettings loginSettingsPage;
    @Page
    private Registration testRealmRegistrationPage;
    @Page
    private ResetCredentials testRealmForgottenPasswordPage;
    @Page
    private VerifyEmail testRealmVerifyEmailPage;
    @Page
    private Account testAccountPage;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistrationPage.setAuthRealm(TEST);
        testRealmForgottenPasswordPage.setAuthRealm(TEST);
        testRealmVerifyEmailPage.setAuthRealm(TEST);
        testAccountPage.setAuthRealm(TEST);
    }
    
    @Before
    public void beforeLoginSettingsTest() {
//        tabs().login();
        loginSettingsPage.navigateTo();
    }
    
    @Test
    public void userRegistration() {

        log.info("enabling registration");
        loginSettingsPage.form().setRegistrationAllowed(true);
        assertTrue(loginSettingsPage.form().isRegistrationAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        assertTrue(testRealmRegistrationPage.isConfirmPasswordPresent());
        assertTrue(testRealmRegistrationPage.isUsernamePresent());
        log.info("verified registration is enabled");

        // test email as username
        log.info("enabling email as username");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setEmailAsUsername(true);
        assertTrue(loginSettingsPage.form().isEmailAsUsername());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");

        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        assertTrue(testRealmRegistrationPage.isConfirmPasswordPresent());
        assertFalse(testRealmRegistrationPage.isUsernamePresent());
        log.info("verified email as username");

        // test user reg. disabled
        log.info("disabling registration");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setRegistrationAllowed(false);
        assertFalse(loginSettingsPage.form().isRegistrationAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("disabled");
        
        testRealmAdminConsolePage.navigateTo();
        assertFalse(testRealmLoginPage.form().isRegisterLinkPresent());
        log.info("verified regisration is disabled");
    }
    
    @Test
    public void editUsername() {
        log.info("enabling edit username");
        loginSettingsPage.form().setEditUsernameAllowed(true);
        assertTrue(loginSettingsPage.form().isEditUsernameAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");
        
        log.info("edit username");
        testAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testAccountPage);
        testAccountPage.setUsername(NEW_USERNAME);
        testAccountPage.save();
        testAccountPage.signOut();
        log.debug("edited");
        
        log.info("log in with edited username");
        assertCurrentUrlStartsWithLoginUrlOf(testAccountPage);
        testRealmLoginPage.form().login(NEW_USERNAME, PASSWORD);
        assertCurrentUrlStartsWith(testAccountPage);
        log.debug("user is logged in with edited username");
        
        log.info("disabling edit username");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setEditUsernameAllowed(false);
        assertFalse(loginSettingsPage.form().isEditUsernameAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("disabled");
        
        
    }
    
    @Test
    public void resetPassword() {
        
        log.info("enabling reset password");
        loginSettingsPage.form().setResetPasswordAllowed(true);
        assertTrue(loginSettingsPage.form().isResetPasswordAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().forgotPassword();
        
        Assert.assertEquals("Enter your username or email address and we will send you instructions on how to create a new password.", 
                testRealmForgottenPasswordPage.getInfoMessage());
        log.info("verified reset password is enabled");
        
        log.info("disabling reset password");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setResetPasswordAllowed(false);
        assertFalse(loginSettingsPage.form().isResetPasswordAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("disabled");
        
        testRealmAdminConsolePage.navigateTo();
        assertFalse(testRealmLoginPage.form().isForgotPasswordLinkPresent());
        log.info("verified reset password is disabled");
    }
    
    @Test
    public void rememberMe() {
        
        log.info("enabling remember me");
        loginSettingsPage.form().setRememberMeAllowed(true);
        assertTrue(loginSettingsPage.form().isRememberMeAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");
        
        log.info("login with remember me checked");
        testAccountPage.navigateTo();
        testRealmLoginPage.form().rememberMe(true);
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testAccountPage);
        
        assertTrue("Cookie KEYCLOAK_REMEMBER_ME should be present.", getCookieNames().contains("KEYCLOAK_REMEMBER_ME"));
        
        log.info("verified remember me is enabled");
        
        log.info("disabling remember me");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setRememberMeAllowed(false);
        assertFalse(loginSettingsPage.form().isRememberMeAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("disabled");
        
        testAccountPage.navigateTo();
        testAccountPage.signOut();
        assertTrue(testRealmLoginPage.form().isLoginButtonPresent());
        assertFalse(testRealmLoginPage.form().isRememberMePresent());
        log.info("verified remember me is disabled");
        
    }
    
    @Test 
    public void verifyEmail() {

        MailServer.start();
        MailServer.createEmailAccount(testUser.getEmail(), "password");        
        
        log.info("enabling verify email in login settings");
        loginSettingsPage.form().setVerifyEmailAllowed(true);
        assertTrue(loginSettingsPage.form().isVerifyEmailAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("enabled");

        log.info("configure smtp server in test realm");
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setSmtpServer(suiteContext.getSmtpServer());
        testRealmResource().update(testRealmRep);
        
        testAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        Assert.assertEquals("An email with instructions to verify your email address has been sent to you.", 
                testRealmVerifyEmailPage.getInstructionMessage());
        
        log.info("verified verify email is enabled");
        
        log.info("disabling verify email");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setVerifyEmailAllowed(false);
        assertFalse(loginSettingsPage.form().isVerifyEmailAllowed());
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("disabled");
        
        log.debug("create new test user");
        UserRepresentation newUser = createUserRepresentation("new_user", "new_user@email.test", "new", "user", true);
        setPasswordFor(newUser, PASSWORD);
        String id = createUserAndResetPasswordWithAdminClient(testRealmResource(), newUser, PASSWORD);
        newUser.setId(id);
        
        log.info("log in as new user");
        testAccountPage.navigateTo();        
        testRealmLoginPage.form().login(newUser);
        assertCurrentUrlStartsWith(testAccountPage);
                
        log.info("verified verify email is disabled");
        
        MailServer.stop();
    }
    
    @Test
    public void requireSSLAllRequests() throws InterruptedException {
        log.info("set require ssl for all requests");
        loginSettingsPage.form().selectRequireSSL(RequireSSLOption.all);
        loginSettingsPage.form().save();
        assertAlertSuccess();
        log.debug("set");
        
        log.info("check HTTPS required");
        String accountPageUri = testAccountPage.toString();
        if (AUTH_SERVER_SSL_REQUIRED) { // quick and dirty (and hopefully provisional) workaround to force HTTP
            accountPageUri = accountPageUri
                    .replace("https", "http")
                    .replace(AUTH_SERVER_PORT, System.getProperty("auth.server.http.port"));
        }
        URLUtils.navigateToUri(accountPageUri);
        Assert.assertEquals("HTTPS required", testAccountPage.getErrorMessage());
    }
    
    private Set<String> getCookieNames() {
        Set<Cookie> cookies = driver.manage().getCookies();
        Set<String> cookieNames = new HashSet<>();
        for (Cookie cookie : cookies) {
            cookieNames.add(cookie.getName());
        }
        return cookieNames;
    }
}
