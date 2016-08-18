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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationEndpoint extends AuthorizationEndpointBase {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static final String CODE_AUTH_TYPE = "code";

    /**
     * Prefix used to store additional HTTP GET params from original client request into {@link ClientSessionModel} note to be available later in Authenticators, RequiredActions etc. Prefix is used to
     * prevent collisions with internally used notes.
     * 
     * @see ClientSessionModel#getNote(String)
     * @see #KNOWN_REQ_PARAMS
     * @see #additionalReqParams
     */
    public static final String CLIENT_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX = "client_request_param_";
    /**
     * Max number of additional req params copied into client session note to prevent DoS attacks
     * 
     * @see #additionalReqParams
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_MUMBER = 5;
    /**
     * Max size of additional req param value copied into client session note to prevent DoS attacks - params with longer value are ignored
     * 
     * @see #additionalReqParams
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_SIZE = 200;

    /** Set of known protocol GET params not to be stored into {@link #additionalReqParams} */
    private static final Set<String> KNOWN_REQ_PARAMS = new HashSet<>();
    static {
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLIENT_ID_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.STATE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.SCOPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.PROMPT_PARAM);
        KNOWN_REQ_PARAMS.add(AdapterConstants.KC_IDP_HINT);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.NONCE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.UI_LOCALES_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_URI_PARAM);
    }

    private enum Action {
        REGISTER, CODE, FORGOT_CREDENTIALS
    }

    private ClientModel client;
    private ClientSessionModel clientSession;

    private Action action;
    private OIDCResponseType parsedResponseType;
    private OIDCResponseMode parsedResponseMode;

    private String clientId;
    private String redirectUri;
    private String redirectUriParam;
    private String responseType;
    private String responseMode;
    private String state;
    private String scope;
    private String loginHint;
    private String prompt;
    private String nonce;
    private String maxAge;
    private String idpHint;
    protected Map<String, String> additionalReqParams = new HashMap<>();

    public AuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        clientId = params.getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM);
        responseType = params.getFirst(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        responseMode = params.getFirst(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        redirectUriParam = params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        state = params.getFirst(OIDCLoginProtocol.STATE_PARAM);
        scope = params.getFirst(OIDCLoginProtocol.SCOPE_PARAM);
        loginHint = params.getFirst(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        prompt = params.getFirst(OIDCLoginProtocol.PROMPT_PARAM);
        idpHint = params.getFirst(AdapterConstants.KC_IDP_HINT);
        nonce = params.getFirst(OIDCLoginProtocol.NONCE_PARAM);
        maxAge = params.getFirst(OIDCLoginProtocol.MAX_AGE_PARAM);

        extractAdditionalReqParams(params);

        checkSsl();
        checkRealm();
        checkClient();
        checkRedirectUri();
        Response errorResponse = checkResponseType();
        if (errorResponse != null) {
            return errorResponse;
        }

        if (!TokenUtil.isOIDCRequest(scope)) {
            logger.oidcScopeMissing();
        }

        errorResponse = checkOIDCParams(params);
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

    protected void extractAdditionalReqParams(MultivaluedMap<String, String> params) {
        for (String paramName : params.keySet()) {
            if (!KNOWN_REQ_PARAMS.contains(paramName)) {
                String value = params.getFirst(paramName);
                if (value != null && value.trim().isEmpty()) {
                    value = null;
                }
                if (value != null && value.length() <= ADDITIONAL_REQ_PARAMS_MAX_SIZE) {
                    if (additionalReqParams.size() >= ADDITIONAL_REQ_PARAMS_MAX_MUMBER) {
                        logger.debug("Maximal number of additional OIDC params (" + ADDITIONAL_REQ_PARAMS_MAX_MUMBER + ") exceeded, ignoring rest of them!");
                        break;
                    }
                    additionalReqParams.put(paramName, value);
                } else {
                    logger.debug("OIDC Additional param " + paramName + " ignored because value is empty or longer than " + ADDITIONAL_REQ_PARAMS_MAX_SIZE);
                }
            }

        }
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

    private void checkClient() {
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
        if (responseType == null) {
            logger.missingParameter(OAuth2Constants.RESPONSE_TYPE);
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
            parsedResponseMode = OIDCResponseMode.parse(responseMode, parsedResponseType);
        } catch (IllegalArgumentException iae) {
            logger.invalidParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.INVALID_REQUEST, "Invalid parameter: response_mode");
        }

        event.detail(Details.RESPONSE_MODE, parsedResponseMode.toString().toLowerCase());

        // Disallowed by OIDC specs
        if (parsedResponseType.isImplicitOrHybridFlow() && parsedResponseMode == OIDCResponseMode.QUERY) {
            logger.responseModeQueryNotAllowed();
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(OIDCResponseMode.QUERY, OAuthErrorException.INVALID_REQUEST, "Response_mode 'query' not allowed for implicit or hybrid flow");
        }

        if ((parsedResponseType.hasResponseType(OIDCResponseType.CODE) || parsedResponseType.hasResponseType(OIDCResponseType.NONE)) && !client.isStandardFlowEnabled()) {
            logger.flowNotAllowed("Standard");
            event.error(Errors.NOT_ALLOWED);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, "Client is not allowed to initiate browser login with given response_type. Standard flow is disabled for the client.");
        }

        if (parsedResponseType.isImplicitOrHybridFlow() && !client.isImplicitFlowEnabled()) {
            logger.flowNotAllowed("Implicit");
            event.error(Errors.NOT_ALLOWED);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, "Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.");
        }

        this.parsedResponseMode = parsedResponseMode;

        return null;
    }

    private Response checkOIDCParams(MultivaluedMap<String, String> params) {
        if (params.getFirst(OIDCLoginProtocol.REQUEST_PARAM) != null) {
            logger.unsupportedParameter(OIDCLoginProtocol.REQUEST_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.REQUEST_NOT_SUPPORTED, null);
        }

        if (params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM) != null) {
            logger.unsupportedParameter(OIDCLoginProtocol.REQUEST_URI_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.REQUEST_URI_NOT_SUPPORTED, null);
        }

        if (parsedResponseType.isImplicitOrHybridFlow() && nonce == null) {
            logger.missingParameter(OIDCLoginProtocol.NONCE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            return redirectErrorToClient(parsedResponseMode, OAuthErrorException.INVALID_REQUEST, "Missing parameter: nonce");
        }

        return null;
    }

    private Response redirectErrorToClient(OIDCResponseMode responseMode, String error, String errorDescription) {
        OIDCRedirectUriBuilder errorResponseBuilder = OIDCRedirectUriBuilder.fromUri(redirectUri, responseMode)
                .addParam(OAuth2Constants.ERROR, error);

        if (errorDescription != null) {
            errorResponseBuilder.addParam(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }

        if (state != null) {
            errorResponseBuilder.addParam(OAuth2Constants.STATE, state);
        }

        return errorResponseBuilder.build();
    }

    private void checkRedirectUri() {
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
        clientSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, responseType);
        clientSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUriParam);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (nonce != null) clientSession.setNote(OIDCLoginProtocol.NONCE_PARAM, nonce);
        if (maxAge != null) clientSession.setNote(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        if (scope != null) clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        if (loginHint != null) clientSession.setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        if (prompt != null) clientSession.setNote(OIDCLoginProtocol.PROMPT_PARAM, prompt);
        if (idpHint != null) clientSession.setNote(AdapterConstants.KC_IDP_HINT, idpHint);
        if (responseMode != null) clientSession.setNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM, responseMode);

        if (additionalReqParams != null) {
            for (String paramName : additionalReqParams.keySet()) {
                clientSession.setNote(CLIENT_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + paramName, additionalReqParams.get(paramName));
            }
        }
    }

    private Response buildAuthorizationCodeAuthorizationResponse() {

        if (idpHint != null && !"".equals(idpHint)) {
            IdentityProviderModel identityProviderModel = realm.getIdentityProviderByAlias(idpHint);

            if (identityProviderModel == null) {
                return session.getProvider(LoginFormsProvider.class)
                        .setError(Messages.IDENTITY_PROVIDER_NOT_FOUND, idpHint)
                        .createErrorPage();
            }
            return buildRedirectToIdentityProvider(idpHint, new ClientSessionCode(realm, clientSession).getCode());
        }

        this.event.event(EventType.LOGIN);
        clientSession.setNote(Details.AUTH_TYPE, CODE_AUTH_TYPE);

        return handleBrowserAuthenticationRequest(clientSession, new OIDCLoginProtocol(session, realm, uriInfo, headers, event), TokenUtil.hasPrompt(prompt, OIDCLoginProtocol.PROMPT_VALUE_NONE), false);
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
