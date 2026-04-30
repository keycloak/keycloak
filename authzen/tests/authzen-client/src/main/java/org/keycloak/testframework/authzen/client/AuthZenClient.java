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
package org.keycloak.testframework.authzen.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.authorization.authzen.AuthZen;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class AuthZenClient {

    private final SimpleHttp simpleHttp;
    private final String realmUrl;

    public AuthZenClient(SimpleHttp simpleHttp, String realmUrl) {
        this.simpleHttp = simpleHttp;
        this.realmUrl = realmUrl;
    }

    public Authenticated withAccessToken(String accessToken) {
        return new Authenticated(simpleHttp, realmUrl, accessToken);
    }

    public EvaluationResult evaluate(AuthZen.EvaluationRequest request) throws IOException {
        return evaluate((Object) request);
    }

    public EvaluationResult evaluate(JsonNode request) throws IOException {
        return evaluate((Object) request);
    }

    private EvaluationResult evaluate(Object req) throws IOException {
        String url = realmUrl + "/authzen/access/v1/evaluation";

        try (SimpleHttpResponse response = req(simpleHttp.doPost(url).json(req))) {
            int status = response.getStatus();
            AuthZen.EvaluationResponse body = response.asJson(AuthZen.EvaluationResponse.class);
            return new EvaluationResult(status, body);
        }
    }

    public WellKnownResponse fetchWellKnownConfiguration() throws IOException {
        String url = realmUrl + "/.well-known/authzen-configuration";
        try (SimpleHttpResponse rsp = req(simpleHttp.doGet(url))) {
            return rsp.asJson(WellKnownResponse.class);
        }
    }

    protected SimpleHttpResponse req(SimpleHttpRequest request) throws IOException {
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.acceptJson();
        return request.asResponse();
    }

    public static class Authenticated extends AuthZenClient {

        private final String accessToken;

        private Authenticated(SimpleHttp simpleHttp, String realmUrl, String accessToken) {
            super(simpleHttp, realmUrl);
            this.accessToken = accessToken;
        }

        @Override
        protected SimpleHttpResponse req(SimpleHttpRequest request) throws IOException {
            return super.req(request.auth(accessToken));
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WellKnownResponse(
          String policyDecisionPoint,
          String accessEvaluationEndpoint,
          String accessEvaluationsEndpoint
    ) {
    }

    public record EvaluationResult(int statusCode, AuthZen.EvaluationResponse response) {

        public boolean decision() {
            return response.decision();
        }
    }

    public static EvaluationRequestBuilder evaluationRequest() {
        return new EvaluationRequestBuilder();
    }

    public static final class EvaluationRequestBuilder {
        private AuthZen.SubjectType subjectType;
        private String subjectId;
        private Map<String, Object> subjectProperties;
        private boolean subjectSet;
        private String resourceType;
        private String resourceId;
        private Map<String, Object> resourceProperties;
        private boolean resourceSet;
        private AuthZen.Action action;
        private Map<String, Object> contextProperties;

        public EvaluationRequestBuilder subject(AuthZen.SubjectType type, String id) {
            this.subjectType = type;
            this.subjectId = id;
            this.subjectSet = true;
            return this;
        }

        public EvaluationRequestBuilder subjectProperty(String key, Object value) {
            if (subjectProperties == null) {
                subjectProperties = new HashMap<>();
            }
            subjectProperties.put(key, value);
            return this;
        }

        public EvaluationRequestBuilder resource(String type, String id) {
            this.resourceType = type;
            this.resourceId = id;
            this.resourceSet = true;
            return this;
        }

        public EvaluationRequestBuilder resourceProperty(String key, Object value) {
            if (resourceProperties == null) {
                resourceProperties = new HashMap<>();
            }
            resourceProperties.put(key, value);
            return this;
        }

        public EvaluationRequestBuilder action(String name) {
            this.action = new AuthZen.Action(name);
            return this;
        }

        public EvaluationRequestBuilder contextProperty(String key, Object value) {
            if (contextProperties == null) {
                contextProperties = new HashMap<>();
            }
            contextProperties.put(key, value);
            return this;
        }

        public AuthZen.EvaluationRequest build() {
            AuthZen.Subject subject = subjectSet ? new AuthZen.Subject(subjectType, subjectId, subjectProperties) : null;
            AuthZen.Resource resource = resourceSet ? new AuthZen.Resource(resourceType, resourceId, resourceProperties) : null;
            return new AuthZen.EvaluationRequest(subject, resource, action, contextProperties);
        }
    }
}
