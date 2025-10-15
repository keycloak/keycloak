/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwe.alg.DirectAlgorithmProvider;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.AesCbcHmacShaEncryptionProvider;
import org.keycloak.jose.jwe.enc.AesGcmJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class JWERegistry {

    // https://tools.ietf.org/html/rfc7518#page-22
    // Registry not pluggable for now. Just supported algorithms included
    private static final Map<String, JWEEncryptionProvider> ENC_PROVIDERS = new HashMap<>();

    static {
        ENC_PROVIDERS.put(JWEConstants.A128GCM, new AesGcmJWEEncryptionProvider(JWEConstants.A128GCM));
        ENC_PROVIDERS.put(JWEConstants.A192GCM, new AesGcmJWEEncryptionProvider(JWEConstants.A192GCM));
        ENC_PROVIDERS.put(JWEConstants.A256GCM, new AesGcmJWEEncryptionProvider(JWEConstants.A256GCM));
        ENC_PROVIDERS.put(JWEConstants.A128CBC_HS256, new AesCbcHmacShaEncryptionProvider.Aes128CbcHmacSha256Provider());
        ENC_PROVIDERS.put(JWEConstants.A192CBC_HS384, new AesCbcHmacShaEncryptionProvider.Aes192CbcHmacSha384Provider());
        ENC_PROVIDERS.put(JWEConstants.A256CBC_HS512, new AesCbcHmacShaEncryptionProvider.Aes256CbcHmacSha512Provider());
    }


    static JWEAlgorithmProvider getAlgProvider(String alg) {
        // https://tools.ietf.org/html/rfc7518#page-12
        if (JWEConstants.DIRECT.equals(alg)) {
            return new DirectAlgorithmProvider();
        } else {
            return CryptoIntegration.getProvider().getAlgorithmProvider(JWEAlgorithmProvider.class, alg);
        }
    }


    static JWEEncryptionProvider getEncProvider(String enc) {
        return ENC_PROVIDERS.get(enc);
    }


}
