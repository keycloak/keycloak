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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.KeycloakUriBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private List<OID4VCAuthorizationDetail> authorizationDetails;

    // =====================================================================================
    // Serialization support
    // =====================================================================================

    public Map<String, List<String>> toRequestParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();

        add(params, "client_id", clientId);
        add(params, "redirect_uri", redirectUri);
        add(params, "response_type", responseType);
        add(params, "response_mode", responseMode);
        add(params, "request", request);
        add(params, "request_uri", requestUri);
        add(params, "response_uri", responseUri);
        add(params, "scope", scope);
        add(params, "nonce", nonce);
        add(params, "state", state);
        add(params, "code_challenge", codeChallenge);
        add(params, "code_challenge_method", codeChallengeMethod);

        if (authorizationDetails != null)
            add(params, "authorization_details", valueAsString(authorizationDetails));

        return params;
    }

    public String toRequestUrl(String endpointUri) {
        Map<String, List<String>> params = toRequestParameters();
        KeycloakUriBuilder b = KeycloakUriBuilder.fromUri(endpointUri, false);
        params.forEach((k, lst) -> b.queryParam(k, lst.toArray()));
        return b.build().toString();
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

    public List<OID4VCAuthorizationDetail> getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(List<OID4VCAuthorizationDetail> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
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
