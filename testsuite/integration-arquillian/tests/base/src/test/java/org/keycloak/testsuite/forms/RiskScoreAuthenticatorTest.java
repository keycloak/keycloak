/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.forms;

import java.util.Map;

import org.keycloak.authentication.authenticators.browser.RiskScoreAuthenticator;
import org.keycloak.authentication.authenticators.browser.RiskScoreAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.WaitUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class RiskScoreAuthenticatorTest extends AbstractChangeImportedUserPasswordsTest {

    private static final String FLOW_ALIAS = "browser - risk score";
    private static final String OTP_USER = "user-with-one-configured-otp";
    private static final String OTP_SECRET = "DJmQfC73VGFhw7D4QJ8A";
    private static final String USER_WITHOUT_OTP = "test-user@localhost";

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage loginConfigTotpPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        configureBruteForceProtection(testRealm, true);
    }

    @Before
    public void configureRiskScoreFlow() {
        clearFailures();
        createRiskScoreFlow(Map.of());
    }

    @After
    public void restoreBrowserFlow() {
        BrowserFlowTest.revertFlows(managedRealm.admin(), FLOW_ALIAS);
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        configureBruteForceProtection(realm, true);
        managedRealm.admin().update(realm);
        clearFailures();
    }

    @Test
    public void noFailureRecordPassesWithoutOtp() {
        loginWithValidPassword(OTP_USER);

        Assertions.assertFalse(loginTotpPage.isCurrent());
    }

    @Test
    public void belowThresholdPassesWithoutOtp() {
        createPasswordFailures(OTP_USER, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD - 1);

        loginWithValidPassword(OTP_USER);

        Assertions.assertFalse(loginTotpPage.isCurrent());
    }

    @Test
    public void thresholdFailuresRequireOtp() {
        createPasswordFailures(OTP_USER, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD);

        loginWithValidPassword(OTP_USER);

        loginTotpPage.assertCurrent();
    }

    @Test
    public void invalidOtpRecordsSecondaryFailure() {
        createPasswordFailures(OTP_USER, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD);
        loginWithValidPassword(OTP_USER);

        loginTotpPage.login("1234567");
        loginTotpPage.assertCurrent();
        WaitUtils.waitForBruteForceExecutors(testingClient);

        Map<String, Object> status = failureStatus(OTP_USER);
        Assertions.assertEquals(RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD + 1, status.get("numFailures"));
        Assertions.assertEquals(1, status.get("numSecondaryAuthFailures"));
    }

    @Test
    public void validOtpCompletesLoginAndClearsFailures() {
        createPasswordFailures(OTP_USER, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD);
        loginWithValidPassword(OTP_USER);

        loginTotpPage.login(new TimeBasedOTP().generateTOTP(OTP_SECRET));
        Assertions.assertFalse(loginTotpPage.isCurrent());
        WaitUtils.waitForBruteForceExecutors(testingClient);

        Assertions.assertEquals(0, failureStatus(OTP_USER).get("numFailures"));
    }

    @Test
    public void highRiskUserWithoutOtpMustConfigureTotp() {
        createPasswordFailures(USER_WITHOUT_OTP, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD);

        loginWithValidPassword(USER_WITHOUT_OTP);

        Assertions.assertTrue(loginConfigTotpPage.isCurrent());
    }

    @Test
    public void disabledBruteForceProtectionDoesNotEscalate() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        configureBruteForceProtection(realm, false);
        managedRealm.admin().update(realm);

        createInvalidPasswordAttempts(OTP_USER, RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD);
        loginWithValidPassword(OTP_USER);

        Assertions.assertFalse(loginTotpPage.isCurrent());
        Assertions.assertEquals(0, failureStatus(OTP_USER).get("numFailures"));
    }

    @Test
    public void configuredThresholdRequiresOtp() {
        BrowserFlowTest.revertFlows(managedRealm.admin(), FLOW_ALIAS);
        createRiskScoreFlow(Map.of(RiskScoreAuthenticatorFactory.FAILURE_THRESHOLD, "2"));
        createPasswordFailures(OTP_USER, 2);

        loginWithValidPassword(OTP_USER);

        loginTotpPage.assertCurrent();
    }

    private void createRiskScoreFlow(Map<String, String> authenticatorConfig) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(Requirement.REQUIRED, RiskScoreAuthenticatorFactory.PROVIDER_ID,
                                config -> config.setConfig(authenticatorConfig)))
                .defineAsBrowserFlow());
    }

    private void createPasswordFailures(String username, int count) {
        createInvalidPasswordAttempts(username, count);
        Assertions.assertEquals(count, failureStatus(username).get("numFailures"));
    }

    private void createInvalidPasswordAttempts(String username, int count) {
        for (int i = 0; i < count; i++) {
            oauth.openLoginForm();
            loginPage.login(username, "invalid");
            loginPage.assertCurrent();
        }
        WaitUtils.waitForBruteForceExecutors(testingClient);
    }

    private void loginWithValidPassword(String username) {
        oauth.openLoginForm();
        loginPage.login(username, getPassword(username));
    }

    private Map<String, Object> failureStatus(String username) {
        return managedRealm.admin().attackDetection().bruteForceUserStatus(findUser(username).getId());
    }

    private void clearFailures() {
        managedRealm.admin().attackDetection().clearAllBruteForce();
    }

    private static void configureBruteForceProtection(RealmRepresentation realm, boolean enabled) {
        realm.setBruteForceProtected(enabled);
        realm.setFailureFactor(100);
        realm.setQuickLoginCheckMilliSeconds(0L);
        realm.setMaxDeltaTimeSeconds(3600);
        realm.setMaxSecondaryAuthFailures(100);
    }
}
