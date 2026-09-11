package org.keycloak.tests.forms;

import java.io.IOException;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.validation.Validation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmAttributesBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@KeycloakIntegrationTest
public class ResetPasswordTest {


    protected static final String CONSUMER_REALM_NAME = "consumer";
    protected static final String PROVIDER_REALM_NAME = "provider";
    protected static final String IDP_ALIAS = "test-identity-provider";
    protected static final String IDP_CLIENT_ID = "test-idp-client";
    protected static final String IDP_CLIENT_SECRET = "test-idp-secret";
    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_EMAIL = "spam@vnagy.eu";
    protected static final String USER_PASSWORD = "password";
    protected static final String BROKER_APP_CLIENT_ID = "broker-app";
    protected static final String BASE_URL = "http://localhost:8080";

    protected static final String TEST_REALM_NAME = "test";
    protected static final String TEST_APP_CLIENT_ID = "test-app";
    protected static final String TEST_APP_SECRET = "password";
    protected static final String LOGIN_TEST_USER = "login-test";
    protected static final String LOGIN_TEST_EMAIL = "login@test.com";
    protected static final String LOGIN_TEST_PASSWORD = "password";
    protected static final String TEST_APP_BASE_URL = BASE_URL + "/app/auth";

    @InjectRealm(ref = PROVIDER_REALM_NAME, config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    protected ManagedRealm providerRealm;

    @InjectRealm(ref = CONSUMER_REALM_NAME, config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    protected ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = CONSUMER_REALM_NAME, config = BrokerAppClientConfig.class, lifecycle = LifeCycle.METHOD)
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected RegisterPage registerPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectMailServer
    MailServer mailServer;

    @InjectEvents(realmRef = CONSUMER_REALM_NAME)
    protected Events events;

    @InjectRealm(ref = TEST_REALM_NAME, config = TestRealmConfig.class, lifecycle = LifeCycle.METHOD)
    protected ManagedRealm testRealm;

    @InjectOAuthClient(ref = TEST_REALM_NAME, realmRef = TEST_REALM_NAME, config = TestAppClientConfig.class, lifecycle = LifeCycle.METHOD)
    protected OAuthClient testOAuth;

    @InjectEvents(ref = TEST_REALM_NAME, realmRef = TEST_REALM_NAME)
    protected Events testEvents;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectTimeOffSet(enableForCaches = true)
    protected TimeOffSet timeOffSet;

    @Test
    public void shouldOfferAllOidcOptionOnLoginPageUserTriesToResetTheirPasswordAndGoesBack() {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
            .alias(IDP_ALIAS)
            .attribute("clientId", IDP_CLIENT_ID)
            .attribute("clientSecret", IDP_CLIENT_SECRET)
            .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
            .attribute("authorizationUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/auth")
            .attribute("tokenUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/token")
            .attribute("jwksUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/certs")
            .attribute("logoutUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/logout")
            .build();

        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).remove());

        oauth.loginForm().open();
        loginPage.assertCurrent();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();
        resetPasswordPage.backToLogin();
        String urlWhenBackFromRegistrationPage = driver.getCurrentUrl();
        loginPage.assertCurrent();
        assertDoesNotThrow(() -> loginPage.findSocialButton(IDP_ALIAS));

