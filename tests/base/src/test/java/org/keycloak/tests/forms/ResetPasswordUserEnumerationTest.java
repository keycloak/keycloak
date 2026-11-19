package org.keycloak.tests.forms;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class ResetPasswordUserEnumerationTest {

    @InjectRealm(config = ResetPasswordRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @InjectMailServer
    MailServer mailServer;

    @InjectEvents
    Events events;

    @ParameterizedTest(name = "username={0}")
    @CsvSource({
            "existing-user, true, none",
            "existing-user@localhost, true, none",
            "unknown-user, false, user_not_found",
            "disabled-user, false, user_disabled",
            "user-without-email, false, invalid_email"
    })
    public void resetPasswordAnswersTheSameForEveryUser(String username, boolean sent, String error) throws MessagingException {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();

        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage().orElse(null));

        if (sent) {
            EventAssertion.expectRequiredAction(events.poll())
                    .type(EventType.SEND_RESET_PASSWORD)
                    .details(Details.USERNAME, username);

            assertTrue(mailServer.waitForIncomingEmail(1));
            Address[] to = mailServer.getLastReceivedMessage().getRecipients(Message.RecipientType.TO);
            assertEquals(1, to.length);
            assertEquals("existing-user@localhost", ((InternetAddress) to[0]).getAddress());
        } else {
            EventAssertion.assertError(events.poll())
                    .type(EventType.RESET_PASSWORD_ERROR)
                    .error(error)
                    .details(Details.USERNAME, username);

            assertFalse(mailServer.waitForIncomingEmail(1000, 1), "Unexpected email was received");
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
