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
package org.keycloak.sdjwt.vp;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import org.keycloak.OID4VCConstants;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.IDToken;
import org.keycloak.sdjwt.JwsToken;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 *
 */
public class KeyBindingJWT extends JwsToken {

    public KeyBindingJWT(String jwsString) {
        super(jwsString);
    }

    protected KeyBindingJWT(JWSHeader jwsHeader, ObjectNode payload, SignatureSignerContext signer) {
        super(jwsHeader, payload);
        getJwsHeader().setType(OID4VCConstants.KEYBINDING_JWT_TYP);
        Optional.ofNullable(signer).ifPresent(this::sign);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        protected JWSHeader jwsHeader;

        protected ObjectNode payload;

        private SignatureSignerContext signerContext;

        public Builder() {
            this.jwsHeader = new JWSHeader();
            this.payload = JsonNodeFactory.instance.objectNode();
        }

        private JWSHeader getJwsHeader() {
            if (jwsHeader == null) {
                this.jwsHeader = new JWSHeader();
            }
            return jwsHeader;
        }

        private ObjectNode getPayload() {
            if (payload == null) {
                this.payload = JsonNodeFactory.instance.objectNode();
            }
            return payload;
        }

        public Builder withJwsHeader(JWSHeader jwsHeader) {
            this.jwsHeader = jwsHeader;
            return this;
        }

        public Builder withPayload(ObjectNode payload) {
            this.payload = payload;
            return this;
        }

        public Builder withIat(long iat)
        {
            getPayload().put(OID4VCConstants.CLAIM_NAME_IAT, iat);
            return this;
        }

        public Builder withNbf(long nbf)
        {
            getPayload().put(OID4VCConstants.CLAIM_NAME_NBF, nbf);
            return this;
        }

        public Builder withExp(long exp)
        {
            getPayload().put(OID4VCConstants.CLAIM_NAME_EXP, exp);
            return this;
        }

        public Builder withNonce(String nonce)
        {
            getPayload().put(IDToken.NONCE, nonce);
            return this;
        }

        public Builder withAudience(String aud)
        {
            getPayload().put(IDToken.AUD, aud);
            return this;
        }

        public Builder withKid(String kid)
        {
            getJwsHeader().setKeyId(kid);
            return this;
        }

        public Builder withX5c(List<String> x5c)
        {
            getJwsHeader().setX5c(x5c);
            return this;
        }

        public Builder withX5c(String x5c)
        {
            getJwsHeader().addX5c(x5c);
            return this;
        }

        public Builder withX5c(Certificate x5c)
        {
            getJwsHeader().addX5c(x5c);
            return this;
        }

        public Builder withSignerContext(SignatureSignerContext signatureSignerContext) {
            this.signerContext = signatureSignerContext;
            return this;
        }

        public KeyBindingJWT build() {
            return new KeyBindingJWT(jwsHeader, payload, signerContext);
        }
    }
}
