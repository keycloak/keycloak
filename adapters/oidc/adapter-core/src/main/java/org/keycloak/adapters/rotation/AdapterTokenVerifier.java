/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.rotation;

import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import java.security.PublicKey;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterTokenVerifier {

    private static final Logger log = Logger.getLogger(AdapterTokenVerifier.class);


    /**
     * Verifies bearer token. Typically called when bearer token (access token) is sent to the service, which wants to verify it. Hence it also checks the audience in the token.
     *
     * @param tokenString
     * @param deployment
     * @return
     * @throws VerificationException
     */
    public static AccessToken verifyToken(String tokenString, KeycloakDeployment deployment) throws VerificationException {
        TokenVerifier<AccessToken> tokenVerifier = createVerifier(tokenString, deployment, true, AccessToken.class);

        // Verify audience of bearer-token
        if (deployment.isVerifyTokenAudience()) {
            tokenVerifier.audience(deployment.getResourceName());
        }

        return tokenVerifier.verify().getToken();
    }


    /**
     * Verify access token and ID token. Typically called after successful tokenResponse is received from Keycloak
     *
     * @param accessTokenString
     * @param idTokenString
     * @param deployment
     * @return verified and parsed accessToken and idToken
     * @throws VerificationException
     */
    public static VerifiedTokens verifyTokens(String accessTokenString, String idTokenString, KeycloakDeployment deployment) throws VerificationException {
        // Adapters currently do most of the checks including signature etc on the access token
        TokenVerifier<AccessToken> tokenVerifier = createVerifier(accessTokenString, deployment, true, AccessToken.class);
        AccessToken accessToken = tokenVerifier.verify().getToken();

        if (idTokenString != null) {
            // Don't verify signature again on IDToken
            IDToken idToken = TokenVerifier.create(idTokenString, IDToken.class).getToken();
            TokenVerifier<IDToken> idTokenVerifier = TokenVerifier.createWithoutSignature(idToken);

            // Always verify audience and azp on IDToken
            idTokenVerifier.audience(deployment.getResourceName());
            idTokenVerifier.issuedFor(deployment.getResourceName());

            idTokenVerifier.verify();
            return new VerifiedTokens(accessToken, idToken);
        } else {
            return new VerifiedTokens(accessToken, null);
        }
    }


    /**
     * Creates verifier, initializes it from the KeycloakDeployment and adds the publicKey and some default basic checks (activeness and tokenType). Useful if caller wants to add/remove/update
     * some checks
     *
     * @param tokenString
     * @param deployment
     * @param withDefaultChecks
     * @param tokenClass
     * @param <T>
     * @return tokenVerifier
     * @throws VerificationException
     */
    public static <T extends JsonWebToken> TokenVerifier<T> createVerifier(String tokenString, KeycloakDeployment deployment, boolean withDefaultChecks, Class<T> tokenClass) throws VerificationException {
        TokenVerifier<T> tokenVerifier = TokenVerifier.create(tokenString, tokenClass);

        if (withDefaultChecks) {
            tokenVerifier
                    .withDefaultChecks()
                    .realmUrl(deployment.getRealmInfoUrl());
        }

        String kid = tokenVerifier.getHeader().getKeyId();
        PublicKey publicKey = getPublicKey(kid, deployment);
        tokenVerifier.publicKey(publicKey);

        return tokenVerifier;
    }


    private static PublicKey getPublicKey(String kid, KeycloakDeployment deployment) throws VerificationException {
        PublicKeyLocator pkLocator = deployment.getPublicKeyLocator();

        PublicKey publicKey = pkLocator.getPublicKey(kid, deployment);
        if (publicKey == null) {
            log.errorf("Didn't find publicKey for kid: %s", kid);
            throw new VerificationException("Didn't find publicKey for specified kid");
        }

        return publicKey;
    }


    public static class VerifiedTokens {

        private final AccessToken accessToken;
        private final IDToken idToken;

        public VerifiedTokens(AccessToken accessToken, IDToken idToken) {
            this.accessToken = accessToken;
            this.idToken = idToken;
        }


        public AccessToken getAccessToken() {
            return accessToken;
        }

        public IDToken getIdToken() {
            return idToken;
        }
    }
}
