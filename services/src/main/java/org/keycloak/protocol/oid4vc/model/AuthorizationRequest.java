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
package org.keycloak.protocol.oid4vc.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.keycloak.util.JsonSerialization.valueAsString;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationRequest {

    // ===== Standard OIDC Parameters =====
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("response_mode")
    private String responseMode;

    @JsonProperty("request")
    private String request;

    @JsonProperty("request_uri")
    private String requestUri;

    @JsonProperty("response_uri")
    private String responseUri;

    private String scope;
    private String nonce;
    private String state;

    @JsonProperty("code_challenge")
    private String codeChallenge;

    @JsonProperty("code_challenge_method")
    private String codeChallengeMethod;


    // ===== OIDC4VCI Fields =====
    @JsonProperty("authorization_details")
    private List<AuthorizationDetail> authorizationDetails;

    @JsonProperty("client_metadata")
    private ClientMetadata clientMetadata;

    public static AuthorizationRequest fromHttpParameters(Map<String, String> params) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            AuthorizationRequest req = new AuthorizationRequest();

            req.clientId = require(params, "client_id");
            req.responseType = require(params, "response_type");

            req.redirectUri = params.get("redirect_uri");
            req.responseMode = params.get("response_mode");
            req.request = params.get("request");
            req.requestUri = params.get("request_uri");
            req.responseUri = params.get("response_uri");
            req.scope = params.get("scope");
            req.nonce = params.get("nonce");
            req.state = params.get("state");

            req.codeChallenge = params.get("code_challenge");
            req.codeChallengeMethod = params.get("code_challenge_method");

            if (params.containsKey("authorization_details")) {
                req.authorizationDetails = mapper.readValue(
                        params.get("authorization_details"),
                        mapper.getTypeFactory().constructCollectionType(List.class, AuthorizationDetail.class)
                );
            }

            if (params.containsKey("client_metadata")) {
                req.clientMetadata = mapper.readValue(params.get("client_metadata"), ClientMetadata.class);
            }
            return req;

        } catch (Exception e) {
            throw new RuntimeException("Invalid AuthorizationRequest", e);
        }
    }

    // =====================================================================================
    // Serialization support
    // =====================================================================================

    public Map<String, List<String>> toRequestParameters() {
        Map<String, List<String>> out = new LinkedHashMap<>();

        add(out, "client_id", clientId);
        add(out, "redirect_uri", redirectUri);
        add(out, "response_type", responseType);
        add(out, "response_mode", responseMode);
        add(out, "request", request);
        add(out, "request_uri", requestUri);
        add(out, "response_uri", responseUri);
        add(out, "scope", scope);
        add(out, "nonce", nonce);
        add(out, "state", state);
        add(out, "code_challenge", codeChallenge);
        add(out, "code_challenge_method", codeChallengeMethod);

        if (authorizationDetails != null)
            add(out, "authorization_details", valueAsString(authorizationDetails));
        if (clientMetadata != null)
            add(out, "client_metadata", valueAsString(clientMetadata));

        return out;
    }

    public String toRequestUrl(String endpointUri) {
        Map<String, List<String>> params = toRequestParameters();
        String query = params.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v ->
                        e.getKey() + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        return endpointUri + "?" + query;
    }

    // Getter/Setter ---------------------------------------------------------------------------------------------------

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getResponseUri() {
        return responseUri;
    }

    public void setResponseUri(String responseUri) {
        this.responseUri = responseUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public List<AuthorizationDetail> getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(List<AuthorizationDetail> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    public ClientMetadata getClientMetadata() {
        return clientMetadata;
    }

    public void setClientMetadata(ClientMetadata clientMetadata) {
        this.clientMetadata = clientMetadata;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void add(Map<String, List<String>> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, List.of(value));
        }
    }

    private static String require(Map<String, String> params, String key) {
        if (!params.containsKey(key)) throw new IllegalArgumentException("Missing required parameter: " + key);
        return params.get(key);
    }
}
