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

package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;

import java.util.List;
import javax.security.cert.X509Certificate;
/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BearerTokenRequestAuthenticator {
    protected Logger log = Logger.getLogger(BearerTokenRequestAuthenticator.class);
    protected String tokenString;
    protected AccessToken token;
    protected String surrogate;
    protected AuthChallenge challenge;
    protected KeycloakDeployment deployment;

    public BearerTokenRequestAuthenticator(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public String getTokenString() {
        return tokenString;
    }

    public AccessToken getToken() {
        return token;
    }

    public String getSurrogate() {
        return surrogate;
    }

    public AuthOutcome authenticate(HttpFacade exchange)  {
        List<String> authHeaders = exchange.getRequest().getHeaders("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.NO_BEARER_TOKEN, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split.length != 2) continue;
            if (split[0].equalsIgnoreCase("Bearer")) {
                tokenString = split[1];

                log.debugf("Found [%d] values in authorization header, selecting the first value for Bearer.", (Integer) authHeaders.size());
                break;
            }
        }

        if (tokenString == null) {
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.NO_BEARER_TOKEN, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        return (authenticateToken(exchange, tokenString));
    }
    
    protected AuthOutcome authenticateToken(HttpFacade exchange, String tokenString) {
        log.debug("Verifying access_token");
        if (log.isTraceEnabled()) {
            try {
                JWSInput jwsInput = new JWSInput(tokenString);
                String wireString = jwsInput.getWireString();
                log.tracef("\taccess_token: %s", wireString.substring(0, wireString.lastIndexOf(".")) + ".signature");
            } catch (JWSInputException e) {
                log.errorf(e, "Failed to parse access_token: %s", tokenString);
            }
        }
        try {
            token = AdapterTokenVerifier.verifyToken(tokenString, deployment);
        } catch (VerificationException e) {
            log.debug("Failed to verify token");
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.INVALID_TOKEN, "invalid_token", e.getMessage());
            return AuthOutcome.FAILED;
        }
        if (token.getIssuedAt() < deployment.getNotBefore()) {
            log.debug("Stale token");
            challenge = challengeResponse(exchange,  OIDCAuthenticationError.Reason.STALE_TOKEN, "invalid_token", "Stale token");
            return AuthOutcome.FAILED;
        }
        boolean verifyCaller = false;
        if (deployment.isUseResourceRoleMappings()) {
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        surrogate = null;
        if (verifyCaller) {
            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().isEmpty()) {
                log.warn("No trusted certificates in token");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }

            // for now, we just make sure Undertow did two-way SSL
            // assume JBoss Web verifies the client cert
            X509Certificate[] chain = new X509Certificate[0];
            try {
                chain = exchange.getCertificateChain();
            } catch (Exception ignore) {

            }
            if (chain == null || chain.length == 0) {
                log.warn("No certificates provided by undertow to verify the caller");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }
            surrogate = chain[0].getSubjectDN().getName();
        }
        log.debug("successful authorized");
        return AuthOutcome.AUTHENTICATED;
    }

    protected AuthChallenge clientCertChallenge() {
        return new AuthChallenge() {
            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public boolean challenge(HttpFacade exchange) {
                // do the same thing as client cert auth
                return false;
            }
        };
    }


    protected AuthChallenge challengeResponse(HttpFacade facade, final OIDCAuthenticationError.Reason reason, final String error, final String description) {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(deployment.getRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        final String challenge = header.toString();
        return new AuthChallenge() {
            @Override
            public int getResponseCode() {
                return 401;
            }

            @Override
            public boolean challenge(HttpFacade facade) {
                if (deployment.getPolicyEnforcer() != null) {
                    deployment.getPolicyEnforcer().enforce(OIDCHttpFacade.class.cast(facade));
                    return true;
                }
                OIDCAuthenticationError error = new OIDCAuthenticationError(reason, description);
                facade.getRequest().setError(error);
                facade.getResponse().addHeader("WWW-Authenticate", challenge);
                if(deployment.isDelegateBearerErrorResponseSending()){
                    facade.getResponse().setStatus(401);
                }
                else {
                    facade.getResponse().sendError(401);
                }
                return true;
            }
        };
    }
}
