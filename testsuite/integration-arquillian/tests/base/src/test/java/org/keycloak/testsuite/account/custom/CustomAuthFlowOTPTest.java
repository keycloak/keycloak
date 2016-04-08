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
package org.keycloak.testsuite.account.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import org.junit.Before;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_ROLE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OTP_CONTROL_USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_ROLE;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class CustomAuthFlowOTPTest extends AbstractCustomAccountManagementTest {

    private final TimeBasedOTP totp = new TimeBasedOTP();
    
    @Page
    private OneTimeCode testLoginOneTimeCodePage;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testLoginOneTimeCodePage.setAuthRealm(testRealmPage);
    }
    
    @Before
    @Override
    public void beforeTest() {
        super.beforeTest();
        //set configure TOTP as required action to test user
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(CONFIGURE_TOTP.name());
        testUser.setRequiredActions(requiredActions);
        testRealmResource().users().get(testUser.getId()).update(testUser);
        
        //configure OTP for test user
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        String totpSecret = testRealmLoginPage.form().totpForm().getTotpSecret();
        testRealmLoginPage.form().totpForm().setTotp(totp.generateTOTP(totpSecret));
        testRealmLoginPage.form().totpForm().submit();
        testRealmAccountManagementPage.signOut();
        
        //verify that user has OTP configured
        testUser = testRealmResource().users().get(testUser.getId()).toRepresentation();
        Users.setPasswordFor(testUser, PASSWORD);
        assertTrue(testUser.getRequiredActions().isEmpty());
    }

    @Test
    public void requireOTPTest() {
        
        updateRequirement("browser", "auth-otp-form", Requirement.REQUIRED);
        
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    public void conditionalOTPNoDefault() {
        //prepare config - no configuration specified
        Map<String, String> config = new HashMap<>();
        setConditionalOTPForm(config);
        
        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    public void conditionalOTPDefaultSkip() {
        //prepare config - default skip
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, SKIP);
        
        setConditionalOTPForm(config);
        
        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
    
    @Test
    public void conditionalOTPDefaultForce() {
        //prepare config - default force
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);
        
        //test OTP is forced
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    public void conditionalOTPUserAttributeSkip() {
        //prepare config - user attribute, default to force
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "userSkipAttribute");
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);

        //add skip user attribute to user
        Map<String, Object> userAttributes = new HashMap<>();
        List<String> attributeValues = new ArrayList<>();
        attributeValues.add("skip");
        userAttributes.put("userSkipAttribute", attributeValues);
        testUser.setAttributes(userAttributes);
        testRealmResource().users().get(testUser.getId()).update(testUser);
        
        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
    
    @Test
    public void conditionalOTPUserAttributeForce() {
        //prepare config - user attribute, default to skip
        Map<String, String> config = new HashMap<>();
        config.put(OTP_CONTROL_USER_ATTRIBUTE, "userSkipAttribute");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);
        
        setConditionalOTPForm(config);

        //add force user attribute to user
        Map<String, Object> userAttributes = new HashMap<>();
        List<String> attributeValues = new ArrayList<>();
        attributeValues.add("force");
        userAttributes.put("userSkipAttribute", attributeValues);
        testUser.setAttributes(userAttributes);
        testRealmResource().users().get(testUser.getId()).update(testUser);
        
        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    public void conditionalOTPRoleSkip() {
        //prepare config - role, default to force
        Map<String, String> config = new HashMap<>();
        config.put(SKIP_OTP_ROLE, "otp_role");
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);

        //create role
        RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
        testRealmResource().roles().create(role);
        //obtain id
        role = testRealmResource().roles().get("otp_role").toRepresentation();
        //add role to user
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        realmRoles.add(role);
        testRealmResource().users().get(testUser.getId()).roles().realmLevel().add(realmRoles);
        
        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
    
    @Test
    public void conditionalOTPRoleForce() {
        //prepare config - role, default to skip
        Map<String, String> config = new HashMap<>();
        config.put(FORCE_OTP_ROLE, "otp_role");
        config.put(DEFAULT_OTP_OUTCOME, SKIP);
        
        setConditionalOTPForm(config);

        //create role
        RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
        testRealmResource().roles().create(role);
        //obtain id
        role = testRealmResource().roles().get("otp_role").toRepresentation();
        //add role to user
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        realmRoles.add(role);
        testRealmResource().users().get(testUser.getId()).roles().realmLevel().add(realmRoles);
        
        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    public void conditionalOTPRequestHeaderSkip() {
        //prepare config - request header skip, default to force
        Map<String, String> config = new HashMap<>();
        String port = System.getProperty("auth.server.http.port", "8180");
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, FORCE);
        
        setConditionalOTPForm(config);

        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
    
    @Test
    public void conditionalOTPRequestHeaderForce() {
        //prepare config - equest header force, default to skip
        Map<String, String> config = new HashMap<>();
        String port = System.getProperty("auth.server.http.port", "8180");
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, SKIP);
        
        setConditionalOTPForm(config);

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmLoginPage.form().totpForm().waitForTotpInputFieldPresent();
        
        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    private void setConditionalOTPForm(Map<String, String> config) {
        String flowAlias = "ConditionalOTPFlow";
        String provider = "auth-conditional-otp-form";
        
        //create flow
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(flowAlias);
        flow.setDescription("");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        
        Response response = getAuthMgmtResource().createFlow(flow);
        Assert.assertEquals(flowAlias + " create success", 201, response.getStatus());
        response.close();
        
        //add execution - username-password form
        Map<String, String> data = new HashMap<>();
        data.put("provider", "auth-username-password-form");
        getAuthMgmtResource().addExecution(flowAlias, data);
        
        //set username-password requirement to required
        updateRequirement(flowAlias, "auth-username-password-form", Requirement.REQUIRED);
        
        //add execution - conditional OTP
        data.clear();
        data.put("provider", provider);
        getAuthMgmtResource().addExecution(flowAlias, data);
        
        //set Conditional OTP requirement to required
        updateRequirement(flowAlias, provider, Requirement.REQUIRED);
        
        //update realm browser flow
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setBrowserFlow(flowAlias);
        testRealmResource().update(realm);
        
        //get executionId
        String executionId = getExecution(flowAlias, provider).getId();

        //prepare auth config
        AuthenticatorConfigRepresentation authConfig = new AuthenticatorConfigRepresentation();
        authConfig.setAlias("Config alias");
        authConfig.setConfig(config);
        
        //add auth config to the execution
        response = getAuthMgmtResource().newExecutionConfig(executionId, authConfig);
        Assert.assertEquals("new execution success", 201, response.getStatus());
        response.close();
    }
}
