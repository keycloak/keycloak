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
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.utils.MediaType;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS_PARAM;

public class PreAuthorizedCodeGrantType extends OAuth2GrantTypeBase {

    private static final Logger LOGGER = Logger.getLogger(PreAuthorizedCodeGrantType.class);

    public static final String VC_ISSUANCE_FLOW = "VC-Issuance-Flow";

    @Override
    public Response process(Context context) {
        LOGGER.debug("Process grant request for preauthorized.");
        setContext(context);

        // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
        String code = formParams.getFirst(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM);

        if (code == null) {
            // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-token-request
            String errorMessage = "Missing parameter: " + PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    errorMessage, Response.Status.BAD_REQUEST);
        }
        OAuth2CodeParser.ParseResult result = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (result.isIllegalCode()) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid",
                    Response.Status.BAD_REQUEST);
        }
        if (result.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired",
                    Response.Status.BAD_REQUEST);
        }
        AuthenticatedClientSessionModel clientSession = result.getClientSession();
        ClientSessionContext sessionContext = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession,
                OAuth2Constants.SCOPE_OPENID, session);
        clientSession.setNote(VC_ISSUANCE_FLOW, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);
        sessionContext.setAttribute(Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);

        // set the client as retrieved from the pre-authorized session
        session.getContext().setClient(result.getClientSession().getClient());

        AccessToken accessToken = tokenManager.createClientAccessToken(session,
                clientSession.getRealm(),
                clientSession.getClient(),
                clientSession.getUserSession().getUser(),
                clientSession.getUserSession(),
                sessionContext);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(
                clientSession.getRealm(),
                clientSession.getClient(),
                event,
                session,
                clientSession.getUserSession(),
                sessionContext).accessToken(accessToken);

        // Process authorization_details using provider discovery
        List<AuthorizationDetailsResponse> authorizationDetailsResponse = processAuthorizationDetails(clientSession.getUserSession(), sessionContext);
        LOGGER.infof("Initial authorization_details processing result: %s", authorizationDetailsResponse);

        // If no authorization_details were processed from the request, try to generate them from credential offer
        if (authorizationDetailsResponse == null || authorizationDetailsResponse.isEmpty()) {
            authorizationDetailsResponse = handleMissingAuthorizationDetails(clientSession.getUserSession(), sessionContext);
        }

        AccessTokenResponse tokenResponse;
        try {
            tokenResponse = responseBuilder.build();
        } catch (RuntimeException re) {
            if ("cannot get encryption KEK".equals(re.getMessage())) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "cannot get encryption KEK", Response.Status.BAD_REQUEST);
            } else {
                throw re;
            }
        }

        // If authorization_details is present, add it to otherClaims
        if (authorizationDetailsResponse != null && !authorizationDetailsResponse.isEmpty()) {
            tokenResponse.setOtherClaims(AUTHORIZATION_DETAILS_PARAM, authorizationDetailsResponse);
        }

        event.success();
        return cors.allowAllOrigins().add(Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public EventType getEventType() {
        return EventType.CODE_TO_TOKEN;
    }

    /**
     * Create a pre-authorized Code for the given client session.
     *
     * @param session                    - keycloak session to be used
     * @param authenticatedClientSession - client session to be persisted
     * @param expirationTime             - expiration time of the code, the code should be short-lived
     * @return the pre-authorized code
     */
    public static String getPreAuthorizedCode(KeycloakSession session, AuthenticatedClientSessionModel authenticatedClientSession, int expirationTime) {
        String codeId = UUID.randomUUID().toString();
        String nonce = SecretGenerator.getInstance().randomString();
        OAuth2Code oAuth2Code = new OAuth2Code(codeId, expirationTime, nonce, null, null, null, null, null,
                authenticatedClientSession.getUserSession().getId());
        return OAuth2CodeParser.persistCode(session, authenticatedClientSession, oAuth2Code);
    }
}
