/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;

public class AsymmetricSignatureVerifierContext implements SignatureVerifierContext {

    private final KeyWrapper key;

    public AsymmetricSignatureVerifierContext(KeyWrapper key) {
        this.key = key;
    }

    @Override
    public String getKid() {
        return key.getKid();
    }

    @Override
    public String getAlgorithm() {
        return key.getAlgorithmOrDefault();
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) throws VerificationException {
        try {
            Signature verifier = getSignature();
            verifier.initVerify((PublicKey) key.getPublicKey());
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception e) {
            throw new VerificationException("Signing failed", e);
        }
    }

    private Signature getSignature()
            throws NoSuchAlgorithmException, NoSuchProviderException {
        try {
            return Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(key.getAlgorithmOrDefault(), key.getCurve()));
        } catch (NoSuchAlgorithmException e) {
            // Retry using the current crypto provider's override implementation
            return CryptoIntegration.getProvider().getSignature(key.getAlgorithmOrDefault());
        }
    }
}
