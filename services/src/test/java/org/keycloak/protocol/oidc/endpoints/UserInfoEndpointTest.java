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
package org.keycloak.protocol.oidc.endpoints;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.common.ClientConnection;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientpolicy.ClientPolicyManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserInfoEndpointTest {

    @Test
    public void shouldSetNoCacheHeadersForGetRequests() {
        EndpointContext context = createEndpointContext("GET", null);

        try {
            context.endpoint.issueUserInfoGet();
        } catch (RuntimeException expected) {
            // The test only verifies headers are set before auth validation fails.
        }

        assertEquals("no-store", context.responseHeaders.get(HttpHeaders.CACHE_CONTROL));
        assertEquals("no-cache", context.responseHeaders.get("Pragma"));
    }

    @Test
    public void shouldSetNoCacheHeadersForPostRequests() {
        EndpointContext context = createEndpointContext("POST", jakarta.ws.rs.core.MediaType.APPLICATION_JSON);

        try {
            context.endpoint.issueUserInfoPost();
        } catch (RuntimeException expected) {
            // The test only verifies headers are set before auth validation fails.
        }

        assertEquals("no-store", context.responseHeaders.get(HttpHeaders.CACHE_CONTROL));
        assertEquals("no-cache", context.responseHeaders.get("Pragma"));
    }

    private EndpointContext createEndpointContext(String httpMethod, String contentType) {
        Map<String, String> responseHeaders = new HashMap<>();
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();

        HttpHeaders headers = proxy(HttpHeaders.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRequestHeaders" -> requestHeaders;
            case "getHeaderString" -> {
                String headerName = (String) args[0];
                if (HttpHeaders.CONTENT_TYPE.equals(headerName)) {
                    yield contentType;
                }
                yield requestHeaders.getFirst(headerName);
            }
            default -> defaultValue(method);
        });

        HttpResponse response = proxy(HttpResponse.class, (proxy, method, args) -> {
            if ("setHeader".equals(method.getName())) {
                responseHeaders.put((String) args[0], (String) args[1]);
                return null;
            }
            return defaultValue(method);
        });

        HttpRequest request = proxy(HttpRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "getHttpMethod" -> httpMethod;
            case "getHttpHeaders" -> headers;
            default -> defaultValue(method);
        });

        RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> {
            if ("getName".equals(method.getName())) {
                return "test";
            }
            return defaultValue(method);
        });

        ClientConnection connection = proxy(ClientConnection.class, (proxy, method, args) -> defaultValue(method));

        KeycloakContext keycloakContext = proxy(KeycloakContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "getConnection" -> connection;
            case "getRealm" -> realm;
            case "getHttpRequest" -> request;
            case "getRequestHeaders" -> headers;
            case "getHttpResponse" -> response;
            default -> defaultValue(method);
        });

        ClientPolicyManager clientPolicyManager = proxy(ClientPolicyManager.class, (proxy, method, args) -> defaultValue(method));

        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getContext" -> keycloakContext;
            case "clientPolicy" -> clientPolicyManager;
            default -> defaultValue(method);
        });

        return new EndpointContext(new UserInfoEndpoint(session, null), responseHeaders);
    }

    private Object defaultValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0f;
        }
        if (double.class.equals(returnType)) {
            return 0d;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static final class EndpointContext {
        private final UserInfoEndpoint endpoint;
        private final Map<String, String> responseHeaders;

        private EndpointContext(UserInfoEndpoint endpoint, Map<String, String> responseHeaders) {
            this.endpoint = endpoint;
            this.responseHeaders = responseHeaders;
        }
    }
}
