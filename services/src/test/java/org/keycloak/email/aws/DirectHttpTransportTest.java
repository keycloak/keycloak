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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * The transport used for the credential endpoints that answer on this machine.
 * <p>
 * Its reason to exist is the proxy: the container and instance-metadata calls carry a bearer token,
 * and a server configured with {@code HTTP_PROXY} and no matching {@code no_proxy} would otherwise
 * send that token through the proxy — and make the address checks in front of those calls
 * meaningless, since the host being validated would no longer be the host being spoken to.
 */
class DirectHttpTransportTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /** The whole point of the class: no proxy can be interposed, whatever the server is configured with. */
    @Test
    void buildsItsClientWithoutAProxy() {
        ProxySelector selector = DirectHttpTransport.directClient(1000).proxy().orElseThrow();
        assertThat(selector.select(URI.create("http://169.254.170.2/v1/credentials")),
                is(List.of(Proxy.NO_PROXY)));
        assertThat(DirectHttpTransport.directClient(2500).connectTimeout().orElseThrow().toMillis(), is(2500L));
    }

    @Test
    void sendsTheRequestAsDescribedAndReturnsTheAnswer() throws Exception {
        Map<String, String> received = new LinkedHashMap<>();
        server.createContext("/v2/credentials", exchange -> {
            exchange.getRequestHeaders().forEach((name, values) -> received.put(name, values.get(0)));
            received.put(":method", exchange.getRequestMethod());
            byte[] body = "{\"AccessKeyId\":\"ASIA\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Answer", "yes");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        AwsHttpResponse response = new DirectHttpTransport().exchange(AwsHttpRequest.get(
                URI.create(baseUrl + "/v2/credentials"), Map.of("Authorization", "Bearer secret"), 1000, 1000));

        assertThat(received.get(":method"), is("GET"));
        assertThat(received.get("Authorization"), is("Bearer secret"));
        assertThat(response.status(), is(200));
        assertThat(response.header("x-answer"), is("yes"));
        assertThat(response.bodyAsString(), is("{\"AccessKeyId\":\"ASIA\"}"));
    }

    /** The IMDSv2 token call: a PUT that carries a header and no body. */
    @Test
    void sendsABodylessPut() throws Exception {
        Map<String, String> received = new LinkedHashMap<>();
        server.createContext("/latest/api/token", exchange -> {
            received.put(":method", exchange.getRequestMethod());
            received.put("ttl", exchange.getRequestHeaders().getFirst("X-aws-ec2-metadata-token-ttl-seconds"));
            byte[] body = "AQAE-token".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        AwsHttpResponse response = new DirectHttpTransport().exchange(new AwsHttpRequest("PUT",
                URI.create(baseUrl + "/latest/api/token"),
                Map.of("X-aws-ec2-metadata-token-ttl-seconds", "21600"), AwsHttpRequest.NO_BODY, 1000, 1000));

        assertThat(received.get(":method"), is("PUT"));
        assertThat(received.get("ttl"), is("21600"));
        assertThat(response.bodyAsString(), is("AQAE-token"));
    }

    /**
     * A redirect is returned rather than followed. Following one would send the bearer token to
     * whatever the endpoint nominated, which is the redirection the address checks exist to prevent.
     */
    @Test
    void doesNotFollowARedirect() throws Exception {
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://example.com/elsewhere");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        AwsHttpResponse response = new DirectHttpTransport()
                .exchange(AwsHttpRequest.get(URI.create(baseUrl + "/moved"), Map.of(), 1000, 1000));

        assertThat(response.status(), is(302));
        assertThat(response.header("Location"), is("http://example.com/elsewhere"));
    }

    @Test
    void refusesAResponseBodyLargerThanTheBound() {
        server.createContext("/flood", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                byte[] chunk = new byte[64 * 1024];
                for (int i = 0; i < 32; i++) {
                    out.write(chunk);
                }
            } catch (IOException ignored) {
                // The client aborts once the bound is passed; that is the behaviour under test.
            }
        });

        IOException failure = org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> new DirectHttpTransport()
                        .exchange(AwsHttpRequest.get(URI.create(baseUrl + "/flood"), Map.of(), 1000, 5000)));

        assertThat(failure.getMessage(), containsString("Response body exceeded 1048576 bytes"));
    }

    /**
     * A peer that sends the headers and then stalls must not pin the thread. With a streaming body
     * handler the request timeout stops applying the moment the headers arrive, so the read would
     * block for as long as the peer liked — on a request thread, inside a Keycloak transaction.
     */
    @Test
    void givesUpOnAPeerThatSendsHeadersAndThenStalls() {
        server.createContext("/stall", exchange -> {
            exchange.sendResponseHeaders(200, 1024);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });

        long startedAt = System.nanoTime();
        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> new DirectHttpTransport()
                        .exchange(AwsHttpRequest.get(URI.create(baseUrl + "/stall"), Map.of(), 1000, 500)));

        assertThat(Duration.ofNanos(System.nanoTime() - startedAt).toMillis() < 2500, is(true));
    }

    @Test
    void returnsAnEmptyBodyRatherThanNull() throws Exception {
        server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        AwsHttpResponse response = new DirectHttpTransport()
                .exchange(AwsHttpRequest.get(URI.create(baseUrl + "/empty"), Map.of(), 1000, 1000));

        assertThat(response.status(), is(204));
        assertThat(response.bodyAsString(), is(""));
        assertThat(response.header("nope"), is(nullValue()));
    }
}
