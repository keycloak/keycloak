/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oidc.endpoints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.RequestUriType;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsProcessorManager;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorValidation;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS;
import static org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor.getRequestUriType;

/**
 * Implements some checks typical for OIDC Authorization Endpoint. Useful to consolidate various checks on single place to avoid duplicated
 * code logic in different contexts (OIDC Authorization Endpoint triggered from browser, PAR)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthorizationEndpointChecker {

    private EventBuilder event;
    private AuthorizationEndpointRequest request;
    private KeycloakSession session;
    private ClientModel client;
    private RealmModel realm;

    private String redirectUri;
    private OIDCResponseType parsedResponseType;
    private OIDCResponseMode parsedResponseMode;
    private MultivaluedMap<String, String> params;

    private static final Logger logger = Logger.getLogger(AuthorizationEndpointChecker.class);

    // https://tools.ietf.org/html/rfc7636#section-4.2
    private static final Pattern VALID_CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    public AuthorizationEndpointChecker event(EventBuilder event) {
        this.event = event;
        return this;
    }

    public AuthorizationEndpointChecker request(AuthorizationEndpointRequest request) {
        this.request = request;
        return this;
    }

    public AuthorizationEndpointChecker session(KeycloakSession session) {
        this.session = session;
        return this;
    }

    public AuthorizationEndpointChecker client(ClientModel client) {
        this.client = client;
        return this;
    }

    public AuthorizationEndpointChecker realm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public AuthorizationEndpointChecker params(MultivaluedMap<String, String> params) {
        this.params = params;
        return this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public OIDCResponseType getParsedResponseType() {
        return parsedResponseType;
    }

    public OIDCResponseMode getParsedResponseMode() {
        return parsedResponseMode;
    }

    public MultivaluedMap<String, String> getRequestParams() {
        return params;
    }

    public AuthorizationEndpointRequest getAuthorizationEndpointRequest() {
        return request;
    }

    public void checkRedirectUri() throws AuthorizationCheckException {
        String redirectUriParam = request != null ? request.getRedirectUriParam() : params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        String scope = request != null ? request.getScope() : params.getFirst(OIDCLoginProtocol.SCOPE_PARAM);

        // The redirect_uri parameter is required for OIDC, but optional for OAuth2
        boolean isOIDCRequest = TokenUtil.isOIDCRequest(scope);

        event.detail(Details.REDIRECT_URI, redirectUriParam);

        this.redirectUri = RedirectUtils.verifyRedirectUri(session, redirectUriParam, client, isOIDCRequest);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new AuthorizationCheckException(OAuthErrorException.INVALID_REDIRECT_URI, Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
        }
    }

    public void checkResponseType() throws AuthorizationCheckException {
        String responseTypeParam = request != null ? request.getResponseType() : params.getFirst(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        String responseModeParam = request != null ? request.getResponseMode() : params.getFirst(OIDCLoginProtocol.RESPONSE_MODE_PARAM);

        if (responseTypeParam == null) {
            ServicesLogger.LOGGER.missingParameter(OAuth2Constants.RESPONSE_TYPE);
            String errorMessage = "Missing parameter: response_type";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        event.detail(Details.RESPONSE_TYPE, responseTypeParam);

        try {
            parsedResponseType = OIDCResponseType.parse(responseTypeParam);
        } catch (IllegalArgumentException iae) {
            event.detail(Details.REASON, iae.getMessage());
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, null);
        }

        OIDCResponseMode responseMode;
        try {
            responseMode = OIDCResponseMode.parse(responseModeParam, parsedResponseType);
        } catch (IllegalArgumentException iae) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
            String errorMessage = "Invalid parameter: " + OIDCLoginProtocol.RESPONSE_MODE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        event.detail(Details.RESPONSE_MODE, responseMode.toString().toLowerCase());

        // Disallowed by OIDC specs
        if (parsedResponseType.isImplicitOrHybridFlow() && responseMode == OIDCResponseMode.QUERY) {
            ServicesLogger.LOGGER.responseModeQueryNotAllowed();
            String errorMessage = "Response_mode 'query' not allowed for implicit or hybrid flow";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        parsedResponseMode = responseMode;

        if (parsedResponseType.isImplicitOrHybridFlow() && responseMode == OIDCResponseMode.QUERY_JWT &&
                (!StringUtil.isNotBlank(client.getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG)) ||
                !StringUtil.isNotBlank(client.getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC)))) {
            ServicesLogger.LOGGER.responseModeQueryJwtNotAllowed();
            String errorMessage = "Response_mode 'query.jwt' is allowed only when the authorization response token is encrypted";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        if ((parsedResponseType.hasResponseType(OIDCResponseType.CODE) || parsedResponseType.hasResponseType(OIDCResponseType.NONE)) && !client.isStandardFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Standard");
            String errorMessage = "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new AuthorizationCheckException(Response.Status.UNAUTHORIZED, OAuthErrorException.UNAUTHORIZED_CLIENT, errorMessage);
        }

        if (parsedResponseType.isImplicitOrHybridFlow() && !client.isImplicitFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Implicit");
            String errorMessage = "Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new AuthorizationCheckException(Response.Status.UNAUTHORIZED, OAuthErrorException.UNAUTHORIZED_CLIENT, errorMessage);
        }

        // DPoP is not supported for implicit and hybrid flows
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (clientConfig.isUseDPoP() && parsedResponseType.isImplicitOrHybridFlow()) {
            ServicesLogger.LOGGER.flowNotAllowed("Implicit/Hybrid with DPoP");
            String errorMessage = "DPoP is not supported for implicit and hybrid flows. Client requires DPoP bound access tokens.";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.NOT_ALLOWED);
            throw new AuthorizationCheckException(Response.Status.UNAUTHORIZED, OAuthErrorException.UNAUTHORIZED_CLIENT, errorMessage);
        }
    }

    public boolean isInvalidResponseType(AuthorizationCheckException ex) {
        return "Missing parameter: response_type".equals(ex.getErrorDescription()) || OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE.equals(ex.getError());
    }

    public void checkInvalidRequestMessage() throws AuthorizationCheckException {
        if (request.getInvalidRequestMessage() != null) {
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, request.getInvalidRequestMessage());
        }
    }

    public void checkOIDCRequest() {
        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }
    }

    public void checkAuthorizationDetails() throws AuthorizationCheckException {
        String authDetailsParam = request.getAdditionalReqParams().get(AUTHORIZATION_DETAILS);
        if (authDetailsParam != null) {
            try {
                new AuthorizationDetailsProcessorManager(session).validateAuthorizationDetail(authDetailsParam);
            } catch (Exception e) {
                event.error(Errors.INVALID_REQUEST);
                throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, e.getMessage());
            }
        }
    }

    public void checkValidScope() throws AuthorizationCheckException {
        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            validScopes = TokenManager.isValidScope(session, request.getScope(), request.getAuthorizationRequestContext(), client, null);
        } else {
            validScopes = TokenManager.isValidScope(session, request.getScope(), client, null);
        }
        if (!validScopes) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.SCOPE_PARAM);
            String errorMessage = "Invalid scopes: " + request.getScope();
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_SCOPE, errorMessage);
        }
    }

    public void checkValidResource() throws AuthorizationCheckException {
        if (!ResourceIndicatorValidation.isValidResourceIndicator(request.getResource())) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.SCOPE_PARAM);
            String errorMessage = "Invalid resource: " + request.getResource();
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_INVALID_RESOURCE);
        }
    }

    public void checkOIDCParams() throws AuthorizationCheckException {
        // If request is not OIDC request, but pure OAuth2 request and response_type is just 'token', then 'nonce' is not mandatory
        boolean isOIDCRequest = TokenUtil.isOIDCRequest(request.getScope());
        if (!isOIDCRequest && parsedResponseType.toString().equals(OIDCResponseType.TOKEN)) {
            return;
        }

        if (parsedResponseType.hasResponseType(OIDCResponseType.ID_TOKEN) && request.getNonce() == null) {
            ServicesLogger.LOGGER.missingParameter(OIDCLoginProtocol.NONCE_PARAM);
            String errorMessage = "Missing parameter: " + OIDCLoginProtocol.NONCE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        return;
    }

    // https://tools.ietf.org/html/rfc7636#section-4
    public void checkPKCEParams() throws AuthorizationCheckException {
        String codeChallenge = request.getCodeChallenge();
        String codeChallengeMethod = request.getCodeChallengeMethod();

        // PKCE not adopted to OAuth2 Implicit Grant and OIDC Implicit Flow,
        // adopted to OAuth2 Authorization Code Grant and OIDC Authorization Code Flow, Hybrid Flow
        // Namely, flows using authorization code.
        if (parsedResponseType != null && parsedResponseType.isImplicitFlow()) return;

        String pkceCodeChallengeMethod = OIDCAdvancedConfigWrapper.fromClientModel(client).getPkceCodeChallengeMethod();

        if (pkceCodeChallengeMethod != null && !pkceCodeChallengeMethod.isEmpty()) {
            checkParamsForPkceEnforcedClient(codeChallengeMethod, pkceCodeChallengeMethod, codeChallenge);
        } else {
            // if PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            checkParamsForPkceNotEnforcedClient(codeChallengeMethod, pkceCodeChallengeMethod, codeChallenge);
        }
    }

    public void checkParRequired() throws AuthorizationCheckException {
        boolean isParRequired = realm.getParPolicy().isRequirePushedAuthorizationRequests(client);
        if (!isParRequired) {
            return;
        }
        String requestUriParam = params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);
        if (requestUriParam != null && getRequestUriType(requestUriParam) == RequestUriType.PAR) {
            return;
        }
        ServicesLogger.LOGGER.missingParameter(OIDCLoginProtocol.REQUEST_URI_PARAM);
        String errorMessage = "Pushed Authorization Request is only allowed.";
        event.detail(Details.REASON, errorMessage);
        event.error(Errors.INVALID_REQUEST);
        throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
    }

    public void checkParRedirectUri() throws AuthorizationCheckException {
        String requestUriParam = params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);
        if (requestUriParam != null && getRequestUriType(requestUriParam) == RequestUriType.PAR) {
            String requestRedirectUriParam = params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
            if (requestRedirectUriParam != null && request.getRedirectUriParam() == null) {
                String errorMessage = "PAR is required to have a 'redirect_uri' parameter";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
            }
        }
    }

    public void checkParDPoPParams() throws AuthorizationCheckException {
        DPoP dpop = session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, DPoP.class);
        if (dpop == null) {
            return;
        }
        if (request.getDpopJkt() != null) {
            if (!request.getDpopJkt().equals(dpop.getThumbprint())) {
                String errorMessage = "DPoP Proof public key thumbprint does not match dpop_jkt.";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
            }
        }
    }

    // https://tools.ietf.org/html/rfc7636#section-4
    private boolean isValidPkceCodeChallenge(String codeChallenge) {
        if (codeChallenge.length() < OIDCLoginProtocol.PKCE_CODE_CHALLENGE_MIN_LENGTH) {
            logger.debugf("PKCE codeChallenge length under lower limit , codeChallenge = %s", codeChallenge);
            return false;
        }
        if (codeChallenge.length() > OIDCLoginProtocol.PKCE_CODE_CHALLENGE_MAX_LENGTH) {
            logger.debugf("PKCE codeChallenge length over upper limit , codeChallenge = %s", codeChallenge);
            return false;
        }
        Matcher m = VALID_CODE_CHALLENGE_PATTERN.matcher(codeChallenge);
        return m.matches();
    }

    private void checkParamsForPkceEnforcedClient(String codeChallengeMethod, String pkceCodeChallengeMethod, String codeChallenge) throws AuthorizationCheckException {
        // check whether code challenge method is specified
        if (codeChallengeMethod == null) {
            logger.info("PKCE enforced Client without code challenge method.");
            String errorMessage = "Missing parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }
        // check whether specified code challenge method is configured one in advance
        if (!codeChallengeMethod.equals(pkceCodeChallengeMethod)) {
            logger.info("PKCE enforced Client code challenge method is not matching the configured one.");
            String errorMessage = "Invalid parameter: code challenge method is not matching the configured one";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }
        // check whether code challenge is specified
        if (codeChallenge == null) {
            logger.info("PKCE supporting Client without code challenge");
            String errorMessage = "Missing parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }
        // check whether code challenge is formatted along with the PKCE specification
        if (!isValidPkceCodeChallenge(codeChallenge)) {
            logger.infof("PKCE supporting Client with invalid code challenge specified in PKCE, codeChallenge = %s", codeChallenge);
            String errorMessage = "Invalid parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }
    }

    private void checkParamsForPkceNotEnforcedClient(String codeChallengeMethod, String pkceCodeChallengeMethod, String codeChallenge) throws AuthorizationCheckException {
        if (codeChallenge == null && codeChallengeMethod != null) {
            logger.info("PKCE supporting Client without code challenge");
            String errorMessage = "Missing parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST,  OAuthErrorException.INVALID_REQUEST, errorMessage);
        }

        // based on code_challenge value decide whether this client(RP) supports PKCE
        if (codeChallenge == null) {
            logger.debug("PKCE non-supporting Client");
            return;
        }

        if (codeChallengeMethod != null) {
            // https://tools.ietf.org/html/rfc7636#section-4.2
            // plain or S256
            if (!codeChallengeMethod.equals(OIDCLoginProtocol.PKCE_METHOD_S256) && !codeChallengeMethod.equals(OIDCLoginProtocol.PKCE_METHOD_PLAIN)) {
                logger.infof("PKCE supporting Client with invalid code challenge method not specified in PKCE, codeChallengeMethod = %s", codeChallengeMethod);
                String errorMessage = "Invalid parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM;
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
            }
        } else {
            // https://tools.ietf.org/html/rfc7636#section-4.3
            // default code_challenge_method is plane
            codeChallengeMethod = OIDCLoginProtocol.PKCE_METHOD_PLAIN;
        }

        if (!isValidPkceCodeChallenge(codeChallenge)) {
            logger.infof("PKCE supporting Client with invalid code challenge specified in PKCE, codeChallenge = %s", codeChallenge);
            String errorMessage = "Invalid parameter: " + OIDCLoginProtocol.CODE_CHALLENGE_PARAM;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorMessage);
        }
    }

    public void throwAsCorsErrorResponseException(Cors cors, AuthorizationCheckException ex) {
        event.detail("detail", ex.getErrorDescription()).error(ex.getError());
        throw new CorsErrorResponseException(cors, ex.getError(), ex.getErrorDescription(), ex.getStatus());
    }
}
