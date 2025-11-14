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

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.sdjwt.SdJws;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 *
 */
public class KeyBindingJWT extends SdJws<KeyBindingPayload> {

    private KeyBindingJWT(KeyBindingPayload payload, SignatureSignerContext signer) {
        super(payload, signer, OID4VCConstants.KB_JWT_TYP);
    }

    public static KeyBindingJWT of(String jwsString) {
        return new KeyBindingJWT(jwsString);
    }

    public static KeyBindingJWT from(KeyBindingPayload payload, SignatureSignerContext signer) {
        return new KeyBindingJWT(payload, signer);
    }

    private KeyBindingJWT(String jwsString) {
        super(jwsString);
    }

    // No need to support this for now.
    @Override
    protected String readClaim(KeyBindingPayload payload, String claimName) throws VerificationException {
        throw new UnsupportedOperationException("Unsupported to retrieve '" + claimName + "' from KeyBindingJWT");
    }

    @Override
    protected KeyBindingPayload readPayload(JWSInput jwsInput) {
        try {
            return JsonSerialization.readValue(jwsInput.getContent(), KeyBindingPayload.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
