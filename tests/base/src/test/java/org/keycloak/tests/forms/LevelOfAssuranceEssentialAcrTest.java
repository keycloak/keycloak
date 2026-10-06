package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class LevelOfAssuranceEssentialAcrTest {

    private static final String FLOW_ALIAS = "step-up-flow";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectEvents
    Events events;

    @InjectMailServer
    MailServer mailServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    RegisterPage registerPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @BeforeEach
    public void setUpFlow() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                        )
                        .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );
    }

    @Test
    public void essentialAcrNotReachedWithSsoFails() {
        loginWithUsernamePassword();

        // the user has no OTP, so level 2 can't be reached and no step is interactive
        loginFormWithAcrClaim(true, "2").open();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void essentialAcrNotReachedWithSsoAndPromptNoneFails() {
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "2").prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE).open();

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.UNMET_AUTHENTICATION_REQUIREMENTS, response.getError());
        assertNull(response.getErrorDescription());
        assertNull(response.getCode());
        assertAcrNotFulfilledEvent(1);
        assertNull(events.poll());
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsFails() {
        configureFlowWithoutLevelConditions();

        loginFormWithAcrClaim(true, "2").open();
        fillUsernameAndPassword();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsWithSsoFails() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "2").open();
        passwordPage.fillPassword(user.getPassword());
        passwordPage.submit();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsAndPromptNoneWithSsoRequiresLogin() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "2").prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE).open();

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.LOGIN_REQUIRED, response.getError());
        assertNull(response.getCode());
    }

    @Test
    public void essentialAcrLevel1WithoutConditionsSucceeds() {
        configureFlowWithoutLevelConditions();

        loginFormWithAcrClaim(true, "1").open();
        fillUsernameAndPassword();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrLevel1WithoutConditionsWithSsoRequiresReauthentication() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "1").open();
        passwordPage.assertCurrent();
        passwordPage.fillPassword(user.getPassword());
        passwordPage.submit();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsAndPromptNoneNonInteractiveFails() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();
        configureFlowWithAllowAccess();

        // the allow access step completes the login without interaction once the cookie authenticator started the step-up
        loginFormWithAcrClaim(true, "2").prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE).open();

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.UNMET_AUTHENTICATION_REQUIREMENTS, response.getError());
        assertNull(response.getCode());
        assertAcrNotFulfilledEvent(1);
    }

    @Test
    public void nonEssentialAcrLevel2WithoutConditionsAndPromptNoneNonInteractiveSucceeds() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();
        configureFlowWithAllowAccess();

        loginFormWithAcrClaim(false, "2").prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE).open();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrLevel1AboveConfiguredLevel0Fails() {
        // the condition defines the password as level 0, the active login does not raise it
        configureFlowWithLevel0Condition();

        loginFormWithAcrClaim(true, "1").open();
        fillUsernameAndPassword();

        assertAcrNotFulfilled(1, 0);
    }

    @Test
    public void essentialAcrLevel0ConfiguredConditionReportsLevel0() {
        configureFlowWithLevel0Condition();

        loginFormWithAcrClaim(true, "0").open();
        fillUsernameAndPassword();

        assertLoggedInWithAcr("0");
    }

    @Test
    public void essentialAcrLevel0WithoutConditionsWithSsoSucceeds() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "0").open();

        assertLoggedInWithAcr("0");
    }

    @Test
    public void essentialAcrLevel0WithoutConditionsAndPromptNoneWithSsoSucceeds() {
        configureFlowWithoutLevelConditions();
        loginWithUsernamePassword();

        loginFormWithAcrClaim(true, "0").prompt(OIDCLoginProtocol.PROMPT_VALUE_NONE).open();

        driver.waiting().waitForOAuthCallback();
        assertNull(oauth.parseLoginResponse().getError());
        assertLoggedInWithAcr("0");
    }

    @Test
    public void nonEssentialAcrLevel2WithoutConditionsSucceeds() {
        configureFlowWithoutLevelConditions();

        loginFormWithAcrClaim(false, "2").open();
        fillUsernameAndPassword();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrNotReachedWithoutSsoFails() {
        // the user is authenticated outside of level subflows at level 1 and can't reach level 2 without OTP
        configureFlowWithFirstFactorOutsideLevelSubflows(false);

        loginFormWithAcrClaim(true, "2").open();
        fillUsernameAndPassword();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void essentialAcrNotReachedWithoutLevelConditionEvaluatedFails() {
        // the level 2 subflow is disabled by the condition evaluated before the level condition, so no level condition is evaluated at all
        configureFlowWithFirstFactorOutsideLevelSubflows(true);

        loginFormWithAcrClaim(true, "2").open();
        fillUsernameAndPassword();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void essentialAcrLevel1WithoutLevelConditionEvaluatedSucceeds() {
        // no level condition is evaluated, but the active login is level 1, the same level as reported in the acr claim
        configureFlowWithFirstFactorOutsideLevelSubflows(true);

        loginFormWithAcrClaim(true, "1").open();
        fillUsernameAndPassword();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrNotReachedWithSsoAndPromptLoginFails() {
        configureFlowWithFirstFactorOutsideLevelSubflows(true);
        loginWithUsernamePassword();

        // prompt=login skips the step-up branch of the cookie authenticator, and the user is already known so only the password is asked
        loginFormWithAcrClaim(true, "2").prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();
        passwordPage.fillPassword(user.getPassword());
        passwordPage.submit();

        assertAcrNotFulfilled(1);
    }

    @Test
    public void optionalAcrNotReachedWithSsoSucceeds() {
        loginWithUsernamePassword();

        loginFormWithAcrClaim(false, "2").open();

        driver.waiting().waitForOAuthCallback();
        EventAssertion.expectLoginSuccess(events.poll());
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals("1", oauth.parseToken(tokenResponse.getIdToken(), IDToken.class).getAcr());
    }

    @Test
    public void essentialAcrLevel1WithoutConditionsWithLevelsFromStepUpFlowReauthenticates() {
        // the levels authenticated by the step-up flow are kept in the user session, but they do not apply to a flow without conditions
        loginWithUsernamePassword();
        configureFlowWithoutLevelConditions();

        loginFormWithAcrClaim(true, "1").open();
        passwordPage.assertCurrent();
        passwordPage.fillPassword(user.getPassword());
        passwordPage.submit();

        assertLoggedInWithAcr("1");
    }

    @Test
    public void essentialAcrLevel2RegistrationFails() {
        String username = registerWithAcrClaim("2");

        assertAcrNotFulfilledOnRegistration(username);
    }

    @Test
    public void essentialAcrLevel1RegistrationSucceeds() {
        registerWithAcrClaim("1");

        assertRegisteredWithAcr("1");
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsRegistrationFails() {
        configureFlowWithoutLevelConditions();

        String username = registerWithAcrClaim("2");

        assertAcrNotFulfilledOnRegistration(username);
    }

    @Test
    public void essentialAcrLevel1WithoutConditionsRegistrationSucceeds() {
        configureFlowWithoutLevelConditions();

        registerWithAcrClaim("1");

        assertRegisteredWithAcr("1");
    }

    @Test
    public void essentialAcrLevel2WithoutConditionsResetCredentialsFails() throws IOException {
        configureFlowWithoutLevelConditions();
        realm.updateWithCleanup(r -> r.resetPasswordAllowed(true)
                .smtp(MailServerConfiguration.HOST, Integer.parseInt(MailServerConfiguration.PORT), MailServerConfiguration.FROM));

        loginFormWithAcrClaim(true, "2").open();
        loginUsernamePage.fillLoginWithUsernameOnly(user.getUsername());
        loginUsernamePage.submit();
        passwordPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(user.getUsername());
        EventAssertion.assertSuccess(events.poll()).type(EventType.SEND_RESET_PASSWORD);

        assertTrue(mailServer.waitForIncomingEmail(1));
        driver.driver().navigate().to(MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage()).trim());

        // the reset credentials flow finishes the authentication at level 1
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Authentication requirements not fulfilled"));
        EventAssertion.assertError(events.poll())
                .error(Errors.GENERIC_AUTHENTICATION_ERROR)
                .details(Details.AUTHENTICATION_ERROR_DETAIL, acrNotFulfilledDetail(2, 1));
    }

    private void loginWithUsernamePassword() {
        oauth.openLoginForm();
        fillUsernameAndPassword();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(events.poll());
    }

    private void fillUsernameAndPassword() {
        loginUsernamePage.fillLoginWithUsernameOnly(user.getUsername());
        loginUsernamePage.submit();
        passwordPage.fillPassword(user.getPassword());
        passwordPage.submit();
    }

    /**
     * A browser flow that contains no {@link ConditionalLoaAuthenticatorFactory} at all: username and password only.
     */
    private void configureFlowWithoutLevelConditions() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }

    /**
     * Username form followed by a conditional subflow with a level 0 condition and the password form.
     */
    private void configureFlowWithLevel0Condition() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "0");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                        )
                )
                .defineAsBrowserFlow()
        );
    }

    /**
     * A browser flow whose forms contain only the allow access authenticator, so a login with a known user completes without interaction.
     */
    private void configureFlowWithAllowAccess() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }

    private void assertLoggedInWithAcr(String expectedAcr) {
        driver.waiting().waitForOAuthCallback();
        EventAssertion.expectLoginSuccess(events.poll());
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(expectedAcr, oauth.parseToken(tokenResponse.getIdToken(), IDToken.class).getAcr());
    }

    /**
     * Username and password forms outside of any level subflow, followed by a conditional level 2 subflow with the OTP form.
     * The "user configured" condition disables the level 2 subflow for a user without OTP. When it is placed before the level
     * condition, the level condition is never evaluated.
     */
    private void configureFlowWithFirstFactorOutsideLevelSubflows(boolean userConfiguredConditionFirst) {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> {
                            if (userConfiguredConditionFirst) {
                                subflow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID);
                            }
                            subflow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                    });
                            if (!userConfiguredConditionFirst) {
                                subflow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID);
                            }
                            subflow.addAuthenticatorExecution(Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                        })
                )
        );
    }

    /**
     * Opens the login form with the essential acr claim and registers a new user from the registration link.
     *
     * @return username of the registered user
     */
    private String registerWithAcrClaim(String acr) {
        realm.updateWithCleanup(r -> r.registrationAllowed(true));
        String username = "registered-" + UUID.randomUUID();
        realm.cleanup().add(r -> r.users().searchByUsername(username, true).forEach(u -> r.users().get(u.getId()).remove()));

        loginFormWithAcrClaim(true, acr).open();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("First", "Last", username + "@example.com", username, "password");
        return username;
    }

    private void assertRegisteredWithAcr(String expectedAcr) {
        driver.waiting().waitForOAuthCallback();
        EventAssertion.assertSuccess(events.poll()).type(EventType.REGISTER);
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(expectedAcr, oauth.parseToken(tokenResponse.getIdToken(), IDToken.class).getAcr());
    }

    private void assertAcrNotFulfilledOnRegistration(String username) {
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Authentication requirements not fulfilled"));
        // the registration starts the login event, which fails; the error page rolls the transaction back, so the user is not created
        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.GENERIC_AUTHENTICATION_ERROR)
                .details(Details.AUTHENTICATION_ERROR_DETAIL, acrNotFulfilledDetail(2, 1));
        assertTrue(realm.admin().users().searchByUsername(username, true).isEmpty());
    }

    private void assertAcrNotFulfilled(int fulfilledLevel) {
        assertAcrNotFulfilled(2, fulfilledLevel);
    }

    private void assertAcrNotFulfilled(int requestedLevel, int fulfilledLevel) {
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Authentication requirements not fulfilled"));
        assertAcrNotFulfilledEvent(requestedLevel, fulfilledLevel);
    }

    private void assertAcrNotFulfilledEvent(int fulfilledLevel) {
        assertAcrNotFulfilledEvent(2, fulfilledLevel);
    }

    private void assertAcrNotFulfilledEvent(int requestedLevel, int fulfilledLevel) {
        EventAssertion.expectLoginError(events.poll())
                .error(Errors.GENERIC_AUTHENTICATION_ERROR)
                .details(Details.AUTHENTICATION_ERROR_DETAIL, acrNotFulfilledDetail(requestedLevel, fulfilledLevel));
    }

    private static String acrNotFulfilledDetail(int requestedLevel, int fulfilledLevel) {
        return "Forced level of authentication did not meet the requirements. Requested level: " + requestedLevel + ", Fulfilled level: " + fulfilledLevel;
    }

    private LoginUrlBuilder loginFormWithAcrClaim(boolean essential, String acr) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(List.of(acr));

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Map.of(IDToken.ACR, acrClaim));

        return oauth.loginForm().claims(claims);
    }
}
