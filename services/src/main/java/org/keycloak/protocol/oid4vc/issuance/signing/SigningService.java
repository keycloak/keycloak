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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jwk.OKPPublicJWK;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.Format;

/**
 * Abstract base class to provide the Signing Services common functionality
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class SigningService<T> implements VerifiableCredentialsSigningService<T> {

    protected final KeycloakSession keycloakSession;
    protected final String keyId;

    // values of the type field are defined by the implementing service. Could f.e. the security suite for ldp_vc or the algorithm to be used for jwt_vc
    protected final String type;

    // As the type is not identical to the format, we use the format as a factory to
    // instantiate provider.
    protected final Format format;

    protected SigningService(KeycloakSession keycloakSession, String keyId, Format format, String type) {
        this.keycloakSession = keycloakSession;
        this.keyId = keyId;
        this.format = format;
        this.type = type;
    }

    @Override
    public String locator() {
        return format.name();
    }

    /**
     * Returns the key stored under kid, or the active key for the given jws algorithm,
     *
     * @param kid
     * @param algorithm
     * @return
     */
    protected KeyWrapper getKey(String kid, String algorithm) {
        // Allow the service to work with the active key if keyId is null
        // And we still have to figure out how to proceed with key rotation
        if(keyId==null){
            return keycloakSession.keys().getActiveKey(keycloakSession.getContext().getRealm(), KeyUse.SIG, algorithm);
        }
        return keycloakSession.keys().getKey(keycloakSession.getContext().getRealm(), kid, KeyUse.SIG, algorithm);
    }

    protected SignatureVerifierContext getVerifier(JWK jwk, String jwsAlgorithm) throws VerificationException {
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, jwsAlgorithm);
        return signatureProvider.verifier(getKeyWraper(jwk, jwsAlgorithm, KeyUse.SIG));
    }

    private KeyWrapper getKeyWraper(JWK jwk, String algorithm, KeyUse keyUse) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setType(jwk.getKeyType());

        // Use the algorithm provided by the caller, and not the one inside the jwk (if any)
        // As jws validation will also check that one against the value "none"
        keyWrapper.setAlgorithm(algorithm);

        // Set the curve if any
        if (jwk.getOtherClaims().get(OKPPublicJWK.CRV) != null) {
            keyWrapper.setCurve((String) jwk.getOtherClaims().get(OKPPublicJWK.CRV));
        }

        keyWrapper.setUse(KeyUse.SIG);
        JWKParser parser = JWKParser.create(jwk);
        keyWrapper.setPublicKey(parser.toPublicKey());
        return keyWrapper;
    }

    @Override
    public void close() {
        // no-op
    }
}
