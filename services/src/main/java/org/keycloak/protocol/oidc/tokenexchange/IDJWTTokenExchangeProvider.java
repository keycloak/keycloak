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

package org.keycloak.protocol.oidc.tokenexchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

/**
 * Provider for token exchange of Identity Assertion JWT Authorization Grant (ID-JAG)(*),
 * which is based on the token exchange specification RFC8693(**).
 *  
 * (*)https://datatracker.ietf.org/doc/draft-ietf-oauth-identity-assertion-authz-grant/
 * (**)https://datatracker.ietf.org/doc/html/rfc8693
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */
public class IDJWTTokenExchangeProvider extends StandardTokenExchangeProvider {

    private static final Logger logger = Logger.getLogger(IDJWTTokenExchangeProvider.class);

    // client attribute which is the key for searching client_id  of resource authorization serverau with audience parameter
    private static final String RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER = "idjag.resource.authorization.server.identifier";

    // clent attribute prefix for the client's client_id in resouce authorization server
    private static final String CLIENTID_IN_RESOURCE_AUTHORIZATION_SERVER = "idjag.clientid.at.";

    // client attribute prefix for the permitted scopes in the resource authorization server
    private static final String PERMITTED_SCOPES_IN_RESOURCE_AUTHORIZATION_SERVER = "idjag.permitted.scopes.at.";

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public boolean supports(TokenExchangeContext context) {
  
        String requestedTokenType = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (!requestedTokenType.equals(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE)) {
            context.setUnsupportedReason("Parameter 'requested_token_type' should be 'urn:ietf:params:oauth:token-type:id-jag' for IDJWT token exchange");
            return false;
        }

        // Subject impersonation request
        String requestedSubject = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        if (requestedSubject != null) {
            context.setUnsupportedReason("Parameter 'requested_subject' is not supported for standard token exchange");
            return false;
        }

        // Internal-external token exchange
        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer != null) {
            context.setUnsupportedReason("Parameter 'requested_issuer' is not supported for standard token exchange");
            return false;
        }

        // External-internal token exchange
        String subjectIssuer = context.getFormParams().getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (subjectIssuer != null) {
            context.setUnsupportedReason("Parameter 'subject_issuer' is not supported for standard token exchange");
            return false;
        }

        if(!OIDCAdvancedConfigWrapper.fromClientModel(context.getClient()).isStandardTokenExchangeEnabled()) {
            context.setUnsupportedReason("Standard token exchange is not enabled for the requested client");
            return false;
        }

        String subjectToken = context.getParams().getSubjectToken();
        if (subjectToken == null) {
            context.setUnsupportedReason("Parameter 'subject_token' required for standard token exchange");
            return false;
        }

        String subjectTokenType = context.getParams().getSubjectTokenType();
        if (subjectTokenType == null) {
            context.setUnsupportedReason("Parameter 'subject_token_type' required for standard token exchange");
            return false;
        }

        if (!subjectTokenType.equals(OAuth2Constants.ID_TOKEN_TYPE)) {
            // For now, only IDToken is supported. SAML 2.0 assertion and refresh token may be supported in the future.
            context.setUnsupportedReason("Parameter 'subject_token' supports IDToken only");
            return false;
        }

