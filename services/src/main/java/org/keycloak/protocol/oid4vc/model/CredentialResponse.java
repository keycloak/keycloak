package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponse {

    private Format format;

    // concrete type depends on the format
    private Object credential;

    @JsonProperty("acceptance_token")
    private String acceptanceToken;

    @JsonProperty("c_nonce")
    private String cNonce;

    @JsonProperty("c_nonce_expires_in")
    private String cNonceExpiresIn;

    public Format getFormat() {
        return format;
    }

    public CredentialResponse setFormat(Format format) {
        this.format = format;
        return this;
    }

    public Object getCredential() {
        return credential;
    }

    public CredentialResponse setCredential(Object credential) {
        this.credential = credential;
        return this;
    }

    public String getAcceptanceToken() {
        return acceptanceToken;
    }

    public CredentialResponse setAcceptanceToken(String acceptanceToken) {
        this.acceptanceToken = acceptanceToken;
        return this;
    }

    public String getcNonce() {
        return cNonce;
    }

    public CredentialResponse setcNonce(String cNonce) {
        this.cNonce = cNonce;
        return this;
    }

    public String getcNonceExpiresIn() {
        return cNonceExpiresIn;
    }

    public CredentialResponse setcNonceExpiresIn(String cNonceExpiresIn) {
        this.cNonceExpiresIn = cNonceExpiresIn;
        return this;
    }
}