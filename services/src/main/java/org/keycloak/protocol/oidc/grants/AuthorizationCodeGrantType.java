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
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;
import org.keycloak.services.clientpolicy.context.TokenResponseContext;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.services.util.DefaultClientSessionContext;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS_PARAM;
import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;

/**
 * OAuth 2.0 Authorization Code Grant
 * https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class AuthorizationCodeGrantType extends OAuth2GrantTypeBase {

    private static final Logger logger = Logger.getLogger(AuthorizationCodeGrantType.class);

    @Override
    public Response process(Context context) {
        setContext(context);

        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            String errorMessage = "Missing parameter: " + OAuth2Constants.CODE;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorMessage, Response.Status.BAD_REQUEST);
        }

        OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (parseResult.isIllegalCode()) {
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

            // Attempt to use same code twice should invalidate existing clientSession
            if (clientSession != null) {
                clientSession.detachFromUserSession();
            }

            event.error(Errors.INVALID_CODE);

            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

        if (parseResult.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        final UserSessionModel userSession;
        if (clientSession != null) {
            userSession = clientSession.getUserSession();
        } else {
            userSession = null;
        }

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }


        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        OAuth2Code codeData = parseResult.getCodeData();
        String redirectUri = codeData.getRedirectUriParam();
        String redirectUriParam = formParams.getFirst(OAuth2Constants.REDIRECT_URI);

        // KEYCLOAK-4478 Backwards compatibility with the adapters earlier than KC 3.4.2
        if (redirectUriParam != null && redirectUriParam.contains("session_state=") && !redirectUri.contains("session_state=")) {
            redirectUriParam = KeycloakUriBuilder.fromUri(redirectUriParam)
                    .replaceQueryParam(OAuth2Constants.SESSION_STATE, null)
                    .build().toString();
        }

        if (redirectUri != null && !redirectUri.equals(redirectUriParam)) {
            String errorMessage = "Parameter 'redirect_uri' did not match originally saved redirect URI used in initial OIDC request. Saved redirectUri: %s, redirectUri parameter: %s";
            event.detail(Details.REASON, String.format(errorMessage, redirectUri, redirectUriParam));
            event.error(Errors.INVALID_REDIRECT_URI);
            logger.tracef(errorMessage, redirectUri, redirectUriParam);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            String errorMessage = "Auth error: Found different client_id in clientSession";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            String errorMessage = "Client not allowed to exchange code";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            String errorMessage = "Session not active";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = codeData.getCodeChallenge();
        String codeChallengeMethod = codeData.getCodeChallengeMethod();
        String authUserId = user.getId();
        String authUsername = user.getUsername();
        if (authUserId == null) {
            authUserId = "unknown";
        }
        if (authUsername == null) {
            authUsername = "unknown";
        }

        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()) {
            PkceUtils.checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        } else {
            // PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            PkceUtils.checkParamsForPkceNotEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        }

        // https://datatracker.ietf.org/doc/html/rfc9449#section-10
        DPoPUtil.validateDPoPJkt(codeData.getDpopJkt(), session, event, cors);

        try {
            session.clientPolicy().triggerOnEvent(new TokenRequestContext(formParams, parseResult, client));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        String scopeParam = codeData.getScope();
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, scopeParam, session);

        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        Supplier<Stream<ClientScopeModel>> clientScopesSupplier = () -> TokenManager.getRequestedClientScopes(session, scopeParam, client, user);
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, clientScopesSupplier.get())) {
            String errorMessage = "Client no longer has requested consent from user";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, codeData.getNonce());

        // Process authorization_details using provider discovery (if present in request)
        List<AuthorizationDetailsResponse> authorizationDetailsResponse = null;
        if (formParams.getFirst(AUTHORIZATION_DETAILS_PARAM) != null) {
            authorizationDetailsResponse = processAuthorizationDetails(userSession, clientSessionCtx);
            if (authorizationDetailsResponse != null && !authorizationDetailsResponse.isEmpty()) {
                clientSessionCtx.setAttribute(AUTHORIZATION_DETAILS_RESPONSE, authorizationDetailsResponse);
            } else {
                logger.debugf("No available AuthorizationDetailsProcessor being able to process authorization_details '%s'", formParams.getFirst(AUTHORIZATION_DETAILS_PARAM));
            }
        }

        // If no authorization_details were processed from the request, try to process stored authorization_details
        if (authorizationDetailsResponse == null || authorizationDetailsResponse.isEmpty()) {
            try {
                authorizationDetailsResponse = processStoredAuthorizationDetails(userSession, clientSessionCtx);
                if (authorizationDetailsResponse != null && !authorizationDetailsResponse.isEmpty()) {
                    clientSessionCtx.setAttribute(AUTHORIZATION_DETAILS_RESPONSE, authorizationDetailsResponse);
                } else {
                    authorizationDetailsResponse = handleMissingAuthorizationDetails(clientSession.getUserSession(), clientSessionCtx);
                    if (authorizationDetailsResponse != null && !authorizationDetailsResponse.isEmpty()) {
                        clientSessionCtx.setAttribute(AUTHORIZATION_DETAILS_RESPONSE, authorizationDetailsResponse);
                    }
                }
            } catch (CorsErrorResponseException e) {
                // Re-throw CorsErrorResponseException as it's already properly formatted for HTTP response
                throw e;
            }
        }

        return createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, s -> {return new TokenResponseContext(formParams, parseResult, clientSessionCtx, s);});
    }

    @Override
    protected void addCustomTokenResponseClaims(AccessTokenResponse res, ClientSessionContext clientSessionCtx) {
        List<AuthorizationDetailsResponse> authDetailsResponse = clientSessionCtx.getAttribute(AUTHORIZATION_DETAILS_RESPONSE, List.class);
        if (authDetailsResponse != null && !authDetailsResponse.isEmpty()) {
            res.setOtherClaims(AUTHORIZATION_DETAILS_PARAM, authDetailsResponse);
        }
    }

    @Override
    public EventType getEventType() {
        return EventType.CODE_TO_TOKEN;
    }

}
