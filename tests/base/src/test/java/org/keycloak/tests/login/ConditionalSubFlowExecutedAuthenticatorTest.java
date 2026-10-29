/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.login;

import java.util.Map;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.access.DenyAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalSubFlowExecutedAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.common.UserWithOneConfiguredOtp;
import org.keycloak.tests.common.UserWithTwoConfiguredOtp;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <p>Test for the ConditionalSubFlowExecutedAuthenticator. A <em>test</em> parent
 * flow is created to substitute the original <em>browser</em> flow. This flow
 * adds inside the forms sub-flow the condition sub-flow executed defined
 * over the conditional OTP step. This way tests check if the OTP step was
 * executed or not. The sub-flow adds a deny step for the condition.</p>
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ConditionalSubFlowExecutedAuthenticatorTest {

    @InjectRealm
    protected ManagedRealm managedRealm;

    @InjectUser(config = TestRealmUserConfig.class, ref = "test-user@localhost")
    protected ManagedUser testUser;

    @InjectUser(config = UserWithOneConfiguredOtp.class, ref = "user-with-one-configured-otp")
    protected ManagedUser userWithOneConfiguredOtp;

    @InjectUser(config = UserWithTwoConfiguredOtp.class, ref = "user-with-two-configured-otp")
    protected ManagedUser userWithTwoConfiguredOtp;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginTotpPage loginTotpPage;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    @InjectEvents
    protected Events events;

    @AfterEach
    void logoutUser() {
        oauth.openLogoutForm();
        logoutConfirmPage.confirmLogout();
    }

    @Test
    public void testWithoutOtpConfiguredExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(testUser.getUsername(), testUser.getPassword());
        loginPage.submit();

        // no otp => check executed => allowed
        checkAllowed(testUser.getUsername());
    }

    @Test
    public void testWithoutOtpConfiguredNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(testUser.getUsername(), testUser.getPassword());
        loginPage.submit();

        // no otp => check not-executed => denied
        checkDenied();
    }

    @Test
    public void testWithOtpConfiguredExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(userWithOneConfiguredOtp.getUsername(), userWithOneConfiguredOtp.getPassword());
        loginPage.submit();

        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP(UserWithOneConfiguredOtp.OTP_SECRET));

        // otp => check executed => denied
        checkDenied();
    }

    @Test
    public void testWithOtpConfiguredNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(userWithTwoConfiguredOtp.getUsername(), userWithTwoConfiguredOtp.getPassword());
        loginPage.submit();

        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP(UserWithOneConfiguredOtp.OTP_SECRET));

        // otp => check not-executed => allowed
        checkAllowed(userWithTwoConfiguredOtp.getUsername());
    }

    @Test
    public void testWithInvalidFlowExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("invalid flow", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(testUser.getUsername(), testUser.getPassword());
        loginPage.submit();

        // no flow => check executed => allowed
        checkAllowed(testUser.getUsername());
    }

    @Test
    public void testWithInvalidFlowNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("invalid flow", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.openLoginForm();
        loginPage.fillLogin(testUser.getUsername(), testUser.getPassword());
        loginPage.submit();

        // no flow => check executed => denied
        checkDenied();
    }

    private void checkDenied() {
        errorPage.assertCurrent();
        Assertions.assertEquals("Access denied", errorPage.getError());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).userId(null).error(Errors.ACCESS_DENIED);
    }

    private void checkAllowed(String username) {
        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertNull(res.getError());
        Assertions.assertNotNull(res.getAccessToken());

        EventAssertion.expectLoginSuccess(events.poll()).hasUserId().details(Details.USERNAME, username);
    }

    private void configureConditionalSubFlowExecutedAuthenticatorInFlow(String flowName, String check) {
        // clone the browser flow and add another conditional flow that checks
        // if the OTP flow was executed or not executed to deny the access

        RealmResource realmRes = managedRealm.admin();
        AuthenticationManagementResource authRes = realmRes.flows();

        // copy the browser flow into a test one
        authRes.copy("browser", Map.of("newName", "test"));

        // create a new flow to check if 2FA/OTP was executed or not set to conditional
        authRes.addExecutionFlow("test forms", Map.of("alias", "2FA Executed", "provider", "registration-page-form", "type", "basic-flow"));
        AuthenticationExecutionInfoRepresentation testFormExec = authRes.getExecutions("test forms").stream()
                .filter(e -> e.getFlowId() != null && AuthenticationExecutionModel.Requirement.DISABLED.name().equals(e.getRequirement()))
                .findAny().get();
        testFormExec.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL.name());
        authRes.updateExecutions("test forms", testFormExec);

        // create the condition for sub-flow executed as required
        authRes.addExecution("2FA Executed", Map.of("provider", ConditionalSubFlowExecutedAuthenticatorFactory.PROVIDER_ID));
        AuthenticationExecutionInfoRepresentation conditionExec = authRes.getExecutions("2FA Executed").stream()
                .filter(e -> ConditionalSubFlowExecutedAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId())).findAny().orElse(null);
        conditionExec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        authRes.updateExecutions("2FA Executed", conditionExec);

        // create the config for the condition
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("config");
        config.setConfig(Map.of(ConditionalSubFlowExecutedAuthenticatorFactory.FLOW_TO_CHECK, flowName, ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT, check));
        authRes.newExecutionConfig(conditionExec.getId(), config);

        // add the deny access as required if condition evaluates to true
        authRes.addExecution("2FA Executed", Map.of("provider", DenyAccessAuthenticatorFactory.PROVIDER_ID));
        AuthenticationExecutionInfoRepresentation denyExec = authRes.getExecutions("2FA Executed").stream()
                .filter(e -> DenyAccessAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId())).findAny().orElse(null);
        denyExec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        authRes.updateExecutions("2FA Executed", denyExec);

        // assign the new flow to the browser binding
        managedRealm.updateWithCleanup(r -> r.browserFlow("test"));
        // revert the flows if already changed
        managedRealm.cleanup().add(r -> {
            r.flows().deleteFlow(r.flows().getFlows().stream().filter(f -> "test".equals(f.getAlias())).findAny().get().getId());
        });
    }
}
