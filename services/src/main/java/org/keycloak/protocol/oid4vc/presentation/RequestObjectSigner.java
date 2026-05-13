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
package org.keycloak.protocol.oid4vc.presentation;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;

final class RequestObjectSigner {

    private final SignatureSignerContext signer;
    private final X509Certificate certificate;

    private RequestObjectSigner(SignatureSignerContext signer, X509Certificate certificate) {
        if (certificate == null) {
            throw new IllegalStateException("No verifier signing certificate available for OID4VP request object x5c header");
        }

        this.signer = signer;
        this.certificate = certificate;
    }

    static RequestObjectSigner fromConfig(OID4VPIdentityProviderConfig config) {
        KeyWrapper signingKey = parseSigningKey(config.getX509CertificatePem(), config.getX509PrivateKeyPem());
        try {
            return new RequestObjectSigner(
                    new ECDSASignatureSignerContext(signingKey),
                    signingKey.getCertificate());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create OID4VP request object signer", e);
        }
    }

    static X509Certificate parseCertificate(String pem) {
        if (pem == null || pem.isBlank()) {
            return null;
        }
        try {
            return PemUtils.decodeCertificate(pem);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse OID4VP X.509 certificate", e);
        }
    }

    X509Certificate getCertificate() {
        return certificate;
    }

    String sign(AuthorizationRequest authorizationRequest) {
        return new JWSBuilder()
                .type(OID4VPConstants.REQUEST_OBJECT_TYPE)
                .kid(signer.getKid())
                .x5c(List.of(certificate))
                .jsonContent(authorizationRequest)
                .sign(signer);
    }

    private static KeyWrapper parseSigningKey(String certificatePem, String privateKeyPem) {
        X509Certificate certificate = parseCertificate(certificatePem);
        if (certificate == null) {
            throw new IllegalArgumentException("OID4VP request object signing requires an X.509 certificate");
        }
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new IllegalArgumentException("OID4VP request object signing requires a private key");
        }

        try {
            if (!(certificate.getPublicKey() instanceof ECPublicKey ecPublicKey)) {
                throw new IllegalArgumentException("Only EC OID4VP request object signing keys are supported");
            }

            PrivateKey privateKey = decodePrivateKey(privateKeyPem);
            KeyWrapper keyWrapper = new KeyWrapper();
            keyWrapper.setKid(KeyUtils.createKeyId(ecPublicKey));
            keyWrapper.setUse(KeyUse.SIG);
            keyWrapper.setType(KeyType.EC);
            keyWrapper.setCurve("P-" + ecPublicKey.getParams().getCurve().getField().getFieldSize());
            keyWrapper.setAlgorithm(Algorithm.ES256);
            keyWrapper.setPublicKey(ecPublicKey);
            keyWrapper.setPrivateKey(privateKey);
            keyWrapper.setCertificate(certificate);
            return keyWrapper;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse OID4VP request object signing material", e);
        }
    }

    private static PrivateKey decodePrivateKey(String privateKeyPem) {
        try {
            return PemUtils.decodePrivateKey(privateKeyPem);
        } catch (Exception e) {
            try {
                return KeyFactory.getInstance("EC")
                        .generatePrivate(new PKCS8EncodedKeySpec(PemUtils.pemToDer(privateKeyPem)));
            } catch (Exception fallbackException) {
                e.addSuppressed(fallbackException);
                throw new IllegalArgumentException("Failed to parse OID4VP request object signing private key", e);
            }
        }
    }
}
