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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_ROLE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OTP_CONTROL_USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_ROLE;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@KeycloakIntegrationTest
public class CustomAuthFlowOTPTest extends AbstractCustomAccountManagementTest {

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @InjectPage
    private LoginConfigTotpConfigPage loginConfigTotpPage;

    @InjectPage
    private LoginTotpPage loginTotpPage;

    @BeforeEach
    public void configureUserProfile() {
        UserProfileResource userProfileRes = managedRealm.admin().users().userProfile();
        UserProfileUtil.enableUnmanagedAttributes(userProfileRes);
    }

    private void configureRequiredActions() {
        //set configure TOTP as required action to test user
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(CONFIGURE_TOTP.name());
        testUser.setRequiredActions(requiredActions);
        managedRealm.admin().users().get(testUser.getId()).update(testUser);
    }

    private void configureOTP() {
        //configure OTP for test user
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        String totpSecret = loginConfigTotpPage.getTotpSecret();
        loginConfigTotpPage.configure(totp.generateTOTP(totpSecret));
        AccountHelper.logout(managedRealm.admin(), testUser.getUsername());

        //verify that user has OTP configured
        testUser = managedRealm.admin().users().get(testUser.getId()).toRepresentation();
        assertTrue(testUser.getRequiredActions().isEmpty());
    }

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

    private void reuseExistingOtp(boolean allowReusingExistingOtp) {
        RealmRepresentation originalRealm = managedRealm.admin().toRepresentation();
        managedRealm.cleanup().add(realmResource -> realmResource.update(originalRealm));

        RealmRepresentation updatedRealm = org.keycloak.testframework.realm.RepresentationUtils.clone(originalRealm);
        updatedRealm.setBrowserFlow("browser");
        updatedRealm.setOtpPolicyCodeReusable(allowReusingExistingOtp);
        managedRealm.admin().update(updatedRealm);

        updateRequirement("browser", Requirement.REQUIRED, (authExec) -> authExec.getDisplayName().equals("Browser - Conditional 2FA"));
        updateRequirement("Browser - Conditional 2FA", OTPFormAuthenticatorFactory.PROVIDER_ID, Requirement.REQUIRED);
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        loginConfigTotpPage.assertCurrent();

        //configure OTP for test user
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        final String totpSecret = loginConfigTotpPage.getTotpSecret();
        assertThat(totpSecret, notNullValue());

        final String generatedOtp = totp.generateTOTP(totpSecret);
        assertThat(generatedOtp, notNullValue());

        loginConfigTotpPage.configure(generatedOtp);
        AccountHelper.logout(managedRealm.admin(), testUser.getUsername());

        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        loginTotpPage.assertCurrent();
        loginTotpPage.login(generatedOtp);
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

    private RoleRepresentation getOrCreateOTPRole() {
        try {
            return managedRealm.admin().roles().get("otp_role").toRepresentation();
        } catch (NotFoundException ex) {
            RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
            managedRealm.admin().roles().create(role);
            //obtain id
            return managedRealm.admin().roles().get("otp_role").toRepresentation();
        }
    }

    private GroupRepresentation getOrCreateOTPRoleInGroup() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("otp_group");
        RoleRepresentation role  = getOrCreateOTPRole();
        managedRealm.admin().groups().add(group);
        // obtain id
        GroupRepresentation groupRep = managedRealm.admin().groups().groups("otp_group",0,1).get(0);
        managedRealm.admin().groups().group(groupRep.getId()).roles().realmLevel().add(Arrays.asList(role));
        // reread
        return managedRealm.admin().groups().groups("otp_group",0,1).get(0);
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

    private String authServerPort() {
        int port = URI.create(managedRealm.getBaseUrl()).getPort();
        return port < 0 ? "80" : String.valueOf(port);
    }

    private void setConditionalOTPForm(Map<String, String> config) {
        List<AuthenticationFlowRepresentation> authFlows = getAuthMgmtResource().getFlows();
        for (AuthenticationFlowRepresentation flow : authFlows) {
            if ("ConditionalOTPFlow".equals(flow.getAlias())) {
                //update realm browser flow
                RealmRepresentation realm = managedRealm.admin().toRepresentation();
                realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
                managedRealm.admin().update(realm);

                getAuthMgmtResource().deleteFlow(flow.getId());
                break;
            }
        }

        String flowAlias = "ConditionalOTPFlow";
        String provider = "auth-conditional-otp-form";
        
        //create flow
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(flowAlias);
        flow.setDescription("");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        
        try (Response response = getAuthMgmtResource().createFlow(flow)) {
            assertEquals(201, response.getStatus(), flowAlias + " create success");
        }
        
        //add execution - username-password form
        Map<String, Object> data = new HashMap<>();
        data.put("provider", "auth-username-password-form");
        getAuthMgmtResource().addExecution(flowAlias, data);
        
        //set username-password requirement to required
        updateRequirement(flowAlias, "auth-username-password-form", Requirement.REQUIRED);

        //add execution - conditional OTP
        data.clear();
        data.put("provider", provider);
        getAuthMgmtResource().addExecution(flowAlias, data);
        
        //set Conditional 2FA requirement to required
        updateRequirement(flowAlias, provider, Requirement.REQUIRED);
        
        //update realm browser flow
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(flowAlias);
        managedRealm.admin().update(realm);

        if (config != null) {
            //get executionId
            String executionId = getExecution(flowAlias, provider).getId();

            //prepare auth config
            AuthenticatorConfigRepresentation authConfig = new AuthenticatorConfigRepresentation();
            authConfig.setAlias("Config alias");
            authConfig.setConfig(config);

            //add auth config to the execution
            try (Response response = getAuthMgmtResource().newExecutionConfig(executionId, authConfig)) {
                assertEquals(201, response.getStatus(), "new execution success");
                String configId = ApiUtil.getCreatedId(response);
                managedRealm.cleanup().add(realmResource -> realmResource.flows().removeAuthenticatorConfig(configId));
            }
        }
    }

    public static class LoginConfigTotpConfigPage extends LoginConfigTotpPage {
        public LoginConfigTotpConfigPage(ManagedWebDriver driver) {
            super(driver);
        }

        public void configure(String totp) {
            WebElement totpInput = driver.findElement(org.openqa.selenium.By.id("totp"));
            totpInput.clear();
            totpInput.sendKeys(totp);

            driver.findElement(org.openqa.selenium.By.cssSelector("input[type=\"submit\"], #saveTOTPBtn")).click();
        }
    }

    public static class LoginTotpPage extends AbstractLoginPage {
        @FindBy(id = "otp")
        private WebElement otpInput;

        @FindBy(css = "[type=\"submit\"]")
        private WebElement submitButton;

        public LoginTotpPage(ManagedWebDriver driver) {
            super(driver);
        }

        public void login(String totp) {
            otpInput.clear();
            if (totp != null) {
                otpInput.sendKeys(totp);
            }
            submitButton.click();
        }

        @Override
        public String getExpectedPageId() {
            return "login-login-otp";
        }
    }

}
