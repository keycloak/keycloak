package org.keycloak.tests.forms;

import java.io.IOException;
import java.net.ServerSocket;
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
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testsuite.util.MailServerConfiguration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = ResetPasswordEmailQueueTest.OneEmailServerConfig.class)
public class ResetPasswordEmailQueueTest {

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
    public void resetPasswordEmailIsDroppedWhenTooManyEmailsAreWaiting() throws IOException {
        // a mail server that accepts connections but never answers, so the first email keeps the only place
        try (ServerSocket unresponsiveMailServer = new ServerSocket(0)) {
            realm.updateWithCleanup(r -> {
                r.smtp(MailServerConfiguration.HOST, unresponsiveMailServer.getLocalPort(), MailServerConfiguration.FROM);
                r.build().getSmtpServer().put("timeout", "60000");
                return r;
            });

            resetPassword("first-user");
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.SEND_RESET_PASSWORD)
                    .details(Details.USERNAME, "first-user");

            resetPassword("second-user");
            EventAssertion.assertError(events.poll())
                    .type(EventType.SEND_RESET_PASSWORD_ERROR)
                    .error(Errors.EMAIL_SEND_FAILED)
                    .details(Details.USERNAME, "second-user")
                    .details(Details.REASON, "Too many emails are waiting to be sent");

            // closing the connection makes sending the first email fail
            unresponsiveMailServer.setSoTimeout(10000);
            unresponsiveMailServer.accept().close();
        }

        EventAssertion.assertError(Awaitility.await().until(events::poll, Objects::nonNull))
                .type(EventType.SEND_RESET_PASSWORD_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .details(Details.USERNAME, "first-user");
    }

    private void resetPassword(String username) {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
    }

    public static class OneEmailServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-email-template--freemarker--delivery-threads", "1")
                    .option("spi-email-template--freemarker--delivery-queue-size", "0");
        }
    }

    public static class ResetPasswordRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.resetPasswordAllowed(true)
                    .users(UserBuilder.create("first-user")
                                    .password("password")
                                    .email("first-user@localhost"),
                            UserBuilder.create("second-user")
                                    .password("password")
                                    .email("second-user@localhost"));
        }
    }
}
