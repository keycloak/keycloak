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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-proof-types
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofTypesSupported {

    protected Map<String, SupportedProofTypeData> supportedProofTypes = new HashMap<>();

    public static ProofTypesSupported parse(KeycloakSession keycloakSession,
                                            List<String> globalSupportedSigningAlgorithms) {
        ProofTypesSupported proofTypesSupported = new ProofTypesSupported();
        keycloakSession.getAllProviders(ProofValidator.class).forEach(proofValidator -> {
            String type = proofValidator.getProofType();
            KeyAttestationRequired keyAttestationRequired = null; // TODO
            SupportedProofTypeData supportedProofTypeData = new SupportedProofTypeData(globalSupportedSigningAlgorithms,
                                                                                       keyAttestationRequired);
            proofTypesSupported.getSupportedProofTypes().put(type, supportedProofTypeData);
        });
        return proofTypesSupported;
    }

    public static ProofTypesSupported fromJsonString(String jsonString) {
        try {
            return JsonSerialization.readValue(jsonString, ProofTypesSupported.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonAnyGetter
    public Map<String, SupportedProofTypeData> getSupportedProofTypes() {
        return supportedProofTypes;
    }

    @JsonAnySetter
    public ProofTypesSupported setSupportedProofTypes(String name, SupportedProofTypeData value) {
        supportedProofTypes.put(name, value);
        return this;
    }

    public String toJsonString() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ProofTypesSupported that)) {
            return false;
        }
        return Objects.equals(supportedProofTypes, that.supportedProofTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(supportedProofTypes);
    }

    public static class SupportedProofTypeData {

        @JsonProperty("proof_signing_alg_values_supported")
        private List<String> signingAlgorithmsSupported;

        @JsonProperty("key_attestations_required")
        private KeyAttestationRequired keyAttestationRequired;

        public SupportedProofTypeData() {
        }

        public SupportedProofTypeData(List<String> signingAlgorithmsSupported,
                                      KeyAttestationRequired keyAttestationRequired) {
            this.signingAlgorithmsSupported = signingAlgorithmsSupported;
            this.keyAttestationRequired = keyAttestationRequired;
        }

        public List<String> getSigningAlgorithmsSupported() {
            return signingAlgorithmsSupported;
        }

        public SupportedProofTypeData setSigningAlgorithmsSupported(List<String> signingAlgorithmsSupported) {
            this.signingAlgorithmsSupported = signingAlgorithmsSupported;
            return this;
        }

        public KeyAttestationRequired getKeyAttestationRequired() {
            return keyAttestationRequired;
        }

        public SupportedProofTypeData setKeyAttestationRequired(KeyAttestationRequired keyAttestationRequired) {
            this.keyAttestationRequired = keyAttestationRequired;
            return this;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof SupportedProofTypeData that)) {
                return false;
            }

            return Objects.equals(signingAlgorithmsSupported,
                                  that.signingAlgorithmsSupported) && Objects.equals(keyAttestationRequired,
                                                                                     that.keyAttestationRequired);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(signingAlgorithmsSupported);
            result = 31 * result + Objects.hashCode(keyAttestationRequired);
            return result;
        }
    }

    public static class KeyAttestationRequired {

        @JsonProperty("key_storage")
        private List<String> keyStorage = new ArrayList<>();

        @JsonProperty("user_authentication")
        private List<String> userAuthentication = new ArrayList<>();

        public KeyAttestationRequired() {
        }

        public KeyAttestationRequired(List<String> keyStorage, List<String> userAuthentication) {
            this.keyStorage = keyStorage;
            this.userAuthentication = userAuthentication;
        }

        public List<String> getKeyStorage() {
            return keyStorage;
        }

        public KeyAttestationRequired setKeyStorage(List<String> keyStorage) {
            this.keyStorage = keyStorage;
            return this;
        }

        public List<String> getUserAuthentication() {
            return userAuthentication;
        }

        public KeyAttestationRequired setUserAuthentication(List<String> userAuthentication) {
            this.userAuthentication = userAuthentication;
            return this;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof KeyAttestationRequired that)) {
                return false;
            }

            return Objects.equals(keyStorage, that.keyStorage) && Objects.equals(userAuthentication,
                                                                                 that.userAuthentication);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(keyStorage);
            result = 31 * result + Objects.hashCode(userAuthentication);
            return result;
        }
    }
}
