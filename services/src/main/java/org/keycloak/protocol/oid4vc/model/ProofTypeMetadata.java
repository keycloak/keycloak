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
 * Metadata describing proof types supported by the Credential Issuer.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-credential-issuer-metadata-p">
 * Credential Issuer Metadata Parameters</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofTypeMetadata {

    // Algorithms supported for this proof type
    @JsonProperty("proof_signing_alg_values_supported")
    private List<String> proofSigningAlgValuesSupported;

    // Requirements on key attestations for this proof type
    // If the Credential Issuer does not require a key attestation,
    // this parameter MUST NOT be present in the metadata.
    @JsonProperty("key_attestations_required")
    private KeyAttestationsRequired keyAttestationsRequired;

    public List<String> getProofSigningAlgValuesSupported() {
        return proofSigningAlgValuesSupported;
    }

    public ProofTypeMetadata setProofSigningAlgValuesSupported(List<String> proofSigningAlgValuesSupported) {
        this.proofSigningAlgValuesSupported = proofSigningAlgValuesSupported;
        return this;
    }

    public KeyAttestationsRequired getKeyAttestationsRequired() {
        return keyAttestationsRequired;
    }

    public ProofTypeMetadata setKeyAttestationsRequired(KeyAttestationsRequired keyAttestationsRequired) {
        this.keyAttestationsRequired = keyAttestationsRequired;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProofTypeMetadata that = (ProofTypeMetadata) o;
        return Objects.equals(proofSigningAlgValuesSupported, that.proofSigningAlgValuesSupported) && Objects.equals(keyAttestationsRequired, that.keyAttestationsRequired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proofSigningAlgValuesSupported, keyAttestationsRequired);
    }
}
