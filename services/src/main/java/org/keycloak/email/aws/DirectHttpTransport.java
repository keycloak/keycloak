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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Talks to the credential endpoints on the local machine, never through a proxy.
 * <p>
 * The ECS/EKS container endpoint and the instance metadata service answer on loopback and link-local
 * addresses, and the request carries a bearer token. If the server is configured with
 * {@code HTTP_PROXY} and no matching {@code no_proxy}, Keycloak's shared client sends everything
 * through that proxy — which would hand the token to it, and would also make the address checks in
 * front of these calls meaningless, because the host being validated is no longer the host being
 * spoken to. This transport is built with {@link HttpClient.Builder#NO_PROXY} so the route is direct
 * by construction.
 * <p>
 * Only those two sources use it. The STS call of the web-identity flow goes to a real internet
 * endpoint and keeps the server's own outbound configuration, proxy included.
 */
final class DirectHttpTransport implements AwsHttpTransport {

    /**
     * A JDK client cannot change its connect timeout per request, so one is kept per timeout value
     * asked for. In practice the map holds a single entry — both callers ask for a second — but the
     * transport contract says the request is executed as described, and a cache is cheaper than
     * either breaking that or building a client, and its threads, on every credential lookup.
     */
    private final Map<Integer, HttpClient> clients = new ConcurrentHashMap<>();

    private final HttpClient fixedClient;

    DirectHttpTransport() {
        this.fixedClient = null;
    }

    DirectHttpTransport(HttpClient httpClient) {
        this.fixedClient = httpClient;
    }

    /** Package-private so a test can assert the one property this class exists for. */
    static HttpClient directClient(int connectTimeoutMillis) {
        return HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();
    }

    private HttpClient clientFor(AwsHttpRequest request) {
        return fixedClient != null ? fixedClient
                : clients.computeIfAbsent(request.connectTimeoutMillis(), DirectHttpTransport::directClient);
    }

    @Override
    public AwsHttpResponse exchange(AwsHttpRequest request) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(Duration.ofMillis(request.readTimeoutMillis()))
                .method(request.method(), bodyOf(request));
        request.headers().forEach(builder::header);

        // Sent asynchronously and waited on with a deadline, because none of the JDK's body
        // handlers is bounded by HttpRequest#timeout: that timeout covers the headers, and a peer
        // that answers and then stalls holds the body read open for as long as it likes. Verified,
        // not assumed — with ofInputStream, ofByteArray and ofByteArrayConsumer alike, a handler
        // that stops writing after the headers keeps the caller blocked. This runs on a request
        // thread inside a Keycloak transaction, so the wait has to end whatever the peer does.
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        CompletableFuture<HttpResponse<Void>> exchange = clientFor(request).sendAsync(builder.build(),
                responseInfo -> HttpResponse.BodySubscribers.ofByteArrayConsumer(
                        chunk -> chunk.ifPresent(bytes -> append(body, bytes))));
        try {
            HttpResponse<Void> response = exchange.get(request.readTimeoutMillis(), TimeUnit.MILLISECONDS);
            return new AwsHttpResponse(response.statusCode(), headersOf(response), body.toByteArray());
        } catch (TimeoutException e) {
            exchange.cancel(true);
            throw new IOException("No complete response from " + request.uri().getHost() + " within "
                    + request.readTimeoutMillis() + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException unchecked) {
                throw unchecked.getCause();
            }
            throw cause instanceof IOException io ? io : new IOException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading " + request.uri().getHost(), e);
        }
    }

    private static void append(ByteArrayOutputStream body, byte[] chunk) {
        if (body.size() + chunk.length > HttpBodies.MAX_RESPONSE_BYTES) {
            // Unchecked because the consumer cannot declare one; unwrapped by the caller.
            throw new UncheckedIOException(
                    new IOException("Response body exceeded " + HttpBodies.MAX_RESPONSE_BYTES + " bytes"));
        }
        body.write(chunk, 0, chunk.length);
    }

    private static HttpRequest.BodyPublisher bodyOf(AwsHttpRequest request) {
        byte[] body = request.body();
        return body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private static Map<String, String> headersOf(HttpResponse<?> response) {
        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });
        return headers;
    }
}
