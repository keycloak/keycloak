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

package org.keycloak.testframework.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Checks with a real client that a response built by {@link HttpServerUtil} arrives as described.
 * <p>
 * The headers are the part worth pinning: they used to be added to the exchange after
 * {@code sendResponseHeaders} had already transmitted the header block, so they never reached the
 * client. Nothing failed, because the only caller did not assert on them — which is why this is a
 * test and not a comment.
 */
public class HttpServerUtilTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void sendsTheHeadersItWasGiven() throws Exception {
        server.createContext("/headers", exchange -> HttpServerUtil.sendResponse(exchange, 200,
                Map.of("X-Custom", List.of("value")), "{}"));

        HttpResponse<String> response = get("/headers");

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("X-Custom").orElse(null), is("value"));
        assertThat(response.body(), is("{}"));
    }

    @Test
    public void sendsAResponseWithNeitherHeadersNorBody() throws Exception {
        server.createContext("/empty", exchange -> HttpServerUtil.sendResponse(exchange, 204, null));

        HttpResponse<String> response = get("/empty");

        assertThat(response.statusCode(), is(204));
        assertThat(response.body(), is(""));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(baseUrl + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
