package org.keycloak.protocol.oid4vc.model;

import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payloads for JWT pre-authorized codes for OpenID4VCI.
 * They embed a partial, public view of the credential offer state.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtPreAuthCode extends JsonWebToken {

    @JsonProperty("context")
    private PreAuthCodeCtx context;

    @JsonProperty("salt")
    private String salt;

    public PreAuthCodeCtx getContext() {
        return context;
    }

    public JwtPreAuthCode context(PreAuthCodeCtx context) {
        this.context = context;
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
