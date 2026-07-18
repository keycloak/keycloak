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

package org.keycloak.protocol.oidc.grants.ciba;

import java.lang.reflect.Proxy;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.KeycloakSession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CibaClientValidationTest {

    @Test
    public void unavailableSignatureProviderIsUnsupported() {
        KeycloakSession session = sessionWith(null);

        assertFalse(CibaClientValidation.isSupportedBackchannelAuthenticationRequestSigningAlg(
                session, Algorithm.ML_DSA_44));
    }

    @Test
    public void onlyAsymmetricSignatureProvidersAreSupported() {
        KeycloakSession session = sessionWith(signatureProvider(false));

        assertFalse(CibaClientValidation.isSupportedBackchannelAuthenticationRequestSigningAlg(
                session, Algorithm.ML_DSA_44));

        session = sessionWith(signatureProvider(true));
        assertTrue(CibaClientValidation.isSupportedBackchannelAuthenticationRequestSigningAlg(
                session, Algorithm.ML_DSA_44));
    }

    @Test
    public void noneDoesNotRequireSignatureProvider() {
        assertTrue(CibaClientValidation.isSupportedBackchannelAuthenticationRequestSigningAlg(
                sessionWith(null), org.keycloak.jose.jws.Algorithm.none.getName()));
    }

    private static KeycloakSession sessionWith(SignatureProvider provider) {
        return (KeycloakSession) Proxy.newProxyInstance(KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> "getProvider".equals(method.getName()) ? provider : null);
    }

    private static SignatureProvider signatureProvider(boolean asymmetric) {
        return new SignatureProvider() {
            @Override
            public SignatureSignerContext signer() {
                return null;
            }

            @Override
            public SignatureSignerContext signer(KeyWrapper key) {
                return null;
            }

            @Override
            public SignatureVerifierContext verifier(String kid) {
                return null;
            }

            @Override
            public SignatureVerifierContext verifier(KeyWrapper key) {
                return null;
            }

            @Override
            public boolean isAsymmetricAlgorithm() {
                return asymmetric;
            }
        };
    }
}
