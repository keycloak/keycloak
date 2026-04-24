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
package org.keycloak.protocol.oid4vp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OID4VPAuthorizationRequest {

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
    private OID4VPDcqlQuery dcqlQuery;

    public String getJti() {
        return jti;
    }

    public OID4VPAuthorizationRequest setJti(String jti) {
        this.jti = jti;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public OID4VPAuthorizationRequest setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getAudience() {
        return audience;
    }

    public OID4VPAuthorizationRequest setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public OID4VPAuthorizationRequest setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public Long getExpiration() {
        return expiration;
    }

    public OID4VPAuthorizationRequest setExpiration(Long expiration) {
        this.expiration = expiration;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public OID4VPAuthorizationRequest setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public OID4VPAuthorizationRequest setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public OID4VPAuthorizationRequest setResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public String getResponseUri() {
        return responseUri;
    }

    public OID4VPAuthorizationRequest setResponseUri(String responseUri) {
        this.responseUri = responseUri;
        return this;
    }

    public String getState() {
        return state;
    }

    public OID4VPAuthorizationRequest setState(String state) {
        this.state = state;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public OID4VPAuthorizationRequest setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public OID4VPDcqlQuery getDcqlQuery() {
        return dcqlQuery;
    }

    public OID4VPAuthorizationRequest setDcqlQuery(OID4VPDcqlQuery dcqlQuery) {
        this.dcqlQuery = dcqlQuery;
        return this;
    }
}
