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

    public static final String TYP = "kb+jwt";

    public KeyBindingJWT(JsonNode payload, SignatureSignerContext signer, String jwsType) {
        super(payload, signer, jwsType);
    }

    public static KeyBindingJWT of(String jwsString) {
        return new KeyBindingJWT(jwsString);
    }

    public static KeyBindingJWT from(JsonNode payload, SignatureSignerContext signer, String jwsType) {
        return new KeyBindingJWT(payload, signer, jwsType);
    }

    private KeyBindingJWT(JsonNode payload, JWSInput jwsInput) {
        super(payload, jwsInput);
    }

    private KeyBindingJWT(String jwsString) {
        super(jwsString);
    }
}
