/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.GET;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationEndpoint extends AuthorizationEndpointBase {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);

    public static final String CODE_AUTH_TYPE = "code";

    /**
     * Prefix used to store additional HTTP GET params from original client request into {@link ClientSessionModel} note to be available later in Authenticators, RequiredActions etc. Prefix is used to
     * prevent collisions with internally used notes.
     *
     * @see ClientSessionModel#getNote(String)
     */
    public static final String CLIENT_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX = "client_request_param_";

    // https://tools.ietf.org/html/rfc7636#section-4.2
    private static final Pattern VALID_CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    private enum Action {
        REGISTER, CODE, FORGOT_CREDENTIALS
    }

    private ClientModel client;
    private ClientSessionModel clientSession;

    private Action action;
    private OIDCResponseType parsedResponseType;
    private OIDCResponseMode parsedResponseMode;

    private AuthorizationEndpointRequest request;
    private String redirectUri;

    public AuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String requestUri = uriInfo.getRequestUri().toString();
        String clientId = params.getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM);

        checkSsl();
        checkRealm();
        checkClient(clientId);

        request = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, params);

        checkRedirectUri();
        Response errorResponse = checkResponseType();
        if (errorResponse != null) {
            return errorResponse;
        }

        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }

        errorResponse = checkOIDCParams();
        if (errorResponse != null) {
            return errorResponse;
        }

        // https://tools.ietf.org/html/rfc7636#section-4
        errorResponse = checkPKCEParams();
        if (errorResponse != null) {
            return errorResponse;
        }

        createClientSession();
        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();
        switch (action) {
            case REGISTER:
                return buildRegister();
            case FORGOT_CREDENTIALS:
                return buildForgotCredential();
            case CODE:
                return buildAuthorizationCodeAuthorizationResponse();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    public AuthorizationEndpoint register() {
        event.event(EventType.REGISTER);
        action = Action.REGISTER;

        if (!realm.isRegistrationAllowed()) {
            throw new ErrorPageException(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        return this;
    }

    public AuthorizationEndpoint forgotCredentials() {
        event.event(EventType.RESET_PASSWORD);
        action = Action.FORGOT_CREDENTIALS;

        if (!realm.isResetPasswordAllowed()) {
            throw new ErrorPageException(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);
        }

        return this;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, Messages.HTTPS_REQUIRED);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, Messages.REALM_NOT_ENABLED);
        }
    }

    private void checkClient(String clientId) {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        event.client(clientId);

        client = realm.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, Messages.CLIENT_DISABLED);
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, Messages.BEARER_ONLY);
        }

        session.getContext().setClient(client);
    }

    private Response checkResponseType() {
        String responseType = request.getResponseType();

        if (responseType == null) {
            ServicesLogger.LOGGER.missingParameter(OAuth2Constants.RESPONSE_TYPE);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.INVALID_REQUEST, "Missing parameter: response_type");
        }

        event.detail(Details.RESPONSE_TYPE, responseType);

        try {
            parsedResponseType = OIDCResponseType.parse(responseType);
            if (action == null) {
                action = Action.CODE;
            }
        } catch (IllegalArgumentException iae) {
            logger.error(iae.getMessage());
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, null);
        }

        OIDCResponseMode parsedResponseMode = null;
        try {
            parsedResponseMode = OIDCResponseMode.parse(request.getResponseMode(), parsedResponseType);
        } catch (IllegalArgumentException iae) {
            ServicesLogger.LOGGER.invalidParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: response_mode");
        }

        event.detail(Details.RESPONSE_MODE, parsedResponseMode.toString().toLowerCase());

        // Disallowed by OIDC specs
        if (parsedResponseType.isImplicitOrHybridFlow() && parsedResponseMode == OIDCResponseMode.QUERY) {
            ServicesLogger.LOGGER.responseModeQueryNotAllowed();
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.INVALID_REQUEST, "Response_mode 'query' not allowed for implicit or hybrid flow");
        }

        if ((parsedResponseType.hasResponseType(OIDCResponseType.CODE) || parsedResponseType.hasResponseType(OIDCResponseType.NONE)) && !client.isStandardFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Standard");
            event.error(Errors.NOT_ALLOWED);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.");
        }

        if (parsedResponseType.isImplicitOrHybridFlow() && !client.isImplicitFlowEnabled()) {
            ServicesLogger.LOGGER.flowNotAllowed("Implicit");
            event.error(Errors.NOT_ALLOWED);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, "Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.");
        }

        this.parsedResponseMode = parsedResponseMode;

        return null;
    }

    private Response checkOIDCParams() {
        if (parsedResponseType.isImplicitOrHybridFlow() && request.getNonce() == null) {
            ServicesLogger.LOGGER.missingParameter(OIDCLoginProtocol.NONCE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.INVALID_REQUEST, "Missing parameter: nonce");
        }

        return null;
    }

    // https://tools.ietf.org/html/rfc7636#section-4
    private Response checkPKCEParams() {
        String codeChallenge = request.getCodeChallenge();
        String codeChallengeMethod = request.getCodeChallengeMethod();
        
        // PKCE not adopted to OAuth2 Implicit Grant and OIDC Implicit Flow,
        // adopted to OAuth2 Authorization Code Grant and OIDC Authorization Code Flow, Hybrid Flow
        // Namely, flows using authorization code.
        if (parsedResponseType.isImplicitFlow()) return null;
        
        if (codeChallenge == null && codeChallengeMethod != null) {
            logger.info("PKCE supporting Client without code challenge");
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.INVALID_REQUEST, "Missing parameter: code_challenge");
        }
        
        // based on code_challenge value decide whether this client(RP) supports PKCE
        if (codeChallenge == null) {
            logger.debug("PKCE non-supporting Client");
            return null;
        }
        
        if (codeChallengeMethod != null) {
        	// https://tools.ietf.org/html/rfc7636#section-4.2
        	// plain or S256
            if (!codeChallengeMethod.equals(OIDCLoginProtocol.PKCE_METHOD_S256) && !codeChallengeMethod.equals(OIDCLoginProtocol.PKCE_METHOD_PLAIN)) {
                logger.infof("PKCE supporting Client with invalid code challenge method not specified in PKCE, codeChallengeMethod = %s", codeChallengeMethod);
                event.error(Errors.INVALID_REQUEST);
                return redirectErrorToClient(parsedResponseMode, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge_method");
            }
        } else {
        	// https://tools.ietf.org/html/rfc7636#section-4.3
        	// default code_challenge_method is plane
        	codeChallengeMethod = OIDCLoginProtocol.PKCE_METHOD_PLAIN;
        }
        
        if (!isValidPkceCodeChallenge(codeChallenge)) {
            logger.infof("PKCE supporting Client with invalid code challenge specified in PKCE, codeChallenge = %s", codeChallenge);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: code_challenge");
        }
        
        return null;
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
       return m.matches() ? true : false;
    }

    private Response redirectErrorToClient(OIDCResponseMode responseMode, String error, String errorDescription) {
        OIDCRedirectUriBuilder errorResponseBuilder = OIDCRedirectUriBuilder.fromUri(redirectUri, responseMode)
                .addParam(OAuth2Constants.ERROR, error);

        if (errorDescription != null) {
            errorResponseBuilder.addParam(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }

        if (request.getState() != null) {
            errorResponseBuilder.addParam(OAuth2Constants.STATE, request.getState());
        }

        return errorResponseBuilder.build();
    }

    private void checkRedirectUri() {
        String redirectUriParam = request.getRedirectUriParam();

        event.detail(Details.REDIRECT_URI, redirectUriParam);

        redirectUri = RedirectUtils.verifyRedirectUri(uriInfo, redirectUriParam, realm, client);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
        }
    }

    private void createClientSession() {
        clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirectUri);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        clientSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, request.getResponseType());
        clientSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, request.getRedirectUriParam());
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        if (request.getState() != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, request.getState());
        if (request.getNonce() != null) clientSession.setNote(OIDCLoginProtocol.NONCE_PARAM, request.getNonce());
        if (request.getMaxAge() != null) clientSession.setNote(OIDCLoginProtocol.MAX_AGE_PARAM, String.valueOf(request.getMaxAge()));
        if (request.getScope() != null) clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, request.getScope());
        if (request.getLoginHint() != null) clientSession.setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, request.getLoginHint());
        if (request.getPrompt() != null) clientSession.setNote(OIDCLoginProtocol.PROMPT_PARAM, request.getPrompt());
        if (request.getIdpHint() != null) clientSession.setNote(AdapterConstants.KC_IDP_HINT, request.getIdpHint());
        if (request.getResponseMode() != null) clientSession.setNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM, request.getResponseMode());

        // https://tools.ietf.org/html/rfc7636#section-4
        if (request.getCodeChallenge() != null) clientSession.setNote(OIDCLoginProtocol.CODE_CHALLENGE_PARAM, request.getCodeChallenge());
        if (request.getCodeChallengeMethod() != null) {
        	clientSession.setNote(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM, request.getCodeChallengeMethod());
        } else {
        	clientSession.setNote(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM, OIDCLoginProtocol.PKCE_METHOD_PLAIN);
        }

        if (request.getAdditionalReqParams() != null) {
            for (String paramName : request.getAdditionalReqParams().keySet()) {
                clientSession.setNote(CLIENT_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + paramName, request.getAdditionalReqParams().get(paramName));
            }
        }
    }

    private Response buildAuthorizationCodeAuthorizationResponse() {
        this.event.event(EventType.LOGIN);
        clientSession.setNote(Details.AUTH_TYPE, CODE_AUTH_TYPE);

        return handleBrowserAuthenticationRequest(clientSession, new OIDCLoginProtocol(session, realm, uriInfo, headers, event), TokenUtil.hasPrompt(request.getPrompt(), OIDCLoginProtocol.PROMPT_VALUE_NONE), false);
    }

    private Response buildRegister() {
        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        AuthenticationFlowModel flow = realm.getRegistrationFlow();
        String flowId = flow.getId();

        AuthenticationProcessor processor = createProcessor(clientSession, flowId, LoginActionsService.REGISTRATION_PATH);

        return processor.authenticate();
    }

    private Response buildForgotCredential() {
        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        AuthenticationFlowModel flow = realm.getResetCredentialsFlow();
        String flowId = flow.getId();

        AuthenticationProcessor processor = createProcessor(clientSession, flowId, LoginActionsService.RESET_CREDENTIALS_PATH);

        return processor.authenticate();
    }

}
