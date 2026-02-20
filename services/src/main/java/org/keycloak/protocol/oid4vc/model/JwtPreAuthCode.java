package org.keycloak.protocol.oid4vc.model;

import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;

/**
 * Payloads for JWT pre-authorized codes for OpenID4VCI.
 * They embed a partial, public view of the credential offer state.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtPreAuthCode extends JsonWebToken {

    @JsonProperty("credentialOfferState")
    private CredentialOfferState credentialOfferState;

    @JsonProperty("salt")
    private String salt;

    public CredentialOfferState getCredentialOfferState() {
        return credentialOfferState;
    }

    public JwtPreAuthCode credentialOfferState(CredentialOfferState credentialOfferState) {
        this.credentialOfferState = credentialOfferState;
        return this;
    }

    public String getSalt() {
        return salt;
    }

    public JwtPreAuthCode salt(String salt) {
        this.salt = salt;
        return this;
    }
}
