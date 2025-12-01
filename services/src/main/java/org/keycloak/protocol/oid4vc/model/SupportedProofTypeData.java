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

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the supported proof type data for a given proof type in the OpenID for Verifiable Credential Issuance.
 *
 * @author <a href="mailto:Bertrand.Ogena@adorsys.com">Bertrand Ogen</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedProofTypeData {

    @JsonProperty("proof_signing_alg_values_supported")
    private List<String> signingAlgorithmsSupported;

    @JsonProperty("key_attestations_required")
    private KeyAttestationsRequired keyAttestationsRequired;
    
    /**
     * Default constructor for Jackson deserialization
     */
    public SupportedProofTypeData() {
        // Default constructor for Jackson deserialization
    }

    public SupportedProofTypeData(List<String> signingAlgorithmsSupported,
                                  KeyAttestationsRequired keyAttestationsRequired) {
        this.signingAlgorithmsSupported = signingAlgorithmsSupported;
        this.keyAttestationsRequired = keyAttestationsRequired;
    }

    public List<String> getSigningAlgorithmsSupported() {
        return signingAlgorithmsSupported;
    }

    public SupportedProofTypeData setSigningAlgorithmsSupported(List<String> signingAlgorithmsSupported) {
        this.signingAlgorithmsSupported = signingAlgorithmsSupported;
        return this;
    }

    public KeyAttestationsRequired getKeyAttestationsRequired() {
        return keyAttestationsRequired;
    }

    public SupportedProofTypeData setKeyAttestationsRequired(KeyAttestationsRequired keyAttestationsRequired) {
        this.keyAttestationsRequired = keyAttestationsRequired;
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof SupportedProofTypeData that)) {
            return false;
        }

        return Objects.equals(signingAlgorithmsSupported,
                that.signingAlgorithmsSupported) && Objects.equals(keyAttestationsRequired,
                that.keyAttestationsRequired);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(signingAlgorithmsSupported);
        result = 31 * result + Objects.hashCode(keyAttestationsRequired);
        return result;
    }
}
