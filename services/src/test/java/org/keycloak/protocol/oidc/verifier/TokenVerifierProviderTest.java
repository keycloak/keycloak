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

import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.protocol.oidc.verifier.TokenVerificationContext.VerificationMode;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider.TokenVerifierProviderContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenVerifierProviderTest extends AbstractUtilSessionTest {

    @AfterEach
    public void resetProfileAndProof() {
        Profile.defaults();
        session.removeAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE);
    }

    @Test
    public void legacyProviderContextPreservesValidationOnly() {
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(new AccessToken());
        TokenVerifierProviderContext legacy = new TokenVerifierProviderContext(verifier, session, null, null);

        new TokenVerifierProviderManager().additionalAccessTokenVerifications(legacy);
        assertSame(verifier, legacy.accessTokenVerifier());
        assertSame(session, legacy.session());
        assertEquals(context(VerificationMode.VALIDATION_ONLY), legacy.verificationContext());
    }

    @ParameterizedTest
    @EnumSource(value = VerificationMode.class, names = {"RESOURCE_HEADER", "RESOURCE_PARAMETER", "TOKEN_EXCHANGE"})
    public void disabledDPoPRejectsBothTypedAndConfirmationBoundTokens(VerificationMode mode) {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("", "dpop"));
        DPoPTokenVerifierProvider provider = new DPoPTokenVerifierProvider();
        TokenVerificationContext context = context(mode);
        for (String type : new String[] {TokenUtil.TOKEN_TYPE_DPOP, TokenUtil.TOKEN_TYPE_BEARER}) {
            AccessToken token = boundToken(type);
            TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
            provider.additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, context));
            assertThrows(VerificationException.class, verifier::verify);
            assertThrows(VerificationException.class, () -> provider.verifyTokenBinding(token, context));
        }
        AccessToken bearer = new AccessToken();
        bearer.type(TokenUtil.TOKEN_TYPE_BEARER);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(bearer);
        provider.additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, context));
        assertDoesNotThrow(verifier::verify);
    }

    @ParameterizedTest
    @EnumSource(value = VerificationMode.class, names = {"RESOURCE_HEADER", "RESOURCE_PARAMETER", "TOKEN_EXCHANGE"})
    public void dPoPTypeRequiresConfirmation(VerificationMode mode) {
        AccessToken token = new AccessToken();
        token.type(TokenUtil.TOKEN_TYPE_DPOP);
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(token);
        new DPoPTokenVerifierProvider().additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, context(mode)));
        assertThrows(VerificationException.class, verifier::verify);
    }

    @Test
    public void validationOnlyPreservesCallerTokenChecks() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("", "dpop"));
        TokenVerifier<AccessToken> verifier = TokenVerifier.createWithoutSignature(boundToken(TokenUtil.TOKEN_TYPE_DPOP));
        TokenVerificationContext verification = context(VerificationMode.VALIDATION_ONLY);
        new DPoPTokenVerifierProvider().additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, verification));
        assertDoesNotThrow(verifier::verify);
    }

    @Test
    public void exchangeMatchesTheValidatedRequestProofWithoutResourceHash() throws Exception {
        DPoPTokenVerifierProvider provider = new DPoPTokenVerifierProvider();
        AccessToken token = boundToken(TokenUtil.TOKEN_TYPE_BEARER);
        TokenVerificationContext context = context(VerificationMode.TOKEN_EXCHANGE);
        assertThrows(VerificationException.class, () -> provider.verifyTokenBinding(token, context));
        DPoP proof = new DPoP();
        proof.setThumbprint("different-key");
        session.setAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, proof);
        assertThrows(VerificationException.class, () -> provider.verifyTokenBinding(token, context));
        proof.setThumbprint("key-thumbprint");
        provider.verifyTokenBinding(token, context);
    }

    @Test
    public void confirmationCannotBePresentedAsBearer() {
        TokenVerificationContext context = new TokenVerificationContext(session, null, null, null,
                "encoded-token", TokenUtil.TOKEN_TYPE_BEARER, VerificationMode.RESOURCE_HEADER);
        assertThrows(TokenBindingException.class,
                () -> new DPoPTokenVerifierProvider().verifyTokenBinding(boundToken(TokenUtil.TOKEN_TYPE_BEARER), context));
    }

    private TokenVerificationContext context(VerificationMode mode) {
        return new TokenVerificationContext(session, null, null, null, null, null, mode);
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
