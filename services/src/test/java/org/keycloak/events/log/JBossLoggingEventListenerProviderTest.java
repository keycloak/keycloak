/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.events.log;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.utils.ScopeUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.WriterHandler;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class JBossLoggingEventListenerProviderTest {

    @Test
    public void testAdminDefaultSuccessNoLog() {
        AdminEvent adminEvent = createEvent();
        test(adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.emptyString());
        });
    }

    @Test
    public void testAdminDefaultSuccessInfo() {
        AdminEvent adminEvent = createEvent();
        test(Map.of("success-level", "info"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("INFO "));
            assertAdminEvent(adminEvent, message);
        });
    }

    @Test
    public void testAdminDefaultErrorWarn() {
        AdminEvent adminEvent = createEvent("error");
        test(adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message);
        });
    }

    @Test
    public void testAdminDefaultErrorError() {
        AdminEvent adminEvent = createEvent("error");
        test(Map.of("error-level", "error"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("ERROR "));
            assertAdminEvent(adminEvent, message);
        });
    }

    @Test
    public void testAdminSanitized() {
        AdminEvent adminEvent = createEvent("error\twith\r\nspaces and \"quote");
        test(adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            adminEvent.setError("error with  spaces and \\\"quote");
            assertAdminEvent(adminEvent, message);
        });
    }

    @Test
    public void testAdminNoSanitized() {
        AdminEvent adminEvent = createEvent("error\twith\r\nspaces and \"quote");
        test(Map.of("sanitize", "false"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message);
        });
    }

    @Test
    public void testAdminOtherQuote() {
        AdminEvent adminEvent = createEvent("error\twith\r\nspaces and \"quote");
        test(Map.of("quotes", "'"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            adminEvent.setError("error with  spaces and \"quote");
            assertAdminEvent(adminEvent, message, "'");
        });
    }

    @Test
    public void testAdminNoQuote() {
        AdminEvent adminEvent = createEvent("error\twith\r\nspaces and \"quote");
        test(Map.of("quotes", "none"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            adminEvent.setError("error with  spaces and \"quote");
            assertAdminEvent(adminEvent, message, "");
        });
    }

    @Test
    public void testAdminNoQuoteNoSanitize() {
        AdminEvent adminEvent = createEvent("error\twith\r\nspaces and \"quote");
        test(Map.of("quotes", "none", "sanitize", "false"), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message, "");
        });
    }

    @Test
    public void testAdminDetails() {
        AdminEvent adminEvent = createEvent("error");
        adminEvent.setDetails(Map.of("detail1", "value1", "detail2", "value2"));
        test(Map.of(), adminEvent, false, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message);
            assertAdminEventKey(message, "detail1", "value1");
            assertAdminEventKey(message, "detail2", "value2");
        });
    }

    @Test
    public void testAdminRepresentation() {
        AdminEvent adminEvent = createEvent("error");
        adminEvent.setRepresentation("{\"claim\": \"value\"}");
        test(Map.of("include-representation", "true"), adminEvent, true, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message);
            assertAdminEventKey(message, "representation", "{\\\"claim\\\": \\\"value\\\"}");
        });
    }

    @Test
    public void testAdminNoRepresentation() {
        AdminEvent adminEvent = createEvent("error");
        adminEvent.setRepresentation("{\"claim\": \"value\"}");
        test(Map.of(), adminEvent, true, message -> {
            MatcherAssert.assertThat(message, Matchers.startsWith("WARN "));
            assertAdminEvent(adminEvent, message);
            assertAdminEventKeyNotPresent(message, "representation");
        });
    }

    private static void test(AdminEvent adminEvent, boolean includeRepresentation, Consumer<String> assertMessage) {
        test(Map.of(), adminEvent, includeRepresentation, assertMessage);
    }

    private static void test(Map<String, String> config, AdminEvent adminEvent, boolean includeRepresentation, Consumer<String> assertMessage) {
        KeycloakSession session = createSession(config);
        Logger logger = Logger.getLogger("org.keycloak.events");
        StringWriter sw = new StringWriter();
        Handler handler = addhandler(logger, sw);
        try {
            JBossLoggingEventListenerProvider prov = (JBossLoggingEventListenerProvider) session.getProvider(
                    EventListenerProvider.class, JBossLoggingEventListenerProviderFactory.ID);
            prov.logAdminEvent(adminEvent, includeRepresentation);

            assertMessage.accept(sw.toString());
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static KeycloakSession createSession(Map<String, String> config) {
        HttpRequest httpRequest = new HttpRequestImpl(MockHttpRequest.create("GET", URI.create("https://keycloak.org/"), URI.create("https://keycloak.org")));
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        Config.init(new Config.ConfigProvider() {
            @Override
            public String getProvider(String spi) {
                return null;
            }

            @Override
            public String getDefaultProvider(String spi) {
                return null;
            }

            @Override
            public Config.Scope scope(String... scope) {
                if (scope.length == 2 && "eventsListener".equals(scope[0]) && JBossLoggingEventListenerProviderFactory.ID.equals(scope[1])) {
                    return ScopeUtil.createScope(config);
                }
                return ScopeUtil.createScope(new HashMap<>());
            }
        });
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        KeycloakSession session = new ResteasyKeycloakSession(sessionFactory);
        session.getContext().setHttpRequest(httpRequest);

        return session;
    }

    private static Handler addhandler(Logger logger, Writer writer) {
        WriterHandler handler = new WriterHandler();
        handler.setWriter(writer);
        handler.setLevel(Level.ALL);
        handler.setAutoFlush(true);
        handler.setFormatter(new PatternFormatter("%p %m%n")); // just level and message
        logger.addHandler(handler);

        return handler;
    }

    private static AdminEvent createEvent() {
        return createEvent(null);
    }

    private static AdminEvent createEvent(String error) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setId("id");
        adminEvent.setOperationType(OperationType.UPDATE);
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId("realm-id");
        authDetails.setRealmName("realm-name");
        authDetails.setClientId("client-id");
        authDetails.setUserId("user-id");
        authDetails.setIpAddress("localhost");
        adminEvent.setAuthDetails(authDetails);
        adminEvent.setResourceType(ResourceType.USER);
        adminEvent.setResourcePath("resource-path");
        adminEvent.setError(error);
        return adminEvent;
    }

    private static void assertAdminEvent(AdminEvent adminEvent, String message) {
        assertAdminEvent(adminEvent, message, "\"");
    }

    private static void assertAdminEvent(AdminEvent adminEvent, String message, String quote) {
        assertAdminEventKey(message, "operationType", adminEvent.getOperationType().name(), quote);
        assertAdminEventKey(message, "realmId", adminEvent.getAuthDetails().getRealmId(), quote);
        assertAdminEventKey(message, "realmName", adminEvent.getAuthDetails().getRealmName(), quote);
        assertAdminEventKey(message, "clientId", adminEvent.getAuthDetails().getClientId(), quote);
        assertAdminEventKey(message, "userId", adminEvent.getAuthDetails().getUserId(), quote);
        assertAdminEventKey(message, "ipAddress", adminEvent.getAuthDetails().getIpAddress(), quote);
        assertAdminEventKey(message, "resourceType", adminEvent.getResourceTypeAsString(), quote);
        assertAdminEventKey(message, "resourcePath", adminEvent.getResourcePath(), quote);
        if (adminEvent.getError() != null) {
            assertAdminEventKey(message, "error", adminEvent.getError(), quote);
        } else {
            assertAdminEventKeyNotPresent(message, "error");
        }
    }

    private static void assertAdminEventKey(String message, String key, String value) {
        assertAdminEventKey(message, key, value, "\"");
    }

    private static void assertAdminEventKey(String message, String key, String value, String quote) {
        MatcherAssert.assertThat(message, Matchers.containsString(" " + key + "=" + quote + value + quote));
    }

    private static void assertAdminEventKeyNotPresent(String message, String key) {
        MatcherAssert.assertThat(message, Matchers.not(Matchers.containsString(" " + key + "=\"")));
    }
}
