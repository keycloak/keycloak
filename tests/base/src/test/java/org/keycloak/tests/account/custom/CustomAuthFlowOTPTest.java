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
package org.keycloak.tests.account.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_ROLE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OTP_CONTROL_USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_ROLE;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@KeycloakIntegrationTest
public class CustomAuthFlowOTPTest extends AbstractCustomAuthFlowOTPTest {

    @Test
    public void requireOTPTest() {
        //update realm browser flow
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow("browser");
        managedRealm.admin().update(realm);

        updateRequirement("browser", Requirement.REQUIRED, (authExec) -> authExec.getDisplayName().equals("Browser - Conditional 2FA"));
        updateRequirement("Browser - Conditional 2FA", OTPFormAuthenticatorFactory.PROVIDER_ID, Requirement.REQUIRED);
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void reuseExistingOTP() {
        reuseExistingOtp(true);
    }

    @Test
    public void notReuseExistingOTP() {
        reuseExistingOtp(false);
    }

    @Test
    public void conditionalOTPNoDefault() {
        configureRequiredActions();
        configureOTP();
        //prepare config - no configuration specified
        Map<String, String> config = new HashMap<>();
        setConditionalOTPForm(config);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPDefaultSkip() {
        //prepare config - default skip
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }
    
    @Test
    public void conditionalOTPDefaultForce() {

        //prepare config - default force
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);
        
        //test OTP is forced
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }
    
    @Test
    
    public void conditionalOTPNoDefaultWithChecks() {
        configureRequiredActions();
        configureOTP();
        //prepare config - no configuration specified
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "noSuchUserSkipAttribute");
        config.put(SKIP_OTP_ROLE, "no_such_otp_role");
        config.put(FORCE_OTP_ROLE, "no_such_otp_role");
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        setConditionalOTPForm(config);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPDefaultSkipWithChecks() {
        //prepare config - default skip
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "noSuchUserSkipAttribute");
        config.put(SKIP_OTP_ROLE, "no_such_otp_role");
        config.put(FORCE_OTP_ROLE, "no_such_otp_role");
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }
    
    @Test
    public void conditionalOTPDefaultForceWithChecks() {
        //prepare config - default force
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "noSuchUserSkipAttribute");
        config.put(SKIP_OTP_ROLE, "no_such_otp_role");
        config.put(FORCE_OTP_ROLE, "no_such_otp_role");
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "NoSuchHost: nolocalhost:65536");
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);
        
        //test OTP is forced
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }
    
    @Test
    public void conditionalOTPUserAttributeSkip() {
        //prepare config - user attribute, default to force
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "userSkipAttribute");
        config.put(DEFAULT_OTP_OUTCOME, FORCE);

        setConditionalOTPForm(config);

        //add skip user attribute to user
        testUser.singleAttribute("userSkipAttribute", "skip");
        managedRealm.admin().users().get(testUser.getId()).update(testUser);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void conditionalOTPUserAttributeForce() {
        //prepare config - user attribute, default to skip
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "userSkipAttribute");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //add force user attribute to user
        testUser.singleAttribute("userSkipAttribute", "force");
        managedRealm.admin().users().get(testUser.getId()).update(testUser);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPRoleSkip() {
        //prepare config - role, default to force
        Map<String, String> config = new HashMap<>();
        config.put(SKIP_OTP_ROLE, "otp_role");
        config.put(DEFAULT_OTP_OUTCOME, FORCE);

        setConditionalOTPForm(config);

        //create role
        RoleRepresentation role = getOrCreateOTPRole();

        //add role to user
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        realmRoles.add(role);
        managedRealm.admin().users().get(testUser.getId()).roles().realmLevel().add(realmRoles);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void conditionalOTPRoleForce() {
        //prepare config - role, default to skip
        Map<String, String> config = new HashMap<>();
        config.put(FORCE_OTP_ROLE, "otp_role");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //create role
        RoleRepresentation role = getOrCreateOTPRole();

        //add role to user
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        realmRoles.add(role);
        managedRealm.admin().users().get(testUser.getId()).roles().realmLevel().add(realmRoles);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPRoleForceViaGroup() {
        //prepare config - role, default to skip
        Map<String, String> config = new HashMap<>();
        config.put(FORCE_OTP_ROLE, "otp_role");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //create otp group with role included
        GroupRepresentation group = getOrCreateOTPRoleInGroup();

        //add group to user
        managedRealm.admin().users().get(testUser.getId()).joinGroup(group.getId());

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPEmptyConfiguration() {
        // prepare config empty
        setConditionalOTPForm(null);

        // test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        loginConfigTotpPage.assertCurrent();

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        // verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    @Test
    public void conditionalOTPRequestHeaderSkip() {
        //prepare config - request header skip, default to force
        Map<String, String> config = new HashMap<>();
        String port = authServerPort();
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, FORCE);

        setConditionalOTPForm(config);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void conditionalOTPRequestHeaderForce() {
        //prepare config - equest header force, default to skip
        Map<String, String> config = new HashMap<>();
        String port = authServerPort();
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertEquals("Mobile Authenticator Setup", driver.findElement(By.id("kc-page-title")).getText());

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

}
