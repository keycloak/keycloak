package org.keycloak.tests.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SMTPConnectionDisabledProviderTest.SMTPDisabledProviderConfig.class)
public class SMTPConnectionDisabledProviderTest {

    private static final String RESET_PASSWORD_REALM = "reset-password";
    private static final String RESET_PASSWORD_USER = "test-user";

    @InjectRealm(config = SMTPConnectionTest.SMTPRealmWithClientAndUser.class)
    private ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    private Keycloak adminClient;

    @InjectRealm(ref = RESET_PASSWORD_REALM, config = ResetPasswordRealmConfig.class)
    private ManagedRealm resetPasswordRealm;

    @InjectOAuthClient(realmRef = RESET_PASSWORD_REALM)
    private OAuthClient oauth;

    @InjectEvents(realmRef = RESET_PASSWORD_REALM)
    private Events events;

    @InjectPage
    private LoginPage loginPage;

    @InjectPage
    private LoginPasswordResetPage resetPasswordPage;

    @Test
    public void testUpdateRealmWithDisabledEmailSenderProviderReturns400() {
        RealmResource realmResource = adminClient.realms().realm(managedRealm.getName());
        RealmRepresentation realmRep = realmResource.toRepresentation();
        
        Map<String, String> smtpSettings = new HashMap<>();
        smtpSettings.put("host", "127.0.0.1");
        smtpSettings.put("port", "3025");
        smtpSettings.put("from", "auto@keycloak.org");
        realmRep.setSmtpServer(smtpSettings);
        
        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> realmResource.update(realmRep));
        Assertions.assertNotNull(exception.getResponse(), "Response should not be null");
        String body = exception.getResponse().readEntity(String.class);
        Assertions.assertTrue(body.contains("Email sender provider is disabled or not configured"), "Message should indicate disabled provider. Found: " + body);
    }

    @Test
    public void testSMTPConnectionWithDisabledEmailSenderProviderReturns400() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("host", "127.0.0.1");
        settings.put("port", "3025");
        settings.put("from", "auto@keycloak.org");

        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings);
        Assertions.assertEquals(400, response.getStatus());
        String body = response.readEntity(String.class);
        Assertions.assertTrue(body.contains("Email sender provider is disabled or not configured"), "Message should indicate disabled provider. Found: " + body);
        response.close();
    }

    @Test
    public void testResetPasswordWithDisabledEmailSenderProviderReportsFailure() {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(RESET_PASSWORD_USER);

        loginPage.assertCurrent();
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.SEND_RESET_PASSWORD)
                .details(Details.USERNAME, RESET_PASSWORD_USER);
        EventAssertion.assertError(Awaitility.await().until(events::poll, Objects::nonNull))
                .type(EventType.SEND_RESET_PASSWORD_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .details(Details.USERNAME, RESET_PASSWORD_USER)
                .details(Details.REASON, "Email sender provider is disabled or not configured");
    }

    public static class SMTPDisabledProviderConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-email-sender-default-enabled", "false");
        }
    }

    public static class ResetPasswordRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .resetPasswordAllowed(true)
                    .users(UserBuilder.create(RESET_PASSWORD_USER)
                            .email("test-user@localhost")
                            .password("password"));
        }
    }
}
