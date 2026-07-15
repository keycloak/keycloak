/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.oid4vp;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;

import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;

/**
 * The per request response encryption key for the {@code direct_post.jwt} response mode. The public
 * JWK is advertised to the wallet in the request object client metadata, the private key stays with
 * the verifier to decrypt the response. Generation takes no algorithm because HAIP pins the key
 * management to {@link JWEConstants#ECDH_ES} over secp256r1. The advertised content encryption
 * algorithms only select the length of the content key derived during key agreement, so the one key
 * serves them all.
 */
public record EphemeralKey(String kid, JWK publicJwk, String privateKeyPkcs8Base64) {

    private static final String CURVE_SEC = "secp256r1";

    public static EphemeralKey generate(String kid) {
        KeyPair keyPair = KeyUtils.generateEcKeyPair(CURVE_SEC);
        JWK publicJwk = JWKBuilder.create()
                .algorithm(ResponseEncryption.KEY_MANAGEMENT_ALG)
                .ec(keyPair.getPublic(), KeyUse.ENC);
        publicJwk.setKeyId(kid);
        String encoded = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        return new EphemeralKey(kid, publicJwk, encoded);
    }

    // The private key is PKCS8 encoded so it can live in the single use object store.
    public PrivateKey privateKey() {
        try {
            return DerUtils.decodePrivateKey(Base64.getDecoder().decode(privateKeyPkcs8Base64));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to restore the ephemeral response encryption key", e);
        }
    }
}
