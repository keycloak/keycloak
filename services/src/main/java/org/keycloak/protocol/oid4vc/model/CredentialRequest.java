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

import java.util.Map;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Represents a CredentialRequest according to OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialRequest {

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @JsonProperty("credential_identifier")
    private String credentialIdentifier;

    @JsonProperty("proofs")
    private Proofs proofs;

    /**
     * Deprecated: use {@link #proofs} instead.
     * This field is kept only for backward compatibility with clients sending a single 'proof'.
     */
    @Deprecated
    @JsonProperty("proof")
    private JwtProof proof;

    // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-format-identifier-3
    @JsonProperty("credential_definition")
    private CredentialDefinition credentialDefinition;

    @JsonProperty("credential_response_encryption")
    private CredentialResponseEncryption credentialResponseEncryption;

    public String getCredentialIdentifier() {
        return credentialIdentifier;
    }

    public CredentialRequest setCredentialIdentifier(String credentialIdentifier) {
        this.credentialIdentifier = credentialIdentifier;
        return this;
    }

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public CredentialRequest setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
        return this;
    }

    public Proofs getProofs() {
        return proofs;
    }

    public CredentialRequest setProofs(Proofs proofs) {
        this.proofs = proofs;
        return this;
    }

    public JwtProof getProof() {
        return proof;
    }

    public CredentialRequest setProof(JwtProof proof) {
        this.proof = proof;
        return this;
    }

    public CredentialDefinition getCredentialDefinition() {
        return credentialDefinition;
    }

    public CredentialRequest setCredentialDefinition(CredentialDefinition credentialDefinition) {
        this.credentialDefinition = credentialDefinition;
        return this;
    }

    public CredentialResponseEncryption getCredentialResponseEncryption() {
        return credentialResponseEncryption;
    }

    public CredentialRequest setCredentialResponseEncryption(CredentialResponseEncryption credentialResponseEncryption) {
        this.credentialResponseEncryption = credentialResponseEncryption;
        return this;
    }

    public Optional<CredentialScopeModel> findCredentialScope(KeycloakSession keycloakSession) {
        Map<String, String> searchAttributeMap =
                Optional.ofNullable(credentialConfigurationId)
                        .map(credentialIdentifier -> {
                            return Map.of(CredentialScopeModel.CONFIGURATION_ID, credentialConfigurationId);
                        }).orElseGet(() -> {
                            return Map.of(CredentialScopeModel.CREDENTIAL_IDENTIFIER, credentialIdentifier);
                        });

        RealmModel currentRealm = keycloakSession.getContext().getRealm();
        final boolean useOrExpression = false;
        return keycloakSession.clientScopes()
                .getClientScopesByAttributes(currentRealm, searchAttributeMap, useOrExpression)
                .map(CredentialScopeModel::new)
                .findAny();
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
