package org.keycloak.events.hooks;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpEventHookTargetProviderFactoryTest {

    private final HttpEventHookTargetProviderFactory factory = new HttpEventHookTargetProviderFactory();

        @Test
        public void shouldExposeAllHttpSettings() {
        assertEquals(
            List.of(
                "method",
                "url",
                "headers",
                "hmacAlgorithm",
                "hmacSecret",
                "customBodyMappingTemplate",
                "connectTimeoutMs",
                "readTimeoutMs"
            ),
            factory.getConfigMetadata().stream().map(ProviderConfigProperty::getName).toList()
        );
        }

    @Test
    public void shouldAcceptValidHttpSettings() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "method", "POST",
                "hmacSecret", "super-secret"
        ));
    }

    @Test
    public void shouldSupportBatchDelivery() {
        assertEquals(true, factory.supportsBatch());
    }

    @Test
    public void shouldSupportAggregation() {
        assertTrue(factory.supportsAggregation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankCustomBodyTemplateWhenEnabled() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "customBodyMappingTemplate", "   "
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidCustomBodyTemplateSyntax() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "customBodyMappingTemplate", "<#if event>"
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidScheme() {
        factory.validateConfig(null, Map.of("url", "ftp://example.org/hooks"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidMethod() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "method", "GET"
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveConnectTimeout() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "connectTimeoutMs", 0
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveReadTimeout() {
        factory.validateConfig(null, Map.of(
                "url", "https://example.org/hooks/keycloak",
                "readTimeoutMs", -1
        ));
    }

    @Test
    public void shouldRedactConfiguredSecrets() {
        Map<String, Object> redacted = factory.redactConfig(Map.of(
                "url", "https://example.org/hooks/keycloak",
                "hmacSecret", "super-secret",
                EventHookAutoDisableSupport.LEGACY_AUTO_DISABLED_UNTIL, 1234L
        ));

        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, redacted.get("hmacSecret"));
        assertEquals("https://example.org/hooks/keycloak", redacted.get("url"));
        assertEquals(null, redacted.get(EventHookAutoDisableSupport.LEGACY_AUTO_DISABLED_UNTIL));
    }

    @Test
    public void shouldPreserveExistingSecretWhenSubmittedValueIsRedacted() {
        Map<String, Object> normalized = factory.normalizeConfig(
                Map.of("url", "https://example.org/hooks/keycloak", "hmacSecret", "super-secret"),
                Map.of("url", "https://example.org/hooks/keycloak", "hmacSecret", EventHookTargetProviderFactory.REDACTED_SECRET_VALUE)
        );

        assertEquals("super-secret", normalized.get("hmacSecret"));
    }

    @Test
    public void shouldPreserveExistingSecretWhenSubmittedValueIsMissing() {
        Map<String, Object> normalized = factory.normalizeConfig(
                Map.of("url", "https://example.org/hooks/keycloak", "hmacSecret", "super-secret"),
                Map.of("url", "https://example.org/hooks/keycloak")
        );

        assertEquals("super-secret", normalized.get("hmacSecret"));
    }

    @Test
    public void shouldExposeDisplayInfoFromMethodAndUrl() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "method", "patch",
                "url", "https://example.org/hooks/keycloak"
        ));

        assertEquals(
            "PATCH: https://example.org/hooks/keycloak",
            factory.getDisplayInfo(target)
        );
    }

    @Test
    public void shouldExecuteTestDeliveryAgainstHttpTarget() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        RecordingHandler handler = new RecordingHandler();
        server.createContext("/hook", handler);
        server.start();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProviderFactory testFactory = new HttpEventHookTargetProviderFactory() {
                @Override
                public EventHookTargetProvider create(org.keycloak.models.KeycloakSession session) {
                    return new HttpEventHookTargetProvider(session, SimpleHttp.create(client));
                }
            };

            EventHookTargetModel target = new EventHookTargetModel();
            target.setId("target-1");
            target.setType(HttpEventHookTargetProviderFactory.ID);
            target.setName("HTTP target");
            target.setEnabled(true);
            target.setSettings(Map.of(
                    "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                    "method", "POST"
            ));

            EventHookDeliveryResult result = testFactory.test(null, realm("realm-1", "demo"), target);

            assertTrue(result.isSuccess());
            assertEquals("200", result.getStatusCode());
            assertEquals("ok", result.getDetails());
            assertEquals("POST", handler.method);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldCreateTestMessagesWithDatabaseSafeIdLength() throws Exception {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setType(HttpEventHookTargetProviderFactory.ID);
        target.setSettings(Map.of(
                "url", "https://example.org/hooks/keycloak",
                "method", "POST"
        ));

        EventHookMessageModel message = factory.createTestMessages(null, realm("realm-1", "demo"), target).get(0);

        assertNotNull(message.getId());
        assertEquals(36, message.getId().length());
        assertTrue(message.isTest());
    }

    private RealmModel realm(String realmId, String realmName) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> realmId;
                    case "getName" -> realmName;
                    default -> null;
                });
    }

    private static final class RecordingHandler implements HttpHandler {

        private String method;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                method = exchange.getRequestMethod();
                exchange.getRequestBody().readAllBytes();
                byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response);
                }
            }
        }
    }
}
