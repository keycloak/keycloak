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

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A running conformance module: the runner creation response and the latest module info. Passed to
 * the interaction callback so a test can drive the system under test once the module waits for it.
 */
public record ModuleRun(JsonNode created, JsonNode info) {

    /**
     * The authorization endpoint of the module's wallet, used to deliver an OID4VP verifier request.
     * Derived from the module URL in the runner creation response.
     */
    public URI authorizationEndpoint() {
        String url = created.path("url").asText(created.path("testUrl").asText(""));
        if (url.isBlank()) {
            throw new IllegalStateException("Conformance module exposes no authorization endpoint. Creation response: "
                    + created + ", module info: " + info);
        }
        URI uri = URI.create(url);
        String path = uri.getPath() != null ? uri.getPath() : "";
        return path.endsWith("/authorize") ? uri : URI.create(url.replaceAll("/$", "") + "/authorize");
    }
}
