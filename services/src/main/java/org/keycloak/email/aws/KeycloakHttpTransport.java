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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Runs an {@link AwsHttpRequest} on Keycloak's shared HTTP client, so SES and the AWS credential
 * endpoints inherit the server's outbound HTTP configuration — truststore, proxy mappings,
 * connection pool — instead of opening a second, differently-configured stack alongside it.
 * <p>
 * Two things are overridden per request, and both matter. Timeouts: the shared client's defaults
 * leave connect and connection-request unbounded, which on a blackholed route would hold a Keycloak
 * transaction open until the operating system gives up on the TCP handshake. And redirects: a
 * redirect would replay a SigV4-signed request against a host the signature does not cover, so the
 * only useful outcome is a clear failure.
 * <p>
 * Note that Apache's per-request configuration <em>replaces</em> the client's rather than merging
 * with it, which is why all three timeouts are set together — setting one alone silently reverts the
 * other two to "infinite".
 */
final class KeycloakHttpTransport implements AwsHttpTransport {

    private final CloseableHttpClient httpClient;

    KeycloakHttpTransport(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public AwsHttpResponse exchange(AwsHttpRequest request) throws IOException {
        HttpRequestBase httpRequest = build(request);
        httpRequest.setConfig(RequestConfig.custom()
                .setConnectTimeout(request.connectTimeoutMillis())
                .setConnectionRequestTimeout(request.connectTimeoutMillis())
                .setSocketTimeout(request.readTimeoutMillis())
                .setRedirectsEnabled(false)
                .build());

        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            httpRequest.setHeader(header.getKey(), header.getValue());
        }

        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            HttpEntity entity = response.getEntity();
            return new AwsHttpResponse(response.getStatusLine().getStatusCode(), headersOf(response),
                    HttpBodies.readBounded(entity == null ? null : entity.getContent()));
        } finally {
            // Releases the pooled connection if the exchange was abandoned mid-flight; a no-op on the
            // normal path, and the difference between a leaked connection and a returned one on the
            // path where the caller's read timed out.
            httpRequest.reset();
        }
    }

    private static HttpRequestBase build(AwsHttpRequest request) {
        String uri = request.uri().toString();
        switch (request.method()) {
            case "GET":
                return new HttpGet(uri);
            case "POST":
                return withBody(new HttpPost(uri), request);
            case "PUT":
                return withBody(new HttpPut(uri), request);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method " + request.method());
        }
    }

    private static HttpRequestBase withBody(HttpEntityEnclosingRequestBase httpRequest, AwsHttpRequest request) {
        byte[] body = request.body();
        if (body.length > 0) {
            // The single-argument constructor attaches no Content-Type of its own, leaving the one
            // the caller set — and signed — untouched. Content-Length is left to Apache, which
            // derives it from these exact bytes; setting it here makes httpcore reject the request.
            httpRequest.setEntity(new ByteArrayEntity(body));
        }
        return httpRequest;
    }

    private static Map<String, String> headersOf(CloseableHttpResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        return headers;
    }
}
