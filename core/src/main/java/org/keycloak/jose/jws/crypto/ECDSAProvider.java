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

package org.keycloak.jose.jws.crypto;


import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.ECDSASignatureVerifierContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;

/**
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class ECDSAProvider implements SignatureProvider {

    public static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case ES256:
                return "SHA256withECDSA";
            case ES384:
                return "SHA384withECDSA";
            case ES512:
                return "SHA512withECDSA";
            default:
                throw new IllegalArgumentException("Not a supported ECDSA Algorithm: " + alg);
        }
    }

    public static boolean verify(JWSInput jws, PublicKey publicKey) {
        String alg = jws.getHeader().getAlgorithm().name();
        try {
            KeyWrapper kw = new KeyWrapper();
            kw.setPublicKey(publicKey);
            kw.setUse(KeyUse.SIG);
            kw.setType("EC");
            kw.setAlgorithm(alg);
            byte[] data = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
            byte[] signature = jws.getSignature();
            return new ECDSASignatureVerifierContext(kw).verify(data, signature);
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public boolean verify(JWSInput input, String cert) {
        return verifyViaCertificate(input, cert);
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private static boolean verifyViaCertificate(JWSInput input, String cert) {
        X509Certificate certificate;
        try {
            certificate = PemUtils.decodeCertificate(cert);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return verify(input, certificate.getPublicKey());
    }
}
