/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RefreshTokenIntrospectionProvider extends AccessTokenIntrospectionProvider<RefreshToken> {

    private static final Logger logger = Logger.getLogger(RefreshTokenIntrospectionProvider.class);

    public RefreshTokenIntrospectionProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected Class<RefreshToken> getTokenClass() {
        return RefreshToken.class;
    }

    @Override
    protected UserSessionUtil.UserSessionValidationResult verifyUserSession() {
        return UserSessionUtil.findValidSessionForRefreshToken(session, realm, token, client, (invalidUserSession -> {}));
    }

    @Override
    protected boolean verifyTokenReuse() {

        String tokenType = token.getType();
        if (realm.isRevokeRefreshToken()
                && (tokenType.equals(TokenUtil.TOKEN_TYPE_REFRESH) || tokenType.equals(TokenUtil.TOKEN_TYPE_OFFLINE))
                && !validateTokenReuse()) {
            logger.debugf("Introspection access token for %s client: failed to validate Token reuse for introspection", token.getIssuedFor());
            eventBuilder.detail(Details.REASON, "Realm revoke refresh token, token type is "+tokenType+ " and token is not eligible for introspection");
            eventBuilder.error(Errors.INVALID_TOKEN);
            return false;
        }

        return true;
    }

    private boolean validateTokenReuse() {
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());

        try {
            tokenManager.validateTokenReuse(session, realm, token, clientSession, false);
            return true;
        } catch (
                OAuthErrorException e) {
            logger.debug("validateTokenReuseForIntrospection is false", e);
            return false;
        }
    }
}
