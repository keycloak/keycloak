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

import java.security.PrivateKey;
import java.security.PublicKey;

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
        return signer(ServerAsymmetricSignatureSignerContext.getKey(session, algorithm));
    }

    @Override
    public SignatureSignerContext signer(KeyWrapper key) throws SignatureException {
        SignatureProvider.checkKeyForSignature(key, algorithm, KeyType.AKP);
        if (!(key.getPrivateKey() instanceof PrivateKey) || !algorithm.equals(key.getPrivateKey().getAlgorithm())) {
            throw new SignatureException("Invalid private key for " + algorithm);
        }
        return new ServerAsymmetricSignatureSignerContext(key);
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return verifier(ServerAsymmetricSignatureVerifierContext.getKey(session, kid, algorithm));
    }

    @Override
    public SignatureVerifierContext verifier(KeyWrapper key) throws VerificationException {
        SignatureProvider.checkKeyForVerification(key, algorithm, KeyType.AKP);
        if (!(key.getPublicKey() instanceof PublicKey) || !algorithm.equals(key.getPublicKey().getAlgorithm())) {
            throw new VerificationException("Invalid public key for " + algorithm);
        }
        return new ServerAsymmetricSignatureVerifierContext(key);
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }
}
