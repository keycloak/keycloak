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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.JsonSerialization;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {

    private final KeycloakSession session;
    private final TokenManager tokenManager;
    private final RealmModel realm;
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
    }

    public Response introspect(String token) {
        try {
            AccessToken accessToken = verifyAccessToken(token);
            accessToken = transformAccessToken(accessToken);
            ObjectNode tokenMetadata;

            if (accessToken != null) {
                tokenMetadata = JsonSerialization.createObjectNode(accessToken);
                tokenMetadata.put("client_id", accessToken.getIssuedFor());

                String scope = accessToken.getScope();
                if (scope != null && scope.trim().isEmpty()) {
                    tokenMetadata.remove("scope");
                }

                if (!tokenMetadata.has("username")) {
                    if (accessToken.getPreferredUsername() != null) {
                        tokenMetadata.put("username", accessToken.getPreferredUsername());
                    } else {
                        UserModel userModel = session.users().getUserById(realm, accessToken.getSubject());
                        if (userModel != null) {
                            tokenMetadata.put("username", userModel.getUsername());
                        }
                    }
                }

                String sessionState = accessToken.getSessionState();

                if (sessionState != null) {
                    UserSessionModel userSession = session.sessions().getUserSession(realm, sessionState);

                    if (userSession != null) {
                        String actor = userSession.getNote(ImpersonationSessionNote.IMPERSONATOR_USERNAME.toString());

                        if (actor != null) {
                            // for token exchange delegation semantics when an entity (actor) other than the subject is the acting party to whom authority has been delegated
                            tokenMetadata.putObject("act").put("sub", actor);
                        }
                    }
                }

                tokenMetadata.put(OAuth2Constants.TOKEN_TYPE, accessToken.getType());

            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
            }

            tokenMetadata.put("active", accessToken != null);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    private AccessToken transformAccessToken(AccessToken token) {
        if (token == null) {
            return null;
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection())
                .event(EventType.INTROSPECT_TOKEN)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
        UserSessionModel userSession;
        try {
            userSession = UserSessionUtil.findValidSession(session, realm, token, event, client);
        } catch (Exception e) {
            logger.debugf("Can not get user session: %s", e.getMessage());
            // Backwards compatibility
            return token;
        }
        if (userSession.getUser() == null) {
            logger.debugf("User not found");
            // Backwards compatibility
            return token;
        }
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, token.getScope(), session);
        AccessToken smallToken = getAccessTokenFromStoredData(token, userSession);
        return tokenManager.transformIntrospectionAccessToken(session, smallToken, userSession, clientSessionCtx);
    }

    private AccessToken getAccessTokenFromStoredData(AccessToken token, UserSessionModel userSession) {
        // Copy just "basic" claims from the initial token. The same like filled in TokenManager.initToken. The rest should be possibly added by protocol mappers (only if configured for introspection response)
        AccessToken newToken = new AccessToken();
        newToken.id(token.getId());
        newToken.type(token.getType());
        newToken.subject(token.getSubject() != null ? token.getSubject() : userSession.getUser().getId());
        newToken.iat(token.getIat());
        newToken.exp(token.getExp());
        newToken.issuedFor(token.getIssuedFor());
        newToken.issuer(token.getIssuer());
        newToken.setNonce(token.getNonce());
        newToken.setScope(token.getScope());
        newToken.setAuth_time(token.getAuth_time());
        newToken.setSessionState(token.getSessionState());

        // In the case of a refresh token, aud is a basic claim.
        newToken.audience(token.getAudience());

        // The cnf is not a claim controlled by the protocol mapper.
        newToken.setConfirmation(token.getConfirmation());
        return newToken;
    }

    protected AccessToken verifyAccessToken(String token) {
        AccessToken accessToken;

        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            accessToken = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.debugf("JWT check failed: %s", e.getMessage());
            return null;
        }

        RealmModel realm = this.session.getContext().getRealm();

        return tokenManager.checkTokenValidForIntrospection(session, realm, accessToken, false) ? accessToken : null;
    }

    @Override
    public void close() {

    }
}
