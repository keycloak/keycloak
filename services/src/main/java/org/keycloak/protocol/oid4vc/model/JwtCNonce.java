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
 *
 */

package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pascal Knüppel
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtCNonce {

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("aud")
    private List<String> audience;

    @JsonProperty("iat")
    private Long issuedAt;

    @JsonProperty("exp")
    private Long expiresAt;

    /**
     * cryptographically strong random string that serves as salt for the predictable c_nonce values
     */
    @JsonProperty("nonce")
    private String nonce;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public JwtCNonce setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    @JsonAnySetter
    public JwtCNonce setAdditionalProperties(String name, Object property) {
        additionalProperties.put(name, property);
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public JwtCNonce issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public List<String> getAudience() {
        return audience;
    }

    public JwtCNonce audience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public JwtCNonce issuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public JwtCNonce expiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public JwtCNonce nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
}
