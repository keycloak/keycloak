package org.keycloak.email;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.events.Event;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link EmailTemplateProvider} default methods behave correctly for
 * implementations that do not override them. Important for third-party SPI implementations:
 * adding a new default method should not force every implementer to provide it.
 */
class EmailTemplateProviderDefaultsTest {

    @Test
    void sendInviteUserEmailFallsBackToSendExecuteActions() throws EmailException {
        AtomicReference<String> capturedLink = new AtomicReference<>();
        AtomicLong capturedExpiration = new AtomicLong(-1);

        EmailTemplateProvider provider = new MinimalEmailTemplateProvider() {
            @Override
            public void sendExecuteActions(String link, long expirationInMinutes) {
                capturedLink.set(link);
                capturedExpiration.set(expirationInMinutes);
            }
        };

        provider.sendInviteUserEmail("https://example.com/action", 60);

        assertNotNull(capturedLink.get(),
                "Default sendInviteUserEmail should delegate to sendExecuteActions");
        assertEquals("https://example.com/action", capturedLink.get());
        assertEquals(60L, capturedExpiration.get());
    }

    /**
     * Stubs every method of the interface as {@code throw UnsupportedOperationException} so
     * individual tests can override only what they exercise.
     */
    private abstract static class MinimalEmailTemplateProvider implements EmailTemplateProvider {

        @Override public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel s) { throw notImplemented(); }
        @Override public EmailTemplateProvider setRealm(RealmModel realm) { throw notImplemented(); }
        @Override public EmailTemplateProvider setUser(UserModel user) { throw notImplemented(); }
        @Override public EmailTemplateProvider setAttribute(String name, Object value) { throw notImplemented(); }
        @Override public void sendEvent(Event event) { throw notImplemented(); }
        @Override public void sendPasswordReset(String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendSmtpTestEmail(Map<String, String> config, UserModel user) { throw notImplemented(); }
        @Override public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendExecuteActions(String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendVerifiableCredentialOffer(String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendVerifyEmail(String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendOrgInviteEmail(OrganizationModel organization, String link, long expirationInMinutes) { throw notImplemented(); }
        @Override public void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String address) { throw notImplemented(); }
        @Override public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) { throw notImplemented(); }
        @Override public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) { throw notImplemented(); }
        @Override public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes, String destinationEmail) { throw notImplemented(); }
        @Override public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes, String destinationEmail) { throw notImplemented(); }
        @Override public void close() { /* no-op */ }

        private static UnsupportedOperationException notImplemented() {
            return new UnsupportedOperationException("not implemented in this test");
        }
    }
}
