/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    protected SdJws(JsonNode payload, SignatureSignerContext signer, String jwsType) {
        this.payload = payload;
        this.jwsInput = sign(payload, signer, jwsType);
    }

    protected static JWSInput sign(JsonNode payload, SignatureSignerContext signer, String jwsType) {
        String jwsString = new JWSBuilder().type(jwsType).jsonContent(payload).sign(signer);
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