        return true;
    }

    @Override
    protected Response tokenExchange() {
        
        String subjectToken = context.getParams().getSubjectToken();

        event.detail(Details.REQUESTED_TOKEN_TYPE, context.getParams().getRequestedTokenType());

        IDToken token= null;
        try{
            RealmModel realm = session.getContext().getRealm();
            String realmUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

            TokenVerifier<IDToken> verifier = TokenVerifier.create(subjectToken, IDToken.class)
                    .withChecks(
                        new TokenVerifier.RealmUrlCheck(realmUrl),          
                        new TokenVerifier.TokenTypeCheck(List.of(TokenUtil.TOKEN_TYPE_ID)),   
                        TokenVerifier.IS_ACTIVE
                    );
                    
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
            if (signatureProvider == null) {
                logger.debugf("Invalid algorithm '%s' in the IDToken", algorithm);
                event.detail(Details.REASON, "Invalid token");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST); 
            }
            SignatureVerifierContext signatureVerifier = signatureProvider.verifier(kid);
            verifier.verifierContext(signatureVerifier);
            token = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.errorf("Verification failed: %s", e.getMessage());
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, e.getMessage(), Response.Status.BAD_REQUEST);
        }
        
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
        if (userSession == null) {
            event.detail(Details.REASON, "Session not found");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Session not found", Response.Status.BAD_REQUEST); 
        }

        KeycloakContext context = session.getContext();


        UserModel user = userSession.getUser();
        if (user == null || !user.isEnabled()) {
            event.detail(Details.REASON, "Invalid user");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid user", Response.Status.BAD_REQUEST);
        }

        Long issuedAt = token.getIat();

        if (issuedAt != null) {
            if (realm.getNotBefore() > issuedAt) {
                    event.detail(Details.REASON, "Token revoked");
                    event.error(Errors.INVALID_TOKEN);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Token revoked", Response.Status.BAD_REQUEST); 
            }

            if (session.users().getNotBeforeOfUser(realm, user) > issuedAt) {
                    event.detail(Details.REASON, "Token revoked");
                    event.error(Errors.INVALID_TOKEN);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Token revoked", Response.Status.BAD_REQUEST); 
            }
        }
        
        context.setClient(client);
        context.setBearerToken(token);
        context.setUserSession(userSession);
        
        event.user(user);
        event.detail(Details.USERNAME, user.getUsername());
        if (token.getSessionId() != null) {
            event.session(userSession);
        }
        event.detail(Details.SUBJECT_TOKEN_CLIENT_ID, token.getIssuedFor());

        return exchangeClientToClient(user, userSession, token, true);

    }

    @Override
    protected void validateAudience(JsonWebToken token, boolean disallowOnHolderOfTokenMismatch, List<ClientModel> targetAudienceClients) {

        ClientModel tokenHolder = token == null ? null : realm.getClientByClientId(token.getIssuedFor());

        if (client.isPublicClient()) {
            String errorMessage = "Public client is not allowed to exchange token";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, errorMessage, Response.Status.BAD_REQUEST);
        }

        ClientModel targetClient = targetAudienceClients.get(0);  
        if (!targetClient.isEnabled()) {
                event.detail(Details.REASON, "audience client disabled");
                event.detail(Details.AUDIENCE, targetClient.getClientId());
                event.error(Errors.CLIENT_DISABLED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client disabled", Response.Status.BAD_REQUEST);
        }
        
        if (token != null && !token.hasAudience(client.getClientId())) {
                event.detail(Details.REASON, "client is not within the token audience");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client is not within the token audience", Response.Status.FORBIDDEN);
        }
            
        if (!token.hasAudience(tokenHolder.getClientId())) {
                event.detail(Details.REASON, "token authorized party is not within the token audience");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "token authorized party is not within the token audience", Response.Status.FORBIDDEN);
        }
        
    }

    @Override
    protected String getRequestedScope(JsonWebToken token, List<ClientModel> targetAudienceClients) {

        String audiencePrameterString = params.getAudience().get(0);
        ClientModel resourceAuthzClient = session.clients().getClientsStream(realm)
            .filter(c -> audiencePrameterString.equals(c.getAttribute(RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER)))
            .findFirst()
            .orElseThrow(() ->  {
                event.detail(Details.REASON, "Client not found for audience identifier: " + audiencePrameterString);
                event.error(Errors.NOT_ALLOWED);
                return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client not found for audience identifier: " + audiencePrameterString, Response.Status.BAD_REQUEST);
            });

        String clientId = resourceAuthzClient.getClientId();
       
        String attrKey = PERMITTED_SCOPES_IN_RESOURCE_AUTHORIZATION_SERVER + clientId;
        String attrValue = client.getAttribute(attrKey);
            
        java.util.Set<String> permittedScopes = new java.util.HashSet<>();
        if (attrValue != null && !attrValue.isEmpty()) {
            // attrValue is expected to be a space separated list of scopes, e.g. "scope1 scope2 scope3"
            permittedScopes.addAll(java.util.Arrays.asList(attrValue.split("\\s+")));
        } else {
            String errorMessage = "Permitted scopes not configured for the audience client";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SERVER_ERROR, errorMessage, Response.Status.BAD_REQUEST);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);
        if (scope == null || scope.isEmpty()) {
            return String.join(" ", permittedScopes);
        }
        String[] requestedScopes = scope.split("\\s+");

        java.util.List<String> filteredScopes = new java.util.ArrayList<>();
        for (String requested : requestedScopes) {
            if (permittedScopes.contains(requested)) {
                filteredScopes.add(requested);
            } else {
                logger.warn("Requested scope [" + requested + "] is not permitted and was filtered out.");
            }
        } 
        
        if (filteredScopes.isEmpty()) {
            String errorMessage = "Invalid scopes: " + scope;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        return String.join(" ", filteredScopes);

    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope, JsonWebToken subjectToken) {
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, client, scope);
        boolean isOfflineSession = targetUserSession.isOffline();

        if (targetUserSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT || isOfflineSession) {
            if (OAuth2Constants.REFRESH_TOKEN_TYPE.equals(requestedTokenType)) {
                event.detail(Details.REASON, "Refresh token not valid as requested_token_type because creating a new session is needed");
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "Refresh token not valid as requested_token_type because creating a new session is needed", Response.Status.BAD_REQUEST);
            }

            // create a transient session now for the token exchange
            if (isOfflineSession) {
                targetUserSession = UserSessionUtil.createTransientUserSession(session, targetUserSession);
            }
        }

        final boolean newClientSessionCreated = targetUserSession.getPersistenceState() != UserSessionModel.SessionPersistenceState.TRANSIENT
                && targetUserSession.getAuthenticatedClientSessionByClient(client.getId()) == null;

        try {
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession,
                    context.getRestrictedScopes(), !OAuth2Constants.REFRESH_TOKEN_TYPE.equals(requestedTokenType)); // create transient session if needed except for refresh

            if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                    && clientSessionCtx.getClientScopesStream().filter(s -> OAuth2Constants.OFFLINE_ACCESS.equals(s.getName())).findAny().isPresent()) {
                event.detail(Details.REASON, "Scope offline_access not allowed for token exchange");
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "Scope offline_access not allowed for token exchange", Response.Status.BAD_REQUEST);
            }

            updateUserSessionFromClientAuth(targetUserSession);

            if (params.getAudience() != null && !targetAudienceClients.isEmpty()) {
                clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, targetAudienceClients.toArray(ClientModel[]::new));
            }

            clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

            TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);

            if (subjectToken != null) {
                AccessTokenContext subjectTokenContext = encoder.getTokenContextFromTokenId(subjectToken.getId());

                //copy subject client from the client session notes if the subject token used has already been exchanged
                if (OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE.equals(subjectTokenContext.getGrantType())) {
                    ClientModel subjectClient = session.clients().getClientByClientId(realm, subjectToken.getIssuedFor());
                    if (subjectClient != null) {
                        AuthenticatedClientSessionModel subjectClientSession = targetUserSession.getAuthenticatedClientSessionByClient(subjectClient.getId());
                        if (subjectClientSession != null) {
                            subjectClientSession.getNotes().entrySet().stream()
                                    .filter(note -> note.getKey().startsWith(Constants.TOKEN_EXCHANGE_SUBJECT_CLIENT))
                                    .forEach(note -> clientSessionCtx.getClientSession().setNote(note.getKey(), note.getValue()));
                        }
                    }
                }

                //store client id of the subject token
                clientSessionCtx.getClientSession().setNote(Constants.TOKEN_EXCHANGE_SUBJECT_CLIENT + subjectToken.getIssuedFor(), subjectToken.getId());
            }

            TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session,
                            clientSessionCtx.getClientSession().getUserSession(), clientSessionCtx).generateIDJag();

            String audiencePrameterString = params.getAudience().get(0);
            
            ClientModel resourceAuthzClient = session.clients().getClientsStream(realm)
                .filter(c -> audiencePrameterString.equals(c.getAttribute(RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER)))
                .findFirst()
                .orElseThrow(() -> {
                    event.detail(Details.REASON, "Client not found for audience identifier: " + audiencePrameterString);
                    event.error(Errors.NOT_ALLOWED);
                    return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client not found for audience identifier: " + audiencePrameterString, Response.Status.BAD_REQUEST);
                });

            String clientId = resourceAuthzClient.getClientId();
            
            String attrKey = CLIENTID_IN_RESOURCE_AUTHORIZATION_SERVER + clientId;
            String attrValue = client.getAttribute(attrKey);
            responseBuilder.getIdjag().setClient_id(attrValue);
            responseBuilder.getIdjag().setScope(scope);

            if (encoder.getTokenContextFromTokenId(responseBuilder.getAccessToken().getId()).getSessionType() == AccessTokenContext.SessionType.TRANSIENT) {
                responseBuilder.getAccessToken().setSessionId(null);
                event.session((String) null);
            }

            responseBuilder.getAccessToken().addAudience(audiencePrameterString);

            AccessTokenResponse res;     
            res = responseBuilder.build();
            res.setTokenType(TokenUtil.TOKEN_TYPE_NA);
            res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);
            res.setScope(scope);

            if (responseBuilder.getAccessToken().getAudience() != null) {
                event.detail(Details.AUDIENCE, CollectionUtil.join(List.of(responseBuilder.getAccessToken().getAudience()), " "));
            }
            event.success();

            return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
        } catch (RuntimeException e) {
            // Cleanup client-session if created in this request
            if (newClientSessionCreated) {
                targetUserSession.removeAuthenticatedClientSessions(Set.of(client.getId()));
            }

            throw e;
        }
    }

    @Override
    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients) {
        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }
    
    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE);
    }

    @Override
    protected String getRequestedTokenType() {
        String requestedTokenType = params.getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
            return requestedTokenType;
        }
        logger.warn("requestedTokenType is not null.");
        if (requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.ID_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.SAML2_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE)) {
            return requestedTokenType;
        }
        OIDCAdvancedConfigWrapper oidcClient = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && oidcClient.isUseRefreshToken()
                && oidcClient.getStandardTokenExchangeRefreshEnabled() != OIDCAdvancedConfigWrapper.TokenExchangeRefreshTokenEnabled.NO) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported in JWTTokenExchangeProvider");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported in JWTTokenExchangeProvider", Response.Status.BAD_REQUEST);
    }

    // Using the value of "audience" parameter to find the target audience clients. 
    // The value of "audience" parameter is expected to be the same as the value of client attribute "idjag.resource.authorization.server.identifier" in the target audience client.
    // If no "audience" parameter is provided, the client itself is considered as the audience.
    protected List<ClientModel> getTargetAudienceClients() {
        List<String> audienceParams = params.getAudience();
        List<ClientModel> targetAudienceClients = new ArrayList<>();
        if (audienceParams != null) {
            if(audienceParams.size() == 0) {
                event.detail(Details.REASON, "audience required");
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "audience required", Response.Status.BAD_REQUEST);
            } else if (audienceParams.size() > 0) {
                // only the first one is used
                String audiencePrameterString = params.getAudience().get(0);
                
                ClientModel targetClient = session.clients().getClientsStream(realm)
                    .filter(c -> audiencePrameterString.equals(c.getAttribute(RESOURCE_AUTHORIZATION_SERVER_IDENTIFIER)))
                    .findFirst()
                    .orElseThrow(() -> {
                        event.detail(Details.REASON, "Client not found for audience identifier: " + audiencePrameterString);
                        event.error(Errors.NOT_ALLOWED);
                        return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client not found for audience identifier: " + audiencePrameterString, Response.Status.BAD_REQUEST);
                    });

                if (targetClient == null) {
                    event.detail(Details.REASON, "audience not found");
                    event.detail(Details.AUDIENCE, audiencePrameterString);
                    event.error(Errors.CLIENT_NOT_FOUND);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Audience not found", Response.Status.BAD_REQUEST);
                } else {
                    targetAudienceClients.add(targetClient);
                }
            } 
        } else {
            event.detail(Details.REASON, "audience required");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "audience required", Response.Status.BAD_REQUEST);
        }
        return targetAudienceClients;
    }
}
