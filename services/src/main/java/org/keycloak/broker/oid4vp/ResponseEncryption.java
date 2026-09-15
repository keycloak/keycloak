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

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.List;

import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;

/**
 * Response encryption for the OID4VP {@code direct_post.jwt} response mode, following HAIP. The
 * verifier hands the wallet a fresh {@link EphemeralKey} with every authorization request
 * (ephemeral {@link JWEConstants#ECDH_ES} over secp256r1) and decrypts the wallet's JWE with it.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-8.3">OID4VP 1.0 §8.3 — Encrypted Responses</a>
 */
public final class ResponseEncryption {

    public static final String KEY_MANAGEMENT_ALG = JWEConstants.ECDH_ES;
    // HAIP section 5 requires the verifier to advertise both, the wallet picks one.
    public static final List<String> CONTENT_ENCRYPTION_ALGS =
            List.of(JWEConstants.A128GCM, JWEConstants.A256GCM);

    // The JWE parser raises unchecked exceptions on malformed input, wrapped here so a rejected
    // response stays a client error.
    public static ParsedResponse parse(String compactJwe) throws JWEException {
        try {
            JWE jwe = new JWE(compactJwe);
            if (jwe.getHeader() instanceof JWEHeader header) {
                return new ParsedResponse(jwe, header);
            }
        } catch (RuntimeException e) {
            throw new JWEException("Malformed encrypted response");
        }
        throw new JWEException("Encrypted response is not a JWE");
    }

    public static String decrypt(ParsedResponse parsed, PrivateKey privateKey) throws JWEException {
        String enc = parsed.header().getEncryptionAlgorithm();
        if (!KEY_MANAGEMENT_ALG.equals(parsed.header().getAlgorithm()) || enc == null
                || !CONTENT_ENCRYPTION_ALGS.contains(enc)) {
            throw new JWEException("Unsupported response encryption algorithms "
                    + parsed.header().getAlgorithm() + "/" + enc);
        }
        parsed.jwe().getKeyStorage().setDecryptionKey(privateKey);
        parsed.jwe().verifyAndDecodeJwe();
        return new String(parsed.jwe().getContent(), StandardCharsets.UTF_8);
    }

    private ResponseEncryption() {
    }
}
