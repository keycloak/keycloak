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

import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessorManager;
import org.keycloak.protocol.oidc.refresh.RefreshTokenContext;
import org.keycloak.protocol.oidc.refresh.RefreshTokenProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRefreshContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshResponseContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;

import org.jboss.logging.Logger;

/**
 * OAuth 2.0 Refresh Token Grant
 * https://datatracker.ietf.org/doc/html/rfc6749#section-6
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class RefreshTokenGrantType extends OAuth2GrantTypeBase {

    private static final Logger logger = Logger.getLogger(RefreshTokenGrantType.class);

    @Override
    public Response process(Context context) {
        setContext(context);

        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        String scopeParameter = getRequestedScopes();
        String resourceParameter = formParams.getFirst(OAuth2Constants.RESOURCE);

        try {
            session.clientPolicy().triggerOnEvent(new TokenRefreshContext(formParams, client, scopeParameter));
            refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        AccessTokenResponse res;
        try {
            RefreshToken oldRefreshToken = tokenManager.verifyRefreshToken(session, realm, client, request, refreshToken, true);

            RefreshTokenContext refreshTokenCtx = new RefreshTokenContext(oldRefreshToken, tokenManager, clientConnection, realm, client, event, headers, scopeParameter, resourceParameter);

            RefreshTokenProvider refreshTokenProvider = session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(RefreshTokenProvider.class)
                    .sorted((f1, f2) -> f2.order() - f1.order())
                    .map(f -> session.getProvider(RefreshTokenProvider.class, f.getId()))
                    .filter(p -> p.supports(refreshTokenCtx))
                    .findFirst()
                    .orElseThrow(() -> {
                        event.error(Errors.INVALID_REQUEST);
                        return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No provider available to handle refresh token", Response.Status.BAD_REQUEST);
                    });

            TokenManager.AccessTokenResponseBuilder responseBuilder = refreshTokenProvider.refreshAccessToken(refreshTokenCtx);

            checkAndBindMtlsHoKToken(responseBuilder, clientConfig.isUseRefreshToken());

            session.clientPolicy().triggerOnEvent(new TokenRefreshResponseContext(formParams, responseBuilder));

            res = responseBuilder.build();

            if (!responseBuilder.isOfflineToken()) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, res.getSessionState());
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                updateClientSession(clientSession);
                updateUserSessionFromClientAuth(userSession);
            }
        } catch (OAuthErrorException e) {
            logger.trace(e.getMessage(), e);
            // KEYCLOAK-6771 Certificate Bound Token
            if (MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC.equals(e.getDescription())) {
                event.detail(Details.REASON, e.getDescription());
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.UNAUTHORIZED);
            } else {
                event.detail(Details.REASON, e.getDescription());
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
            }
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        // Sanitize authorization details before they are sent as part of the Token Response
        var authDetailsProcessor = new AuthorizationDetailsProcessorManager(session);
        authDetailsProcessor.sanitizeBeforeSendingTokenResponse(res);

        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public EventType getEventType() {
        return EventType.REFRESH_TOKEN;
    }
    
    @Override
    public Set<String> getTokenParameterNames() {
        return Collections.emptySet();
    }
}
