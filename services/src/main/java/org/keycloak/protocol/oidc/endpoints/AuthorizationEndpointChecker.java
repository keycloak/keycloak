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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
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
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.endpoints.request.RequestUriType;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.StringUtil;

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

    public void checkRedirectUri() throws AuthorizationCheckException {
        String redirectUriParam = request.getRedirectUriParam();
        boolean isOIDCRequest = TokenUtil.isOIDCRequest(request.getScope());

        event.detail(Details.REDIRECT_URI, redirectUriParam);

        // redirect_uri parameter is required per OpenID Connect, but optional per OAuth2
        this.redirectUri = RedirectUtils.verifyRedirectUri(session, redirectUriParam, client, isOIDCRequest);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
        }
    }


    public void checkResponseType() throws AuthorizationCheckException {
        String responseType = request.getResponseType();

        if (responseType == null) {
            ServicesLogger.LOGGER.missingParameter(OAuth2Constants.RESPONSE_TYPE);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Missing parameter: response_type");
        }

        event.detail(Details.RESPONSE_TYPE, responseType);

        try {
            this.parsedResponseType = OIDCResponseType.parse(responseType);
        } catch (IllegalArgumentException iae) {
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, null);
        }

        OIDCResponseMode parsedResponseMode = null;
        try {
            parsedResponseMode = OIDCResponseMode.parse(request.getResponseMode(), parsedResponseType);
        } catch (IllegalArgumentException iae) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: response_mode");
        }

        event.detail(Details.RESPONSE_MODE, parsedResponseMode.toString().toLowerCase());

        // Disallowed by OIDC specs
        if (parsedResponseType.isImplicitOrHybridFlow() && parsedResponseMode == OIDCResponseMode.QUERY) {
            ServicesLogger.LOGGER.responseModeQueryNotAllowed();
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Response_mode 'query' not allowed for implicit or hybrid flow");
        }

        this.parsedResponseMode = parsedResponseMode;

        if (parsedResponseType.isImplicitOrHybridFlow() && parsedResponseMode == OIDCResponseMode.QUERY_JWT &&
                (!StringUtil.isNotBlank(client.getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG)) ||
                !StringUtil.isNotBlank(client.getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC)))) {
            ServicesLogger.LOGGER.responseModeQueryJwtNotAllowed();
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Response_mode 'query.jwt' is allowed only when the authorization response token is encrypted");
        }

        if ((parsedResponseType.hasResponseType(OIDCResponseType.CODE) || parsedResponseType.hasResponseType(OIDCResponseType.NONE)) && !client.isStandardFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Standard");
            event.error(Errors.NOT_ALLOWED);
            throw new AuthorizationCheckException(Response.Status.UNAUTHORIZED, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.");
        }

        if (parsedResponseType.isImplicitOrHybridFlow() && !client.isImplicitFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Implicit");
            event.error(Errors.NOT_ALLOWED);
            throw new AuthorizationCheckException(Response.Status.UNAUTHORIZED, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.");
        }
    }

    public boolean isInvalidResponseType(AuthorizationEndpointChecker.AuthorizationCheckException ex) {
        return "Missing parameter: response_type".equals(ex.getErrorDescription()) || OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE.equals(ex.getError());
    }

    public void checkInvalidRequestMessage() throws AuthorizationCheckException {
        if (request.getInvalidRequestMessage() != null) {
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, Errors.INVALID_REQUEST, request.getInvalidRequestMessage());
        }
    }

    public void checkOIDCRequest() {
        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }
    }

    public void checkValidScope() throws AuthorizationCheckException {
        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            validScopes = TokenManager.isValidScope(request.getScope(), request.getAuthorizationRequestContext(), client);
        } else {
            validScopes = TokenManager.isValidScope(request.getScope(), client);
        }
        if (!validScopes) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.SCOPE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_SCOPE, "Invalid scopes: " + request.getScope());
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
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Missing parameter: nonce");
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
        if (requestUriParam != null && AuthorizationEndpointRequestParserProcessor.getRequestUriType(requestUriParam) == RequestUriType.PAR) {
            return;
        }
        ServicesLogger.LOGGER.missingParameter(OIDCLoginProtocol.REQUEST_URI_PARAM);
        event.error(Errors.INVALID_REQUEST);
        throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Pushed Authorization Request is only allowed.");
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
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge_method");
        }
        // check whether specified code challenge method is configured one in advance
        if (!codeChallengeMethod.equals(pkceCodeChallengeMethod)) {
            logger.info("PKCE enforced Client code challenge method is not configured one.");
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code challenge method is not configured one");
        }
        // check whether code challenge is specified
        if (codeChallenge == null) {
            logger.info("PKCE supporting Client without code challenge");
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge");
        }
        // check whether code challenge is formatted along with the PKCE specification
        if (!isValidPkceCodeChallenge(codeChallenge)) {
            logger.infof("PKCE supporting Client with invalid code challenge specified in PKCE, codeChallenge = %s", codeChallenge);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge");
        }
    }

    private void checkParamsForPkceNotEnforcedClient(String codeChallengeMethod, String pkceCodeChallengeMethod, String codeChallenge) throws AuthorizationCheckException {
        if (codeChallenge == null && codeChallengeMethod != null) {
            logger.info("PKCE supporting Client without code challenge");
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST,  OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge");
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
                event.error(Errors.INVALID_REQUEST);
                throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge_method");
            }
        } else {
            // https://tools.ietf.org/html/rfc7636#section-4.3
            // default code_challenge_method is plane
            codeChallengeMethod = OIDCLoginProtocol.PKCE_METHOD_PLAIN;
        }

        if (!isValidPkceCodeChallenge(codeChallenge)) {
            logger.infof("PKCE supporting Client with invalid code challenge specified in PKCE, codeChallenge = %s", codeChallenge);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge");
        }
    }


    // Exception propagated to the caller, which will allow caller to send proper error response based on the context (Browser OIDC Authorization Endpoint, PAR etc)
    public class AuthorizationCheckException extends Exception {

        private final Response.Status status;
        private final String error;
        private final String errorDescription;

        public AuthorizationCheckException(Response.Status status, String error, String errorDescription) {
            this.status = status;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public void throwAsErrorPageException(AuthenticationSessionModel authenticationSession) {
            throw new ErrorPageException(session, authenticationSession, status, error, errorDescription);
        }

        public void throwAsCorsErrorResponseException(Cors cors) {
            AuthorizationEndpointChecker.this.event.detail("detail", errorDescription).error(error);
            throw new CorsErrorResponseException(cors, error, errorDescription, status);
        }

        public String getError() {
            return error;
        }

        public String getErrorDescription() {
            return errorDescription;
        }
    }
}
