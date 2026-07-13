package org.keycloak.events.hooks;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpEventHookTargetProviderTest {

    private HttpServer server;

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void shouldDeliverSingleEventWithHeadersAndSignature() throws Exception {
        RecordingHandler handler = startServer(200, "accepted");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of(
                    "method", "PUT",
                    "headers", Map.of("X-Test-Header", "hook"),
                    "hmacAlgorithm", "HmacSHA256",
                    "hmacSecret", "top-secret"
            )), message("evt-1", Map.of("eventId", "evt-1", "realmId", "realm-a")));

            assertTrue(result.isSuccess());
            assertFalse(result.isRetryable());
            assertEquals("200", result.getStatusCode());
            assertEquals("accepted", result.getDetails());
            assertEquals("PUT", handler.method);
            assertEquals("application/json", headerValue(handler.headers, "Content-type"));
            assertEquals("hook", headerValue(handler.headers, "X-test-header"));
            assertEquals("HmacSHA256", headerValue(handler.headers, "X-keycloak-signature-algorithm"));

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            assertEquals("evt-1", body.get("eventId"));
            assertEquals("realm-a", body.get("realmId"));
            assertEquals(sign(handler.body, "HmacSHA256", "top-secret"), headerValue(handler.headers, "X-keycloak-signature"));
        }
    }

    @Test
    public void shouldDeliverBulkPayload() throws Exception {
        RecordingHandler handler = startServer(202, "queued");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliverBatch(target(handler.url(), Map.of()), List.of(
                    message("evt-1", Map.of("eventId", "evt-1", "type", "LOGIN")),
                    message("evt-2", Map.of("eventId", "evt-2", "type", "LOGOUT"))
            ));

            assertTrue(result.isSuccess());
            assertFalse(result.isRetryable());
            assertEquals("202", result.getStatusCode());

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) body.get("events");
            assertNotNull(events);
            assertEquals(2, events.size());
            assertEquals("evt-1", events.get(0).get("eventId"));
            assertEquals("evt-2", events.get(1).get("eventId"));
        }
    }

    @Test
    public void shouldRenderCustomBodyTemplateForSingleDelivery() throws Exception {
        RecordingHandler handler = startServer(200, "accepted");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of(
                    "customBodyMappingTemplate", "{\"customEventId\": \"${eventId?json_string}\", \"details\": ${details}, \"event\": ${event}}"
            )), message("evt-custom", Map.of(
                    "eventId", "evt-custom",
                    "details", Map.of("clientId", "security-admin-console")
            )));

            assertTrue(result.isSuccess());

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            assertEquals("evt-custom", body.get("customEventId"));
            assertEquals("security-admin-console", ((Map<?, ?>) body.get("details")).get("clientId"));
            assertEquals("evt-custom", ((Map<?, ?>) body.get("event")).get("eventId"));
        }
    }

    @Test
    public void shouldRenderCustomBodyTemplateForBatchDelivery() throws Exception {
        RecordingHandler handler = startServer(202, "queued");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliverBatch(target(handler.url(), Map.of(
                    "customBodyMappingTemplate", "{\"items\": [<#list events as item>${item}<#sep>,</#sep></#list>]}"
            )), List.of(
                    message("evt-1", Map.of("eventId", "evt-1", "type", "LOGIN")),
                    message("evt-2", Map.of("eventId", "evt-2", "type", "LOGOUT"))
            ));

            assertTrue(result.isSuccess());

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            assertEquals(2, items.size());
            assertEquals("evt-1", items.get(0).get("eventId"));
            assertEquals("evt-2", items.get(1).get("eventId"));
        }
    }

    @Test
    public void shouldReturnParseFailedForInvalidRenderedCustomBody() throws Exception {
        RecordingHandler handler = startServer(200, "accepted");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of(
                    "customBodyMappingTemplate", "{\"eventId\": ${eventId}}"
            )), message("evt-invalid", Map.of("eventId", "evt-invalid")));

            assertFalse(result.isSuccess());
            assertFalse(result.isRetryable());
            assertEquals("PARSE_FAILED", result.getStatusCode());
        }
    }

    @Test
    public void shouldParseRepresentationJsonForHttpDelivery() throws Exception {
        RecordingHandler handler = startServer(200, "accepted");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of()), message("evt-3", Map.of(
                    "eventId", "evt-3",
                    "resourceType", "USER",
                    "representation", "{\"id\":\"user-1\",\"enabled\":true}"
            )));

            assertTrue(result.isSuccess());

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            assertTrue(body.get("representation") instanceof Map<?, ?>);
            assertEquals("user-1", ((Map<?, ?>) body.get("representation")).get("id"));
            assertEquals(Boolean.TRUE, ((Map<?, ?>) body.get("representation")).get("enabled"));
        }
    }

    @Test
    public void shouldKeepRepresentationStringWhenItIsNotJson() throws Exception {
        RecordingHandler handler = startServer(200, "accepted");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of()), message("evt-4", Map.of(
                    "eventId", "evt-4",
                    "representation", "plain-text"
            )));

            assertTrue(result.isSuccess());

            Map<String, Object> body = JsonSerialization.readValue(handler.body, new TypeReference<Map<String, Object>>() {
            });
            assertEquals("plain-text", body.get("representation"));
        }
    }

    @Test
    public void shouldMark429AsRetryable() throws Exception {
        RecordingHandler handler = startServer(429, "busy", Map.of("Retry-After", List.of("12")));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of()), message("evt-1", Map.of("eventId", "evt-1")));

            assertFalse(result.isSuccess());
            assertTrue(result.isRetryable());
            assertTrue(result.isAutoDisableEligible());
            assertEquals("429", result.getStatusCode());
            assertEquals("busy", result.getDetails());
            assertEquals(Long.valueOf(12_000L), result.getRetryAfterMillis());
        }
    }

    @Test
    public void shouldMark400AsNonRetryable() throws Exception {
        RecordingHandler handler = startServer(400, "invalid");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target(handler.url(), Map.of()), message("evt-1", Map.of("eventId", "evt-1")));

            assertFalse(result.isSuccess());
            assertFalse(result.isRetryable());
            assertFalse(result.isAutoDisableEligible());
            assertEquals("400", result.getStatusCode());
            assertEquals("invalid", result.getDetails());
        }
    }

    @Test
    public void shouldReturnRetryableResultOnIoFailure() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEventHookTargetProvider provider = new HttpEventHookTargetProvider(null, SimpleHttp.create(client));
            EventHookDeliveryResult result = provider.deliver(target("http://127.0.0.1:1/unreachable", Map.of(
                    "connectTimeoutMs", 200,
                    "readTimeoutMs", 200
            )), message("evt-1", Map.of("eventId", "evt-1")));

            assertFalse(result.isSuccess());
            assertTrue(result.isRetryable());
            assertFalse(result.isAutoDisableEligible());
            assertNull(result.getStatusCode());
            assertNotNull(result.getDetails());
        }
    }

    private RecordingHandler startServer(int status, String responseBody) throws IOException {
        return startServer(status, responseBody, Map.of());
    }

    private RecordingHandler startServer(int status, String responseBody, Map<String, List<String>> responseHeaders) throws IOException {
        RecordingHandler handler = new RecordingHandler(status, responseBody, responseHeaders);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/hook", handler);
        server.setExecutor(null);
        server.start();
        return handler;
    }

    private EventHookTargetModel target(String url, Map<String, Object> additionalSettings) {
        EventHookTargetModel target = new EventHookTargetModel();
        Map<String, Object> settings = new HashMap<>();
        settings.put("url", url);
        settings.putAll(additionalSettings);
        target.setSettings(settings);
        return target;
    }

    private EventHookMessageModel message(String eventId, Map<String, Object> payload) throws IOException {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-" + eventId);
        message.setSourceEventId(eventId);
        message.setPayload(JsonSerialization.writeValueAsString(payload));
        return message;
    }

    private String headerValue(Map<String, List<String>> headers, String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String sign(String body, String algorithm, String secret) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
        byte[] signature = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(signature.length * 2);
        for (byte current : signature) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }

    private final class RecordingHandler implements HttpHandler {

        private final int status;
        private final byte[] responseBody;
        private final Map<String, List<String>> responseHeaders;
        private String method;
        private String body;
        private Map<String, List<String>> headers;

        private RecordingHandler(int status, String responseBody, Map<String, List<String>> responseHeaders) {
            this.status = status;
            this.responseBody = responseBody.getBytes(StandardCharsets.UTF_8);
            this.responseHeaders = responseHeaders;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                method = exchange.getRequestMethod();
                headers = new HashMap<>();
                exchange.getRequestHeaders().forEach((key, value) -> headers.put(key, new ArrayList<>(value)));
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
                responseHeaders.forEach((key, values) -> values.forEach(value -> exchange.getResponseHeaders().add(key, value)));
                exchange.sendResponseHeaders(status, responseBody.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            }
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
        }
    }
}
