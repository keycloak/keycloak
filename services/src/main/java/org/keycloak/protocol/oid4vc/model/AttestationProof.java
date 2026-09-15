package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deprecated: Represents a single attestation-based proof (historical 'proof' structure).
 * Prefer using {@link Proofs} with the appropriate array field (e.g., attestation).
 * This class is kept for backward compatibility only.
 * Supports 'attestation' proof type as per OID4VCI Draft 15.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-ID2.html#name-credential-request">OID4VCI Credential Request</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated
public class AttestationProof {

    @JsonProperty("attestation")
    private String attestation;

    @JsonProperty("proof_type")
    private String proofType;

    public AttestationProof() {
    }

    public AttestationProof(String attestation, String proofType) {
        this.attestation = attestation;
        this.proofType = proofType;
    }

    public String getAttestation() {
        return attestation;
    }

    public AttestationProof setAttestation(String attestation) {
        this.attestation = attestation;
        return this;
    }

    public String getProofType() {
        return proofType;
    }

    public AttestationProof setProofType(String proofType) {
        this.proofType = proofType;
        return this;
    }
}
