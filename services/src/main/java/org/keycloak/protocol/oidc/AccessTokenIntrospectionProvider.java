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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.tracing.TracingAttributes;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider<T extends AccessToken> implements TokenIntrospectionProvider {

    protected final KeycloakSession session;
    protected final TokenManager tokenManager;
    protected final RealmModel realm;
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);
    protected EventBuilder eventBuilder;

    // Those are set after successfully verified
    protected T token;
    protected ClientModel client;
    protected UserSessionModel userSession;
    protected UserModel user;

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
    }

    @Override
    public Response introspect(String tokenStr, EventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
        AccessToken accessToken = null;
        try {
            ClientModel authenticatedClient = session.getContext().getClient();

            ObjectNode tokenMetadata;
            if (introspectionChecks(tokenStr)) {
                accessToken = transformAccessToken(this.token, userSession);

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
                        UserModel userModel = userSession.getUser();
                        if (userModel != null) {
                            tokenMetadata.put("username", userModel.getUsername());
                            eventBuilder.user(userModel);
                        }
                    }
                }

                String actor = userSession.getNote(ImpersonationSessionNote.IMPERSONATOR_USERNAME.toString());
                if (actor != null) {
                    // for token exchange delegation semantics when an entity (actor) other than the subject is the acting party to whom authority has been delegated
                    tokenMetadata.putObject("act").put("sub", actor);
                }

                tokenMetadata.put(OAuth2Constants.TOKEN_TYPE, accessToken.getType());
                tokenMetadata.put("active", true);
                eventBuilder.success();
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
                logger.debug("Keycloak token introspection return false");
                tokenMetadata.put("active", false);
            }

            // if consumer requests application/jwt return a JWT representation of the introspection contents in an jwt field
            if (accessToken != null) {
                boolean isJwtRequest = org.keycloak.utils.MediaType.APPLICATION_JWT.equals(session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.ACCEPT));
                if (isJwtRequest && Boolean.parseBoolean(authenticatedClient.getAttribute(Constants.SUPPORT_JWT_CLAIM_IN_INTROSPECTION_RESPONSE_ENABLED))) {
                    // consumers can use this to convert an opaque token into an JWT based token
                    tokenMetadata.put("jwt", session.tokens().encode(accessToken));
                }
            }

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            String clientId = accessToken != null ? accessToken.getIssuedFor() : "unknown";
            logger.debugf(e, "Exception during Keycloak introspection for %s client in realm %s", clientId, realm.getName());
            eventBuilder.detail(Details.REASON, e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }


    public AccessToken transformAccessToken(AccessToken token, UserSessionModel userSession) {
        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if(clientSession == null) {
            return token;
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, token.getScope(), session);
        AccessToken smallToken = getAccessTokenFromStoredData(token);
        return tokenManager.transformIntrospectionAccessToken(session, smallToken, userSession, clientSessionCtx);
    }

    private AccessToken getAccessTokenFromStoredData(AccessToken token) {
        // Copy just "basic" claims from the initial token. The same like filled in TokenManager.initToken. The rest should be possibly added by protocol mappers (only if configured for introspection response)
        AccessToken newToken = new AccessToken();
        newToken.id(token.getId());
        newToken.type(token.getType());
        newToken.subject(token.getSubject());
        newToken.iat(token.getIat());
        newToken.exp(token.getExp());
        newToken.issuedFor(token.getIssuedFor());
        newToken.issuer(token.getIssuer());
        newToken.setNonce(token.getNonce());
        newToken.setScope(token.getScope());
        newToken.setSessionId(token.getSessionId());

        // In the case of a refresh token, aud is a basic claim.
        newToken.audience(token.getAudience());

        // The cnf is not a claim controlled by the protocol mapper.
        newToken.setConfirmation(token.getConfirmation());
        return newToken;
    }

    /**
     * Performs introspection checks related to token, client, userSession, user etc. If some of the checks failed, this method is supposed to already set an error event.
     * If all the checks are successful, the instance variables are supposed to be set
     *
     * @return true just if all the checks are working
     */
    protected boolean introspectionChecks(String tokenStr) {
        if (!verifyToken(tokenStr)) {
            return false;
        }
        if (!verifyClient()) {
            return false;
        }

        eventBuilder.session(this.token.getSessionId());
        UserSessionUtil.UserSessionValidationResult result = verifyUserSession();
        if (result.getError() != null) {
            logger.debugf( "Introspection access token for " + token.getIssuedFor() + " client: " + result.getError());
            eventBuilder.detail(Details.REASON,  "Introspection access token for " + token.getIssuedFor() + " client: " + result.getError());
            eventBuilder.error(result.getError());
            return false;
        } else {
            this.userSession = result.getUserSession();
        }

        this.user = userSession.getUser();
        eventBuilder.user(user);
        if (!TokenManager.isUserValid(session, realm, token, userSession.getUser())) {
            logger.debugf("Could not find valid user from user session " + userSession.getId());
            eventBuilder.detail(Details.REASON, "Could not find valid user from user session " + userSession.getId());
            eventBuilder.error(user == null ? Errors.USER_NOT_FOUND : Errors.USER_DISABLED);
            return false;
        }

        if (userSession.isOffline() && !UserSessionUtil.isOfflineAccessGranted(
                session, userSession.getAuthenticatedClientSessionByClient(client.getId()))) {
            logger.debugf("Offline session invalid because offline access not granted anymore");
            eventBuilder.detail(Details.REASON, "Offline session invalid because offline access not granted anymore");
            eventBuilder.error(Errors.SESSION_EXPIRED);
            return false;
        }

        if (!verifyTokenReuse()) {
            return false;
        }

        return true;
    }

    protected boolean verifyToken(String tokenStr) {
        try {
            TokenVerifier<T> verifier = TokenVerifier.create(tokenStr, getTokenClass())
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            this.token = verifier.verify().getToken();
            eventBuilder.detail(Details.TOKEN_ID, token.getId());
            eventBuilder.detail(Details.TOKEN_TYPE, token.getType());

            var tracing = session.getProvider(TracingProvider.class);
            var span = tracing.getCurrentSpan();
            if (span.isRecording()) {
                span.setAttribute(TracingAttributes.TOKEN_ISSUER, token.getIssuer());
                span.setAttribute(TracingAttributes.TOKEN_SID, token.getSessionId());
                span.setAttribute(TracingAttributes.TOKEN_ID, token.getId());
            }

            return true;
        } catch (VerificationException e) {
            logger.debugf("Introspection access token : JWT check failed: %s", e.getMessage());
            eventBuilder.detail(Details.REASON,"Access token JWT check failed");
            eventBuilder.error(Errors.INVALID_TOKEN);
            return false;
        }
    }


    protected Class<T> getTokenClass() {
        return (Class<T>) AccessToken.class;
    }

    protected boolean verifyClient() {
        eventBuilder.detail(Details.TOKEN_ISSUED_FOR, token.getIssuedFor());
        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            logger.debugf("Introspection access token : client with clientId %s does not exist", token.getIssuedFor() );
            eventBuilder.detail(Details.REASON, String.format("Could not find client for %s", token.getIssuedFor()));
            eventBuilder.error(Errors.CLIENT_NOT_FOUND);
            return false;
        } else {
            if (!client.isEnabled()) {
                logger.debugf("Introspection access token : client with clientId %s is disabled", token.getIssuedFor() );
                eventBuilder.detail(Details.REASON, String.format("Client with clientId %s is disabled", token.getIssuedFor()));
                eventBuilder.error(Errors.CLIENT_DISABLED);
                return false;
            } else {

                try {
                    TokenVerifier.createWithoutSignature(token)
                            .withChecks(TokenManager.NotBeforeCheck.forModel(client), TokenVerifier.IS_ACTIVE, new TokenManager.TokenRevocationCheck(session))
                            .verify();
                    this.client = client;
                    return true;
                } catch (VerificationException e) {
                    logger.debugf("Introspection access token for %s client: JWT check failed: %s", token.getIssuedFor(), e.getMessage());
                    eventBuilder.detail(Details.REASON, "Introspection access token for " + token.getIssuedFor() +" client: JWT check failed");
                    eventBuilder.error(Errors.INVALID_TOKEN);
                    return false;
                }
            }
        }
    }


    protected UserSessionUtil.UserSessionValidationResult verifyUserSession() {
        return UserSessionUtil.findValidSessionForAccessToken(session, realm, token, client, (invalidUserSession -> {}));
    }


    protected boolean verifyTokenReuse() {
        return true;
    }

    @Override
    public void close() {

    }
}
