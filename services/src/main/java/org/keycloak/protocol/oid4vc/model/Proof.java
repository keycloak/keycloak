package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proof {

    @JsonProperty("proof_type")
    private ProofType proofType;

    private String jwt;

    public ProofType getProofType() {
        return proofType;
    }

    public Proof setProofType(ProofType proofType) {
        this.proofType = proofType;
        return this;
    }

    public String getJwt() {
        return jwt;
    }

    public Proof setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }
}