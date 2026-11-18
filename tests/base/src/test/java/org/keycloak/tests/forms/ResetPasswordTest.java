package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.authentication.requiredactions.util.EmailCooldownManager;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
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
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.MailServerConfiguration;

import org.junit.jupiter.api.Test;

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
    public void resetPasswordSendsOneEmailDuringCooldown() {
        String userId = consumerRealm.admin().users().search(USER_LOGIN).get(0).getId();

        submitResetPasswordForm(USER_LOGIN);
        EventAssertion.expectRequiredAction(events.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .userId(userId);
        assertTrue(mailServer.waitForIncomingEmail(1));

        // the username and the email are the same user, so they share the cooldown
        submitResetPasswordForm(USER_EMAIL);
        EventAssertion.assertError(events.poll())
                .type(EventType.SEND_RESET_PASSWORD_ERROR)
                .error(Errors.SLOW_DOWN)
                .userId(userId)
                .details(Details.USERNAME, USER_EMAIL);
        assertFalse(mailServer.waitForIncomingEmail(1000, 2), "Unexpected email was received");

        timeOffSet.set(EmailCooldownManager.EMAIL_RESEND_COOLDOWN_DEFAULT_SECONDS + 1);

        submitResetPasswordForm(USER_LOGIN);
        EventAssertion.expectRequiredAction(events.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .userId(userId);
        assertTrue(mailServer.waitForIncomingEmail(2));
    }

    @Test
    public void resetPasswordSendsEveryEmailWhenCooldownIsDisabled() {
        configureEmailCooldown("0");

        submitResetPasswordForm(USER_LOGIN);
        submitResetPasswordForm(USER_LOGIN);

        assertTrue(mailServer.waitForIncomingEmail(2));
    }

    @Test
    public void resetPasswordUsesDefaultCooldownWhenValueIsInvalid() {
        configureEmailCooldown("30s");

        submitResetPasswordForm(USER_LOGIN);
        submitResetPasswordForm(USER_LOGIN);

        assertTrue(mailServer.waitForIncomingEmail(1));
        assertFalse(mailServer.waitForIncomingEmail(1000, 2), "Unexpected email was received");
    }

    private void submitResetPasswordForm(String username) {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
    }

    private void configureEmailCooldown(String seconds) {
        AuthenticationManagementResource flows = consumerRealm.admin().flows();
        String executionId = flows.getExecutions(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW).stream()
                .filter(e -> ResetCredentialEmail.PROVIDER_ID.equals(e.getProviderId()))
                .findAny().orElseThrow().getId();
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("reset-email-cooldown");
        config.setConfig(Map.of(EmailCooldownManager.EMAIL_RESEND_COOLDOWN_SECONDS, seconds));
        String configId = ApiUtil.getCreatedId(flows.newExecutionConfig(executionId, config));
        consumerRealm.cleanup().add(r -> r.flows().removeAuthenticatorConfig(configId));
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
}
