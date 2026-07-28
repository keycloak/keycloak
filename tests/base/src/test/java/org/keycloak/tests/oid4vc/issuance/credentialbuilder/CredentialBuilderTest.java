/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oid4vc.issuance.credentialbuilder;

import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.tests.oid4vc.issuance.signing.OID4VCTest;

public abstract class CredentialBuilderTest extends OID4VCTest {

    private static final KeyWrapper KEY_WRAPPER = createRsaKey();

    protected static SignatureSignerContext exampleSigner() {
        return new AsymmetricSignatureSignerContext(KEY_WRAPPER);
    }

    protected static SignatureVerifierContext exampleVerifier() {
        return new AsymmetricSignatureVerifierContext(KEY_WRAPPER);
    }

    private static KeyWrapper createRsaKey() {
        try {
            var kpg = java.security.KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var kp = kpg.generateKeyPair();
            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(kp.getPrivate());
            kw.setPublicKey(kp.getPublic());
            kw.setKid(java.util.UUID.randomUUID().toString());
            kw.setType("RSA");
            kw.setAlgorithm("RS256");
            return kw;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
