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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.OID4VCConstants;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.util.JsonSerialization;

/**
 * The verifier side state of one wallet login, written to the single use object store when the login
 * page is rendered and read while serving the request object and the presentation. It carries the ids
 * to recover the authentication session, the nonce the wallet must echo in the key binding JWT, and
 * (for {@code direct_post.jwt}) the ephemeral response encryption key.
 */
public record RequestContext(String rootSessionId, String tabId, String nonce,
        EphemeralKey encryptionKey) {

    private static final String KEY_NONCE = "nonce";
    private static final String KEY_ENC_KID = "encKid";
    private static final String KEY_ENC_PRIVATE_KEY = "encPrivateKey";
    private static final String KEY_ENC_PUBLIC_JWK = "encPublicJwk";

    public boolean isEncrypted() {
        return encryptionKey != null;
    }

    // The client_metadata advertised in the request object, including the ephemeral encryption key for
    // an encrypted response.
    public Map<String, Object> clientMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(OID4VCConstants.VP_FORMATS_SUPPORTED, Map.of(
                OID4VCConstants.FORMAT_SD_JWT_VC, Map.of(
                        OID4VCConstants.SD_JWT_ALG_VALUES, OID4VPIdentityProvider.ACCEPTED_ALGORITHMS,
                        OID4VCConstants.KB_JWT_ALG_VALUES, OID4VPIdentityProvider.ACCEPTED_ALGORITHMS)));
        if (isEncrypted()) {
            metadata.put(OID4VCConstants.JWKS,
                    Map.of(OID4VCConstants.JWKS_KEYS, List.of(encryptionKey.publicJwk())));
            metadata.put(OID4VCConstants.ENCRYPTED_RESPONSE_ENC_VALUES_SUPPORTED,
                    ResponseEncryption.CONTENT_ENCRYPTION_ALGS);
        }
        return metadata;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put(OID4VPIdentityProvider.KEY_ROOT_SESSION_ID, rootSessionId);
        map.put(OID4VPIdentityProvider.KEY_TAB_ID, tabId);
        map.put(KEY_NONCE, nonce);
        if (encryptionKey != null) {
            try {
                map.put(KEY_ENC_KID, encryptionKey.kid());
                map.put(KEY_ENC_PRIVATE_KEY, encryptionKey.privateKeyPkcs8Base64());
                map.put(KEY_ENC_PUBLIC_JWK, JsonSerialization.writeValueAsString(encryptionKey.publicJwk()));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to serialize the ephemeral response encryption key", e);
            }
        }
        return map;
    }

    public static RequestContext fromMap(Map<String, String> map) {
        EphemeralKey encryptionKey = null;
        if (map.containsKey(KEY_ENC_KID)) {
            try {
                JWK publicJwk = JsonSerialization.readValue(map.get(KEY_ENC_PUBLIC_JWK), JWK.class);
                encryptionKey = new EphemeralKey(map.get(KEY_ENC_KID), publicJwk,
                        map.get(KEY_ENC_PRIVATE_KEY));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to restore the ephemeral response encryption key", e);
            }
        }
        return new RequestContext(
                map.get(OID4VPIdentityProvider.KEY_ROOT_SESSION_ID),
                map.get(OID4VPIdentityProvider.KEY_TAB_ID),
                map.get(KEY_NONCE),
                encryptionKey);
    }
}
