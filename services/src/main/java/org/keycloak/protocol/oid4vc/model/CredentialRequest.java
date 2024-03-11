package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialRequest {

    private Format format;

    @JsonProperty("credential_identifier")
    private String credentialIdentifier;

    private Proof proof;

    public Format getFormat() {
        return format;
    }

    public CredentialRequest setFormat(Format format) {
        this.format = format;
        return this;
    }

    public String getCredentialIdentifier() {
        return credentialIdentifier;
    }

    public CredentialRequest setCredentialIdentifier(String credentialIdentifier) {
        this.credentialIdentifier = credentialIdentifier;
        return this;
    }

    public Proof getProof() {
        return proof;
    }

    public CredentialRequest setProof(Proof proof) {
        this.proof = proof;
        return this;
    }
}