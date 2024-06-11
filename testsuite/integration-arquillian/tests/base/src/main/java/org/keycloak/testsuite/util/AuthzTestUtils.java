/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.testsuite.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.adapters.authorization.spi.HttpResponse;

/**
 *
 * @author rmartinc
 */
public class AuthzTestUtils {

    private AuthzTestUtils() {
    }

    public static InputStream httpsAwareConfigurationStream(InputStream input) throws IOException {
        if (!ServerURLs.AUTH_SERVER_SSL_REQUIRED) {
            return input;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out);
                Scanner s = new Scanner(input)) {
            while (s.hasNextLine()) {
                String lineWithReplaces = s.nextLine().replace("http://localhost:8180/auth",
                        ServerURLs.AUTH_SERVER_SCHEME + "://localhost:" + ServerURLs.AUTH_SERVER_PORT + "/auth");
                pw.println(lineWithReplaces);
            }
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public static InputStream getAdapterConfiguration(String fileName) {
        try {
            return httpsAwareConfigurationStream(AuthzTestUtils.class.getResourceAsStream("/authorization-test/" + fileName));
        } catch (IOException e) {
            throw new AssertionError("Could not load keycloak configuration", e);
        }
    }

    public static PolicyEnforcer createPolicyEnforcer(String resource, boolean bearerOnly) {
        try (InputStream is = getAdapterConfiguration(resource)) {
            return PolicyEnforcer.builder().enforcerConfig(is).bearerOnly(bearerOnly).build();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid resource " + resource, e);
        }
    }

    public static HttpRequest createHttpRequest(String path) {
        return createHttpRequest(path, null, null, null, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public static HttpRequest createHttpRequest(String path, String token) {
        return createHttpRequest(path, null, null, token, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public static HttpRequest createHttpRequest(String path, String token, String method) {
        return createHttpRequest(path, null, method, token, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public static HttpRequest createHttpRequest(String path, String token, Map<String, List<String>> parameters) {
        return createHttpRequest(path, null, null, token, Collections.emptyMap(), parameters, Collections.emptyMap(), null);
    }

    public static HttpRequest createHttpRequest(String path, String method, String token, Map<String,
            List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody) {
        return createHttpRequest(path, null, method, token, headers, parameters, Collections.emptyMap(), null);
    }

    public static HttpRequest createHttpRequest(String path, String relativePath, String method, String token, Map<String,
            List<String>> headers, Map<String, List<String>> parameters, Map<String, String> cookies, InputStream requestBody) {
        return new HttpRequest() {

            private InputStream inputStream;

            @Override
            public String getRelativePath() {
                return relativePath != null? relativePath : path;
            }

            @Override
            public String getMethod() {
                return method == null ? "GET" : method;
            }

            @Override
            public String getURI() {
                return path;
            }

            @Override
            public List<String> getHeaders(String name) {
                return headers.getOrDefault(name, Collections.emptyList());
            }

            @Override
            public String getFirstParam(String name) {
                List<String> values = parameters.getOrDefault(name, Collections.emptyList());
                return values.isEmpty()? null : values.iterator().next();
            }

            @Override
            public String getCookieValue(String name) {
                return cookies.get(name);
            }

            @Override
            public String getRemoteAddr() {
                return "user-remote-addr";
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getHeader(String name) {
                List<String> headers = getHeaders(name);
                return headers.isEmpty()? null : headers.iterator().next();
            }

            @Override
            public InputStream getInputStream(boolean buffered) {
                if (requestBody == null) {
                    return new ByteArrayInputStream(new byte[] {});
                }

                if (inputStream != null) {
                    return inputStream;
                }

                if (buffered) {
                    return inputStream = new BufferedInputStream(requestBody);
                }

                return requestBody;
            }

            @Override
            public TokenPrincipal getPrincipal() {
                return () -> token;
            }
        };
    }

    public static class TestResponse implements HttpResponse {

        private final Map<String, List<String>> headers;
        private int status;

        public TestResponse() {
            this.headers = new HashMap<>();
        }

        public TestResponse(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public int getStatus() {
            return status;
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, Arrays.asList(value));
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public void sendError(int code) {
            status = code;
        }

        @Override
        public void sendError(int code, String message) {
            status = code;
        }

        public TestResponse clear() {
            this.status = -1;
            this.headers.clear();
            return this;
        }
    }
}
