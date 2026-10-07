package org.keycloak.tests.forms;

import java.util.List;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.email.CountingEmailTemplateProviderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * a reset password request must do the same work whether or not the user exists, so its timing doesn't reveal it.
 */
@KeycloakIntegrationTest(config = ResetPasswordUserEnumerationTest.CountingEmailServerConfig.class)
public class ResetPasswordUserEnumerationTest {

    @InjectRealm(config = ResetPasswordRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @ParameterizedTest(name = "username={0}")
    @CsvSource({
            "existing-user, true",
            "existing-user@localhost, true",
            "unknown-user, false",
            "disabled-user, false",
            "user-without-email, false"
    })
    public void resetPasswordRendersTheEmailForEveryUser(String username, boolean sent) {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();

        runOnServer.run(CountingEmailTemplateProviderFactory::getAndResetOperations);
        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        List<String> expected = sent ? List.of("renderPasswordReset", "queue") : List.of("renderPasswordReset");
        runOnServer.run(session -> assertEquals(expected, CountingEmailTemplateProviderFactory.getAndResetOperations(session)));
    }

    @AfterEach
    public void clearOperations() {
        runOnServer.run(CountingEmailTemplateProviderFactory::getAndResetOperations);
    }

    public static class CountingEmailServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return new CustomProvidersServerConfig().configure(config)
                    .option("spi-email-template--" + CountingEmailTemplateProviderFactory.ID + "--enabled", "true");
        }
    }

    public static class ResetPasswordRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.resetPasswordAllowed(true)
                    .users(UserBuilder.create("existing-user")
                                    .password("password")
                                    .email("existing-user@localhost")
                                    .name("First", "Last"),
                            UserBuilder.create("disabled-user")
                                    .password("password")
                                    .email("disabled-user@localhost")
                                    .enabled(false),
                            UserBuilder.create("user-without-email")
                                    .password("password"));
        }
    }
}
