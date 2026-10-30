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
package org.keycloak.headers;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.KeycloakSession;

public final class SecurityHeadersUtils {

    private SecurityHeadersUtils() {
    }

    public static void addHeader(BrowserSecurityHeaders header, Map<String, String> sourceHeaders, BiConsumer<String, String> headerWriter) {
        addHeaderIfAbsent(header, sourceHeaders, ignored -> false, headerWriter);
    }

    public static void addDefaultHeaderIfAbsent(BrowserSecurityHeaders header, Predicate<String> hasHeader, BiConsumer<String, String> headerWriter) {
        writeIfAbsent(header, header.getDefaultValue(), hasHeader, headerWriter);
    }

    public static void addHeaderIfAbsent(BrowserSecurityHeaders header, Map<String, String> sourceHeaders, Predicate<String> hasHeader, BiConsumer<String, String> headerWriter) {
        writeIfAbsent(header, sourceHeaders.getOrDefault(header.getKey(), header.getDefaultValue()), hasHeader, headerWriter);
    }

    /**
     * Apply realm browser security headers while a session is available, returning a rebuilt {@link Response}.
     * Needed for AsyncResponse flows where {@code KeycloakSecurityHeadersFilter} runs after the session is closed.
     * Rebuilds the response rather than mutating {@code response.getHeaders()}, which Quarkus REST does not
     * propagate when an AsyncResponse is resumed. Existing values for applied header names are replaced
     * (matching {@code putSingle}) so a prior CSP is not kept alongside the realm policy.
     */
    public static Response applyBrowserSecurityHeaders(KeycloakSession session, Response response) {
        if (response == null || session.getContext().getRealm() == null) {
            return response;
        }
        SecurityHeadersProvider securityHeaders = session.getProvider(SecurityHeadersProvider.class);
        if (securityHeaders == null) {
            return response;
        }
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        securityHeaders.addHeaders(headers);
        if (headers.isEmpty()) {
            return response;
        }
        Response.ResponseBuilder builder = Response.fromResponse(response);
        headers.forEach((name, values) -> {
            builder.header(name, null); // remove values copied by fromResponse
            values.forEach(value -> builder.header(name, value));
        });
        return builder.build();
    }

    private static void writeIfAbsent(BrowserSecurityHeaders header, String value, Predicate<String> hasHeader, BiConsumer<String, String> headerWriter) {
        String headerName = header.getHeaderName();

        if (hasHeader.test(headerName) || value == null || value.isEmpty()) {
            return;
        }

        headerWriter.accept(headerName, value);
    }
}
