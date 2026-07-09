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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public final class AuthZen {

    public static final String AUTHZEN_ACCESS_PATH = "access/v1";
    public static final String EVALUATION_PATH = AUTHZEN_ACCESS_PATH + "/evaluation";
    public static final String EVALUATIONS_PATH = AUTHZEN_ACCESS_PATH + "/evaluations";

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

    public record EvaluationResponse(boolean decision, Map<String, Object> context) {
        public EvaluationResponse(boolean decision) {
            this(decision, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationItem(
            Subject subject,
            Resource resource,
            Action action,
            Map<String, Object> context) {}

    public enum EvaluationsSemantic {
        @JsonProperty("execute_all")
        EXECUTE_ALL,

        @JsonProperty("deny_on_first_deny")
        DENY_ON_FIRST_DENY,

        @JsonProperty("permit_on_first_permit")
        PERMIT_ON_FIRST_PERMIT;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Options(EvaluationsSemantic evaluationsSemantic) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationsRequest(
            Subject subject,
            Resource resource,
            Action action,
            Map<String, Object> context,
            Options options,
            List<EvaluationItem> evaluations) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationsResponse(List<EvaluationResponse> evaluations) {}
}
