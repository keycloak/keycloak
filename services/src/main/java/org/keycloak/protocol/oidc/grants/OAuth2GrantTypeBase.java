/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessor;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS_PARAM;

/**
 * Base class for OAuth 2.0 grant types
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public abstract class OAuth2GrantTypeBase implements OAuth2GrantType {

    private static final Logger logger = Logger.getLogger(OAuth2GrantTypeBase.class);

    protected OAuth2GrantType.Context context;

    protected KeycloakSession session;
    protected RealmModel realm;
    protected ClientModel client;
    protected OIDCAdvancedConfigWrapper clientConfig;
    protected ClientConnection clientConnection;
    protected Map<String, String> clientAuthAttributes;
    protected MultivaluedMap<String, String> formParams;
    protected EventBuilder event;
    protected Cors cors;
    protected TokenManager tokenManager;
    protected HttpRequest request;
    protected HttpResponse response;
    protected HttpHeaders headers;

    protected void setContext(Context context) {
        this.context = context;
        this.session = context.session;
        this.realm = context.realm;
        this.client = context.client;
        this.clientConfig = (OIDCAdvancedConfigWrapper) context.clientConfig;
        this.clientConnection = context.clientConnection;
        this.clientAuthAttributes = context.clientAuthAttributes;
        this.request = context.request;
        this.response = context.response;
        this.headers = context.headers;
        this.formParams = context.formParams;
        this.event = context.event;
        this.cors = context.cors;
        this.tokenManager = (TokenManager) context.tokenManager;
    }

    protected Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
        String scopeParam, boolean code, Function<TokenManager.AccessTokenResponseBuilder, ClientPolicyContext> clientPolicyContextGenerator) {
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, context.getGrantType());
        AccessToken token = tokenManager.createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager
            .responseBuilder(realm, client, event, session, userSession, clientSessionCtx).accessToken(token);
        boolean useRefreshToken = useRefreshToken();
        if (useRefreshToken) {
            responseBuilder.generateRefreshToken();
            if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(responseBuilder.getRefreshToken().getType())
                    && clientSessionCtx.getClientSession().getNote(AuthenticationProcessor.FIRST_OFFLINE_ACCESS) != null) {
                // the online session can be removed if first created for offline access
                session.sessions().removeUserSession(realm, userSession);
            }
        } else {
            TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);
            if (encoder.getTokenContextFromTokenId(responseBuilder.getAccessToken().getId()).getSessionType() == AccessTokenContext.SessionType.TRANSIENT) {
                // transient sessions do not add the session ID to the token
                responseBuilder.getAccessToken().setSessionId(null);
                event.session((String) null);
            }
        }

        checkAndBindMtlsHoKToken(responseBuilder, useRefreshToken);

        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        if (clientPolicyContextGenerator != null) {
            try {
                session.clientPolicy().triggerOnEvent(clientPolicyContextGenerator.apply(responseBuilder));
            } catch (ClientPolicyException cpe) {
                event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
                event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
                event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
                event.error(cpe.getError());
                throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
            }
        }

        AccessTokenResponse res = null;
        if (code) {
            try {
                res = responseBuilder.build();
            } catch (RuntimeException re) {
                if ("can not get encryption KEK".equals(re.getMessage())) {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "can not get encryption KEK", Response.Status.BAD_REQUEST);
                } else {
                    throw re;
                }
            }
        } else {
            res = responseBuilder.build();
        }

        // Extension point for subclasses to add custom claims
        addCustomTokenResponseClaims(res, clientSessionCtx);

        event.success();

        return cors.add(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE));
    }

    protected void checkAndBindMtlsHoKToken(TokenManager.AccessTokenResponseBuilder responseBuilder, boolean useRefreshToken) {
        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if (clientConfig.isUseMtlsHokToken()) {
            AccessToken.Confirmation confirmation = MtlsHoKTokenUtil.bindTokenWithClientCertificate(request, session);
            if (confirmation != null) {
                responseBuilder.getAccessToken().setConfirmation(confirmation);
                if (useRefreshToken) {
                    responseBuilder.getRefreshToken().setConfirmation(confirmation);
                }
            } else {
                String errorMessage = "Client Certification missing for MTLS HoK Token Binding";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        errorMessage, Response.Status.BAD_REQUEST);
            }
        }
    }

    protected void updateClientSession(AuthenticatedClientSessionModel clientSession) {

        if(clientSession == null) {
            ServicesLogger.LOGGER.clientSessionNull();
            return;
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.CLIENT_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.CLIENT_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            String oldClientSessionState = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
            if (!adapterSessionId.equals(oldClientSessionState)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            }

            String oldClientSessionHost = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
            if (!Objects.equals(adapterSessionHost, oldClientSessionHost)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            }
        }
    }

    protected void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    protected String getRequestedScopes() {
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            AuthorizationRequestContext authorizationRequestContext = AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, scope);
            validScopes = TokenManager.isValidScope(session, scope, authorizationRequestContext, client, null);
        } else {
            validScopes = TokenManager.isValidScope(session, scope, client, null);
        }

        if (!validScopes) {
            String errorMessage = "Invalid scopes: " + scope;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        return scope;
    }

    protected void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();
        clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Extension point for subclasses to add custom claims to the AccessTokenResponse before it is returned.
     * Default implementation does nothing.
     */
    protected void addCustomTokenResponseClaims(AccessTokenResponse res, ClientSessionContext clientSessionCtx) {
        // Default: do nothing
    }

    /**
     * Processes the authorization_details parameter using provider discovery.
     * This method can be overridden by subclasses to customize the behavior.
     *
     * @param userSession      the user session
     * @param clientSessionCtx the client session context
     * @return the authorization details response if processing was successful, null otherwise
     */
    protected List<AuthorizationDetailsResponse> processAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        String authorizationDetailsParam = formParams.getFirst(AUTHORIZATION_DETAILS_PARAM);
        if (authorizationDetailsParam != null) {
            try {
                return session.getKeycloakSessionFactory()
                        .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                        .sorted((f1, f2) -> f2.order() - f1.order())
                        .map(f -> session.getProvider(AuthorizationDetailsProcessor.class, f.getId()))
                        .map(authzDetailsProcessor -> authzDetailsProcessor.process(userSession, clientSessionCtx, authorizationDetailsParam))
                        .filter(authzDetailsResponse -> authzDetailsResponse != null)
                        .findFirst()
                        .orElse(null);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Invalid authorization_details")) {
                    logger.warnf(e, "Error when processing authorization_details");
                    event.error(Errors.INVALID_REQUEST);
                    throw new CorsErrorResponseException(cors, "invalid_request", "Error when processing authorization_details", Response.Status.BAD_REQUEST);
                } else {
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * Allows processors to generate an authorization details response when the authorization_details parameter is missing in the request.
     * This applies to flows where pre-authorization or credential offers are present, and is general to all AuthorizationDetailsProcessor implementations.
     *
     * @param userSession the user session
     * @param clientSessionCtx the client session context
     * @return the authorization details response if generation was successful, null otherwise
     */
    protected List<AuthorizationDetailsResponse> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        try {
            return session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                    .sorted((f1, f2) -> f2.order() - f1.order())
                    .map(f -> session.getProvider(AuthorizationDetailsProcessor.class, f.getId()))
                    .map(processor -> processor.handleMissingAuthorizationDetails(userSession, clientSessionCtx))
                    .filter(authzDetailsResponse -> authzDetailsResponse != null)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            logger.warnf(e, "Error when handling missing authorization_details");
            return null;
        }
    }

    /**
     * Process stored authorization_details from the authorization request (e.g., from PAR).
     * This method is specifically for Authorization Code Flow where authorization_details was used
     * in the authorization request but is missing from the token request.
     *
     * @param userSession the user session
     * @param clientSessionCtx the client session context
     * @return the authorization details response if processing was successful, null otherwise
     */
    protected List<AuthorizationDetailsResponse> processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) throws CorsErrorResponseException {
        // Check if authorization_details was stored during authorization request (e.g., from PAR)
        String storedAuthDetails = clientSessionCtx.getClientSession().getNote(AUTHORIZATION_DETAILS_PARAM);
        if (storedAuthDetails != null) {
            logger.debugf("Found authorization_details in client session, processing it");
            try {
                return session.getKeycloakSessionFactory()
                        .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                        .sorted((f1, f2) -> f2.order() - f1.order())
                        .map(f -> session.getProvider(AuthorizationDetailsProcessor.class, f.getId()))
                        .map(processor -> {
                            try {
                                return processor.processStoredAuthorizationDetails(userSession, clientSessionCtx, storedAuthDetails);
                            } catch (OAuthErrorException e) {
                                // Wrap OAuthErrorException in CorsErrorResponseException for proper HTTP response
                                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
                            }
                        })
                        .filter(authzDetailsResponse -> authzDetailsResponse != null)
                        .findFirst()
                        .orElse(null);
            } catch (RuntimeException e) {
                logger.warnf(e, "Error when processing stored authorization_details");
                throw e;
            }
        }
        return null;
    }

    /*
     * If the grant type generates a refresh token or just the access token.
     * @return true if refresh token is generated by the grant, false if not
     */
    protected boolean useRefreshToken() {
        return clientConfig.isUseRefreshToken();
    }

    @Override
    public void close() {
    }

}
