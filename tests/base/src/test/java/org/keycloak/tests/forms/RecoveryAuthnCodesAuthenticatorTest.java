package org.keycloak.tests.forms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticatorFactory;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.RecoveryAuthnCodesBean;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.credential.dto.RecoveryAuthnCodesCredentialData;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.account.CredentialMetadataRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.EnterRecoveryAuthnCodePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.SelectAuthenticatorPage;
import org.keycloak.testframework.ui.page.SetupRecoveryAuthnCodesPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.requiredactions.RecoveryAuthnCodesAction.WARNING_THRESHOLD;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Backup Code Authentication test
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
@KeycloakIntegrationTest
public class RecoveryAuthnCodesAuthenticatorTest {

    private static final int BRUTE_FORCE_FAIL_ATTEMPTS = 3;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectWebDriver(ref = "driver2", lifecycle = LifeCycle.CLASS)
    ManagedWebDriver driver2;

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(ref = "test-user@localhost", config = UserCredentialTestUserConf.class, lifecycle = LifeCycle.METHOD)
    ManagedUser testUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectOAuthClient(ref = "oauth2", webDriverRef = "driver2")
    OAuthClient oauth2;

    @InjectEvents
    Events events;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    SetupRecoveryAuthnCodesPage setupRecoveryAuthnCodesPage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    SelectAuthenticatorPage selectAuthenticatorPage;

    @InjectPage
    EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePage;

    @InjectPage(ref = "enterRecoveryAuthnCodePage2", webDriverRef = "driver2")
    EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePage2;

