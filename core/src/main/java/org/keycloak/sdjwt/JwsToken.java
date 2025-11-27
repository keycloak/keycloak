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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handle jws, either the issuer jwt or the holder key binding jwt.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 *
 */
public abstract class JwsToken {

    protected JWSHeader jwsHeader;

    protected ObjectNode payload;

    protected String jws;

    protected JWSInput jwsInput;

    protected JwsToken(String jws) {
        parse(jws);
    }

    protected JwsToken(JWSHeader jwsHeader, ObjectNode payload) {
        this.jwsHeader = jwsHeader;
        this.payload = payload;
    }

    protected JwsToken(JWSHeader jwsHeader, ObjectNode payload, SignatureSignerContext signerContext) {
        this.jwsHeader = jwsHeader;
        this.payload = payload;
        this.jws = sign(signerContext);
    }

    public String sign(SignatureSignerContext signerContext) {
        jws = new JWSBuilder().header(jwsHeader).jsonContent(payload).sign(signerContext);
        try {
            jwsInput = new JWSInput(jws);
            jwsHeader = jwsInput.getHeader();
            payload = jwsInput.readJsonContent(ObjectNode.class);
        } catch (JWSInputException e) {
            throw new IllegalStateException(String.format("Got invalid JWS '%s'", jws), e);
        }
        return jws;
    }

    public void verifySignature(SignatureVerifierContext verifier) throws VerificationException {
        Objects.requireNonNull(verifier, "verifier must not be null");
        try {
            if (!verifier.verify(jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                                 jwsInput.getSignature())) {
                throw new VerificationException("Invalid jws signature");
            }
        } catch (Exception e) {
            throw new VerificationException(e);
        }
    }

    public Optional<String> getSdHashAlgorithm() {
        return Optional.ofNullable(payload.get(OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM))
                       .map(JsonNode::textValue);
    }

    public String getJws() {
        return jws;
    }

    public void setJws(String jws) {
        this.jws = jws;
    }

    public JWSInput getJwsInput() {
        return jwsInput;
    }

    public void setJwsInput(JWSInput jwsInput) {
        this.jwsInput = jwsInput;
        if (jwsInput != null) {
            setJwsHeader(jwsInput.getHeader());
            try {
                setPayload(jwsInput.readJsonContent(ObjectNode.class));
            } catch (JWSInputException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public JWSHeader getJwsHeader() {
        return jwsHeader;
    }

    public ObjectNode getJwsHeaderAsNode() {
        return JsonSerialization.mapper.convertValue(jwsHeader, ObjectNode.class);
    }

    public void setJwsHeader(JWSHeader jwsHeader) {
        this.jwsHeader = jwsHeader;
    }

    public ObjectNode getPayload() {
        return payload;
    }

    public void setPayload(ObjectNode payload) {
        this.payload = payload;
    }

    private void parse(String jwsString) {
        try {
            this.jws = jwsString;
            this.jwsInput = new JWSInput(Objects.requireNonNull(jwsString, "jwsString must not be null"));
            this.jwsHeader = jwsInput.getHeader();
            this.payload = JsonSerialization.mapper.readValue(jwsInput.getContent(), ObjectNode.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
