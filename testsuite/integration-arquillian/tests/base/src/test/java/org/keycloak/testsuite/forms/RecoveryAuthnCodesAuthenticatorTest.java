package org.keycloak.testsuite.forms;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.RecoveryAuthnCodesBean;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.EnterRecoveryAuthnCodePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.SelectAuthenticatorPage;
import org.keycloak.testsuite.pages.SetupRecoveryAuthnCodesPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.keycloak.common.Profile.Feature.RECOVERY_CODES;

/**
 * Backup Code Authentication test
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@EnableFeature(value = RECOVERY_CODES, skipRestart = true)
public class RecoveryAuthnCodesAuthenticatorTest extends AbstractChangeImportedUserPasswordsTest {

    private static final String BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES = "Browser with Recovery Authentication Codes";

    private static final int BRUTE_FORCE_FAIL_ATTEMPTS = 3;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    @SecondBrowser
    protected LoginUsernameOnlyPage loginUsernameOnlyPageSecondBrowser;

    @Page
    protected EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePage;

    @Page
    @SecondBrowser
    protected EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePageSecondBrowser;

    @Page
    protected SetupRecoveryAuthnCodesPage setupRecoveryAuthnCodesPage;

    @Page
    protected SelectAuthenticatorPage selectAuthenticatorPage;

    @Page
    @SecondBrowser
    protected SelectAuthenticatorPage selectAuthenticatorPageSecondBrowser;

    @Page
    protected PasswordPage passwordPage;

    @Page
    @SecondBrowser
    protected PasswordPage passwordPageSecondBrowser;

    @Page
    protected AppPage appPage;

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    void configureBrowserFlowWithRecoveryAuthnCodes(KeycloakTestingClient testingClient, long delay) {
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
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, "delayed-authenticator", config -> {
                                            config.setAlias("delayed-suthenticator-config");
                                            config.setConfig(Map.of("delay", Long.toString(delay)));
                                        })
                                )
                        )
                )
                .defineAsBrowserFlow()
        );

        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        createUser("test", "test-user@localhost", generatePassword("test-user@localhost"), UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
    }

    private void testSetupRecoveryAuthnCodesLogoutOtherSessions(boolean logoutOtherSessions) {
        // login with the user using the second driver
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);
        oauth2.doLogin("test-user@localhost", getPassword("test-user@localhost"));
        EventRepresentation event1 = events.expectLogin().assertEvent();
        assertEquals(1, testUser.getUserSessions().size());

        // add action to recovery codes for the test user
        UserRepresentation userRepresentation = testUser.toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.update(userRepresentation);

        // login and configure codes
        loginPage.open();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        setupRecoveryAuthnCodesPage.assertCurrent();
        if (!logoutOtherSessions) {
            setupRecoveryAuthnCodesPage.uncheckLogoutSessions();
        }
        Assert.assertEquals(logoutOtherSessions, setupRecoveryAuthnCodesPage.isLogoutSessionsChecked());
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();
        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        if (logoutOtherSessions) {
            events.expectLogout(event1.getSessionId())
                    .detail(Details.LOGOUT_TRIGGERED_BY_REQUIRED_ACTION, UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name())
                    .assertEvent();
        }

        EventRepresentation event2 = events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(event1.getUserId())
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE)
                .assertEvent();
        event2 = events.expectLogin().user(event2.getUserId()).session(event2.getDetails().get(Details.CODE_ID))
                .detail(Details.USERNAME, "test-user@localhost").assertEvent();

        // assert old session is gone or is maintained
        List<UserSessionRepresentation> sessions = testUser.getUserSessions();
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
    public void test03SetupRecoveryAuthnCodesModifyGeneratedAt() {
        // add the configure recovery codes action
        UserResource testUser = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRepresentation = testUser.toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.update(userRepresentation);

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        setupRecoveryAuthnCodesPage.assertCurrent();

        // modify generatedAt to a fixed value
        setupRecoveryAuthnCodesPage.setGeneratedAtHidden("10000");
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        // the recovery codes are regerated as they were tampered
        setupRecoveryAuthnCodesPage.assertCurrent();
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation event = events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userRepresentation.getId())
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE)
                .assertEvent();
        events.expectLogin().user(event.getUserId()).session(event.getDetails().get(Details.CODE_ID))
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();
    }

    @Test
    public void test04SetupRecoveryAuthnCodesModifyGeneratedCodes() {
        // add the configure recovery codes action
        UserResource testUser = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRepresentation = testUser.toRepresentation();
        userRepresentation.setRequiredActions(Arrays.asList(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()));
        testUser.update(userRepresentation);

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        setupRecoveryAuthnCodesPage.assertCurrent();

        // modify the codes with a new generated ones
        setupRecoveryAuthnCodesPage.setGeneratedRecoveryAuthnCodesHidden(new RecoveryAuthnCodesBean().getGeneratedRecoveryAuthnCodesAsString());
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        // the recovery codes are regerated as they were tampered
        setupRecoveryAuthnCodesPage.assertCurrent();
        setupRecoveryAuthnCodesPage.clickSaveRecoveryAuthnCodesButton();

        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation event = events.expectRequiredAction(EventType.UPDATE_CREDENTIAL)
                .user(userRepresentation.getId())
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE)
                .assertEvent();
        events.expectLogin().user(event.getUserId()).session(event.getDetails().get(Details.CODE_ID))
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();
    }

    private void loginUsername(LoginUsernameOnlyPage loginUsernameOnlyPage, WebDriver driver) {
        loginUsernameOnlyPage.setDriver(driver);
        oauth.openLoginForm();
        loginUsernameOnlyPage.assertCurrent();
        loginUsernameOnlyPage.assertAttemptedUsernameAvailability(false);
        loginUsernameOnlyPage.login("test-user@localhost");
    }

    private void tryAnotherWay(PasswordPage passwordPage, WebDriver driver) {
        passwordPage.setDriver(driver);
        passwordPage.assertCurrent();
        passwordPage.assertAttemptedUsernameAvailability(true);
        // On the password page, username should be shown as we know the user
        Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
        passwordPage.assertTryAnotherWayLinkAvailability(true);
        passwordPage.clickTryAnotherWayLink();
    }

    private void selectRecoveryAuthnCodes(SelectAuthenticatorPage selectAuthenticatorPage, WebDriver driver) {
        selectAuthenticatorPage.setDriver(driver);
        selectAuthenticatorPage.assertCurrent();
        Assert.assertEquals(Arrays.asList(SelectAuthenticatorPage.PASSWORD, SelectAuthenticatorPage.RECOVERY_AUTHN_CODES), selectAuthenticatorPage.getAvailableLoginMethods());
        selectAuthenticatorPage.selectLoginMethod(SelectAuthenticatorPage.RECOVERY_AUTHN_CODES);
    }

    private void enterRecoveryCodes(EnterRecoveryAuthnCodePage enterRecoveryAuthnCodePage, WebDriver driver,
            int expectedCode, List<String> generatedRecoveryAuthnCodes) {
        enterRecoveryAuthnCodePage.setDriver(driver);
        enterRecoveryAuthnCodePage.assertCurrent();
        int requestedCode = enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber();
        Assert.assertEquals("Incorrect code presented to login", expectedCode, requestedCode);
        enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(requestedCode));
    }

    private void removeRequiredActionIfPresent() {
        AuthenticationManagementResource authMgt = testRealm().flows();
        authMgt.getRequiredActions().stream()
                .filter(action -> UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name().equals(action.getAlias()))
                .findAny()
                .ifPresent(action -> authMgt.removeRequiredAction(action.getAlias()));
    }

    private List<String> createRecoveryAuthnCodesForUser() {
        List<String> generatedRecoveryAuthnCodes = RecoveryAuthnCodesUtils.generateRawCodes();
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "test-user@localhost");
            CredentialModel recoveryAuthnCodesCred = RecoveryAuthnCodesCredentialModel.createFromValues(
                    generatedRecoveryAuthnCodes,
                    Time.currentTimeMillis(),
                    null);
            user.credentialManager().createStoredCredential(recoveryAuthnCodesCred);
        });
        return generatedRecoveryAuthnCodes;
    }

    private void assertIsContained(AssertEvents.ExpectedEvent expectedEvent, List<? extends EventRepresentation> actualEvents) {
        for (EventRepresentation e : actualEvents) {
            try {
                expectedEvent.assertEvent(e);
                return;
            } catch (AssertionError error) {
                // silently fail because it can be other event in the list
            }
        }
        Assert.fail("No event found in the list for " + expectedEvent);
    }


    // In a sub-flow with alternative credential executors, test whether Recovery Authentication Codes are working
    @Test
    public void test05AuthenticateRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient, 0);
            removeRequiredActionIfPresent();
            List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

            // perform the login username
            loginUsername(loginUsernameOnlyPage, driver);
            // click try another way
            tryAnotherWay(passwordPage, driver);
            // select recovery codes to authenticate
            selectRecoveryAuthnCodes(selectAuthenticatorPage, driver);
            // enter recovery codes and submit
            enterRecoveryCodes(enterRecoveryAuthnCodePage, driver, 0, generatedRecoveryAuthnCodes);
            enterRecoveryAuthnCodePage.clickSignInButton();
            enterRecoveryAuthnCodePage.assertAccountLinkAvailability(true);
            events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        } finally {
            // Revert copy of browser flow to original to keep clean slate after this test
            BrowserFlowTest.revertFlows(testRealm(), BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES);
        }
    }

    @Test
    public void test06AuthenticateRecoveryAuthnCodesSimultaneous() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient, 1000);
            removeRequiredActionIfPresent();
            List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

            // perform the login username
            loginUsername(loginUsernameOnlyPage, driver);
            loginUsername(loginUsernameOnlyPageSecondBrowser, driver2);
            // click try another way
            tryAnotherWay(passwordPage, driver);
            tryAnotherWay(passwordPageSecondBrowser, driver2);
            // select recivery codes to authenticate
            selectRecoveryAuthnCodes(selectAuthenticatorPage, driver);
            selectRecoveryAuthnCodes(selectAuthenticatorPageSecondBrowser, driver2);
            // enter the same recovery code to the two browers
            enterRecoveryCodes(enterRecoveryAuthnCodePage, driver, 0, generatedRecoveryAuthnCodes);
            enterRecoveryCodes(enterRecoveryAuthnCodePageSecondBrowser, driver2, 0, generatedRecoveryAuthnCodes);
            // submit fast in the two browsers using javascript to not wait for the page to load
            enterRecoveryAuthnCodePage.clickSignInButtonViaJavaScriptNoDelay();
            enterRecoveryAuthnCodePageSecondBrowser.clickSignInButtonViaJavaScriptNoDelay();

            // one event should be a login and the other a login error
            List<EventRepresentation> actualEvents = Arrays.asList(events.poll(5), events.poll(5));
            assertIsContained(events.expectLogin().detail(Details.USERNAME, "test-user@localhost"), actualEvents);
            assertIsContained(events.expect(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS), actualEvents);
        } finally {
            // Revert copy of browser flow to original to keep clean slate after this test
            BrowserFlowTest.revertFlows(testRealm(), BROWSER_FLOW_WITH_RECOVERY_AUTHN_CODES);
        }
    }

    //// In a sub-flow with alternative credential executors, test whether setup Recovery Authentication Codes flow is working
    @Test
    public void test07SetupRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient, 0);
            RequiredActionProviderSimpleRepresentation simpleRepresentation = new RequiredActionProviderSimpleRepresentation();
            simpleRepresentation.setProviderId(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            simpleRepresentation.setName(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
            testRealm().flows().registerRequiredAction(simpleRepresentation);
            loginUsername(loginUsernameOnlyPage, driver);
            // On the password page, username should be shown as we know the user
            passwordPage.assertCurrent();
            //passwordPage.assertAttemptedUsernameAvailability(true);
            Assert.assertEquals("test-user@localhost", passwordPage.getAttemptedUsername());
            passwordPage.login(getPassword("test-user@localhost"));
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
    public void test08BruteforceProtectionRecoveryAuthnCodes() {
        try {
            configureBrowserFlowWithRecoveryAuthnCodes(testingClient, 0);
            RealmRepresentation rep = testRealm().toRepresentation();
            rep.setBruteForceProtected(true);
            testRealm().update(rep);

            List<String> generatedRecoveryAuthnCodes = createRecoveryAuthnCodesForUser();

            loginUsername(loginUsernameOnlyPage, driver);
            tryAnotherWay(passwordPage, driver);
            selectRecoveryAuthnCodes(selectAuthenticatorPage, driver);

            generatedRecoveryAuthnCodes.forEach(code -> System.out.println(code));
            for(int i=0; i < (BRUTE_FORCE_FAIL_ATTEMPTS - 1); i++) {
                long randomNumber = (long)Math.random()*1000000000000L;
                enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(String.valueOf(randomNumber));
                enterRecoveryAuthnCodePage.clickSignInButton();
                enterRecoveryAuthnCodePage.assertCurrent();
                String feedbackText = enterRecoveryAuthnCodePage.getFeedbackText();
                Assert.assertEquals("Invalid recovery authentication code", feedbackText);
            }
            // Now enter the right code which should not work
            enterRecoveryAuthnCodePage.enterRecoveryAuthnCode(generatedRecoveryAuthnCodes.get(enterRecoveryAuthnCodePage.getRecoveryAuthnCodeToEnterNumber()));
            enterRecoveryAuthnCodePage.clickSignInButton();
            // Message changes after exhausting number of brute force attempts
            Assert.assertEquals("Invalid username or password.", enterRecoveryAuthnCodePage.getFeedbackText());
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
