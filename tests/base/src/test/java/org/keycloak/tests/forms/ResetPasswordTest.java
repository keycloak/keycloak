package org.keycloak.tests.forms;

import java.io.IOException;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.TokenVerifier;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.validation.Validation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.MailServerConfiguration;

import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oidc.par.endpoints.ParEndpoint.REQUEST_URI_PREFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @InjectRealm(ref = PROVIDER_REALM_NAME, config = ProviderRealmConfig.class)
    protected ManagedRealm providerRealm;

    @InjectRealm(ref = CONSUMER_REALM_NAME, config = ConsumerRealmConfig.class)
    protected ManagedRealm consumerRealm;

    @InjectUser(realmRef = CONSUMER_REALM_NAME, ref = "consumerUser", config = ConsumerUserConfig.class)
    protected ManagedUser consumerUser;

    @InjectOAuthClient(realmRef = CONSUMER_REALM_NAME, config = BrokerAppClientConfig.class)
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected RegisterPage registerPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;
    
    @InjectPage
    protected InfoPage infoPage;

    @InjectMailServer
    MailServer mailServer;

    @InjectEvents(realmRef = CONSUMER_REALM_NAME)
    protected Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

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
        driver.open(changePasswordUrl.trim());

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

        assertTrue(driver.page().getPageSource().contains("Your account has been updated."));
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
    public void resetPassword() throws IOException {
        resetPassword(USER_LOGIN);
    }

    @Test
    public void resetPasswordByEmail() throws IOException {
        resetPassword(USER_EMAIL);
    }

    @Test
    public void resetPasswordWithSpacesInUsername() throws IOException {
        resetPassword(" " + USER_LOGIN + " ");
    }

    @Test
    public void resetPasswordDoesNotSetEmailVerified() throws IOException {
        consumerUser.updateWithCleanup(u -> u.emailVerified(false));
        resetPassword(USER_LOGIN);

        assertFalse(consumerUser.admin().toRepresentation().isEmailVerified());
    }

    @Test
    public void resetPasswordTwice() throws IOException {
        String changePasswordUrl = resetPassword(USER_LOGIN);
        events.clear();

        // KC_RESTART doesn't exist, it was deleted after the first successful reset-password flow finished
        assertSecondPasswordResetFails(changePasswordUrl, BROKER_APP_CLIENT_ID);
    }

    @Test
    public void resetPasswordTwiceInNewBrowser() throws IOException {
        String changePasswordUrl = resetPassword(USER_LOGIN);
        events.clear();

        String resetUri = consumerRealm.getBaseUrl() + "/login-actions/reset-credentials";
        driver.open(resetUri); // This is necessary to delete the KC_RESTART cookie that is restricted to the realm path
        driver.cookies().deleteAll();

        assertSecondPasswordResetFails(changePasswordUrl, BROKER_APP_CLIENT_ID);
    }

    @Test
    public void resetPasswordLinkTestAppWithRedirectUriParam() throws IOException {
        String redirectUri = oauth.getRedirectUri();
        String resetUri = consumerRealm.getBaseUrl() + "/login-actions/reset-credentials?client_id=" + BROKER_APP_CLIENT_ID + "&redirect_uri=" + redirectUri;
        String userId = consumerUser.getId();

        driver.open(resetUri);

        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(USER_LOGIN);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage().orElse(null));

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.SEND_RESET_PASSWORD)
                .userId(userId).clientId(BROKER_APP_CLIENT_ID)
                .details(Details.USERNAME, USER_LOGIN).details(Details.EMAIL, USER_EMAIL)
                .details(Details.REDIRECT_URI, redirectUri).sessionId(null);

        MimeMessage message = mailServer.getLastReceivedMessage();
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
        driver.open(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword("resetPassword", "resetPassword");

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).clientId(BROKER_APP_CLIENT_ID)
                .userId(userId).details(Details.USERNAME, USER_LOGIN).details(Details.REDIRECT_URI, redirectUri);
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).clientId(BROKER_APP_CLIENT_ID)
                .userId(userId).details(Details.USERNAME, USER_LOGIN).details(Details.REDIRECT_URI, redirectUri);

        // Re-authentication is forced after a credential reset by default, so the info page is shown
        // instead of being redirected directly to the application.
        assertTrue(driver.page().getPageSource().contains("Your account has been updated."));
    }

    @Test
    public void resetPasswordTwiceWithPARRequestInBetween() throws Exception {
        String changePasswordUrl = resetPassword(USER_LOGIN);
        events.clear();

        // Try to create manually the cache key from actionToken
        String queryString = changePasswordUrl.substring(changePasswordUrl.indexOf("?") + 1);
        MultivaluedHashMap<String, String> params = UriUtils.decodeQueryString(queryString);
        String key = params.getFirst(Constants.KEY);
        TokenVerifier<DefaultActionTokenKey> tokenVerifier = TokenVerifier.create(key, DefaultActionTokenKey.class);
        DefaultActionTokenKey aToken = tokenVerifier.getToken();
        String serializedKey1 = aToken.serializeKey();
        String serializedKey2 = serializedKey1 + SingleUseObjectProvider.REVOKED_KEY;

        // Try to send PAR request with manually created requestUri related to the manually created serialized keys
        String state = "testSuccessfulSinglePar";
        oauth.loginForm()
                .requestUri(REQUEST_URI_PREFIX + serializedKey1)
                .state(state)
                .open();
        oauth.loginForm()
                .requestUri(REQUEST_URI_PREFIX + serializedKey2)
                .state(state)
                .open();
        events.clear();

        // KC_RESTART doesn't exist, it was deleted after the first successful reset-password flow finished
        assertSecondPasswordResetFails(changePasswordUrl, BROKER_APP_CLIENT_ID);
    }

    @Test
    public void resetPasswordWithLengthPasswordPolicy() throws IOException {
        consumerRealm.updateWithCleanup(r -> r.passwordPolicy("length"));
        String userId = consumerUser.getId();

        initiateResetPasswordFromResetPasswordPage(USER_LOGIN);

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.SEND_RESET_PASSWORD).sessionId(null)
                .userId(userId).details(Details.USERNAME, USER_LOGIN).details(Details.EMAIL, USER_EMAIL);

        MimeMessage message = mailServer.getLastReceivedMessage();
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.open(changePasswordUrl.trim());
        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("invalid", "invalid");
        assertEquals("Invalid password: minimum length 8.", updatePasswordPage.getError());

        EventAssertion.assertError(events.poll()).type(EventType.UPDATE_CREDENTIAL_ERROR).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).userId(userId).details(Details.USERNAME, USER_LOGIN);
        EventAssertion.assertError(events.poll()).type(EventType.UPDATE_PASSWORD_ERROR).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).userId(userId).details(Details.USERNAME, USER_LOGIN);

        updatePasswordPage.changePassword("resetPasswordWithPasswordPolicy", "resetPasswordWithPasswordPolicy");

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PASSWORD).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).userId(userId).details(Details.USERNAME, USER_LOGIN);
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).userId(userId).details(Details.USERNAME, USER_LOGIN);
    }

    @Test
    public void resetPasswordWithPasswordHistoryPolicy() throws IOException {
        // Block passwords that are equal to previous passwords. Default value is 3.
        consumerRealm.updateWithCleanup(r -> r.passwordPolicy("passwordHistory"));

        final String historyError = "Invalid password: must not be equal to any of last 3 passwords.";
        try {
            timeOffSet.set(2000000);
            resetPassword(USER_LOGIN, "password1");

            resetPasswordInvalidPassword(USER_LOGIN, "password1", historyError);

            timeOffSet.set(4000000);
            resetPassword(USER_LOGIN, "password2");

            resetPasswordInvalidPassword(USER_LOGIN, "password1", historyError);
            resetPasswordInvalidPassword(USER_LOGIN, "password2", historyError);

            timeOffSet.set(6000000);
            resetPassword(USER_LOGIN, "password3");

            resetPasswordInvalidPassword(USER_LOGIN, "password1", historyError);
            resetPasswordInvalidPassword(USER_LOGIN, "password2", historyError);
            resetPasswordInvalidPassword(USER_LOGIN, "password3", historyError);

            timeOffSet.set(8000000);
            resetPassword(USER_LOGIN, USER_PASSWORD);
        } finally {
            timeOffSet.set(0);
        }
    }

    private void initiateResetPasswordFromResetPasswordPage(String username) {
        oauth.openLoginForm();
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);

        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage().orElse(null));
    }

    private String resetPassword(String username) throws IOException {
        return resetPassword(username, "resetPassword");
    }

    private String resetPassword(String username, String password) throws IOException {
        String userId = consumerUser.getId();
        initiateResetPasswordFromResetPasswordPage(username);

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.SEND_RESET_PASSWORD)
                .userId(userId)
                .details(Details.USERNAME, username.trim())
                .details(Details.EMAIL, USER_EMAIL)
                .sessionId(null);

        MimeMessage message = mailServer.getLastReceivedMessage();
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.open(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        assertEquals("You need to change your password.", updatePasswordPage.getFeedbackMessage());
        updatePasswordPage.changePassword(password, password);

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId).details(Details.USERNAME, username.trim());
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .userId(userId).details(Details.USERNAME, username.trim());

        // Re-authentication is forced after a credential reset by default, so the info page is shown
        // instead of the user being automatically logged in to the application.
        assertEquals("Your account has been updated.", infoPage.getInfo());

        return changePasswordUrl;
    }

    private void resetPasswordInvalidPassword(String username, String password, String error) throws IOException {
        String userId = consumerUser.getId();
        initiateResetPasswordFromResetPasswordPage(username);

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.SEND_RESET_PASSWORD).userId(userId).sessionId(null)
                .details(Details.USERNAME, username).details(Details.EMAIL, USER_EMAIL);

        MimeMessage message = mailServer.getLastReceivedMessage();
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

        driver.open(changePasswordUrl.trim());

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword(password, password);

        updatePasswordPage.assertCurrent();
        assertEquals(error, updatePasswordPage.getError());
        EventAssertion.assertError(events.poll()).type(EventType.UPDATE_CREDENTIAL_ERROR).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).userId(userId).details(Details.USERNAME, USER_LOGIN);
        EventAssertion.assertError(events.poll()).type(EventType.UPDATE_PASSWORD_ERROR).details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).error(Errors.PASSWORD_REJECTED).userId(userId).details(Details.USERNAME, USER_LOGIN);
    }

    private void assertSecondPasswordResetFails(String changePasswordUrl, String clientId) {
        driver.open(changePasswordUrl.trim());

        errorPage.assertCurrent();
        assertEquals("Action expired. Please continue with login now.", errorPage.getError());

        EventAssertion.assertError(events.poll()).type(EventType.RESET_PASSWORD_ERROR)
                .clientId(clientId)
                .sessionId(null)
                .userId(consumerUser.getId())
                .error(Errors.EXPIRED_CODE);
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                .resetPasswordAllowed(true)
                .registrationAllowed(true)
                .smtp(MailServerConfiguration.HOST, Integer.parseInt(MailServerConfiguration.PORT), MailServerConfiguration.FROM);
        }
    }

    private static class ConsumerUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username(USER_LOGIN)
                    .name("Vilmos", "Szabó-Nagy")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD);
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
}
