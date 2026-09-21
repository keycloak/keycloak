/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.endpoints.request;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.utils.ScopeUtil;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Unit tests for log injection vulnerability fix in AuthzEndpointRequestParser.
 * Tests verify that ANSI escape codes and control characters are properly
 * sanitized from OIDC parameters.
 */
public class AuthzEndpointRequestParserSanitizationTest {

    private static KeycloakSession session;

    @BeforeClass
    public static void setup() {
        HttpRequest httpRequest = new HttpRequestImpl(
            MockHttpRequest.create("GET", URI.create("https://keycloak.org/"),
            URI.create("https://keycloak.org"))
        );

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
                return ScopeUtil.createScope(new HashMap<>());
            }
        });

        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
        session.getContext().setHttpRequest(httpRequest);
    }

    @Test
    public void testSanitizeClientIdWithANSICodes() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.CLIENT_ID_PARAM, "fake_client\r\n\u001B[32m[FORGED INFO]\u001B[0m");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.clientId);
        assertFalse("Newlines should be removed", request.clientId.contains("\r"));
        assertFalse("Newlines should be removed", request.clientId.contains("\n"));
        assertFalse("ANSI codes should be removed", request.clientId.contains("\u001B"));
        assertEquals("fake_client[FORGED INFO]", request.clientId);
    }


    @Test
    public void testSanitizeScopeWithURLEncodedANSI() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.SCOPE_PARAM, "openid%1B[31mprofile%1B[0m");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.scope);
        assertFalse("ANSI codes should be removed", request.scope.contains("\u001B"));
        // URL-encoded ANSI codes are removed
        assertEquals("openidprofile", request.scope);
    }

    @Test
    public void testSanitizeResponseTypeWithTabs() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, "code\t\tid_token");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.responseType);
        assertFalse("Tabs should be removed", request.responseType.contains("\t"));
        assertEquals("codeid_token", request.responseType);
    }

    @Test
    public void testSanitizeStateWithNewlineInjection() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.STATE_PARAM, "valid_state\r\n[FAKE] Log entry");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.state);
        assertFalse("Carriage return should be removed", request.state.contains("\r"));
        assertFalse("Newline should be removed", request.state.contains("\n"));
        assertEquals("valid_state[FAKE] Log entry", request.state);
    }

    @Test
    public void testSanitizeLoginHintWithControlCharacters() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.LOGIN_HINT_PARAM, "admin@example.com");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.loginHint);
        assertFalse("Control characters should be removed",
            request.loginHint.matches(".*[\\x00-\\x1F\\x7F].*"));
        assertEquals("admin@example.com", request.loginHint);
    }

    @Test
    public void testSanitizeNonceWithMixedAttackVectors() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.NONCE_PARAM, "nonce123\r\n\u001B[32mFAKE\u001B[0m\tvalue");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertAll(
                () -> assertNotNull(request.nonce),
                () -> assertFalse("Newlines should be removed", request.nonce.contains("\r")),
                () -> assertFalse("Newlines should be removed", request.nonce.contains("\n")),
                () -> assertFalse("Tabs should be removed", request.nonce.contains("\t")),
                () -> assertFalse("ANSI codes should be removed", request.nonce.contains("\u001B")),
                () -> assertEquals("nonce123FAKEvalue", request.nonce)
         );

    }

    @Test
    public void testSanitizeAdditionalParametersAreSanitized() {
        TestParser parser = new TestParser(session);
        parser.params.put("custom_param", "value\r\n\u001B[31minjected\u001B[0m");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        String customParam = request.additionalReqParams.get("custom_param");
        assertAll(
                () -> assertNotNull(customParam),
                () -> assertFalse("Control characters should be removed from additional params", customParam.contains("\r")),
                () -> assertFalse("Control characters should be removed from additional params", customParam.contains("\n")),
                () -> assertFalse("ANSI codes should be removed from additional params", customParam.contains("\u001B")),
                () -> assertEquals("valueinjected", customParam)
        );

    }

    @Test
    public void testNormalStringsUnchanged() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.CLIENT_ID_PARAM, "my-client-123");
        parser.params.put(OIDCLoginProtocol.SCOPE_PARAM, "openid profile email");
        parser.params.put(OIDCLoginProtocol.STATE_PARAM, "abc123xyz");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertEquals("my-client-123", request.clientId);
        assertEquals("openid profile email", request.scope);
        assertEquals("abc123xyz", request.state);
    }


    @Test
    public void testDeleteCharacter_0x7F_IsRemoved() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.CLIENT_ID_PARAM, "client\u007Fid");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.clientId);
        assertFalse("DELETE character should be removed", request.clientId.contains("\u007F"));
        assertEquals("clientid", request.clientId);
    }

    @Test
    public void testCombinedAttackVector() {
        TestParser parser = new TestParser(session);
        parser.params.put(OIDCLoginProtocol.CLIENT_ID_PARAM, "client%0D%0A%1B[32m[FORGED]%1B[0m%09%00");

        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        parser.parseRequest(request);

        assertNotNull(request.clientId);
        assertFalse("No control characters should remain", request.clientId.matches(".*[\\x00-\\x1F\\x7F].*"));
        assertEquals("client[FORGED]", request.clientId);
    }


    private static class TestParser extends AuthzEndpointRequestParser {
        private Map<String, String> params = new HashMap<>();

        public TestParser(KeycloakSession session) {
            super(session);
        }

        @Override
        protected String getParameter(String paramName) {
            return params.get(paramName);
        }

        @Override
        protected Integer getIntParameter(String paramName) {
            String val = params.get(paramName);
            return val == null ? null : Integer.valueOf(val);
        }

        @Override
        protected Set<String> keySet() {
            return params.keySet();
        }
    }
}
