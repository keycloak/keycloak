/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.events.hooks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.config.RequestConfig;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

public class HttpEventHookTargetProvider implements EventHookTargetProvider {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000;

    private final KeycloakSession session;
    private final SimpleHttp simpleHttp;

    public HttpEventHookTargetProvider(KeycloakSession session) {
        this(session, null);
    }

    HttpEventHookTargetProvider(KeycloakSession session, SimpleHttp simpleHttp) {
        this.session = session;
        this.simpleHttp = simpleHttp;
    }

    @Override
    public EventHookDeliveryResult deliver(EventHookTargetModel target, EventHookMessageModel message) throws IOException {
        return deliverPayload(target, singlePayload(message));
    }

    @Override
    public EventHookDeliveryResult deliverBatch(EventHookTargetModel target, List<EventHookMessageModel> messages) throws IOException {
        return deliverPayload(target, bulkPayload(messages));
    }

    private EventHookDeliveryResult deliverPayload(EventHookTargetModel target, Object entity) throws IOException {
        Map<String, Object> settings = target.getSettings();
        String url = stringSetting(settings, "url", null);
        String method = stringSetting(settings, "method", "POST").toUpperCase(Locale.ROOT);
        String requestBody = JsonSerialization.writeValueAsString(entity);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(intSetting(settings, "connectTimeoutMs", DEFAULT_CONNECT_TIMEOUT_MS))
                .setSocketTimeout(intSetting(settings, "readTimeoutMs", DEFAULT_READ_TIMEOUT_MS))
                .setConnectionRequestTimeout(intSetting(settings, "connectionRequestTimeoutMs", DEFAULT_CONNECT_TIMEOUT_MS))
                .build();

        long started = System.currentTimeMillis();
        SimpleHttpRequest request = requestForMethod(resolveHttp().withRequestConfig(requestConfig), method, url)
                .header("Content-Type", "application/json")
                .json(entity);

        headersSetting(settings).forEach(request::header);

        String hmacSecret = stringSetting(settings, "hmacSecret", null);
        if (hmacSecret != null && !hmacSecret.isBlank()) {
            String hmacAlgorithm = stringSetting(settings, "hmacAlgorithm", "HmacSHA256");
            request.header("X-Keycloak-Signature", sign(requestBody, hmacAlgorithm, hmacSecret));
            request.header("X-Keycloak-Signature-Algorithm", hmacAlgorithm);
        }

        try (SimpleHttpResponse response = request.asResponse()) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setDurationMillis(System.currentTimeMillis() - started);
            int status = response.getStatus();
            result.setStatusCode(Integer.toString(status));

            String responseBody = response.asString();
            result.setDetails(truncate(responseBody, 1024));
            result.setSuccess(status >= 200 && status < 300);
            result.setRetryable(status == 429 || status >= 500);
            result.setAutoDisableEligible(status == 429);
            result.setRetryAfterMillis(parseRetryAfterMillis(response.getFirstHeader("Retry-After")));
            return result;
        } catch (IOException exception) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setDurationMillis(System.currentTimeMillis() - started);
            result.setSuccess(false);
            result.setRetryable(true);
            result.setAutoDisableEligible(false);
            result.setDetails(truncate(exception.getMessage(), 1024));
            return result;
        }
    }

    @Override
    public void close() {
    }

    private SimpleHttp resolveHttp() {
        return simpleHttp != null ? simpleHttp : SimpleHttp.create(session);
    }

    private SimpleHttpRequest requestForMethod(SimpleHttp http, String method, String url) {
        return switch (method) {
            case "PUT" -> http.doPut(url);
            case "PATCH" -> http.doPatch(url);
            default -> http.doPost(url);
        };
    }

    private Object singlePayload(EventHookMessageModel message) throws IOException {
        return readPayload(message.getPayload());
    }

    private Object bulkPayload(List<EventHookMessageModel> messages) throws IOException {
        return Map.of("events", messages.stream().map(message -> readPayload(message.getPayload())).toList());
    }

    private Object readPayload(String payload) {
        try {
            return EventHookPayloadNormalizer.readPayload(payload);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse event hook payload", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> headersSetting(Map<String, Object> settings) {
        Object value = settings == null ? null : settings.get("headers");
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Map.of();
        }

        return (Map<String, String>) mapValue.entrySet().stream().collect(java.util.stream.Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue() == null ? "" : entry.getValue().toString()));
    }

    private String stringSetting(Map<String, Object> settings, String key, String defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private int intSetting(Map<String, Object> settings, String key, int defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private String sign(String body, String algorithm, String secret) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] signature = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(signature.length * 2);
            for (byte current : signature) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute event hook signature", exception);
        }
    }

    private Long parseRetryAfterMillis(String retryAfter) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }

        String value = retryAfter.trim();
        try {
            return Math.max(0L, Long.parseLong(value) * 1000L);
        } catch (NumberFormatException ignored) {
        }

        try {
            long millis = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .toEpochMilli() - Instant.now().toEpochMilli();
            return Math.max(0L, millis);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
