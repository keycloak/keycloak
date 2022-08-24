package org.keycloak.testsuite.forms;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.EnterRecoveryAuthnCodePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.SelectAuthenticatorPage;
import org.keycloak.testsuite.pages.SetupRecoveryAuthnCodesPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.openqa.selenium.WebDriver;
import org.junit.Assert;
import  org.keycloak.testsuite.util.WaitUtils;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.keycloak.common.Profile.Feature.RECOVERY_CODES;

/**
 * Backup Code Authentication test
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
@EnableFeature(value = RECOVERY_CODES, skipRestart = true)
public class RecoveryAuthnCodesAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    private static final String BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES = "Browser with Recovery Authentication Codes";

    private static final int BRUTE_FORCE_FAIL_ATTEMPTS = 3;

    @Drone
    protected WebDriver driver;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePage;

    @Page
    protected SetupRecoveryAuthnCodesPage setupRecoveryAuthnCodesPage;

    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;

    @Page
    protected PasswordPage passwordPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    void configureBrowserFlowWithRecoveryAuthnCodes(KeycloakTestingClient testingClient) {
        final String newFlowAlias = BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES;
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.REQUIRED, reqSubFlow -> reqSubFlow
                                // Add authenticators to this flow: 1 PASSWORD, 2 Another subflow with having only OTP as child
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, PasswordFormFactory.PROVIDER_ID)
                                .addSubFlowExecution("Recovery-Authn-Codes subflow", AuthenticationFlow.BASIC_FLOW, AuthenticationExecutionModel.Requirement.ALTERNATIVE, altSubFlow -> altSubFlow
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RecoveryAuthnCodesFormAuthenticatorFactory.PROVIDER_ID)
                                )
                        )
                )
                .defineAsBrowserFlow()
        );

        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        String userId = createUser("test", "test-user@localhost", "password", UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
    }

    // In a sub-flow with alternative credential executors, test whether Recovery Authentication Codes are working
    @Test
    public void testAuthenticateRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient);
            testRealm().flows().removeRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertAttemptedUsernameAvailability(false);
            loginUsernameOnlyPage.login("test-user@localhost");
            // On the password page, username should be shown as we know the user
            passwordPage.assertCurrent();
            passwordPage.assertAttemptedUsernameAvailability(true);
            Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
            passwordPage.assertTryAnotherWayLinkAvailability(true);
            List<String> generatedRecoveryAuthnCodes = RecoveryAuthnCodesUtils.generateRawCodes();
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
                CredentialModel recoveryAuthnCodesCred = RecoveryAuthnCodesCredentialModel.createFromValues(
                        generatedRecoveryAuthnCodes,
                        System.currentTimeMillis(),
                        null);
                user.credentialManager().createStoredCredential(recoveryAuthnCodesCred);
            });
            passwordPage.clickTryAnotherWayLink();
            selectAuthenticatorPage.assertCurrent();
            Assert.assertEquals(Arrays.asList(SelectAuthenticatorPage.PASSWORD, SelectAuthenticatorPage.RECOVERY_AUTHN_CODES), selectAuthenticatorPage.getAvailableLoginMethods());
            selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.RECOVERY_AUTHN_CODES);
            enterRecoveryAuthnCodePage.assertCurrent();
            enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber()));
            enterRecoveryAuthnCodePage.clickSignInButton();
            enterRecoveryAuthnCodePage.assertAccountLinkAvailability(true);
        } finally {
            // Remove saved Recovery Authentication Codes to keep a clean slate after this test
            enterRecoveryAuthnCodePage.assertAccountLinkAvailability(true);
            enterRecoveryAuthnCodePage.clickAccountLink();
            assertThat(driver.getTitle(), containsString("Account Management"));
            // Revert copy of browser flow to original to keep clean slate after this test
            BrowserFlowTest.revertFlows(testRealm(), BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES);
        }
    }

    //// In a sub-flow with alternative credential executors, test whether setup Recovery Authentication Codes flow is working
    @Test
    public void testSetupRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient);
            RequiredActionProviderSimpleRepresentation simpleRepresentation = new RequiredActionProviderSimpleRepresentation();
            simpleRepresentation.setProviderId(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            simpleRepresentation.setName(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            testRealm().flows().registerRequiredAction(simpleRepresentation);
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertAttemptedUsernameAvailability(false);
            loginUsernameOnlyPage.login("test-user@localhost");
            // On the password page, username should be shown as we know the user
            passwordPage.assertCurrent();
            //passwordPage.assertAttemptedUsernameAvailability(true);
            Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
            passwordPage.login("password");
            setupRecoveryAuthnCodesPage.assertCurrent();
            setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        } finally {
            // Remove saved backup codes to keep a clean slate after this test
            setupRecoveryAuthnCodesPage.assertAccountLinkAvailability(true);
            setupRecoveryAuthnCodesPage.clickAccountLink();
            assertThat(driver.getTitle(), containsString("Account Management"));
            testRealm().flows().removeRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            // Revert copy of browser flow to original to keep clean slate after this test
            BrowserFlowTest.revertFlows(testRealm(), BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES);
        }
    }


    @Test
    public void testBruteforceProtectionRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient);
            RealmRepresentation rep = testRealm().toRepresentation();
            rep.setBruteForceProtected(true);
            testRealm().update(rep);
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertAttemptedUsernameAvailability(false);
            loginUsernameOnlyPage.login("test-user@localhost");
            // On the password page, username should be shown as we know the user
            passwordPage.assertCurrent();
            passwordPage.assertAttemptedUsernameAvailability(true);
            Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
            passwordPage.assertTryAnotherWayLinkAvailability(true);
            List<String> generatedRecoveryAuthnCodes = RecoveryAuthnCodesUtils.generateRawCodes();
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
                CredentialModel recoveryAuthnCodesCred = RecoveryAuthnCodesCredentialModel.createFromValues(
                        generatedRecoveryAuthnCodes,
                        System.currentTimeMillis(),
                        null);
                user.credentialManager().createStoredCredential(recoveryAuthnCodesCred);
            });
            passwordPage.clickTryAnotherWayLink();
            selectAuthenticatorPage.assertCurrent();
            Assert.assertEquals(Arrays.asList(SelectAuthenticatorPage.PASSWORD, SelectAuthenticatorPage.RECOVERY_AUTHN_CODES), selectAuthenticatorPage.getAvailableLoginMethods());
            selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.RECOVERY_AUTHN_CODES);
            enterRecoveryAuthnCodePage.assertCurrent();
            generatedRecoveryAuthnCodes.forEach(code -> System.out.println(code));
            for(int i=0; i < (BRUTE_FORCE_FAIL_ATTEMPTS - 1); i++) {
                long randomNumber = (long)Math.random()*1000000000000L;
                enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(String.valueOf(randomNumber));
                enterRecoveryAuthnCodePage.clickSignInButton();
                WaitUtils.waitForPageToLoad();
                enterRecoveryAuthnCodePage.assertCurrent();
                String feedbackText = enterRecoveryAuthnCodePage.getFeedbackText();
                Assert.assertEquals(feedbackText, "Invalid recovery authentication code");
            }
            // Now enter the right code which should not work
            enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber()));
            enterRecoveryAuthnCodePage.clickSignInButton();
            // Message changes after exhausting number of brute force attempts
            Assert.assertEquals(enterRecoveryAuthnCodePage.getFeedbackText(), "Invalid username or password.");
            enterRecoveryAuthnCodePage.assertAccountLinkAvailability(false);
        } finally {
            RealmRepresentation rep = testRealm().toRepresentation();
            rep.setBruteForceProtected(false);
            testRealm().update(rep);
            // Revert copy of browser flow to original to keep clean slate after this test
            BrowserFlowTest.revertFlows(testRealm(), BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES);
        }
    }

}
