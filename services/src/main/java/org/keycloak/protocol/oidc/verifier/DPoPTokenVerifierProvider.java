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

import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.TokenUtil;

public class DPoPTokenVerifierProvider implements TokenVerifierProvider {

    private static final String RESOURCE_PROOF = DPoPTokenVerifierProvider.class.getName() + ".resourceProof";

    public static DPoP getVerifiedResourceProof(KeycloakSession session) {
        return session.getAttribute(RESOURCE_PROOF, DPoP.class);
    }

    @Override
    public void additionalAccessTokenVerifications(TokenVerifierProviderContext context) {
        // Endpoints such as introspection define their own token rules.
    }

    @Override
    public void addAccessTokenClaimChecks(TokenVerifierProviderContext context) {
        boolean enabled = Profile.isFeatureEnabled(Profile.Feature.DPOP);
        context.accessTokenVerifier().withChecks(token -> {
            if (!enabled && hasKeyThumbprint(token)) {
                throw new TokenBindingException(Errors.INVALID_DPOP_PROOF, "DPoP is disabled");
            }
            if (TokenUtil.TOKEN_TYPE_DPOP.equalsIgnoreCase(token.getType()) && !hasKeyThumbprint(token)) {
                throw new TokenBindingException(Errors.INVALID_DPOP_PROOF, "No DPoP key thumbprint in access token");
            }
            return true;
        });
    }

    @Override
    public void verifyTokenBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
        boolean boundToken = isBound(token);
        if (!isEnabledFor(boundToken)) {
            return;
        }
        try {
            verifyResourceBinding(token, context, boundToken);
        } catch (VerificationException ex) {
            throw new TokenBindingException(Errors.INVALID_DPOP_PROOF, ex);
        }
    }

    @Override
    public void verifyTokenExchangeBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
        boolean boundToken = isBound(token);
        if (!isEnabledFor(boundToken) || !boundToken) {
            return;
        }
        try {
            DPoP proof = context.session().getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, DPoP.class);
            if (proof == null) {
                throw new VerificationException("DPoP proof is missing");
            }
            DPoPUtil.validateBinding(token, proof);
        } catch (VerificationException ex) {
            throw new TokenBindingException(Errors.INVALID_DPOP_PROOF, ex);
        }
    }

    private static boolean isBound(AccessToken token) {
        return hasKeyThumbprint(token) || TokenUtil.TOKEN_TYPE_DPOP.equalsIgnoreCase(token.getType());
    }

    private static boolean isEnabledFor(boolean boundToken) throws TokenBindingException {
        boolean enabled = Profile.isFeatureEnabled(Profile.Feature.DPOP);
        if (!enabled && boundToken) {
            throw new TokenBindingException(Errors.INVALID_DPOP_PROOF, "DPoP is disabled");
        }
        return enabled;
    }

    private void verifyResourceBinding(AccessToken token, TokenVerificationContext context, boolean boundToken) throws VerificationException {
        if (context.authorizationScheme() != null) {
            boolean dPoPScheme = TokenUtil.TOKEN_TYPE_DPOP.equalsIgnoreCase(context.authorizationScheme());
            if (boundToken && !dPoPScheme) {
                throw new VerificationException("A DPoP-bound access token requires the DPoP authorization scheme");
            }
            if (!boundToken && dPoPScheme) {
                throw new VerificationException("The DPoP authorization scheme requires a DPoP-bound access token");
            }
        }

        ClientModel client = context.realm().getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new VerificationException("Client not found");
        }
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseDPoP() && !boundToken) {
            throw new VerificationException("This client requires a DPoP-bound access token");
        }
        DPoP proof = null;
        if (boundToken) {
            if (context.tokenString() == null) {
                throw new VerificationException("Access Token not set for validator");
            }
            proof = new DPoPUtil.Validator(context.session()).request(context.request()).uriInfo(context.uriInfo())
                    .accessToken(context.tokenString()).validate();
            DPoPUtil.validateBinding(token, proof);
        }
        if (context.authorizationScheme() != null) {
            context.session().setAttribute(RESOURCE_PROOF, proof);
        }
    }

    private static boolean hasKeyThumbprint(AccessToken token) {
        return token.getConfirmation() != null && token.getConfirmation().getKeyThumbprint() != null;
    }
}
