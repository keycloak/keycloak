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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.services.client.ClientField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSortParameterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CLIENTS_LIST_PATH_SUFFIX = "/clients/v2";
    private static final String CLIENT_ALLOWED_FIELDS_MARKER = "Allowed fields: " + ClientField.allowedApiNames();
    private static final String SORT_EXPRESSION_MARKER = "Sort expression.";

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
        assertTrue(sortParam.path("description").asText().contains(CLIENT_ALLOWED_FIELDS_MARKER),
                "sort parameter description must list allowed fields from ClientField metadata");
    }

    @Test
    void sortParameterDescriptionKeepsSortExpressionGuidance() throws Exception {
        String description = enhanceSortParameterDescription(listOptionsSortDescription());

        assertTrue(description.contains(SORT_EXPRESSION_MARKER),
                "sort parameter description must preserve sort syntax guidance from ListOptions");
        assertTrue(description.contains(CLIENT_ALLOWED_FIELDS_MARKER),
                "sort parameter description must list allowed fields from ClientField metadata");
    }

    @Test
    void blankSortParameterDescriptionIsReplacedWithoutLeadingWhitespace() throws Exception {
        String description = enhanceSortParameterDescription("");
        String description2 = enhanceSortParameterDescription(description);

        assertEquals(CLIENT_ALLOWED_FIELDS_MARKER + ".", description,
                "blank sort descriptions should be treated as missing so only allowed-fields text is emitted");
        assertEquals(CLIENT_ALLOWED_FIELDS_MARKER + ".", description2,
                "a second filter pass duplicates the \"Allowed fields:\" text");
    }

    @Test
    void sortParameterAllowedFieldsAreEndpointScoped() throws Exception {
        Path openApiFile = Path.of("target/openapi.json");
        assertTrue(Files.exists(openApiFile), "OpenAPI spec must be generated before running this test");

        JsonNode paths = MAPPER.readTree(openApiFile.toFile()).path("paths");
        for (Map.Entry<String, JsonNode> pathEntry : paths.properties()) {
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();
            for (Map.Entry<String, JsonNode> operationEntry : pathItem.properties()) {
                if (!isHttpMethod(operationEntry.getKey())) {
                    continue;
                }
                JsonNode parameters = operationEntry.getValue().path("parameters");
                if (!parameters.isArray()) {
                    continue;
                }
                for (JsonNode parameter : parameters) {
                    if (!"sort".equals(parameter.path("name").asText())) {
                        continue;
                    }
                    String description = parameter.path("description").asText();
                    if (path.endsWith(CLIENTS_LIST_PATH_SUFFIX)) {
                        assertTrue(description.contains(CLIENT_ALLOWED_FIELDS_MARKER),
                                "clients list sort parameter must list ClientField allowed names at " + path);
                    } else {
                        assertFalse(description.contains(CLIENT_ALLOWED_FIELDS_MARKER),
                                "sort parameter must not list ClientField allowed names outside clients list at " + path);
                    }
                }
            }
        }
    }

    private static boolean isHttpMethod(String name) {
        return switch (name) {
            case "get", "post", "put", "patch", "delete", "head", "options", "trace" -> true;
            default -> false;
        };
    }

    private static String enhanceSortParameterDescription(String description) throws Exception {
        Method method = OASModelFilter.class.getDeclaredMethod("enhanceSortParameterDescription", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, description, ClientField.allowedApiNames());
    }

    private static String listOptionsSortDescription() throws Exception {
        Field sortField = ListOptions.class.getDeclaredField("sort");
        Parameter parameter = sortField.getAnnotation(Parameter.class);
        assertNotNull(parameter, "sort query parameter in ListOptions must declare @Parameter");
        return parameter.description();
    }
}
