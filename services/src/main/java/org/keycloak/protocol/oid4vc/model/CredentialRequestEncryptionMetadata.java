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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;

import java.util.List;
import java.util.Map;

/**
 * Represents the credential_request_encryption metadata for an OID4VCI Credential Issuer.
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-credential-issuer-metadata-p}
 *
 * @author <a href="mailto:Bertrand.Ogen@adorsys.com">Bertrand Ogen</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialRequestEncryptionMetadata {

    @JsonProperty("jwks")
    private JSONWebKeySet jwks;

    @JsonProperty("enc_values_supported")
    private List<String> encValuesSupported;

    @JsonProperty("zip_values_supported")
    private List<String> zipValuesSupported;

    @JsonProperty("encryption_required")
    private Boolean encryptionRequired;

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public CredentialRequestEncryptionMetadata setJwks(JSONWebKeySet jwks) {
        if (jwks != null && jwks.getKeys() != null) {
            if (jwks.getKeys().length == 0) {
                throw new IllegalArgumentException("JWKS must contain at least one key");
            }
            for (JWK jwk : jwks.getKeys()) {
                if (jwk.getKeyId() == null || jwk.getKeyId().isEmpty()) {
                    throw new IllegalArgumentException("Each JWK must have a kid");
                }
                if (jwk.getAlgorithm() == null) {
                    throw new IllegalArgumentException("Each JWK must have an alg");
                }
                if (!"enc".equalsIgnoreCase(jwk.getPublicKeyUse())) {
                    throw new IllegalArgumentException("Each JWK must have use=enc");
                }
            }
        }
        this.jwks = jwks;
        return this;
    }

    public List<String> getEncValuesSupported() {
        return encValuesSupported;
    }

    public CredentialRequestEncryptionMetadata setEncValuesSupported(List<String> encValuesSupported) {
        this.encValuesSupported = encValuesSupported;
        return this;
    }

    public List<String> getZipValuesSupported() {
        return zipValuesSupported;
    }

    public CredentialRequestEncryptionMetadata setZipValuesSupported(List<String> zipValuesSupported) {
        this.zipValuesSupported = zipValuesSupported;
        return this;
    }

    public Boolean getEncryptionRequired() {
        return encryptionRequired;
    }

    public CredentialRequestEncryptionMetadata setEncryptionRequired(Boolean encryptionRequired) {
        this.encryptionRequired = encryptionRequired;
        return this;
    }
}
