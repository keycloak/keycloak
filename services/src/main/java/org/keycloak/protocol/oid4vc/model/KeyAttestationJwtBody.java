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

package org.keycloak.protocol.oid4vc.model;

import java.util.List;
import java.util.Map;

import org.keycloak.jose.jwk.JWK;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the JWT payload for a key attestation as per OID4VCI specification.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-key-attestations">OID4VCI Specification</a>
 *
 * @author Bertrand Ogen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyAttestationJwtBody {

    @JsonProperty("iat")
    private Long iat;

    @JsonProperty("exp")
    private Long exp;

    @JsonProperty("attested_keys")
    private List<JWK> attestedKeys;

    @JsonProperty("key_storage")
    private List<String> keyStorage;

    @JsonProperty("user_authentication")
    private List<String> userAuthentication;

    @JsonProperty("certification")
    private String certification;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("status")
    private Map<String, Object> status;

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public List<JWK> getAttestedKeys() {
        return attestedKeys;
    }

    public KeyAttestationJwtBody setAttestedKeys(List<JWK> attestedKeys) {
        this.attestedKeys = attestedKeys;
        return this;
    }

    public List<String> getKeyStorage() {
        return keyStorage;
    }

    public KeyAttestationJwtBody setKeyStorage(List<String> keyStorage) {
        this.keyStorage = keyStorage;
        return this;
    }

    public List<String> getUserAuthentication() {
        return userAuthentication;
    }

    public KeyAttestationJwtBody setUserAuthentication(List<String> userAuthentication) {
        this.userAuthentication = userAuthentication;
        return this;
    }

    public String getCertification() {
        return certification;
    }

    public KeyAttestationJwtBody setCertification(String certification) {
        this.certification = certification;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public KeyAttestationJwtBody setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public Map<String, Object> getStatus() {
        return status;
    }

    public void setStatus(Map<String, Object> status) {
        this.status = status;
    }
}
