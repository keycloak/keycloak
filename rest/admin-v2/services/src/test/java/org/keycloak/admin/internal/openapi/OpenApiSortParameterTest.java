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

package org.keycloak.admin.internal.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import org.keycloak.admin.api.ClientField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSortParameterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sortParameterIsString() throws Exception {
        Path openApiFile = Path.of("target/openapi.json");
        assertTrue(Files.exists(openApiFile), "OpenAPI spec must be generated before running this test");

        JsonNode spec = MAPPER.readTree(openApiFile.toFile());
        JsonNode params = spec.at("/paths/~1admin~1api~1{realmName}~1clients~1v2/get/parameters");
        JsonNode sortParam = StreamSupport.stream(params.spliterator(), false)
                .filter(parameter -> "sort".equals(parameter.path("name").asText()))
                .findFirst()
                .orElse(null);

        assertNotNull(sortParam, "sort parameter must exist in the spec");
        assertEquals("string", sortParam.at("/schema/type").asText(),
                "sort schema type must be 'string' to support per-field sort directions");
        assertTrue(sortParam.path("description").asText().contains("Allowed fields: " + ClientField.allowedApiNames()),
                "sort parameter description must list allowed fields from ClientField metadata");
    }
}
