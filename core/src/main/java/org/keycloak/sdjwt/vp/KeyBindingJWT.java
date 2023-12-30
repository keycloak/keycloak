package org.keycloak.sdjwt.vp;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.sdjwt.SdJws;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class KeyBindingJWT extends SdJws {

    public static KeyBindingJWT of(String jwsString) {
        return new KeyBindingJWT(jwsString);
    }

    public static KeyBindingJWT from(JsonNode payload, SignatureSignerContext signer) {
        JWSInput jwsInput = sign(payload, signer);
        return new KeyBindingJWT(payload, jwsInput);
    }

    private KeyBindingJWT(JsonNode payload, JWSInput jwsInput) {
        super(payload, jwsInput);
    }

    private KeyBindingJWT(String jwsString) {
        super(jwsString);
    }
}