        loginPage.resetPassword();
        resetPasswordPage.changePassword(USER_LOGIN);
        driver.driver().navigate().back();
        driver.driver().navigate().back();
        String urlWhenWentBackFromResetPassword = driver.getCurrentUrl();
        assertEquals(
            "The user clicks the back button twice. Their browser sends them to the same URL where they were previously",
            urlWhenBackFromRegistrationPage, urlWhenWentBackFromResetPassword
        );
        loginPage.assertCurrent();
        assertDoesNotThrow(() -> loginPage.findSocialButton(IDP_ALIAS));
    }

    @Test
    public void testLoginPageClearsUserFromContextIfUserNavigatesBackFromResetPassword() {
        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.clickBackToLogin();
        loginPage.assertCurrent();

        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(USER_LOGIN);

        driver.driver().navigate().back();
        driver.driver().navigate().back();
        // we're at the login page now, and if we go back, the register page opens correctly
        driver.driver().navigate().back();

        registerPage.assertCurrent();
    }

    @Test
    public void resetPasswordEmailLinkWorksAfterNavigatingBackToLoginPage() throws IOException {
        final var user = consumerRealm.admin().users().search(USER_LOGIN).get(0);
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.backToLogin();

        String urlWhenBackFromRegistrationPage = driver.getCurrentUrl();

        loginPage.assertCurrent();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(USER_LOGIN);

        EventRepresentation sendResetPasswordEvent = events.poll();
        EventAssertion.assertSuccess(sendResetPasswordEvent)
            .type(EventType.SEND_RESET_PASSWORD)
            .sessionId(sendResetPasswordEvent.getSessionId())
            .userId(user.getId())
            .details(Details.USERNAME, USER_LOGIN)
            .details(Details.EMAIL, USER_EMAIL);


        MimeMessage message = mailServer.getReceivedMessages()[0];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        // Navigate back to the login page, which triggers UsernamePasswordForm to clear the user from the auth session
        driver.driver().navigate().back();
        driver.driver().navigate().back();
        String urlWhenWentBackFromResetPassword = driver.getCurrentUrl();
        assertEquals(urlWhenBackFromRegistrationPage, urlWhenWentBackFromResetPassword);
        loginPage.assertCurrent();

        events.clear();
        driver.driver().navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        assertEquals("You need to change your password.", updatePasswordPage.getFeedbackMessage());
        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        EventRepresentation updatePasswordEvent = events.poll();
        EventAssertion.assertSuccess(updatePasswordEvent)
            .type(EventType.UPDATE_PASSWORD)
            .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
            .details(Details.USERNAME, USER_LOGIN)
            .userId(user.getId());


        EventRepresentation updateCredentialEvent = events.poll();
        EventAssertion.assertSuccess(updateCredentialEvent)
            .type(EventType.UPDATE_CREDENTIAL)
            .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
            .details(Details.USERNAME, USER_LOGIN)
            .userId(user.getId());

        assertTrue(driver.page().getPageSource().contains("Happy days"));
    }

    @Test
    public void resetPasswordMaxLengthUsername() {

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword("a".repeat(Validation.MAX_USERNAME_LENGTH + 1));
        resetPasswordPage.assertCurrent();

        EventAssertion.assertError(events.poll())
                .type(EventType.RESET_PASSWORD_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .withoutDetails(Details.USERNAME);
    }

    @Test
    public void resetPasswordExactMaxLengthUsername() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword("a".repeat(Validation.MAX_USERNAME_LENGTH));
        loginPage.assertCurrent();

        EventAssertion.assertError(events.poll())
                .type(EventType.RESET_PASSWORD_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .details(Details.USERNAME, "a".repeat(Validation.MAX_USERNAME_LENGTH));
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();
        testRealm.updateWithCleanup(r -> r.passwordPolicy("length"));

        initiateResetPasswordFromResetPasswordPage(LOGIN_TEST_USER);

        assertEquals(1, mailServer.getReceivedMessages().length);
        MimeMessage message = mailServer.getReceivedMessages()[0];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .sessionId(null)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER)
                .details(Details.EMAIL, LOGIN_TEST_EMAIL);

        driver.driver().navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword("invalid", "invalid");

        assertEquals("Invalid password: minimum length 8.", updatePasswordPage.getError());

        EventAssertion.assertError(testEvents.poll())
                .type(EventType.UPDATE_CREDENTIAL_ERROR)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .error(Errors.PASSWORD_REJECTED)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);
        EventAssertion.assertError(testEvents.poll())
                .type(EventType.UPDATE_PASSWORD_ERROR)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .error(Errors.PASSWORD_REJECTED)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);
        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);

        assertTrue(testOAuth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(testEvents.poll())
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER)
                .getEvent();
        String sessionId = loginEvent.getSessionId();

        AccessTokenResponse tokenResponse = getTokenResponse();
        testOAuth.logoutForm().idTokenHint(tokenResponse.getIdToken()).postLogoutRedirectUri(testOAuth.getRedirectUri()).open();

        EventAssertion.expectLogoutSuccess(testEvents.poll()).sessionId(sessionId).userId(userId);

        testOAuth.openLoginForm();
        loginPage.fillLogin(LOGIN_TEST_USER, "resetPasswordWithPasswordPolicy");
        loginPage.submit();

        assertTrue(testOAuth.parseLoginResponse().isSuccess());
        EventAssertion.expectLoginSuccess(testEvents.poll())
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);
    }

    @Test
    public void resetPasswordExpiredCode() throws IOException {
        assertLinkExpiredOnLoginPage(360);
    }

    @Test
    public void resetPasswordExpiredCodeShort() throws IOException {
        testRealm.updateWithCleanup(r -> r.actionTokenGeneratedByUserLifespan(60));

        assertLinkExpiredOnLoginPage(70);
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionLifespan() throws IOException {
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create().resetCredentialsLifespan(60)));

        assertLinkExpiredOnLoginPage(70);
    }

    @Test
    public void resetPasswordExpiredCodeShortPerActionMultipleTimeouts() throws IOException {
        // Make sure that one attribute setting won't affect the other
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create()
                .resetCredentialsLifespan(60)
                .verifyEmailLifespan(300)));

        assertLinkExpiredOnLoginPage(70);
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSession() throws IOException {
        testRealm.updateWithCleanup(r -> r.actionTokenGeneratedByUserLifespan(60));

        assertLinkExpiredOnErrorPage(70);
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionLifespan() throws IOException {
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create().resetCredentialsLifespan(60)));

        assertLinkExpiredOnErrorPage(70);
    }

    @Test
    public void resetPasswordExpiredCodeAndAuthSessionPerActionMultipleTimeouts() throws IOException {
        // Make sure that one attribute setting won't affect the other
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create()
                .resetCredentialsLifespan(60)
                .verifyEmailLifespan(300)));

        assertLinkExpiredOnErrorPage(70);
    }

    // KEYCLOAK-5061
    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlow() throws IOException {
        testRealm.updateWithCleanup(r -> r.actionTokenGeneratedByUserLifespan(60));

        assertLinkExpiredInForgotPasswordFlow(70);
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionLifespan() throws IOException {
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create().resetCredentialsLifespan(60)));

        assertLinkExpiredInForgotPasswordFlow(70);
    }

    @Test
    public void resetPasswordExpiredCodeForgotPasswordFlowPerActionMultipleTimeouts() throws IOException {
        // Make sure that one attribute setting won't affect the other
        testRealm.updateWithCleanup(r -> r.attributes(RealmAttributesBuilder.create()
                .resetCredentialsLifespan(60)
                .verifyEmailLifespan(300)));

        assertLinkExpiredInForgotPasswordFlow(70);
    }

    @Test
    public void resetPasswordWithPasswordHistoryPolicy() throws IOException {
        // Block passwords that are equal to previous passwords. Default value is 3.
        testRealm.updateWithCleanup(r -> r.passwordPolicy("passwordHistory"));

        String notEqualToLastThree = "Invalid password: must not be equal to any of last 3 passwords.";

        timeOffSet.set(2000000);
        resetPassword(LOGIN_TEST_USER, "password1");

        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password1", notEqualToLastThree);

        timeOffSet.set(4000000);
        resetPassword(LOGIN_TEST_USER, "password2");

        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password1", notEqualToLastThree);
        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password2", notEqualToLastThree);

        timeOffSet.set(6000000);
        resetPassword(LOGIN_TEST_USER, "password3");

        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password1", notEqualToLastThree);
        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password2", notEqualToLastThree);
        resetPasswordInvalidPassword(LOGIN_TEST_USER, "password3", notEqualToLastThree);

        timeOffSet.set(8000000);
        resetPassword(LOGIN_TEST_USER, LOGIN_TEST_PASSWORD);
    }

    // Requests a reset-password email and returns the link it contains.
    private String requestResetPasswordEmail() throws IOException {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();

        initiateResetPasswordFromResetPasswordPage(LOGIN_TEST_USER);

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .sessionId(null)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER)
                .details(Details.EMAIL, LOGIN_TEST_EMAIL);

        assertEquals(1, mailServer.getReceivedMessages().length);
        return MailUtils.getPasswordResetEmailLink(mailServer.getReceivedMessages()[0]);
    }

    // The auth session is still around, so the expired link sends the user back to the login page.
    private void assertLinkExpiredOnLoginPage(int offsetSeconds) throws IOException {
        String changePasswordUrl = requestResetPasswordEmail();

        timeOffSet.set(offsetSeconds);

        driver.driver().navigate().to(changePasswordUrl.trim());

        loginPage.assertCurrent();
        assertEquals("Action expired. Please start again.", loginPage.getErrorMessage().orElse(null));

        assertExpiredActionTokenEvent();
    }

    // Without the auth session (cookies cleared) the expired link lands on the error page instead.
    private void assertLinkExpiredOnErrorPage(int offsetSeconds) throws IOException {
        String changePasswordUrl = requestResetPasswordEmail().replace("&amp;", "&");

        // Necessary to delete the KC_RESTART cookie, which is restricted to the /realms/test path
        driver.driver().manage().deleteAllCookies();

        timeOffSet.set(offsetSeconds);

        driver.driver().navigate().to(changePasswordUrl.trim());

        errorPage.assertCurrent();
        assertEquals("Action expired.", errorPage.getError());
        assertEquals(TEST_APP_BASE_URL, errorPage.getBackToApplicationLink());

        assertExpiredActionTokenEvent();
    }

    // Same as above but entered through the "forgot credentials" endpoint rather than the login form.
    private void assertLinkExpiredInForgotPasswordFlow(int offsetSeconds) throws IOException {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();

        // Redirect directly to the KC "forgot password" endpoint instead of the "authenticate" endpoint
        String forgotPasswordUrl = testOAuth.loginForm().build().replace("/auth?", "/forgot-credentials?");

        driver.driver().navigate().to(forgotPasswordUrl);
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(LOGIN_TEST_USER);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .sessionId(null)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER)
                .details(Details.EMAIL, LOGIN_TEST_EMAIL);

        assertEquals(1, mailServer.getReceivedMessages().length);
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(mailServer.getReceivedMessages()[0]);

        timeOffSet.set(offsetSeconds);

        driver.driver().navigate().to(changePasswordUrl.trim());

        resetPasswordPage.assertCurrent();
        assertEquals("Action expired. Please start again.", loginPage.getErrorMessage().orElse(null));

        assertExpiredActionTokenEvent();
    }

    private void assertExpiredActionTokenEvent() {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();

        EventAssertion.assertError(testEvents.poll())
                .type(EventType.EXECUTE_ACTION_TOKEN_ERROR)
                .error("expired_code")
                .clientId(null)
                .userId(userId)
                .sessionId(null)
                .details(Details.ACTION, ResetCredentialsActionToken.TOKEN_TYPE);
    }

    // Full successful reset-password flow: request the email, set a new password, land back on the app,
    // then log out and log in again with the new password.
    private void resetPassword(String username, String password) throws IOException {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();

        initiateResetPasswordFromResetPasswordPage(username);

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .userId(userId)
                .details(Details.USERNAME, username.trim())
                .details(Details.EMAIL, LOGIN_TEST_EMAIL)
                .sessionId(null);

        MimeMessage message = mailServer.getReceivedMessages()[mailServer.getReceivedMessages().length - 1];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.driver().navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        assertEquals("You need to change your password.", updatePasswordPage.getFeedbackMessage());
        updatePasswordPage.changePassword(password, password);

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .details(Details.USERNAME, username.trim());
        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId)
                .details(Details.USERNAME, username.trim());

        assertTrue(testOAuth.parseLoginResponse().isSuccess());

        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(testEvents.poll())
                .userId(userId)
                .details(Details.USERNAME, username.trim())
                .getEvent();

        AccessTokenResponse tokenResponse = getTokenResponse();
        testOAuth.logoutForm().idTokenHint(tokenResponse.getIdToken()).postLogoutRedirectUri(testOAuth.getRedirectUri()).open();

        EventAssertion.expectLogoutSuccess(testEvents.poll())
                .sessionId(loginEvent.getSessionId())
                .userId(userId);
    }

    // Requests a reset and then tries a password the policy rejects.
    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException {
        String userId = testRealm.admin().users().search(LOGIN_TEST_USER).get(0).getId();

        initiateResetPasswordFromResetPasswordPage(username);

        EventAssertion.expectRequiredAction(testEvents.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .userId(userId)
                .sessionId(null)
                .details(Details.USERNAME, username)
                .details(Details.EMAIL, LOGIN_TEST_EMAIL);

        MimeMessage message = mailServer.getReceivedMessages()[mailServer.getReceivedMessages().length - 1];
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.driver().navigate().to(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword(password, password);

        updatePasswordPage.assertCurrent();
        assertEquals(error, updatePasswordPage.getError());

        EventAssertion.assertError(testEvents.poll())
                .type(EventType.UPDATE_CREDENTIAL_ERROR)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .error(Errors.PASSWORD_REJECTED)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);
        EventAssertion.assertError(testEvents.poll())
                .type(EventType.UPDATE_PASSWORD_ERROR)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .error(Errors.PASSWORD_REJECTED)
                .userId(userId)
                .details(Details.USERNAME, LOGIN_TEST_USER);
    }

    private void initiateResetPasswordFromResetPasswordPage(String username) {
        testOAuth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
    }

    // Exchanges the authorization code for tokens and drains the resulting CODE_TO_TOKEN event.
    private AccessTokenResponse getTokenResponse() {
        String code = testOAuth.parseLoginResponse().getCode();
        AccessTokenResponse response = testOAuth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());
        testEvents.skip(); // CODE_TO_TOKEN
        return response;
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                .users(UserBuilder.create(USER_LOGIN)
                    .name("Vilmos", "Szabó-Nagy")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD))
                .resetPasswordAllowed(true)
                .registrationAllowed(true)
                .smtp(MailServerConfiguration.HOST, Integer.parseInt(MailServerConfiguration.PORT), MailServerConfiguration.FROM);
        }
    }


    static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                .users(UserBuilder.create(USER_LOGIN)
                    .name("Vilmos", "Szabó-Nagy")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD))
                .clients(ClientBuilder.create()
                    .clientId(IDP_CLIENT_ID)
                    .secret(IDP_CLIENT_SECRET)
                    .redirectUris(BASE_URL + "/realms/" + CONSUMER_REALM_NAME + "/broker/" + IDP_ALIAS + "/endpoint*")
                    .build());
        }
    }

    static class BrokerAppClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                .clientId(BROKER_APP_CLIENT_ID)
                .publicClient()
                .redirectUris(BASE_URL + "/*");
        }
    }

    static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                .eventsEnabled(true)
                .resetPasswordAllowed(true)
                .registrationAllowed(true)
                .smtp(MailServerConfiguration.HOST, Integer.parseInt(MailServerConfiguration.PORT), MailServerConfiguration.FROM)
                .users(UserBuilder.create(LOGIN_TEST_USER)
                    .name("Login", "Test")
                    .email(LOGIN_TEST_EMAIL)
                    .enabled(true)
                    .password(LOGIN_TEST_PASSWORD));
        }
    }

    static class TestAppClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                .clientId(TEST_APP_CLIENT_ID)
                .secret(TEST_APP_SECRET)
                .baseUrl(TEST_APP_BASE_URL)
                .redirectUris(BASE_URL + "/*");
        }
    }
}
