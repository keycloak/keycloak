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

package org.keycloak.email.aws;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One outbound HTTP call, described in full before it is executed.
 * <p>
 * The request is a value object on purpose: SigV4 signs a byte-exact rendering of method, URI,
 * headers and body, so the bytes that are signed and the bytes that reach the socket must come from
 * the same immutable object. Anything that lets a layer below add or rewrite a signed header
 * invalidates the signature.
 *
 * @param method               HTTP method, uppercase
 * @param uri                  absolute request URI; must carry no query string for SES
 * @param headers              headers to place on the request, insertion-ordered
 * @param body                 request body, empty (never {@code null}) for bodyless requests
 * @param connectTimeoutMillis TCP connect timeout
 * @param readTimeoutMillis    socket read timeout
 */
public record AwsHttpRequest(String method, URI uri, Map<String, String> headers, byte[] body,
                             int connectTimeoutMillis, int readTimeoutMillis) {

    public static final byte[] NO_BODY = new byte[0];

    public AwsHttpRequest {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        body = body == null ? NO_BODY : body.clone();
    }

    public static AwsHttpRequest get(URI uri, Map<String, String> headers, int connectTimeoutMillis, int readTimeoutMillis) {
        return new AwsHttpRequest("GET", uri, headers, NO_BODY, connectTimeoutMillis, readTimeoutMillis);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /** The body as text, for the JSON/XML responses of the credential endpoints. */
    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public AwsHttpRequest withHeaders(Map<String, String> additional) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.putAll(additional);
        return new AwsHttpRequest(method, uri, merged, body, connectTimeoutMillis, readTimeoutMillis);
    }
}
