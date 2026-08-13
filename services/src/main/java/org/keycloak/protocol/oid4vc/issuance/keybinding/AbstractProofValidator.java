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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jwk.OKPPublicJWK;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractProofValidator implements ProofValidator {

    protected final KeycloakSession keycloakSession;

    protected AbstractProofValidator(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    protected SignatureVerifierContext getVerifier(JWK jwk, String jwsAlgorithm) throws VerificationException {
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, jwsAlgorithm);
        KeyWrapper keyWrapper = getKeyWrapper(jwk, jwsAlgorithm);
        keyWrapper.setUse(KeyUse.SIG);
        return signatureProvider.verifier(keyWrapper);
    }

    private KeyWrapper getKeyWrapper(JWK jwk, String algorithm) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setType(jwk.getKeyType());

        // Use the algorithm provided by the caller, and not the one inside the jwk (if any)
        // As jws validation will also check that one against the value "none"
        keyWrapper.setAlgorithm(algorithm);

        // Set the curve if any
        if (jwk.getOtherClaim(OKPPublicJWK.CRV, String.class) != null) {
            keyWrapper.setCurve(jwk.getOtherClaim(OKPPublicJWK.CRV, String.class));
        }

        JWKParser parser = JWKParser.create(jwk);
        keyWrapper.setPublicKey(parser.toPublicKey());
        return keyWrapper;
    }
}
