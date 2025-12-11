/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm.authorization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class AuthorizationSchema {

    @JsonDeserialize(using = ResourceTypeDeserializer.class)
    private final Map<String, ResourceType> resourceTypes;

    @JsonCreator
    public AuthorizationSchema(@JsonProperty("resourceTypes") Map<String, ResourceType> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public Map<String, ResourceType> getResourceTypes() {
        return Collections.unmodifiableMap(resourceTypes);
    }

    // Custom deserializer to handle both arrays and maps
    public static class ResourceTypeDeserializer extends JsonDeserializer<Map<String, ResourceType>> {
        @Override
        public Map<String, ResourceType> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            // Check if the input is an array or an object
            if (parser.isExpectedStartArrayToken()) {
                // Deserialize array of ResourceType and convert to Map
                List<ResourceType> resourceTypeList = parser.readValueAs(new TypeReference<List<ResourceType>>() {});
                return resourceTypeList.stream()
                        .collect(Collectors.toMap(ResourceType::getType, Function.identity()));
            } else if (parser.isExpectedStartObjectToken()) {
                // Deserialize directly as a Map
                return parser.readValueAs(new TypeReference<Map<String, ResourceType>>() {});
            } else {
                // Throw JsonMappingException for unexpected formats
                throw JsonMappingException.from(parser, "Expected an array or object for resourceTypes");
            }
        }
    }
}
