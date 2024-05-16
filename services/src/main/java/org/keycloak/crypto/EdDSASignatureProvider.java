/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class EdDSASignatureProvider implements SignatureProvider {

    private final KeycloakSession session;

    public EdDSASignatureProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SignatureSignerContext signer() throws SignatureException {
        return new ServerEdDSASignatureSignerContext(session, Algorithm.EdDSA);
    }

    @Override
    public SignatureSignerContext signer(KeyWrapper key) throws SignatureException {
        SignatureProvider.checkKeyForSignature(key, Algorithm.EdDSA, KeyType.OKP);
        return new ServerEdDSASignatureSignerContext(key);
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return new ServerEdDSASignatureVerifierContext(session, kid, Algorithm.EdDSA);
    }

    @Override
    public SignatureVerifierContext verifier(KeyWrapper key) throws VerificationException {
        SignatureProvider.checkKeyForVerification(key, Algorithm.EdDSA, KeyType.OKP);
        return new ServerEdDSASignatureVerifierContext(key);
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }

}
