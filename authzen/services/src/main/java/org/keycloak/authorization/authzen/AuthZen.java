/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.authzen;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public final class AuthZen {

    public static final String AUTHZEN_ACCESS_PATH = "access/v1";
    public static final String EVALUATION_PATH = AUTHZEN_ACCESS_PATH + "/evaluation";

    private AuthZen() {
    }

    public enum SubjectType {
        CLIENT("client"),
        USER("user");

        private final String value;

        SubjectType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static SubjectType fromValue(String value) {
            for (SubjectType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported subject type: " + value);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Subject(
            @JsonProperty(required = true) SubjectType type,
            @JsonProperty(required = true) String id,
            Map<String, Object> properties) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resource(
            @JsonProperty(required = true) String type,
            @JsonProperty(required = true) String id,
            Map<String, Object> properties) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Action(String name) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationRequest(
            @JsonProperty(required = true) Subject subject,
            @JsonProperty(required = true) Resource resource,
            @JsonProperty(required = true) Action action,
            Map<String, Object> context) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvaluationResponse(boolean decision) {}
}
