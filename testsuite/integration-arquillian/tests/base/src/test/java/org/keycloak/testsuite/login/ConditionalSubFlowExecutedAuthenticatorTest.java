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
package org.keycloak.testsuite.login;

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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * <p>Test for the ConditionalSubFlowExecutedAuthenticator. A <em>test</em> parent
 * flow is created to substitute the original <em>browser</em> flow. This flow
 * adds inside the forms sub-flow the condition sub-flow executed defined
 * over the conditional OTP step. This way tests check if the OTP step was
 * executed or not. The sub-flow adds a deny step for the condition.</p>
 *
 * @author rmartinc
 */
public class ConditionalSubFlowExecutedAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected OneTimeCode oneTimeCodePage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // no-op
    }

    @Test
    public void testWithoutOtpConfiguredExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.doLogin("test-user@localhost", "password");

        // no otp => check executed => allowed
        checkAllowed("test-user@localhost");
    }

    @Test
    public void testWithoutOtpConfiguredNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.doLogin("test-user@localhost", "password");

        // no otp => check not-executed => denied
        checkDenied();
    }

    @Test
    public void testWithOtpConfiguredExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.doLogin("user-with-one-configured-otp", "password");

        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));

        // otp => check executed => denied
        checkDenied();
    }

    @Test
    public void testWithOtpConfiguredNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("test Browser - Conditional 2FA", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.doLogin("user-with-two-configured-otp", "password");

        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));

        // otp => check not-executed => allowed
        checkAllowed("user-with-two-configured-otp");
    }

    @Test
    public void testWithInvalidFlowExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("invalid flow", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_EXECUTED);

        oauth.doLogin("test-user@localhost", "password");

        // no flow => check executed => allowed
        checkAllowed("test-user@localhost");
    }

    @Test
    public void testWithInvalidFlowNotExecuted() {
        configureConditionalSubFlowExecutedAuthenticatorInFlow("invalid flow", ConditionalSubFlowExecutedAuthenticatorFactory.CHECK_RESULT_NOT_EXECUTED);

        oauth.doLogin("test-user@localhost", "password");

        // no flow => check executed => denied
        checkDenied();
    }

    private void checkDenied() {
        errorPage.assertCurrent();
        Assert.assertEquals("Access denied", errorPage.getError());

        events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.ACCESS_DENIED).assertEvent();
    }

    private void checkAllowed(String username) {
        String code = oauth.parseLoginResponse().getCode();
        Assert.assertNotNull(code);
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assert.assertNull(res.getError());
        Assert.assertNotNull(res.getAccessToken());

        events.expectLogin().user(AssertEvents.isUUID()).detail(Details.USERNAME, username).assertEvent();
    }

    private void configureConditionalSubFlowExecutedAuthenticatorInFlow(String flowName, String check) {
        // clone the browser flow and add another conditional flow that checks
        // if the OTP flow was executed or not executed to deny the access

        RealmResource realmRes = testRealm();
        AuthenticationManagementResource authRes = realmRes.flows();

        // revert the flows if already changed
        RealmRepresentation realmRep = realmRes.toRepresentation();
        if (!realmRep.getBrowserFlow().equals("browser")) {
            realmRep.setBrowserFlow("browser");
            realmRes.update(realmRep);
            authRes.deleteFlow(authRes.getFlows().stream().filter(f -> "test".equals(f.getAlias())).findAny().get().getId());
        }

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
        realmRep.setBrowserFlow("test");
        realmRes.update(realmRep);
    }
}
