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
package org.keycloak.protocol.oidc.verifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider.TokenVerifierProviderContext;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class TokenVerifierProviderTest extends AbstractUtilSessionTest {

    private final TokenVerifierProviderManager manager = new TokenVerifierProviderManager();

    @AfterEach
    public void resetProfileAndProof() {
        Profile.defaults();
        session.removeAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE);
    }

    @Test
    public void legacyProviderContextPreservesValidationOnly() {
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(new AccessToken());
        TokenVerifierProviderContext legacy = new TokenVerifierProviderContext(verifier, session, null, null);

        manager.additionalAccessTokenVerifications(legacy);
        assertSame(verifier, legacy.accessTokenVerifier());
        assertSame(session, legacy.session());
        assertEquals(context(), legacy.verificationContext());
    }

    @Test
    public void disabledDPoPRejectsBothTypedAndConfirmationBoundTokens() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("", "dpop"));
        DPoPTokenVerifierProvider provider = new DPoPTokenVerifierProvider();
        TokenVerificationContext context = context();
        for (String type : new String[] {TokenUtil.TOKEN_TYPE_DPOP, TokenUtil.TOKEN_TYPE_BEARER}) {
            AccessToken token = boundToken(type);
            TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
            provider.addAccessTokenClaimChecks(new TokenVerifierProviderContext(verifier, context));
            assertThrows(VerificationException.class, verifier::verify);
            assertThrows(VerificationException.class, () -> provider.verifyTokenExchangeBinding(token, context));
            assertThrows(VerificationException.class, () -> provider.verifyTokenBinding(token, resourceContext()));
        }
        AccessToken bearer = new AccessToken();
        bearer.type(TokenUtil.TOKEN_TYPE_BEARER);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(bearer);
        provider.addAccessTokenClaimChecks(new TokenVerifierProviderContext(verifier, context));
        assertDoesNotThrow(verifier::verify);
    }

    @Test
    public void dPoPTypeRequiresConfirmation() {
        AccessToken token = new AccessToken();
        token.type(TokenUtil.TOKEN_TYPE_DPOP);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
        new DPoPTokenVerifierProvider().addAccessTokenClaimChecks(new TokenVerifierProviderContext(verifier, context()));
        assertThrows(VerificationException.class, verifier::verify);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void extensionCanRestrictAccessTokenTypesRegardlessOfProviderOrder(boolean extensionFirst) {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("dpop", ""));
        TokenVerifierProvider bearerOnly = checks -> checks.accessTokenVerifier().tokenType(List.of(TokenUtil.TOKEN_TYPE_BEARER));
        TokenVerifierProvider dpop = new DPoPTokenVerifierProvider();
        List<TokenVerifierProvider> providers = extensionFirst ? List.of(bearerOnly, dpop) : List.of(dpop, bearerOnly);
        TokenVerificationContext context = contextWithProviders(providers);

        AccessToken token = boundToken(TokenUtil.TOKEN_TYPE_DPOP);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
        manager.addAccessTokenChecks(verifier, context);
        assertThrows(VerificationException.class, verifier::verify);
        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        assertDoesNotThrow(verifier::verify);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void accessTokenChecksSetDefaultTypes(boolean enableDPoP) {
        Profile.configure(enableDPoP ? new CommaSeparatedListProfileConfigResolver("dpop", "")
                : new CommaSeparatedListProfileConfigResolver("", "dpop"));
        TokenVerificationContext context = contextWithProviders(List.of());
        AccessToken token = new AccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
        manager.addAccessTokenChecks(verifier, context);

        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        assertDoesNotThrow(verifier::verify);
        token.type(TokenUtil.TOKEN_TYPE_ID);
        assertThrows(VerificationException.class, verifier::verify);
        token.type(TokenUtil.TOKEN_TYPE_DPOP);
        if (enableDPoP) {
            assertDoesNotThrow(verifier::verify);
        } else {
            assertThrows(VerificationException.class, verifier::verify);
        }
    }

    @Test
    public void endpointChecksPreserveTheExpectedTokenType() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("", "dpop"));
        AccessToken token = boundToken(TokenUtil.TOKEN_TYPE_DPOP);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token).tokenType(List.of(TokenUtil.TOKEN_TYPE_DPOP));
        TokenVerificationContext verification = contextWithProviders(List.of(new DPoPTokenVerifierProvider()));
        manager.addEndpointChecks(verifier, verification);
        assertDoesNotThrow(verifier::verify);
        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        assertThrows(VerificationException.class, verifier::verify);
    }

    @Test
    public void managerAppliesCommonRestrictionsAndChecksBindingOnlyForResourceAccess() {
        TokenVerifierProvider audience = checks -> checks.accessTokenVerifier().audience("resource");
        KeycloakSession requestSession = contextWithProviders(List.of(audience, new DPoPTokenVerifierProvider())).session();
        TokenVerificationContext context = TokenVerificationContext.forResource(requestSession, null, null, null, "encoded-token", TokenUtil.TOKEN_TYPE_BEARER);
        AccessToken token = boundToken(TokenUtil.TOKEN_TYPE_BEARER);

        TokenVerifier<AccessToken> endpoint = TokenVerifier.createWithoutSignature(token);
        manager.addEndpointChecks(endpoint, context);
        TokenVerifier<AccessToken> accessToken = TokenVerifier.createWithoutSignature(token);
        manager.addAccessTokenChecks(accessToken, context);
        TokenVerifier<AccessToken> resource = TokenVerifier.createWithoutSignature(token);
        manager.addResourceAccessChecks(resource, context);

        assertThrows(VerificationException.class, endpoint::verify);
        assertThrows(VerificationException.class, accessToken::verify);
        assertThrowsExactly(VerificationException.class, resource::verify);

        token.audience("resource");
        assertDoesNotThrow(endpoint::verify);
        assertDoesNotThrow(accessToken::verify);
        assertThrows(TokenBindingException.class, resource::verify);
    }

    @Test
    public void exchangeMatchesTheValidatedRequestProofWithoutResourceHash() throws Exception {
        DPoPTokenVerifierProvider provider = new DPoPTokenVerifierProvider();
        AccessToken token = boundToken(TokenUtil.TOKEN_TYPE_BEARER);
        TokenVerificationContext context = context();
        assertThrows(VerificationException.class, () -> provider.verifyTokenExchangeBinding(token, context));
        DPoP proof = new DPoP();
        proof.setThumbprint("different-key");
        session.setAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, proof);
        assertThrows(VerificationException.class, () -> provider.verifyTokenExchangeBinding(token, context));
        proof.setThumbprint("key-thumbprint");
        provider.verifyTokenExchangeBinding(token, context);
    }

    @Test
    public void confirmationCannotBePresentedAsBearer() {
        TokenVerificationContext context = resourceContext();
        assertThrows(TokenBindingException.class,
                () -> new DPoPTokenVerifierProvider().verifyTokenBinding(boundToken(TokenUtil.TOKEN_TYPE_BEARER), context));
    }

    private TokenVerificationContext contextWithProviders(List<TokenVerifierProvider> providers) {
        KeycloakSession testSession = new ResteasyKeycloakSession(null) {
            @Override
            public <T extends Provider> Set<T> getAllProviders(Class<T> type) {
                Set<T> ordered = new LinkedHashSet<>();
                providers.forEach(provider -> ordered.add(type.cast(provider)));
                return ordered;
            }
        };
        return new TokenVerificationContext(testSession, null, null, null, null, null);
    }

    private TokenVerificationContext resourceContext() {
        return TokenVerificationContext.forResource(session, null, null, null, "encoded-token", TokenUtil.TOKEN_TYPE_BEARER);
    }

    private TokenVerificationContext context() {
        return new TokenVerificationContext(session, null, null, null, null, null);
    }

    private AccessToken boundToken(String type) {
        AccessToken token = new AccessToken();
        token.type(type);
        AccessToken.Confirmation confirmation = new AccessToken.Confirmation();
        confirmation.setKeyThumbprint("key-thumbprint");
        token.setConfirmation(confirmation);
        return token;
    }
}
