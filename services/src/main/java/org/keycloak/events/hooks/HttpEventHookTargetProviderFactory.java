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

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class HttpEventHookTargetProviderFactory implements EventHookTargetProviderFactory {

    public static final String ID = "http";
    private static final String DEFAULT_METHOD = "POST";

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name("method")
            .label("eventHookTargetMethod")
            .helpText("eventHookTargetMethodHelp")
            .type(ProviderConfigProperty.LIST_TYPE)
            .options("POST", "PUT", "PATCH")
            .defaultValue(DEFAULT_METHOD)
            .add()
            .property()
            .name("url")
            .label("eventHookTargetUrl")
            .helpText("eventHookTargetUrlHelp")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue("https://")
            .required(true)
            .add()
            .property()
            .name("headers")
            .label("eventHookTargetHeaders")
            .helpText("eventHookTargetHeadersHelp")
            .type(ProviderConfigProperty.MAP_TYPE)
            .add()
            .property()
            .name("hmacAlgorithm")
            .label("eventHookTargetHmacAlgorithm")
            .helpText("eventHookTargetHmacAlgorithmHelp")
            .type(ProviderConfigProperty.LIST_TYPE)
            .options("HmacSHA256", "HmacSHA512")
            .defaultValue("HmacSHA256")
            .add()
            .property()
            .name("hmacSecret")
            .label("eventHookTargetHmacSecret")
            .helpText("eventHookTargetHmacSecretHelp")
            .type(ProviderConfigProperty.PASSWORD)
            .secret(true)
            .add()
            .property()
            .name("connectTimeoutMs")
            .label("eventHookTargetConnectTimeoutMs")
            .helpText("eventHookTargetConnectTimeoutMsHelp")
            .type(ProviderConfigProperty.INTEGER_TYPE)
            .defaultValue(5000)
            .add()
            .property()
            .name("readTimeoutMs")
            .label("eventHookTargetReadTimeoutMs")
            .helpText("eventHookTargetReadTimeoutMsHelp")
            .type(ProviderConfigProperty.INTEGER_TYPE)
            .defaultValue(10000)
            .add()
            .build();

    @Override
    public EventHookTargetProvider create(KeycloakSession session) {
        return new HttpEventHookTargetProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return CONFIG;
    }

    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public boolean supportsAggregation() {
        return true;
    }

    @Override
    public String getDisplayInfo(EventHookTargetModel target) {
        String method = stringValue(target.getSettings(), "method", false);
        String url = stringValue(target.getSettings(), "url", false);

        if (method == null && url == null) {
            return null;
        }

        if (method == null) {
            return url;
        }

        if (url == null) {
            return method.toUpperCase(Locale.ROOT);
        }

        return method.toUpperCase(Locale.ROOT) + ": " + url;
    }

    @Override
    public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
        String url = stringValue(settings, "url", true);
        URI uri = URI.create(url);
        if (!uri.isAbsolute() || uri.getScheme() == null) {
            throw new IllegalArgumentException("Target URL must be absolute");
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Target URL must use http or https");
        }

        String method = stringValue(settings, "method", false);
        if (method != null && !List.of("POST", "PUT", "PATCH").contains(method.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        positiveInteger(settings, "connectTimeoutMs");
        positiveInteger(settings, "readTimeoutMs");
    }

    private String stringValue(Map<String, Object> settings, String key, boolean required) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Missing required setting: " + key);
            }
            return null;
        }

        String stringValue = value.toString().trim();
        if (required && stringValue.isEmpty()) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }
        return stringValue.isEmpty() ? null : stringValue;
    }

    private void positiveInteger(Map<String, Object> settings, String key) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return;
        }

        int numericValue = value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
        if (numericValue <= 0) {
            throw new IllegalArgumentException("Setting must be greater than zero: " + key);
        }
    }
}
