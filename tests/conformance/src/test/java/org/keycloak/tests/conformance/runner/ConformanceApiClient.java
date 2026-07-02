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

package org.keycloak.tests.conformance.runner;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;


public final class ConformanceApiClient {

    private final URI baseUri;
    private final HttpClient httpClient;

    public ConformanceApiClient(URI baseUri, SSLContext sslContext) {
        this.baseUri = baseUri;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .sslContext(sslContext)
                .build();
    }

    public void waitUntilAvailable(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = send(request("/api/runner/available").GET().build());
                if (response.statusCode() == 200) {
                    return;
                }
                lastFailure = new IllegalStateException("Conformance server returned HTTP " + response.statusCode());
            } catch (RuntimeException e) {
                lastFailure = e;
            }
            sleep(Duration.ofSeconds(2));
        }
        throw new IllegalStateException("Conformance server did not become available at " + baseUri, lastFailure);
    }

    /**
     * Discovers all variant combinations of a module from a created plan.
     */
    public Stream<Map<String, String>> discoverModuleVariants(String planName, Map<String, String> planVariant,
            String moduleName, JsonNode suiteConfig) {
        return discoverModuleVariants(planName, planVariant, List.of(moduleName), suiteConfig)
                .get(moduleName).stream();
    }

    /**
     * Discovers all variant combinations of several modules from a single created plan.
     */
    public Map<String, List<Map<String, String>>> discoverModuleVariants(String planName,
            Map<String, String> planVariant, List<String> moduleNames, JsonNode suiteConfig) {
        JsonNode plan = createPlan(planName, suiteConfig, planVariant);
        Map<String, List<Map<String, String>>> variants = new LinkedHashMap<>();
        moduleNames.forEach(name -> variants.put(name, new ArrayList<>()));
        for (JsonNode entry : plan.path("modules")) {
            List<Map<String, String>> moduleVariants = variants.get(entry.path("testModule").asText());
            if (moduleVariants != null) {
                moduleVariants.add(variantMap(entry.path("variant")));
            }
        }
        variants.forEach((name, moduleVariants) -> {
            if (moduleVariants.isEmpty()) {
                throw new IllegalStateException("Plan contains no module named '" + name + "': " + plan);
            }
        });
        return variants;
    }

    private static Map<String, String> variantMap(JsonNode variant) {
        Map<String, String> map = new LinkedHashMap<>();
        variant.fields().forEachRemaining(field -> map.put(field.getKey(), field.getValue().asText()));
        return map;
    }

    public ConformanceModuleResult run(ConformanceModuleVariant module, JsonNode suiteConfig) {
        return run(module, suiteConfig, null);
    }

    /**
     * Runs a module, optionally driving the system under test through {@code interaction} once the module waits
     * for it, e.g. delivering the credential offer the issuer initiated modules wait for.
     */
    public ConformanceModuleResult run(ConformanceModuleVariant module, JsonNode suiteConfig,
            Consumer<ModuleRun> interaction) {
        JsonNode plan = createPlan(module.plan(), suiteConfig, module.planVariant());
        String planId = requiredText(plan, "id");

        JsonNode moduleNode = createModule(planId, module.name(), module.moduleVariant());
        String moduleId = requiredText(moduleNode, "id");
        JsonNode info = waitForRunnableOrFinished(planId, moduleId);
        if ("CONFIGURED".equals(info.path("status").asText())) {
            startModule(planId, moduleId);
        }
        if (interaction != null) {
            info = waitForState(planId, moduleId, List.of("WAITING", "FINISHED"), Duration.ofMinutes(4));
            if ("WAITING".equals(info.path("status").asText())) {
                interaction.accept(new ModuleRun(moduleNode, info));
            }
        }
        info = waitForFinished(planId, moduleId);
        JsonNode logs = getLogs(planId, moduleId);

        return new ConformanceModuleResult(module.plan(), module.planVariant(), module.name(), module.moduleVariant(),
                planId, moduleId, info.path("status").asText(), info.path("result").asText("UNKNOWN"), logs);
    }

    /**
     * Issues a GET to a per test endpoint exposed by the suite, i.e. {@code /test/a/<alias>/<path>?<query>}.
     * Used to deliver out of band input the suite is waiting for, so it is not replayed on IO failures.
     */
    public void visitTestEndpoint(String alias, String path, String query) {
        String requestPath = "/test/a/" + alias + "/" + path + (query == null || query.isEmpty() ? "" : "?" + query);
        HttpResponse<String> response = send(request(requestPath).GET().build(), false);
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Suite test endpoint " + path + " returned HTTP "
                    + response.statusCode() + ": " + response.body());
        }
    }

    private JsonNode createPlan(String planName, JsonNode suiteConfig, Map<String, String> variants) {
        StringBuilder path = new StringBuilder("/api/plan?planName=").append(URLEncoder.encode(planName, StandardCharsets.UTF_8));
        if (variants != null && !variants.isEmpty()) {
            path.append("&variant=").append(URLEncoder.encode(JsonSerialization.valueAsString(variants), StandardCharsets.UTF_8));
        }
        HttpRequest request = request(path.toString())
                .POST(HttpRequest.BodyPublishers.ofString(suiteConfig.toString()))
                .header("Content-Type", "application/json")
                .build();
        return expectJson(request, 201);
    }

    private JsonNode createModule(String planId, String module, Map<String, String> variants) {
        StringBuilder path = new StringBuilder("/api/runner?test=")
                .append(URLEncoder.encode(module, StandardCharsets.UTF_8))
                .append("&plan=")
                .append(URLEncoder.encode(planId, StandardCharsets.UTF_8));
        if (variants != null && !variants.isEmpty()) {
            path.append("&variant=").append(URLEncoder.encode(JsonSerialization.valueAsString(variants), StandardCharsets.UTF_8));
        }
        return expectJson(request(path.toString()).POST(HttpRequest.BodyPublishers.noBody()).build(), 201);
    }

    private void startModule(String planId, String moduleId) {
        // not replayed on IO failures as a lost response would start the module a second time
        expectJson(request("/api/runner/" + moduleId).POST(HttpRequest.BodyPublishers.noBody()).build(), 200,
                idContext(planId, moduleId), false);
    }

    private JsonNode waitForRunnableOrFinished(String planId, String moduleId) {
        return waitForState(planId, moduleId, List.of("CONFIGURED", "WAITING", "FINISHED"), Duration.ofMinutes(4));
    }

    private JsonNode waitForFinished(String planId, String moduleId) {
        return waitForState(planId, moduleId, List.of("FINISHED"), Duration.ofMinutes(8));
    }

    private JsonNode waitForState(String planId, String moduleId, List<String> states, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode lastInfo = null;
        RuntimeException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                lastInfo = getInfo(planId, moduleId);
                String status = lastInfo.path("status").asText();
                // INTERRUPTED is a terminal state too and surfaces as a normal assertion failure downstream
                if (states.contains(status) || "INTERRUPTED".equals(status)) {
                    return lastInfo;
                }
            } catch (RuntimeException e) {
                // Transient failures are tolerated until the deadline
                lastFailure = e;
            }
            sleep(Duration.ofSeconds(1));
        }
        throw new IllegalStateException("Timed out waiting for conformance module (planId=" + planId
                + ", moduleId=" + moduleId + ") to reach " + states + "; last info: " + lastInfo, lastFailure);
    }

    private JsonNode getInfo(String planId, String moduleId) {
        return expectJson(request("/api/info/" + moduleId).GET().build(), 200, planId, moduleId);
    }

    private JsonNode getLogs(String planId, String moduleId) {
        return expectJson(request("/api/log/" + moduleId).GET().build(), 200, planId, moduleId);
    }

    private JsonNode expectJson(HttpRequest request, int expectedStatus) {
        return expectJson(request, expectedStatus, "", true);
    }

    private JsonNode expectJson(HttpRequest request, int expectedStatus, String planId, String moduleId) {
        return expectJson(request, expectedStatus, idContext(planId, moduleId), true);
    }

    private JsonNode expectJson(HttpRequest request, int expectedStatus, String idContext, boolean replayable) {
        HttpResponse<String> response = send(request, replayable);
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException("Conformance API " + request.method() + " " + request.uri()
                    + idContext + " returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return JsonSerialization.valueFromString(response.body(), JsonNode.class);
    }

    private static String idContext(String planId, String moduleId) {
        // The plan and module ids help cross-reference the failure with the OIDF suite UI
        return " (planId=" + planId + ", moduleId=" + moduleId + ")";
    }

    private HttpResponse<String> send(HttpRequest request) {
        return send(request, true);
    }

    /**
     * The suite occasionally drops a connection (EOF with no bytes), so transient IO failures are retried for
     * replayable requests. A replayed plan or module creation at worst leaves an unused entry behind, while
     * requests that trigger suite actions must be sent exactly once.
     */
    private HttpResponse<String> send(HttpRequest request, boolean replayable) {
        int attempts = replayable ? 4 : 1;
        for (int attempt = 1; ; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                if (attempt >= attempts) {
                    throw new RuntimeException("Conformance API request failed: " + request.uri(), e);
                }
                sleep(Duration.ofSeconds(attempt));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while calling conformance API: " + request.uri(), e);
            }
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json");
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Conformance API response missing '" + field + "': " + node);
        }
        return value;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
