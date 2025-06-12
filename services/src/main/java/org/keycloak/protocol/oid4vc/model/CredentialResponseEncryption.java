package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.keycloak.jose.jwk.JWK;

/**
 * Represents the credential_response_encryption object
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request}
 *
 * @author <a href="mailto:Bertrand.Ogen@adorsys.com">Bertrand Ogen</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponseEncryption {
    private String alg;
    private String enc;
    private JWK jwk;

    public String getAlg() {
        return alg;
    }

    public CredentialResponseEncryption setAlg(String alg) {
        this.alg = alg;
        return this;
    }

    public String getEnc() {
        return enc;
    }

    public CredentialResponseEncryption setEnc(String enc) {
        this.enc = enc;
        return this;
    }

    public JWK getJwk() {
        return jwk;
    }

    public CredentialResponseEncryption setJwk(JWK jwk) {
        this.jwk = jwk;
        return this;
    }
}
