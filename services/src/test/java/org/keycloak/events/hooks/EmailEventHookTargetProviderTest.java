package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ThemeManager;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailEventHookTargetProviderTest {

    @Test
    public void shouldRenderAndSendEmailForSingleDelivery() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        KeycloakSession session = session(
            realm("realm-1", "demo", Map.of("host", "smtp.example.org", "from", "noreply@example.org")),
            user("test-user", "test@example.org"));
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session, dispatcher);

        EventHookDeliveryResult result = provider.deliver(target(Map.of(
                "recipientTemplate", "${user.email}",
                "subjectTemplate", "${msg(\"emailTestSubject\")}: ${eventId}",
                "textBodyTemplate", "Hello ${user.username}! ${msg(\"emailTestBody\", realmName)}",
                "htmlBodyTemplate", "<p>${event}</p>"
        )), message("evt-1", "test-user", Map.of("eventId", "evt-1", "eventType", "LOGIN")));

        assertTrue(result.isSuccess());
        assertEquals("EMAIL_SENT", result.getStatusCode());
        assertEquals("test@example.org", dispatcher.recipient);
        assertTrue(dispatcher.subject.contains("evt-1"));
        assertTrue(dispatcher.textBody.contains("This is a test message"));
        assertTrue(dispatcher.htmlBody.contains("LOGIN"));
    }

    @Test
    public void shouldRenderBulkEmailsUsingEventsArray() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        KeycloakSession session = session(
            realm("realm-1", "demo", Map.of("host", "smtp.example.org", "from", "noreply@example.org")),
            user("test-user", "digest@example.org"));
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session, dispatcher);

        EventHookDeliveryResult result = provider.deliverBatch(target(Map.of(
                "recipientTemplate", "digest@example.org",
                "subjectTemplate", "Digest (${eventCount})",
                "textBodyTemplate", "<#list events as current>${current.eventId}<#sep>,</#sep></#list>"
        )), List.of(
                message("evt-1", "test-user", Map.of("eventId", "evt-1", "eventType", "LOGIN")),
                message("evt-2", "test-user", Map.of("eventId", "evt-2", "eventType", "LOGOUT"))
        ));

        assertTrue(result.isSuccess());
        assertEquals("digest@example.org", dispatcher.recipient);
        assertEquals("Digest (2)", dispatcher.subject);
        assertEquals("evt-1,evt-2", dispatcher.textBody);
    }

    @Test
    public void shouldRenderFrontendDefaultTemplatesWithoutResolvedUserOrDetails() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        KeycloakSession session = session(
            realm("realm-1", "demo", Map.of("host", "smtp.example.org", "from", "noreply@example.org")),
            null);
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session, dispatcher);

        EventHookDeliveryResult result = provider.deliver(target(Map.of(
                "recipientTemplate", "ops@example.org",
                "subjectTemplate", "<#if event??>${event.eventType!event.operationType!\"UNKNOWN\"} for ${(user.username)!(event.userId!\"unknown user\")}<#else>${events?size} grouped events</#if>",
                "textBodyTemplate", "<#if event??>Event ${event.eventType!event.operationType!\"UNKNOWN\"} for ${(user.username)!(event.userId!\"unknown user\")}<#else>${events?size} events were grouped for delivery.</#if>",
                "htmlBodyTemplate", "<#if event??><p><strong>${event.eventType!event.operationType!\"UNKNOWN\"}</strong> for ${(user.username)!(event.userId!\"unknown user\")}</p><#else><p>${events?size} events were grouped for delivery.</p></#if>"
        )), message("evt-1", null, Map.of("eventId", "evt-1", "eventType", "LOGIN")));

        assertTrue(result.isSuccess());
        assertEquals("ops@example.org", dispatcher.recipient);
        assertEquals("LOGIN for unknown user", dispatcher.subject);
        assertTrue(dispatcher.textBody.contains("LOGIN for unknown user"));
        assertTrue(dispatcher.htmlBody.contains("LOGIN"));
    }

    @Test
    public void shouldReturnParseFailedWhenTemplateRendersEmptyRecipient() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        KeycloakSession session = session(
            realm("realm-1", "demo", Map.of("host", "smtp.example.org", "from", "noreply@example.org")),
            user("test-user", "test@example.org"));
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session, dispatcher);

        EventHookDeliveryResult result = provider.deliver(target(Map.of(
                "recipientTemplate", "${missing!}",
                "subjectTemplate", "Subject",
                "textBodyTemplate", "Body"
        )), message("evt-1", "test-user", Map.of("eventId", "evt-1")));

        assertFalse(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("PARSE_FAILED", result.getStatusCode());
    }

    @Test
    public void shouldReturnSendFailedWhenDispatcherThrows() throws Exception {
        KeycloakSession session = session(realm("realm-1", "demo", Map.of("host", "smtp.example.org", "from", "noreply@example.org")), user("test-user", "test@example.org"));
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session,
                (realm, email) -> {
                    throw new EmailException("SMTP unavailable");
                });

        EventHookDeliveryResult result = provider.deliver(target(Map.of(
                "recipientTemplate", "test@example.org",
                "subjectTemplate", "Subject",
                "textBodyTemplate", "Body"
        )), message("evt-1", "test-user", Map.of("eventId", "evt-1")));

        assertFalse(result.isSuccess());
        assertTrue(result.isRetryable());
        assertEquals("EMAIL_SEND_FAILED", result.getStatusCode());
    }

    @Test
    public void shouldReturnNonRetryableSendFailedWhenRealmSmtpIsNotConfigured() throws Exception {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        KeycloakSession session = session(realm("realm-1", "demo", Map.of()), user("test-user", "test@example.org"));
        EmailEventHookTargetProvider provider = new EmailEventHookTargetProvider(session, dispatcher);

        EventHookDeliveryResult result = provider.deliver(target(Map.of(
                "recipientTemplate", "test@example.org",
                "subjectTemplate", "Subject",
                "textBodyTemplate", "Body"
        )), message("evt-1", "test-user", Map.of("eventId", "evt-1")));

        assertFalse(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("EMAIL_SEND_FAILED", result.getStatusCode());
        assertEquals(null, dispatcher.recipient);
    }

    private EventHookTargetModel target(Map<String, Object> settings) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setRealmId("realm-1");
        target.setRealmName("demo");
        target.setType(EmailEventHookTargetProviderFactory.ID);
        target.setEnabled(true);
        target.setSettings(settings);
        return target;
    }

    private EventHookMessageModel message(String eventId, String userId, Map<String, Object> payload) throws Exception {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-" + eventId);
        message.setRealmId("realm-1");
        message.setTargetId("target-1");
        message.setSourceEventId(eventId);
        message.setUserId(userId);
        message.setPayload(org.keycloak.util.JsonSerialization.writeValueAsString(payload));
        return message;
    }

    private RealmModel realm(String id, String name, Map<String, String> smtpConfig) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> name;
                    case "getDisplayName" -> name;
                    case "getSmtpConfig" -> smtpConfig;
                    default -> null;
                });
    }

    private UserModel user(String id, String email) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("department", List.of("security"));

        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class<?>[] { UserModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getUsername" -> id;
                    case "getEmail" -> email;
                    case "getFirstName" -> "Test";
                    case "getLastName" -> "User";
                    case "getAttributes" -> attributes;
                    case "isEmailVerified" -> true;
                    case "isEnabled" -> true;
                    default -> null;
                });
    }

    private KeycloakSession session(RealmModel realm, UserModel user) {
        Theme theme = (Theme) Proxy.newProxyInstance(
                Theme.class.getClassLoader(),
                new Class<?>[] { Theme.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEnhancedMessages" -> {
                        Properties properties = new Properties();
                        properties.setProperty("emailTestSubject", "[KEYCLOAK] - SMTP test message");
                        properties.setProperty("emailTestBody", "This is a test message");
                        properties.setProperty("eventHookEmailSubject", "{0}");
                        yield properties;
                    }
                    case "getProperties" -> new Properties();
                    case "getName" -> "base";
                    case "getType" -> Theme.Type.EMAIL;
                    default -> null;
                });

        ThemeManager themeManager = (ThemeManager) Proxy.newProxyInstance(
                ThemeManager.class.getClassLoader(),
                new Class<?>[] { ThemeManager.class },
                (proxy, method, args) -> {
                    if ("getTheme".equals(method.getName())) {
                        return theme;
                    }
                    return null;
                });

        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                KeycloakContext.class.getClassLoader(),
                new Class<?>[] { KeycloakContext.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRealm" -> realm;
                    case "resolveLocale" -> Locale.ENGLISH;
                    default -> null;
                });

        Object userProvider = Proxy.newProxyInstance(
                org.keycloak.models.UserProvider.class.getClassLoader(),
                new Class<?>[] { org.keycloak.models.UserProvider.class },
                (proxy, method, args) -> {
                    if ("getUserById".equals(method.getName())) {
                        return user;
                    }
                    return null;
                });

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContext" -> context;
                    case "theme" -> themeManager;
                    case "users" -> userProvider;
                    case "realms" -> Proxy.newProxyInstance(
                            org.keycloak.models.RealmProvider.class.getClassLoader(),
                            new Class<?>[] { org.keycloak.models.RealmProvider.class },
                            (innerProxy, innerMethod, innerArgs) -> "getRealm".equals(innerMethod.getName()) ? realm : null);
                    default -> null;
                });
    }

    private static final class RecordingDispatcher implements EmailEventHookTargetProvider.EmailDispatcher {

        private String recipient;
        private String subject;
        private String textBody;
        private String htmlBody;

        @Override
        public void send(RealmModel realm, EventHookEmailTemplateSupport.RenderedEmail email) {
            this.recipient = email.recipient();
            this.subject = email.subject();
            this.textBody = email.textBody();
            this.htmlBody = email.htmlBody();
        }
    }
}
