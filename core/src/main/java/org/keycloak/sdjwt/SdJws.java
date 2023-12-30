package org.keycloak.sdjwt;

import java.io.IOException;
import java.util.Objects;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handle jws, either the issuer jwt or the holder key binding jwt.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class SdJws {
    private final JWSInput jwsInput;
    private final JsonNode payload;

    public String toJws() {
        if (jwsInput == null) {
            throw new IllegalStateException("JWS not yet signed");
        }
        return jwsInput.getWireString();
    }

    public JsonNode getPayload() {
        return payload;
    }

    public String getJwsString() {
        return jwsInput.getWireString();
    }

    // Constructor for unsigned JWS
    protected SdJws(JsonNode payload) {
        this.payload = payload;
        this.jwsInput = null;
    }

    // Constructor from jws string with all parts
    protected SdJws(String jwsString) {
        this.jwsInput = parse(jwsString);
        this.payload = readPayload(jwsInput);
    }

    // Constructor for signed JWS
    protected SdJws(JsonNode payload, JWSInput jwsInput) {
        this.payload = payload;
        this.jwsInput = jwsInput;
    }

    protected SdJws(JsonNode payload, SignatureSignerContext signer) {
        this.payload = payload;
        this.jwsInput = sign(payload, signer);
    }

    protected static JWSInput sign(JsonNode payload, SignatureSignerContext signer) {
        String jwsString = new JWSBuilder().type("vc+sd-jwt").jsonContent(payload).sign(signer);
        return parse(jwsString);
    }

    public void verifySignature(SignatureVerifierContext verifier) throws VerificationException {
        Objects.requireNonNull(verifier, "verifier must not be null");
        try {
            if (!verifier.verify(jwsInput.getEncodedSignatureInput().getBytes("UTF-8"), jwsInput.getSignature())) {
                throw new VerificationException("Invalid jws signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }

    private static final JWSInput parse(String jwsString) {
        try {
            return new JWSInput(Objects.requireNonNull(jwsString, "jwsString must not be null"));
        } catch (JWSInputException e) {
            throw new RuntimeException(e);
        }
    }

    private static final JsonNode readPayload(JWSInput jwsInput) {
        try {
            return SdJwtUtils.mapper.readTree(jwsInput.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
