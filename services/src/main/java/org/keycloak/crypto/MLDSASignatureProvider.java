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

package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

public class MLDSASignatureProvider implements SignatureProvider {

    private final KeycloakSession session;
    private final String algorithm;

    public MLDSASignatureProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureSignerContext signer() throws SignatureException {
        return new ServerAsymmetricSignatureSignerContext(session, algorithm) {
            @Override
            public byte[] sign(byte[] data) throws SignatureException {
                try {
                    java.security.Signature signature;
                    try {
                        signature = java.security.Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(this.key.getAlgorithmOrDefault(), this.key.getCurve()));
                    } catch (java.security.NoSuchAlgorithmException e) {
                        signature = org.keycloak.common.crypto.CryptoIntegration.getProvider().getSignature(this.key.getAlgorithmOrDefault());
                    }
                    signature.initSign((java.security.PrivateKey) this.key.getPrivateKey());
                    signature.update(data);
                    return signature.sign();
                } catch (Exception e) {
                    throw new SignatureException("Signing failed", e);
                }
            }
        };
    }

    @Override
    public SignatureSignerContext signer(KeyWrapper key) throws SignatureException {
        SignatureProvider.checkKeyForSignature(key, algorithm, KeyType.AKP);
        return new ServerAsymmetricSignatureSignerContext(key) {
            @Override
            public byte[] sign(byte[] data) throws SignatureException {
                try {
                    java.security.Signature signature;
                    try {
                        signature = java.security.Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(this.key.getAlgorithmOrDefault(), this.key.getCurve()));
                    } catch (java.security.NoSuchAlgorithmException e) {
                        signature = org.keycloak.common.crypto.CryptoIntegration.getProvider().getSignature(this.key.getAlgorithmOrDefault());
                    }
                    signature.initSign((java.security.PrivateKey) this.key.getPrivateKey());
                    signature.update(data);
                    return signature.sign();
                } catch (Exception e) {
                    throw new SignatureException("Signing failed", e);
                }
            }
        };
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return new ServerAsymmetricSignatureVerifierContext(session, kid, algorithm);
    }

    @Override
    public SignatureVerifierContext verifier(KeyWrapper key) throws VerificationException {
        SignatureProvider.checkKeyForVerification(key, algorithm, KeyType.AKP);
        return new ServerAsymmetricSignatureVerifierContext(key);
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }
}
