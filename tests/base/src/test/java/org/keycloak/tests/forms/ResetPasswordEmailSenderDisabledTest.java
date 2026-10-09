package org.keycloak.tests.forms;

import java.util.Objects;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.tests.admin.SMTPConnectionDisabledProviderTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

/**
 * Issue 26625 - the reset password email is sent in the background, and a failure there must still be reported
 */
@KeycloakIntegrationTest(config = SMTPConnectionDisabledProviderTest.SMTPDisabledProviderConfig.class)
public class ResetPasswordEmailSenderDisabledTest {

    private static final String USERNAME = "test-user";

    @InjectRealm(config = ResetPasswordRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @Test
    public void failureIsReportedWhenEmailSenderIsDisabled() {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(USERNAME);

        loginPage.assertCurrent();
        EventAssertion.assertError(Awaitility.await().until(events::poll, Objects::nonNull))
                .type(EventType.SEND_RESET_PASSWORD_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .details(Details.USERNAME, USERNAME)
                .details(Details.REASON, "Email sender provider is disabled or not configured");
    }

    public static class ResetPasswordRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .resetPasswordAllowed(true)
                    .users(UserBuilder.create(USERNAME)
                            .email("test-user@localhost")
                            .password("password"));
        }
    }
}
