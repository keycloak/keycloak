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

package org.keycloak.testframework.conformance.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cleanup of a module whose run failed, against a fake suite API: the module waits for an interaction, which fails.
 */
class ConformanceApiClientTest {

    private static final String MODULE_ID = "module-1";
    private static final ConformanceModuleVariant MODULE = new ConformanceModuleVariant("plan", Map.of(), "module",
            Map.of(), ConformanceResult.PASSED, BrowserInteraction.NONE);

    private HttpServer suite;
    private ConformanceApiClient client;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile String status = "WAITING";
    // the HTTP status the fake suite answers a request to stop the module with
    private volatile int stopStatus = 200;
    // whether the module stops once the fake suite answered the request to stop it with 200
    private volatile boolean stops = true;

    @BeforeEach
    void startSuite() throws Exception {
        suite = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        suite.createContext("/api/", this::handle);
        suite.start();
        URI baseUri = URI.create("http://" + suite.getAddress().getAddress().getHostAddress() + ":"
                + suite.getAddress().getPort());
        client = new ConformanceApiClient(baseUri, SSLContext.getDefault(), Duration.ofSeconds(3));
    }

    @AfterEach
    void stopSuite() {
        suite.stop(0);
    }

    @Test
    void rethrowsTheFailureOfAModuleThatStopped() {
        IllegalStateException failure = new IllegalStateException("interaction failed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> runFailingWith(failure));

        assertSame(failure, thrown);
        assertTrue(requests.contains("DELETE /api/runner/" + MODULE_ID), requests::toString);
        assertEquals("INTERRUPTED", status);
    }

    @Test
    void rethrowsTheFailureOfAModuleNotRunningAnyMore() {
        stopStatus = 404;
        IllegalStateException failure = new IllegalStateException("interaction failed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> runFailingWith(failure));

        assertSame(failure, thrown);
    }

    @Test
    void reportsAModuleThatCannotBeStopped() {
        stopStatus = 500;
        IllegalStateException failure = new IllegalStateException("interaction failed");

        ModuleCleanupFailedException thrown = assertThrows(ModuleCleanupFailedException.class, () -> runFailingWith(failure));

        assertSame(failure, thrown.getCause());
        assertEquals(1, thrown.getSuppressed().length);
        assertTrue(thrown.getSuppressed()[0].getMessage().contains("HTTP 500"), thrown.getSuppressed()[0]::getMessage);
    }

    @Test
    void reportsAModuleThatDoesNotStop() {
        stops = false;
        IllegalStateException failure = new IllegalStateException("interaction failed");

        ModuleCleanupFailedException thrown = assertThrows(ModuleCleanupFailedException.class, () -> runFailingWith(failure));

        assertSame(failure, thrown.getCause());
        assertTrue(thrown.getSuppressed()[0].getMessage().contains("Timed out"), thrown.getSuppressed()[0]::getMessage);
    }

    @Test
    void doesNotStopAModuleWhoseRunSucceeded() {
        status = "FINISHED";

        ConformanceModuleResult result = client.run(MODULE, JsonNodeFactory.instance.objectNode(), null, null);

        assertTrue(result.finishedWith(ConformanceResult.PASSED));
        assertTrue(requests.stream().noneMatch(request -> request.startsWith("DELETE")), requests::toString);
    }

    private void runFailingWith(RuntimeException failure) {
        client.run(MODULE, JsonNodeFactory.instance.objectNode(), null, moduleRun -> {
            throw failure;
        });
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        requests.add(method + " " + path);
        exchange.getRequestBody().readAllBytes();
        if ("POST".equals(method) && path.equals("/api/plan")) {
            respond(exchange, 201, "{\"id\":\"plan-1\",\"modules\":[]}");
        } else if ("POST".equals(method) && path.equals("/api/runner")) {
            respond(exchange, 201, "{\"id\":\"" + MODULE_ID + "\"}");
        } else if ("GET".equals(method) && path.equals("/api/info/" + MODULE_ID)) {
            respond(exchange, 200, "{\"status\":\"" + status + "\",\"result\":\"PASSED\"}");
        } else if ("GET".equals(method) && path.equals("/api/log/" + MODULE_ID)) {
            respond(exchange, 200, "[]");
        } else if ("DELETE".equals(method) && path.equals("/api/runner/" + MODULE_ID)) {
            if (stopStatus == 200 && stops) {
                status = "INTERRUPTED";
            }
            respond(exchange, stopStatus, stopStatus == 200 ? "{}" : "");
        } else {
            respond(exchange, 404, "");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
