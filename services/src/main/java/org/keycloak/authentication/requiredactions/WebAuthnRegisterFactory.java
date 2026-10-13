/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.truststore.TruststoreProvider;

import com.webauthn4j.verifier.attestation.trustworthiness.certpath.CertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.DefaultCertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.NullCertPathTrustworthinessVerifier;

public class WebAuthnRegisterFactory implements RequiredActionFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "webauthn-register";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return createProvider(session, buildTrustVerifier(session));
    }

    /**
     * Builds a {@link CertPathTrustworthinessVerifier} backed by a {@link KeycloakTrustAnchorRepository}, which
     * looks up trust anchors from the truststore on demand rather than scanning it up front - so building this
     * verifier here, on every login, is cheap: the truststore itself is only actually read once an actual
     * WebAuthn registration is being verified.
     */
    private static CertPathTrustworthinessVerifier buildTrustVerifier(KeycloakSession session) {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            return new NullCertPathTrustworthinessVerifier();
        }
        return new DefaultCertPathTrustworthinessVerifier(new KeycloakTrustAnchorRepository(session));
    }

    protected WebAuthnRegister createProvider(KeycloakSession session, CertPathTrustworthinessVerifier trustVerifier) {
         return new WebAuthnRegister(session, trustVerifier);
    }

    @Override
    public void init(Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Webauthn Register";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
    }
}
