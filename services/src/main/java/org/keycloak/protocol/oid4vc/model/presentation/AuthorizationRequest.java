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
package org.keycloak.protocol.oid4vc.model.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationRequest {

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("aud")
    private String audience;

    @JsonProperty("iat")
    private Long issuedAt;

    @JsonProperty("exp")
    private Long expiration;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("response_mode")
    private String responseMode;

    @JsonProperty("response_uri")
    private String responseUri;

    @JsonProperty("state")
    private String state;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("dcql_query")
    private DcqlQuery dcqlQuery;

    public String getJti() {
        return jti;
    }

    public AuthorizationRequest setJti(String jti) {
        this.jti = jti;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public AuthorizationRequest setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getAudience() {
        return audience;
    }

    public AuthorizationRequest setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public AuthorizationRequest setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public Long getExpiration() {
        return expiration;
    }

    public AuthorizationRequest setExpiration(Long expiration) {
        this.expiration = expiration;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public AuthorizationRequest setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public AuthorizationRequest setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public AuthorizationRequest setResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public String getResponseUri() {
        return responseUri;
    }

    public AuthorizationRequest setResponseUri(String responseUri) {
        this.responseUri = responseUri;
        return this;
    }

    public String getState() {
        return state;
    }

    public AuthorizationRequest setState(String state) {
        this.state = state;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public AuthorizationRequest setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public DcqlQuery getDcqlQuery() {
        return dcqlQuery;
    }

    public AuthorizationRequest setDcqlQuery(DcqlQuery dcqlQuery) {
        this.dcqlQuery = dcqlQuery;
        return this;
    }
}
