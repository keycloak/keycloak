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

package org.keycloak.adapters.saml.profile;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import org.keycloak.adapters.saml.DefaultSamlDeployment;
import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests that the SAML adapter rejects assertions and responses whose issuer
 * does not match the configured IdP entity ID, even when the signature is
 * valid with a trusted key.
 */
public class SamlIssuerValidationTest {

    private static final String TRUSTED_ISSUER = "https://trusted-tenant.idp.example";
    private static final String ATTACKER_ISSUER = "https://attacker-tenant.idp.example";
    private static final String SP_ENTITY_ID = "https://sp.example/application";
    private static final String ACS = "https://sp.example/application/saml";

    @Test
    public void wrongAssertionIssuerIsRejected() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(ATTACKER_ISSUER, sharedKey);
        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Assertion from wrong issuer must be rejected",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved for wrong issuer",
                store.savedAccounts, is(equalTo(0)));
    }

    @Test
    public void correctIssuerIsAuthenticated() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(TRUSTED_ISSUER, sharedKey);
        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Assertion from correct issuer must be authenticated",
                outcome, is(equalTo(AuthOutcome.AUTHENTICATED)));
        assertThat("Session must be saved for correct issuer",
                store.savedAccounts, is(equalTo(1)));
    }

    @Test
    public void untrustedSigningKeyIsRejected() throws Exception {
        KeyPair trustedKey = rsaKeyPair();
        KeyPair untrustedKey = rsaKeyPair();

        // Sign with untrusted key, configure adapter with trusted key
        SAMLDocumentHolder holder = buildSignedResponse(TRUSTED_ISSUER, untrustedKey);
        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, trustedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Assertion signed with untrusted key must be rejected",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved for untrusted key",
                store.savedAccounts, is(equalTo(0)));
    }

    @Test
    public void wrongIssuerWithBothSignaturesValidIsRejected() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        // Build and sign both response and assertion
        SAML2LoginResponseBuilder responseBuilder = responseBuilder(ATTACKER_ISSUER);
        ResponseType response = responseBuilder.buildModel();
        Document responseDocument = responseBuilder.buildDocument(response);

        BaseSAML2BindingBuilder<?> signer = new BaseSAML2BindingBuilder<>();
        signer.signWith("shared-key", sharedKey)
              .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        signer.signAssertion(responseDocument);
        signer.signDocument(responseDocument);

        SAMLDocumentHolder holder = new SAMLDocumentHolder(response, responseDocument);
        RecordingSessionStore store = new RecordingSessionStore();
        DefaultSamlDeployment dep = deployment(TRUSTED_ISSUER, sharedKey, true);
        ExposedHandler handler = new ExposedHandler(dep, store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Wrong issuer must be rejected even with both signatures valid",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved",
                store.savedAccounts, is(equalTo(0)));
    }

    @Test
    public void nullResponseIssuerWithCorrectAssertionIssuerIsAccepted() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(TRUSTED_ISSUER, sharedKey);
        // Remove the Response-level issuer (it is optional per SAML 2.0 spec)
        ResponseType response = (ResponseType) holder.getSamlObject();
        response.setIssuer(null);

        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Null response issuer with correct assertion issuer must be accepted",
                outcome, is(equalTo(AuthOutcome.AUTHENTICATED)));
        assertThat("Session must be saved",
                store.savedAccounts, is(equalTo(1)));
    }

    @Test
    public void wrongAssertionIssuerWithNullResponseIssuerIsRejected() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(ATTACKER_ISSUER, sharedKey);
        ResponseType response = (ResponseType) holder.getSamlObject();
        // Remove the response-level issuer to bypass the response check
        // and exercise the assertion-level issuer check directly
        response.setIssuer(null);

        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Assertion from wrong issuer must be rejected even when response issuer is absent",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved",
                store.savedAccounts, is(equalTo(0)));
    }

    @Test
    public void wrongResponseIssuerWithCorrectAssertionIssuerIsRejected() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(TRUSTED_ISSUER, sharedKey);
        ResponseType response = (ResponseType) holder.getSamlObject();
        // Set the response-level issuer to the attacker while assertion issuer remains correct
        NameIDType wrongIssuer = new NameIDType();
        wrongIssuer.setValue(ATTACKER_ISSUER);
        response.setIssuer(wrongIssuer);

        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Wrong response issuer must be rejected even when assertion issuer is correct",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved",
                store.savedAccounts, is(equalTo(0)));
    }

    @Test
    public void nullAssertionIssuerIsRejected() throws Exception {
        KeyPair sharedKey = rsaKeyPair();

        SAMLDocumentHolder holder = buildSignedResponse(TRUSTED_ISSUER, sharedKey);
        ResponseType response = (ResponseType) holder.getSamlObject();
        // Remove both issuers to test the null assertion issuer path
        response.setIssuer(null);
        response.getAssertions().get(0).getAssertion().setIssuer(null);

        RecordingSessionStore store = new RecordingSessionStore();
        ExposedHandler handler = new ExposedHandler(deployment(TRUSTED_ISSUER, sharedKey), store);

        AuthOutcome outcome = handler.login(holder);

        assertThat("Assertion with null issuer must be rejected",
                outcome, is(equalTo(AuthOutcome.FAILED)));
        assertThat("No session must be saved",
                store.savedAccounts, is(equalTo(0)));
    }

    // ---- helpers ----

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private static SAML2LoginResponseBuilder responseBuilder(String issuer) {
        return new SAML2LoginResponseBuilder()
                .destination(ACS)
                .issuer(issuer)
                .requestIssuer(SP_ENTITY_ID)
                .requestID("test-request-id")
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(), "testuser")
                .assertionExpiration(300)
                .subjectExpiration(300)
                .sessionExpiration(300);
    }

    private static SAMLDocumentHolder buildSignedResponse(String issuer, KeyPair signingKey) throws Exception {
        SAML2LoginResponseBuilder responseBuilder = responseBuilder(issuer);
        ResponseType response = responseBuilder.buildModel();
        Document responseDocument = responseBuilder.buildDocument(response);

        BaseSAML2BindingBuilder<?> signer = new BaseSAML2BindingBuilder<>();
        signer.signWith("signing-key", signingKey)
              .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        signer.signAssertion(responseDocument);

        return new SAMLDocumentHolder(response, responseDocument);
    }

    private static DefaultSamlDeployment deployment(String trustedIssuer, KeyPair signingKey) {
        return deployment(trustedIssuer, signingKey, false);
    }

    private static DefaultSamlDeployment deployment(String trustedIssuer, KeyPair signingKey,
            boolean validateResponseSignature) {
        DefaultSamlDeployment.DefaultSingleSignOnService sso =
                new DefaultSamlDeployment.DefaultSingleSignOnService();
        sso.setAssertionConsumerServiceUrl(URI.create(ACS));
        sso.setValidateAssertionSignature(true);
        sso.setValidateResponseSignature(validateResponseSignature);

        DefaultSamlDeployment.DefaultIDP idp = new DefaultSamlDeployment.DefaultIDP();
        idp.setEntityID(trustedIssuer);
        idp.setSingleSignOnService(sso);
        idp.addSignatureValidationKey(signingKey.getPublic());
        idp.refreshKeyLocatorConfiguration();

        DefaultSamlDeployment deployment = new DefaultSamlDeployment();
        deployment.setEntityID(SP_ENTITY_ID);
        deployment.setIdp(idp);
        return deployment;
    }

    // ---- test infrastructure ----

    private static final class ExposedHandler extends AbstractSamlAuthenticationHandler {

        private ExposedHandler(DefaultSamlDeployment deployment, SamlSessionStore store) {
            super(null, deployment, store);
        }

        private AuthOutcome login(SAMLDocumentHolder holder) {
            return handleLoginResponse(holder, true, ignored -> { });
        }

        @Override
        public AuthOutcome handle(OnSessionCreated onCreateSession) {
            return AuthOutcome.NOT_ATTEMPTED;
        }

        @Override
        protected AuthOutcome logoutRequest(LogoutRequestType request, String relayState) {
            return AuthOutcome.NOT_ATTEMPTED;
        }
    }

    private static final class RecordingSessionStore implements SamlSessionStore {

        private int savedAccounts;

        @Override
        public void saveAccount(SamlSession account) {
            savedAccounts++;
        }

        @Override
        public boolean isLoggingIn() {
            return false;
        }

        @Override
        public void setCurrentAction(CurrentAction action) {
        }

        @Override
        public boolean isLoggingOut() {
            return false;
        }

        @Override
        public boolean isLoggedIn() {
            return false;
        }

        @Override
        public SamlSession getAccount() {
            return null;
        }

        @Override
        public String getRedirectUri() {
            return null;
        }

        @Override
        public void logoutAccount() {
        }

        @Override
        public void logoutByPrincipal(String principal) {
        }

        @Override
        public void logoutBySsoId(List<String> ssoIds) {
        }

        @Override
        public void saveRequest() {
        }

        @Override
        public boolean restoreRequest() {
            return false;
        }
    }
}
