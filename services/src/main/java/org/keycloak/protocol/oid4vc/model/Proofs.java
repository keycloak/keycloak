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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Proofs object for Credential Request in OID4VCI (Section 8.2).
 * Contains arrays of different proof types (jwt, di_vp, attestation).
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-16.html#name-credential-request">OID4VCI Credential Request</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proofs {

    @JsonProperty("jwt")
    private List<String> jwt;

    @JsonProperty("di_vp")
    private List<DiVpProof> diVp;

    @JsonProperty("attestation")
    private List<String> attestation;

    public Proofs() {
    }

    public Proofs(List<String> jwt, List<DiVpProof> diVp, List<String> attestation) {
        this.jwt = jwt;
        this.diVp = diVp;
        this.attestation = attestation;
    }

    public List<String> getJwt() {
        return jwt;
    }

    public Proofs setJwt(List<String> jwt) {
        this.jwt = jwt;
        return this;
    }

    public List<DiVpProof> getDiVp() {
        return diVp;
    }

    public Proofs setDiVp(List<DiVpProof> diVp) {
        this.diVp = diVp;
        return this;
    }

    public List<String> getAttestation() {
        return attestation;
    }

    public Proofs setAttestation(List<String> attestation) {
        this.attestation = attestation;
        return this;
    }

    /**
     * Determines the proof type based on which field is populated.
     * Checks JWT first, then Attestation.
     *
     * @return the proof type string (ProofType.JWT or ProofType.ATTESTATION), or null if no proof type is found
     */
    @JsonIgnore
    public String getProofType() {
        if (jwt != null && !jwt.isEmpty()) {
            return ProofType.JWT;
        } else if (attestation != null && !attestation.isEmpty()) {
            return ProofType.ATTESTATION;
        }
        return null;
    }

    /**
     * Sets the proof value based on the proof type.
     * Sets the appropriate field (JWT or Attestation) depending on the proof type.
     *
     * @param proofType the proof type (ProofType.JWT or ProofType.ATTESTATION)
     * @param proof     the proof value to set
     * @return this instance for method chaining
     */
    public Proofs setProofByType(String proofType, String proof) {
        if (ProofType.JWT.equals(proofType)) {
            setJwt(List.of(proof));
        } else if (ProofType.ATTESTATION.equals(proofType)) {
            setAttestation(List.of(proof));
        }
        return this;
    }

    /**
     * Returns all proof values as a list.
     * Checks JWT proofs first, then Attestation proofs.
     * Returns an empty list if no proofs are present.
     *
     * @return a list containing all proof values, or an empty list if no proofs are present
     */
    @JsonIgnore
    public List<String> getAllProofs() {
        List<String> allProofs = new ArrayList<>();
        if (jwt != null && !jwt.isEmpty()) {
            allProofs.addAll(jwt);
        } else if (attestation != null && !attestation.isEmpty()) {
            allProofs.addAll(attestation);
        }
        return allProofs;
    }

    /**
     * Returns a list of proof types that are present (non-null and non-empty).
     * This can be used to iterate over proof types that need validation.
     *
     * @return a list of proof type strings (ProofType.JWT, ProofType.ATTESTATION, etc.) that are present
     */
    @JsonIgnore
    public List<String> getPresentProofTypes() {
        List<String> presentTypes = new ArrayList<>();
        if (jwt != null && !jwt.isEmpty()) {
            presentTypes.add(ProofType.JWT);
        }
        if (attestation != null && !attestation.isEmpty()) {
            presentTypes.add(ProofType.ATTESTATION);
        }
        return presentTypes;
    }
} 
