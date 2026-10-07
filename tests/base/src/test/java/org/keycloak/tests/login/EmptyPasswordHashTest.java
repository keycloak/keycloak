package org.keycloak.tests.login;

import java.util.List;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.hash.CountingPasswordHashProviderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class EmptyPasswordHashTest {

    @InjectRealm(config = HashRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @ParameterizedTest(name = "username={0}, password={1}")
    @CsvSource({
            "existing-user, '', encode:1000",
            "nonexistent-user, '', encode:1000",
            "existing-user, wrong-password, verify:1000",
            "nonexistent-user, wrong-password, encode:1000"
    })
    public void rejectedLoginPerformsOneHash(String username, String password, String expectedOperation) {
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);

        // Discard credential creation hashes before measuring the login attempt.
        runOnServer.run(CountingPasswordHashProviderFactory::getAndResetHashOperations);
        loginPage.submit();
        loginPage.waitForUsernameInputError("Invalid username or password.");

        // Count completed PBKDF2 operations instead of asserting wall-clock latency.
        runOnServer.run(session -> assertEquals(List.of(expectedOperation),
                CountingPasswordHashProviderFactory.getAndResetHashOperations(session)));
    }

    @Test
    public void successfulLoginPerformsOneVerification() {
        oauth.openLoginForm();
        loginPage.fillLogin("existing-user", "correct-password");
        runOnServer.run(CountingPasswordHashProviderFactory::getAndResetHashOperations);
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(session -> assertEquals(List.of("verify:1000"),
                CountingPasswordHashProviderFactory.getAndResetHashOperations(session)));
    }

    @AfterEach
    public void clearHashOperations() {
        runOnServer.run(CountingPasswordHashProviderFactory::getAndResetHashOperations);
    }

    public static class HashRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.bruteForceProtected(false)
                    .passwordPolicy("hashAlgorithm(" + CountingPasswordHashProviderFactory.ID + ") and hashIterations(1000)")
                    .users(UserBuilder.create("existing-user")
                            .password("correct-password")
                            .email("existing-user@localhost")
                            .name("First", "Last"));
        }
    }
}
