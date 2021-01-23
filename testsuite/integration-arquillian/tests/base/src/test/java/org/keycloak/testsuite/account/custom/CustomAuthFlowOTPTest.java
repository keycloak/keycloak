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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.PageUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.*;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class CustomAuthFlowOTPTest extends AbstractCustomAccountManagementTest {

    private final TimeBasedOTP totp = new TimeBasedOTP();
    
    @Page
    private OneTimeCode testLoginOneTimeCodePage;

    @Page
    private LoginConfigTotpPage loginConfigTotpPage;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testLoginOneTimeCodePage.setAuthRealm(testRealmPage);
    }
    
    @Before
    @Override
    public void beforeTest() {
        super.beforeTest();
    }

    private void configureRequiredActions() {
        //set configure TOTP as required action to test user
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(CONFIGURE_TOTP.name());
        testUser.setRequiredActions(requiredActions);
        testRealmResource().users().get(testUser.getId()).update(testUser);
    }

    private void configureOTP() {
        //configure OTP for test user
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
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
        //update realm browser flow
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setBrowserFlow("browser");
        testRealmResource().update(realm);

        updateRequirement("browser", Requirement.REQUIRED, (authExec) -> authExec.getDisplayName().equals("Browser - Conditional OTP"));
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void conditionalOTPNoDefault() {
        configureRequiredActions();
        configureOTP();
        //prepare config - no configuration specified
        Map<String, String> config = new HashMap<>();
        setConditionalOTPForm(config);

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

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
        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }
    
    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
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
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
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
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
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
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

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
        testUser.singleAttribute("userSkipAttribute", "skip");
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
        testUser.singleAttribute("userSkipAttribute", "force");
        testRealmResource().users().get(testUser.getId()).update(testUser);

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

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
        RoleRepresentation role = getOrCreateOTPRole();

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
        RoleRepresentation role = getOrCreateOTPRole();

        //add role to user
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        realmRoles.add(role);
        testRealmResource().users().get(testUser.getId()).roles().realmLevel().add(realmRoles);

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
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
        testRealmResource().users().get(testUser.getId()).joinGroup(group.getId());

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);

        assertTrue(loginConfigTotpPage.isCurrent());

        configureOTP();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }

    private RoleRepresentation getOrCreateOTPRole() {
        try {
            return testRealmResource().roles().get("otp_role").toRepresentation();
        } catch (NotFoundException ex) {
            RoleRepresentation role = new RoleRepresentation("otp_role", "", false);
            testRealmResource().roles().create(role);
            //obtain id
            return testRealmResource().roles().get("otp_role").toRepresentation();
        }
    }

    private GroupRepresentation getOrCreateOTPRoleInGroup() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("otp_group");
        RoleRepresentation role  = getOrCreateOTPRole();
        testRealmResource().groups().add(group);
        // obtain id
        GroupRepresentation groupRep = testRealmResource().groups().groups("otp_group",0,1).get(0);
        testRealmResource().groups().group(groupRep.getId()).roles().realmLevel().add(Arrays.asList(role));
        // reread
        return testRealmResource().groups().groups("otp_group",0,1).get(0);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void conditionalOTPRequestHeaderSkip() {
        //prepare config - request header skip, default to force
        Map<String, String> config = new HashMap<>();
        String port = AUTH_SERVER_PORT;
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, FORCE);

        setConditionalOTPForm(config);

        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void conditionalOTPRequestHeaderForce() {
        //prepare config - equest header force, default to skip
        Map<String, String> config = new HashMap<>();
        String port = AUTH_SERVER_PORT;
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //test OTP is required
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertEquals(PageUtils.getPageTitle(driver), "Mobile Authenticator Setup");

        configureOTP();
        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }

    private void setConditionalOTPForm(Map<String, String> config) {
        List<AuthenticationFlowRepresentation> authFlows = getAuthMgmtResource().getFlows();
        for (AuthenticationFlowRepresentation flow : authFlows) {
            if ("ConditionalOTPFlow".equals(flow.getAlias())) {
                //update realm browser flow
                RealmRepresentation realm = testRealmResource().toRepresentation();
                realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
                testRealmResource().update(realm);

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
        
        Response response = getAuthMgmtResource().createFlow(flow);
        assertEquals(flowAlias + " create success", 201, response.getStatus());
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
        assertEquals("new execution success", 201, response.getStatus());
        getCleanup().addAuthenticationConfigId(ApiUtil.getCreatedId(response));
        response.close();
    }

}
