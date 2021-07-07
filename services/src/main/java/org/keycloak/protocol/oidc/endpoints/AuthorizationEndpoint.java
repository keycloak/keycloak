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
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.grants.device.endpoints.DeviceEndpoint;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationEndpoint extends AuthorizationEndpointBase {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);

    public static final String CODE_AUTH_TYPE = "code";

    /**
     * Prefix used to store additional HTTP GET params from original client request into {@link AuthenticationSessionModel} note to be available later in Authenticators, RequiredActions etc. Prefix is used to
     * prevent collisions with internally used notes.
     *
     * @see AuthenticationSessionModel#getClientNote(String)
     */
    public static final String LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX = "client_request_param_";

    private enum Action {
        REGISTER, CODE, FORGOT_CREDENTIALS
    }

    private ClientModel client;
    private AuthenticationSessionModel authenticationSession;

    private Action action;
    private OIDCResponseType parsedResponseType;
    private OIDCResponseMode parsedResponseMode;

    private AuthorizationEndpointRequest request;
    private String redirectUri;

    public AuthorizationEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
        event.event(EventType.LOGIN);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response buildPost() {
        logger.trace("Processing @POST request");
        return process(httpRequest.getDecodedFormParameters());
    }

    @GET
    public Response buildGet() {
        logger.trace("Processing @GET request");
        return process(session.getContext().getUri().getQueryParameters());
    }

    /**
     * OAuth 2.0 Device Authorization endpoint
     */
    @Path("device")
    public Object authorizeDevice() {
        DeviceEndpoint endpoint = new DeviceEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    private Response process(MultivaluedMap<String, String> params) {
        String clientId = AuthorizationEndpointRequestParserProcessor.getClientId(event, session, params);

        checkSsl();
        checkRealm();
        checkClient(clientId);

        request = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, params);

        AuthorizationEndpointChecker checker = new AuthorizationEndpointChecker()
                .event(event)
                .client(client)
                .realm(realm)
                .request(request)
                .session(session)
                .params(params);

        try {
            checker.checkRedirectUri();
            this.redirectUri = checker.getRedirectUri();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            ex.throwAsErrorPageException(authenticationSession);
        }

        try {
            checker.checkResponseType();
            this.parsedResponseType = checker.getParsedResponseType();
            this.parsedResponseMode = checker.getParsedResponseMode();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            OIDCResponseMode responseMode = checker.getParsedResponseMode() != null ? checker.getParsedResponseMode() : OIDCResponseMode.QUERY;
            return redirectErrorToClient(responseMode, ex.getError(), ex.getErrorDescription());
        }
        if (action == null) {
            action = AuthorizationEndpoint.Action.CODE;
        }

        try {
            checker.checkParRequired();
            checker.checkInvalidRequestMessage();
            checker.checkOIDCRequest();
            checker.checkValidScope();
            checker.checkOIDCParams();
            checker.checkPKCEParams();
        } catch (AuthorizationEndpointChecker.AuthorizationCheckException ex) {
            return redirectErrorToClient(parsedResponseMode, ex.getError(), ex.getErrorDescription());
        }

        try {
            session.clientPolicy().triggerOnEvent(new AuthorizationRequestContext(parsedResponseType, request, redirectUri, params));
        } catch (ClientPolicyException cpe) {
            return redirectErrorToClient(parsedResponseMode, cpe.getError(), cpe.getErrorDetail());
        }

        authenticationSession = createAuthenticationSession(client, request.getState());
        updateAuthenticationSession();

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
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.REGISTRATION_NOT_ALLOWED);
        }

        return this;
    }

    public AuthorizationEndpoint forgotCredentials() {
        event.event(EventType.RESET_PASSWORD);
        action = Action.FORGOT_CREDENTIALS;

        if (!realm.isResetPasswordAllowed()) {
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.RESET_CREDENTIAL_NOT_ALLOWED);
        }

        return this;
    }

    private void checkClient(String clientId) {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        event.client(clientId);

        client = realm.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.CLIENT_DISABLED);
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, authenticationSession, Response.Status.FORBIDDEN, Messages.BEARER_ONLY);
        }

        String protocol = client.getProtocol();
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration", clientId);
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }
        if (!protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorPageException(session, authenticationSession, Response.Status.BAD_REQUEST, "Wrong client protocol.");
        }

        session.getContext().setClient(client);
    }

    private Response redirectErrorToClient(OIDCResponseMode responseMode, String error, String errorDescription) {
        OIDCRedirectUriBuilder errorResponseBuilder = OIDCRedirectUriBuilder.fromUri(redirectUri, responseMode, session, null)
                .addParam(OAuth2Constants.ERROR, error);

        if (errorDescription != null) {
            errorResponseBuilder.addParam(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }

        if (request.getState() != null) {
            errorResponseBuilder.addParam(OAuth2Constants.STATE, request.getState());
        }

        return errorResponseBuilder.build();
    }

    private void updateAuthenticationSession() {
        authenticationSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authenticationSession.setRedirectUri(redirectUri);
        authenticationSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        authenticationSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, request.getResponseType());
        authenticationSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, request.getRedirectUriParam());
        authenticationSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        if (request.getState() != null) authenticationSession.setClientNote(OIDCLoginProtocol.STATE_PARAM, request.getState());
        if (request.getNonce() != null) authenticationSession.setClientNote(OIDCLoginProtocol.NONCE_PARAM, request.getNonce());
        if (request.getMaxAge() != null) authenticationSession.setClientNote(OIDCLoginProtocol.MAX_AGE_PARAM, String.valueOf(request.getMaxAge()));
        if (request.getScope() != null) authenticationSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, request.getScope());
        if (request.getLoginHint() != null) authenticationSession.setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, request.getLoginHint());
        if (request.getPrompt() != null) authenticationSession.setClientNote(OIDCLoginProtocol.PROMPT_PARAM, request.getPrompt());
        if (request.getIdpHint() != null) authenticationSession.setClientNote(AdapterConstants.KC_IDP_HINT, request.getIdpHint());
        if (request.getAction() != null) authenticationSession.setClientNote(Constants.KC_ACTION, request.getAction());
        if (request.getResponseMode() != null) authenticationSession.setClientNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM, request.getResponseMode());
        if (request.getClaims()!= null) authenticationSession.setClientNote(OIDCLoginProtocol.CLAIMS_PARAM, request.getClaims());
        if (request.getAcr() != null) authenticationSession.setClientNote(OIDCLoginProtocol.ACR_PARAM, request.getAcr());
        if (request.getDisplay() != null) authenticationSession.setAuthNote(OAuth2Constants.DISPLAY, request.getDisplay());
        if (request.getUiLocales() != null) authenticationSession.setAuthNote(LocaleSelectorProvider.CLIENT_REQUEST_LOCALE, request.getUiLocales());

        // https://tools.ietf.org/html/rfc7636#section-4
        if (request.getCodeChallenge() != null) authenticationSession.setClientNote(OIDCLoginProtocol.CODE_CHALLENGE_PARAM, request.getCodeChallenge());
        if (request.getCodeChallengeMethod() != null) authenticationSession.setClientNote(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM, request.getCodeChallengeMethod());

        if (request.getAdditionalReqParams() != null) {
            for (String paramName : request.getAdditionalReqParams().keySet()) {
                authenticationSession.setClientNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + paramName, request.getAdditionalReqParams().get(paramName));
            }
        }
    }

    private Response buildAuthorizationCodeAuthorizationResponse() {
        this.event.event(EventType.LOGIN);
        authenticationSession.setAuthNote(Details.AUTH_TYPE, CODE_AUTH_TYPE);

        return handleBrowserAuthenticationRequest(authenticationSession, new OIDCLoginProtocol(session, realm, session.getContext().getUri(), headers, event), TokenUtil.hasPrompt(request.getPrompt(), OIDCLoginProtocol.PROMPT_VALUE_NONE), false);
    }

    private Response buildRegister() {
        authManager.expireIdentityCookie(realm, session.getContext().getUri(), clientConnection);

        AuthenticationFlowModel flow = realm.getRegistrationFlow();
        String flowId = flow.getId();

        AuthenticationProcessor processor = createProcessor(authenticationSession, flowId, LoginActionsService.REGISTRATION_PATH);
        authenticationSession.setClientNote(APP_INITIATED_FLOW, LoginActionsService.REGISTRATION_PATH);

        return processor.authenticate();
    }

    private Response buildForgotCredential() {
        authManager.expireIdentityCookie(realm, session.getContext().getUri(), clientConnection);

        AuthenticationFlowModel flow = realm.getResetCredentialsFlow();
        String flowId = flow.getId();

        AuthenticationProcessor processor = createProcessor(authenticationSession, flowId, LoginActionsService.RESET_CREDENTIALS_PATH);
        authenticationSession.setClientNote(APP_INITIATED_FLOW, LoginActionsService.RESET_CREDENTIALS_PATH);

        return processor.authenticate();
    }

}
