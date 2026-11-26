/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.grants;


import org.keycloak.authentication.authenticators.client.ClientAssertionState;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import org.jboss.logging.Logger;

/**
 * the assertion validator for Identity Assertion JWT Authorization Grant (ID-JAG).
 * Identity Assertion JWT is a new type of JWT that can be used as an authorization grant per RFC 7523.
 *  
 * https://datatracker.ietf.org/doc/draft-ietf-oauth-identity-assertion-authz-grant/
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */
public class IDJWTAuthorizationGrantValidator extends DefaultJWTAuthorizationGrantValidator {

    private static final Logger logger = Logger.getLogger(IDJWTAuthorizationGrantValidator.class);

    public static IDJWTAuthorizationGrantValidator createValidator(KeycloakSession session, String scope, ClientAssertionState clientAssertionState) {
        return new IDJWTAuthorizationGrantValidator(session, scope, clientAssertionState);
    }

    private IDJWTAuthorizationGrantValidator(KeycloakSession session, String scope, ClientAssertionState clientAssertionState) {
        super(session, scope, clientAssertionState);
    }

    public void validateClient() {
        super.validateClient();

        JsonWebToken accessToken = clientAssertionState.getToken();
        String clientIdInToken = (String) accessToken.getOtherClaims().get("client_id");
        String clientIdInRequestHeaderOrBody = session.getContext().getClient().getClientId();
        if (clientIdInToken == null || !clientIdInRequestHeaderOrBody.equals(clientIdInToken)) {
                logger.warn("client id in assertion : " + clientIdInToken + " and client id in request header/body : " + clientIdInRequestHeaderOrBody);
                failureCallback("client id in assertion : " + clientIdInToken + " and client id in request header/body : " + clientIdInRequestHeaderOrBody);
                return;
        }
    }

    public boolean validateTokenActive(int allowedClockSkew, int maxExp, boolean reusePermitted) {

        JsonWebToken accessToken = clientAssertionState.getToken();
        if (accessToken.getIat() == null) {
            failureCallback("Token iat claim is required");
            return false;
        }

        if (reusePermitted) {
            logger.warn("Token reuse is not permitted. Token reuse permitted setting is ignored.");            
        }

        return super.validateTokenActive(allowedClockSkew, maxExp, false);

    }

}