    @TestSetup
    public void setup() {
        // enable the recovery codes as alternative in the default flow
        AuthenticationExecutionInfoRepresentation execution = managedRealm.admin().flows()
                .getExecutions(DefaultAuthenticationFlows.BROWSER_FLOW).stream()
                .filter(e -> RecoveryAuthnCodesFormAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId()))
                .findAny().orElseThrow();
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());
        managedRealm.admin().flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, execution);
    }

    private void testSetupRecoveryAuthnCodesLogoutOtherSessions(boolean logoutOtherSessions) {
        // login with the user and clean cookies
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        EventRepresentation event1 = EventAssertion.expectLoginSuccess(events.poll()).getEvent();
        assertEquals(1, testUser.admin().getUserSessions().size());
        driver.driver().navigate().to(managedRealm.getBaseUrl() + "/");
        driver.cookies().deleteAll();

        // add action to recovery codes for the test user
        UserRepresentation userRepresentation = testUser.admin().toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.admin().update(userRepresentation);

        // login and configure codes
        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        setupRecoveryAuthnCodesPage.assertCurrent();
        if (logoutOtherSessions) {
            setupRecoveryAuthnCodesPage.checkLogoutSessions();
        }
        Assertions.assertEquals(logoutOtherSessions, setupRecoveryAuthnCodesPage.isLogoutSessionsChecked());
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        if (logoutOtherSessions) {
            EventAssertion.expectLogoutSuccess(events.poll())
                    .sessionId(event1.getSessionId())
                    .details(Details.LOGOUT_TRIGGERED_BY_REQUIRED_ACTION, UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
        }

        EventRepresentation event2 = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .userId(event1.getUserId())
                .details(Details.USERNAME, testUser.getUsername())
                .details(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE).getEvent();
        event2 = EventAssertion.expectLoginSuccess(events.poll()).sessionId(event2.getDetails().get(Details.CODE_ID)).userId(event2.getUserId())
                .details(Details.USERNAME, testUser.getUsername()).getEvent();

        // assert old session is gone or is maintained
        List<UserSessionRepresentation> sessions = testUser.admin().getUserSessions();
        if (logoutOtherSessions) {
            assertEquals(1, sessions.size());
            assertEquals(event2.getSessionId(), sessions.iterator().next().getId());
        } else {
            assertEquals(2, sessions.size());
            MatcherAssert.assertThat(sessions.stream().map(UserSessionRepresentation::getId).collect(Collectors.toList()),
                    Matchers.containsInAnyOrder(event1.getSessionId(), event2.getSessionId()));
        }
    }

    @Test
    public void test01SetupRecoveryAuthnCodesLogoutOtherSessionsChecked() throws Exception {
        testSetupRecoveryAuthnCodesLogoutOtherSessions(true);
    }

    @Test
    public void test02SetupRecoveryAuthnCodesLogoutOtherSessionsNotChecked() {
        testSetupRecoveryAuthnCodesLogoutOtherSessions(false);
    }

    @Test
    public void test03SetupRecoveryAuthnCodesModifyGeneratedAt() throws Exception {
        // add the configure recovery codes action
        UserRepresentation userRepresentation = testUser.admin().toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.admin().update(userRepresentation);

        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        setupRecoveryAuthnCodesPage.assertCurrent();
        List<String> values = setupRecoveryAuthnCodesPage.getRecoveryAuthnCodes();

        // modify generatedAt to a fixed value
        setupRecoveryAuthnCodesPage.setGeneratedAtHidden("10000");
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        setupRecoveryAuthnCodesPage.waitUntilReloaded();

        // the recovery codes are regerated as they were tampered
        setupRecoveryAuthnCodesPage.assertCurrent();
        List<String> newValues = setupRecoveryAuthnCodesPage.getRecoveryAuthnCodes();
        Assertions.assertNotEquals(values, newValues);
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventRepresentation event = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .userId(userRepresentation.getId())
                .details(Details.USERNAME, testUser.getUsername())
                .details(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE).getEvent();
        EventAssertion.expectLoginSuccess(events.poll()).sessionId(event.getDetails().get(Details.CODE_ID)).userId(event.getUserId())
                .details(Details.USERNAME, testUser.getUsername());
    }

    @Test
    public void test04SetupRecoveryAuthnCodesModifyGeneratedCodes() {
        // add the configure recovery codes action
        UserRepresentation userRepresentation = testUser.admin().toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.admin().update(userRepresentation);

        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        setupRecoveryAuthnCodesPage.assertCurrent();
        List<String> values = setupRecoveryAuthnCodesPage.getRecoveryAuthnCodes();

        // modify the codes with a new generated ones
        setupRecoveryAuthnCodesPage.setGeneratedRecoveryAuthnCodesHidden(new RecoveryAuthnCodesBean().getGeneratedRecoveryAuthnCodesAsString());
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        setupRecoveryAuthnCodesPage.waitUntilReloaded();

        // the recovery codes are regerated as they were tampered
        setupRecoveryAuthnCodesPage.assertCurrent();
        List<String> newValues = setupRecoveryAuthnCodesPage.getRecoveryAuthnCodes();
        Assertions.assertNotEquals(values, newValues);
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventRepresentation event = EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .userId(userRepresentation.getId())
                .details(Details.USERNAME, "test-user@localhost")
                .details(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE).getEvent();
        EventAssertion.expectLoginSuccess(events.poll()).sessionId(event.getDetails().get(Details.CODE_ID)).userId(event.getUserId())
                .details(Details.USERNAME, "test-user@localhost");
    }

    private List<String> createRecoveryAuthnCodesForUser() {
        List<String> generatedRecoveryAuthnCodes = RecoveryAuthnCodesUtils.generateRawCodes();
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
            CredentialModel recoveryAuthnCodesCred = RecoveryAuthnCodesCredentialModel.createFromValues(
                    generatedRecoveryAuthnCodes,
                    Time.currentTimeMillis(),
                    null);
            user.credentialManager().createStoredCredential(recoveryAuthnCodesCred);
        });
        return generatedRecoveryAuthnCodes;
    }

    private void createOtpForUser() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
            CredentialModel otp = OTPCredentialModel.createFromPolicy(realm, "secret1", "label1");
            user.credentialManager().createStoredCredential(otp);
        });
    }

    // In a sub-flow with alternative credential executors, test whether Recovery Authentication Codes are working
    @Test
    public void test05AuthenticateRecoveryAuthnCodes() {
        createOtpForUser();
        List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

        // perform the login username
        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        // click try another way
        passwordPage.clickTryAnotherWayLink();
        // select recovery codes to authenticate
        selectAuthenticatorPage.assertCurrent();
        selectAuthenticatorPage.selectLoginMethod("Recovery Authentication Code");
        enterRecoveryAuthnCodePage.assertCurrent();
        // enter recovery codes and submit
        int requestedCode = enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber();
        Assertions.assertEquals(0, requestedCode, "Incorrect code presented to login");
        enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(requestedCode));
        enterRecoveryAuthnCodePage.clickSignInButton();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, testUser.getUsername());
    }

    @Test
    public void test06AuthenticateRecoveryAuthnCodesSimultaneous() {
        List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

        // perform the login username
        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        enterRecoveryAuthnCodePage.assertCurrent();

        oauth2.openLoginForm();
        oauth2.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        enterRecoveryAuthnCodePage2.assertCurrent();

        // enter the same recovery code to the two browers
        int requestedCode = enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber();
        Assertions.assertEquals(0, requestedCode, "Incorrect code presented to login");
        enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(requestedCode));

        requestedCode = enterRecoveryAuthnCodePage2.getRecoveryAuthnCodeToEnterNumber();
        Assertions.assertEquals(0, requestedCode, "Incorrect code presented to login");
        enterRecoveryAuthnCodePage2.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(requestedCode));

        enterRecoveryAuthnCodePage.clickSignInButton();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "test-user@localhost");
        enterRecoveryAuthnCodePage2.clickSignInButton();
        enterRecoveryAuthnCodePage2.waitUntilReloaded();
        EventAssertion.expectLoginError(events.poll()).error(Errors.INVALID_USER_CREDENTIALS);
    }

    //// In a sub-flow with alternative credential executors, test whether setup Recovery Authentication Codes flow is working
    @Test
    public void test07SetupRecoveryAuthnCodes() {
        // make recovery codes required in the default browser flow
        List<AuthenticationExecutionInfoRepresentation> executions = managedRealm.admin().flows().getExecutions(DefaultAuthenticationFlows.BROWSER_FLOW);
        // change conditional 2FA sub-flow to required
        AuthenticationExecutionInfoRepresentation conditionalExec = executions.stream()
                .filter(e -> "Browser - Conditional 2FA".equals(e.getDisplayName()))
                .findAny().orElseThrow();
        conditionalExec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        managedRealm.admin().flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, conditionalExec);
        // change recovery codes to required
        AuthenticationExecutionInfoRepresentation recoveryCodesExec = executions.stream()
                .filter(e -> RecoveryAuthnCodesFormAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId()))
                .findAny().orElseThrow();
        recoveryCodesExec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        managedRealm.admin().flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, recoveryCodesExec);
        // change otp to disabled
        AuthenticationExecutionInfoRepresentation otpExec = executions.stream()
                .filter(e -> OTPFormAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId()))
                .findAny().orElseThrow();
        otpExec.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.name());
        managedRealm.admin().flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, otpExec);

        managedRealm.cleanup().add(r -> {
            // set again as default: 2FA conditional, otp and codes alternative
            conditionalExec.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL.name());
            r.flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, conditionalExec);
            recoveryCodesExec.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            r.flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, recoveryCodesExec);
            otpExec.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            r.flows().updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, otpExec);
        });

        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        setupRecoveryAuthnCodesPage.assertCurrent();
        Assertions.assertTrue(oauth.getDriver().getPageSource().contains("\"<p>\" + "),
                "recovery code download messages should be inserted via ?c, not inline in a JS string");
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void test08BruteforceProtectionRecoveryAuthnCodes() {
        managedRealm.updateWithCleanup(r -> r.bruteForceProtected(true).maxSecondaryAuthFailures(100));

        List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        enterRecoveryAuthnCodePage.assertCurrent();

        for (int i = 0; i < (BRUTE_FORCE_FAIL_ATTEMPTS - 1); i++) {
            long randomNumber = (long) (Math.random() * 1000000000000L);
            enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(String.valueOf(randomNumber));
            enterRecoveryAuthnCodePage.clickSignInButton();
            enterRecoveryAuthnCodePage.waitUntilReloaded();
            String feedbackText = enterRecoveryAuthnCodePage.getFeedbackText();
            Assertions.assertEquals("Invalid recovery authentication code", feedbackText);
        }
        // Now enter the right code which should not work
        enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber()));
        enterRecoveryAuthnCodePage.clickSignInButton();
        enterRecoveryAuthnCodePage.waitUntilReloaded();
        // Message changes after exhausting number of brute force attempts
        Assertions.assertEquals("Invalid username or password.", enterRecoveryAuthnCodePage.getFeedbackText());
    }

    @Test
    public void test09recoveryAuthnCodesWithThresholdConfigured() throws Exception {
        AuthenticationManagementResource authMgt = managedRealm.admin().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name().equals(action.getAlias()))
                .findAny().get();
        Map<String, String> origReqActionConfig = new HashMap<>(requiredAction.getConfig());

        // Configure required action with big threshold
        requiredAction.getConfig().put(WARNING_THRESHOLD, String.valueOf(RecoveryAuthnCodesUtils.QUANTITY_OF_CODES_TO_GENERATE));
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        managedRealm.cleanup().add(r -> {
            requiredAction.setConfig(origReqActionConfig);
            r.flows().updateRequiredAction(requiredAction.getAlias(), requiredAction);
        });

        // Add required action to the user
        UserRepresentation userRepresentation = testUser.admin().toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.admin().update(userRepresentation);

        // Login and setup recovery-codes
        oauth.openLoginForm();
        oauth.fillLoginForm(testUser.getUsername(), testUser.getPassword());
        setupRecoveryAuthnCodesPage.assertCurrent();
        List<String> recoveryCodes = setupRecoveryAuthnCodesPage.getRecoveryAuthnCodes();
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        // Check account REST API that warning threshold not there on recovery-codes credential as user has full count of recovery codes
        CredentialMetadataRepresentation recoveryCodesMetadata = getRecoveryCodeCredentialFromAccountRestApi(response.getAccessToken());
        Assertions.assertNull(recoveryCodesMetadata.getWarningMessageTitle(), "Expected not warning");
        Assertions.assertEquals("0/12", recoveryCodesMetadata.getInfoMessage().getParameters()[0]);
        Assertions.assertNotNull(recoveryCodesMetadata.getCredential().getCredentialData());
        RecoveryAuthnCodesCredentialData data = JsonSerialization.readValue(
                recoveryCodesMetadata.getCredential().getCredentialData(), RecoveryAuthnCodesCredentialData.class);
        Assertions.assertEquals(12, data.getTotalCodes());
        Assertions.assertEquals(12, data.getRemainingCodes());
        Assertions.assertEquals(JavaAlgorithm.SHA512, data.getAlgorithm());
        Assertions.assertNull(data.getHashIterations());

        // Re-authenticate with recovery codes
        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        loginPage.fillPassword(testUser.getPassword());
        loginPage.submit();
        enterRecoveryAuthnCodePage.assertCurrent();
        int requestedCode = enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber();
        Assertions.assertEquals(0, requestedCode, "Incorrect code presented to login");
        enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(recoveryCodes.get(requestedCode));
        enterRecoveryAuthnCodePage.clickSignInButton();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // Check warning is there as only 11 recovery codes remaining
        recoveryCodesMetadata = getRecoveryCodeCredentialFromAccountRestApi(response.getAccessToken());
        Assertions.assertEquals("recovery-codes-number-remaining", recoveryCodesMetadata.getWarningMessageTitle().getKey());
        Assertions.assertEquals("1/12", recoveryCodesMetadata.getInfoMessage().getParameters()[0]);
    }

    private CredentialMetadataRepresentation getRecoveryCodeCredentialFromAccountRestApi(String accessToken) throws Exception {
        List<AccountCredentialResource.CredentialContainer> credentials = simpleHttp
                .doGet(managedRealm.getBaseUrl() + "/account/credentials")
                .auth(accessToken).asJson(new TypeReference<>() {});
        AccountCredentialResource.CredentialContainer recoveryCode = credentials.stream()
                .filter(credential -> RecoveryAuthnCodesCredentialModel.TYPE.equals(credential.getType()))
                .findFirst().get();
        return recoveryCode.getUserCredentialMetadatas().get(0);
    }

    static class UserCredentialTestUserConf implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder builder) {
            return builder.username("test-user@localhost")
                    .password(PasswordGenerateUtil.generatePassword())
                    .name("Tom", "Brady")
                    .email("test-user@localhost")
                    .emailVerified(true);
        }
    }
}
