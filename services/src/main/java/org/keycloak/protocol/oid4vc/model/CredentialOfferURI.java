package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URL;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialOfferURI {

    private String issuer;
    private String nonce;

    public String getIssuer() {
        return issuer;
    }

    public CredentialOfferURI setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public CredentialOfferURI setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
}