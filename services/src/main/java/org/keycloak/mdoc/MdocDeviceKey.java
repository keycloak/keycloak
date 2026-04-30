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
package org.keycloak.mdoc;

import java.util.Map;
import java.util.Objects;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.util.JsonSerialization;

import com.authlete.cose.COSEException;
import com.authlete.cose.COSEKey;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Holder binding key for an ISO mdoc Mobile Security Object.
 *
 * Wallet proofs arrive as JWT proofs containing a JWK, but mDoc DeviceKeyInfo stores the holder public key as a
 * COSE_Key.
 */
final class MdocDeviceKey {

    private final COSEKey coseKey;

    private MdocDeviceKey(COSEKey coseKey) {
        this.coseKey = Objects.requireNonNull(coseKey, "coseKey");
    }

    static MdocDeviceKey fromProofJwk(JWK jwk) {
        Objects.requireNonNull(jwk, "jwk");

        try {
            Map<String, Object> jwkMap = JsonSerialization.mapper.convertValue(jwk, new TypeReference<>() {
            });

            return new MdocDeviceKey(COSEKey.fromJwk(jwkMap));
        } catch (COSEException e) {
            throw new MdocException("Could not convert proof key to COSE_Key", e);
        }
    }

    COSEKey toAuthleteCoseKey() {
        return coseKey;
    }
}
