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
package org.keycloak.services.resources;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsServiceChecks.RestartFlowException;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.PageExpiredRedirect;
import org.keycloak.services.util.BrowserHistoryHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import javax.ws.rs.core.*;
import static org.keycloak.authentication.actiontoken.DefaultActionToken.ACTION_TOKEN_BASIC_CHECKS;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    private static final Logger logger = Logger.getLogger(LoginActionsService.class);

    public static final String AUTHENTICATE_PATH = "authenticate";
    public static final String REGISTRATION_PATH = "registration";
    public static final String RESET_CREDENTIALS_PATH = "reset-credentials";
    public static final String REQUIRED_ACTION = "required-action";
    public static final String FIRST_BROKER_LOGIN_PATH = "first-broker-login";
    public static final String POST_BROKER_LOGIN_PATH = "post-broker-login";

    public static final String RESTART_PATH = "restart";

    public static final String FORWARDED_ERROR_MESSAGE_NOTE = "forwardedErrorMessage";

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    @Context
    protected Providers providers;

    @Context
    protected KeycloakSession session;

    private EventBuilder event;

    public static UriBuilder loginActionsBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return loginActionsBaseUrl(baseUriBuilder);
    }

    public static UriBuilder authenticationFormProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "authenticateForm");
    }

    public static UriBuilder requiredActionProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "requiredActionPOST");
    }

    public static UriBuilder actionTokenProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "executeActionToken");
    }

    public static UriBuilder registrationFormProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "processRegister");
    }

    public static UriBuilder firstBrokerLoginProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "firstBrokerLoginGet");
    }

    public static UriBuilder postBrokerLoginProcessor(UriInfo uriInfo) {
        return loginActionsBaseUrl(uriInfo).path(LoginActionsService.class, "postBrokerLoginGet");
    }

    public static UriBuilder loginActionsBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getLoginActionsService");
    }

    public LoginActionsService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
        CacheControlUtil.noBackButtonCacheControlHeader();
    }

    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }

    private SessionCodeChecks checksForCode(String code, String execution, String flowPath) {
        SessionCodeChecks res = new SessionCodeChecks(code, execution, flowPath);
        res.initialVerify();
        return res;
    }

    private SessionCodeChecks checksForCodeRefreshNotAllowed(String code, String execution, String flowPath) {
        SessionCodeChecks res = new SessionCodeChecks(code, execution, flowPath);
        res.setAllowRefresh(false);
        res.initialVerify();
        return res;
    }



    private class SessionCodeChecks {
        ClientSessionCode<AuthenticationSessionModel> clientCode;
        Response response;
        ClientSessionCode.ParseResult<AuthenticationSessionModel> result;
        private boolean actionRequest;
        private boolean allowRefresh = true;

        private final String code;
        private final String execution;
        private final String flowPath;

        public SessionCodeChecks(String code, String execution, String flowPath) {
            this.code = code;
            this.execution = execution;
            this.flowPath = flowPath;
        }

        public AuthenticationSessionModel getAuthenticationSession() {
            return clientCode == null ? null : clientCode.getClientSession();
        }

        public boolean passed() {
            return response == null;
        }

        public boolean failed() {
            return response != null;
        }

        public boolean isAllowRefresh() {
            return allowRefresh;
        }

        public void setAllowRefresh(boolean allowRefresh) {
            this.allowRefresh = allowRefresh;
        }


        boolean verifyCode(String expectedAction, ClientSessionCode.ActionType actionType) {
            if (failed()) {
                return false;
            }

            if (!isActionActive(actionType)) {
                return false;
            }

            if (!clientCode.isValidAction(expectedAction)) {
                AuthenticationSessionModel authSession = getAuthenticationSession();
                if (ClientSessionModel.Action.REQUIRED_ACTIONS.name().equals(authSession.getAction())) {
                    // TODO:mposolda debug or trace
                    logger.info("Incorrect flow '%s' . User authenticated already. Redirecting to requiredActions now.");
                    response = redirectToRequiredActions(null);
                    return false;
                } else {
                    // TODO:mposolda could this happen? Doublecheck if we use other AuthenticationSession.Action besides AUTHENTICATE and REQUIRED_ACTIONS
                    logger.errorf("Bad action. Expected action '%s', current action '%s'", expectedAction, authSession.getAction());
                    response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                    return false;
                }
            }

            return true;
        }


        private boolean isActionActive(ClientSessionCode.ActionType actionType) {
            if (!clientCode.isActionActive(actionType)) {
                event.client(getAuthenticationSession().getClient());
                event.clone().error(Errors.EXPIRED_CODE);

                AuthenticationSessionModel authSession = getAuthenticationSession();
                AuthenticationProcessor.resetFlow(authSession, AUTHENTICATE_PATH);
                response = processAuthentication(false, null, authSession, Messages.LOGIN_TIMEOUT);
                return false;
            }
            return true;
        }


        private AuthenticationSessionModel initialVerifyAuthSession() {
            // Basic realm checks
            if (!checkSsl()) {
                event.error(Errors.SSL_REQUIRED);
                response = ErrorPage.error(session, Messages.HTTPS_REQUIRED);
                return null;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
                return null;
            }

            // object retrieve
            AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(code, session, realm, AuthenticationSessionModel.class);
            if (authSession != null) {
                return authSession;
            }

            // See if we are already authenticated and userSession with same ID exists.
            String sessionId = new AuthenticationSessionManager(session).getCurrentAuthenticationSessionId(realm);
            if (sessionId != null) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
                if (userSession != null) {

                    LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class)
                            .setSuccess(Messages.ALREADY_LOGGED_IN);

                    ClientModel client = null;
                    String lastClientUuid = userSession.getNote(AuthenticationManager.LAST_AUTHENTICATED_CLIENT);
                    if (lastClientUuid != null) {
                        client = realm.getClientById(lastClientUuid);
                    }

                    if (client != null) {
                        session.getContext().setClient(client);
                    } else {
                        loginForm.setAttribute("skipLink", true);
                    }

                    response = loginForm.createInfoPage();
                    return null;
                }
            }

            // Otherwise just try to restart from the cookie
            response = restartAuthenticationSessionFromCookie();
            return null;
        }


        private boolean initialVerify() {
            // Basic realm checks and authenticationSession retrieve
            AuthenticationSessionModel authSession = initialVerifyAuthSession();
            if (authSession == null) {
                return false;
            }

            // Check cached response from previous action request
            response = BrowserHistoryHelper.getInstance().loadSavedResponse(session, authSession);
            if (response != null) {
                return false;
            }

            // Client checks
            event.detail(Details.CODE_ID, authSession.getId());
            ClientModel client = authSession.getClient();
            if (client == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                response = ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER);
                clientCode.removeExpiredClientSession();
                return false;
            }
            if (!client.isEnabled()) {
                event.error(Errors.CLIENT_DISABLED);
                response = ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED);
                clientCode.removeExpiredClientSession();
                return false;
            }
            session.getContext().setClient(client);


            // Check if it's action or not
            if (code == null) {
                String lastExecFromSession = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                String lastFlow = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);

                // Check if we transitted between flows (eg. clicking "register" on login screen)
                if (execution==null && !flowPath.equals(lastFlow)) {
                    logger.infof("Transition between flows! Current flow: %s, Previous flow: %s", flowPath, lastFlow);

                    if (lastFlow == null || isFlowTransitionAllowed(flowPath, lastFlow)) {
                        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);
                        authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                        lastExecFromSession = null;
                    }
                }

                if (ObjectUtil.isEqualOrBothNull(execution, lastExecFromSession)) {
                    // Allow refresh of previous page
                    clientCode = new ClientSessionCode<>(session, realm, authSession);
                    actionRequest = false;
                    return true;
                } else {
                    response = showPageExpired(authSession);
                    return false;
                }
            } else {
                result = ClientSessionCode.parseResult(code, session, realm, AuthenticationSessionModel.class);
                clientCode = result.getCode();
                if (clientCode == null) {

                    // In case that is replayed action, but sent to the same FORM like actual FORM, we just re-render the page
                    if (allowRefresh && ObjectUtil.isEqualOrBothNull(execution, authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION))) {
                        String latestFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
                        URI redirectUri = getLastExecutionUrl(latestFlowPath, execution);

                        logger.infof("Invalid action code, but execution matches. So just redirecting to %s", redirectUri);
                        authSession.setAuthNote(FORWARDED_ERROR_MESSAGE_NOTE, Messages.EXPIRED_ACTION);
                        response = Response.status(Response.Status.FOUND).location(redirectUri).build();
                    } else {
                        response = showPageExpired(authSession);
                    }
                    return false;
                }


                actionRequest = execution != null;
                authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, execution);
                return true;
            }
        }


        public boolean verifyRequiredAction(String executedAction) {
            if (failed()) {
                return false;
            }

            if (!clientCode.isValidAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name())) {
                // TODO:mposolda debug or trace
                logger.infof("Expected required action, but session action is '%s' . Showing expired page now.", getAuthenticationSession().getAction());
                event.client(getAuthenticationSession().getClient());
                event.error(Errors.INVALID_CODE);

                response = showPageExpired(getAuthenticationSession());

                return false;
            }

            if (!isActionActive(ClientSessionCode.ActionType.USER)) return false;

            final AuthenticationSessionModel authSession = getAuthenticationSession();

            if (actionRequest) {
                String currentRequiredAction = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                if (executedAction == null || !executedAction.equals(currentRequiredAction)) {
                    logger.debug("required action doesn't match current required action");
                    response = redirectToRequiredActions(currentRequiredAction);
                    return false;
                }
            }
            return true;
        }
    }


    public static boolean isFlowTransitionAllowed(String currentFlow, String previousFlow) {
        if (currentFlow.equals(AUTHENTICATE_PATH) && (previousFlow.equals(REGISTRATION_PATH) || previousFlow.equals(RESET_CREDENTIALS_PATH))) {
            return true;
        }

        if (currentFlow.equals(REGISTRATION_PATH) && (previousFlow.equals(AUTHENTICATE_PATH))) {
            return true;
        }

        if (currentFlow.equals(RESET_CREDENTIALS_PATH) && (previousFlow.equals(AUTHENTICATE_PATH) || previousFlow.equals(FIRST_BROKER_LOGIN_PATH))) {
            return true;
        }

        if (currentFlow.equals(FIRST_BROKER_LOGIN_PATH) && (previousFlow.equals(AUTHENTICATE_PATH) || previousFlow.equals(POST_BROKER_LOGIN_PATH))) {
            return true;
        }

        if (currentFlow.equals(POST_BROKER_LOGIN_PATH) && (previousFlow.equals(AUTHENTICATE_PATH) || previousFlow.equals(FIRST_BROKER_LOGIN_PATH))) {
            return true;
        }

        return false;
    }


    protected Response restartAuthenticationSessionFromCookie() {
        logger.info("Authentication session not found. Trying to restart from cookie.");
        AuthenticationSessionModel authSession = null;
        try {
            authSession = RestartLoginCookie.restartSession(session, realm);
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToParseRestartLoginCookie(e);
        }

        if (authSession != null) {

            event.clone();
            event.detail(Details.RESTART_AFTER_TIMEOUT, "true");
            event.error(Errors.EXPIRED_CODE);

            String warningMessage = Messages.LOGIN_TIMEOUT;
            authSession.setAuthNote(FORWARDED_ERROR_MESSAGE_NOTE, warningMessage);

            String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
            if (flowPath == null) {
                flowPath = AUTHENTICATE_PATH;
            }

            URI redirectUri = getLastExecutionUrl(flowPath, null);
            logger.infof("Authentication session restart from cookie succeeded. Redirecting to %s", redirectUri);
            return Response.status(Response.Status.FOUND).location(redirectUri).build();
        } else {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }
    }


    protected Response showPageExpired(AuthenticationSessionModel authSession) {
        return new PageExpiredRedirect(session, realm, uriInfo)
                .showPageExpired(authSession);
    }


    protected URI getLastExecutionUrl(String flowPath, String executionId) {
        return new PageExpiredRedirect(session, realm, uriInfo)
                .getLastExecutionUrl(flowPath, executionId);
    }


    /**
     * protocol independent page for restart of the flow
     *
     * @return
     */
    @Path(RESTART_PATH)
    @GET
    public Response restartSession() {
        event.event(EventType.RESTART_AUTHENTICATION);
        SessionCodeChecks checks = new SessionCodeChecks(null, null, null);

        AuthenticationSessionModel authSession = checks.initialVerifyAuthSession();
        if (authSession == null) {
            return checks.response;
        }

        String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
        if (flowPath == null) {
            flowPath = AUTHENTICATE_PATH;
        }

        AuthenticationProcessor.resetFlow(authSession, flowPath);

        // TODO:mposolda Check it's better to put it to AuthenticationProcessor.resetFlow (with consider of brokering)
        authSession.setAction(CommonClientSessionModel.Action.AUTHENTICATE.name());

        URI redirectUri = getLastExecutionUrl(flowPath, null);
        logger.infof("Flow restart requested. Redirecting to %s", redirectUri);
        return Response.status(Response.Status.FOUND).location(redirectUri).build();
    }


    /**
     * protocol independent login page entry point
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @GET
    public Response authenticate(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        event.event(EventType.LOGIN);

        SessionCodeChecks checks = checksForCode(code, execution, AUTHENTICATE_PATH);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();
        boolean actionRequest = checks.actionRequest;

        return processAuthentication(actionRequest, execution, authSession, null);
    }

    protected Response processAuthentication(boolean action, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        return processFlow(action, execution, authSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), errorMessage, new AuthenticationProcessor());
    }

    protected Response processFlow(boolean action, String execution, AuthenticationSessionModel authSession, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor) {
        processor.setAuthenticationSession(authSession)
                .setFlowPath(flowPath)
                .setBrowserFlow(true)
                .setFlowId(flow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        if (errorMessage != null) {
            processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));
        }

        // Check the forwarded error message, which was set by previous HTTP request
        String forwardedErrorMessage = authSession.getAuthNote(FORWARDED_ERROR_MESSAGE_NOTE);
        if (forwardedErrorMessage != null) {
            authSession.removeAuthNote(FORWARDED_ERROR_MESSAGE_NOTE);
            processor.setForwardedErrorMessage(new FormMessage(null, forwardedErrorMessage));
        }


        Response response;
        try {
            if (action) {
                response = processor.authenticationAction(execution);
            } else {
                response = processor.authenticate();
            }
        } catch (Exception e) {
            response = processor.handleBrowserException(e);
            authSession = processor.getAuthenticationSession(); // Could be changed (eg. Forked flow)
        }

        return BrowserHistoryHelper.getInstance().saveResponseAndRedirect(session, authSession, response, action);
    }

    /**
     * URL called after login page.  YOU SHOULD NEVER INVOKE THIS DIRECTLY!
     *
     * @param code
     * @return
     */
    @Path(AUTHENTICATE_PATH)
    @POST
    public Response authenticateForm(@QueryParam("code") String code,
                                     @QueryParam("execution") String execution) {
        return authenticate(code, execution);
    }

    @Path(RESET_CREDENTIALS_PATH)
    @POST
    public Response resetCredentialsPOST(@QueryParam("code") String code,
                                         @QueryParam("execution") String execution,
                                         @QueryParam(Constants.KEY) String key) {
        if (key != null) {
            return handleActionToken(key, execution);
        }

        event.event(EventType.RESET_PASSWORD);

        return resetCredentials(code, execution);
    }

    /**
     * Endpoint for executing reset credentials flow.  If token is null, a client session is created with the account
     * service as the client.  Successful reset sends you to the account page.  Note, account service must be enabled.
     *
     * @param code
     * @param execution
     * @return
     */
    @Path(RESET_CREDENTIALS_PATH)
    @GET
    public Response resetCredentialsGET(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).getCurrentAuthenticationSession(realm);

        // we allow applications to link to reset credentials without going through OAuth or SAML handshakes
        if (authSession == null && code == null) {
            if (!realm.isResetPasswordAllowed()) {
                event.event(EventType.RESET_PASSWORD);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

            }
            authSession = createAuthenticationSessionForClient(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            return processResetCredentials(false, null, authSession);
        }

        event.event(EventType.RESET_PASSWORD);
        return resetCredentials(code, execution);
    }

    AuthenticationSessionModel createAuthenticationSessionForClient(String clientId)
      throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        // set up the account service as the endpoint to call.
        ClientModel client = realm.getClientByClientId(clientId == null ? Constants.ACCOUNT_MANAGEMENT_CLIENT_ID : clientId);
        authSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, client, true);
        authSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        //authSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String redirectUri = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString(); // TODO:mposolda It seems that this should be taken from client rather then hardcoded to account?
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        return authSession;
    }

    /**
     * @param code
     * @param execution
     * @return
     */
    protected Response resetCredentials(String code, String execution) {
        SessionCodeChecks checks = checksForCode(code, execution, RESET_CREDENTIALS_PATH);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.USER)) {
            return checks.response;
        }
        final AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        if (!realm.isResetPasswordAllowed()) {
            if (authSession != null) {
                event.client(authSession.getClient());
            }
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

        }

        return processResetCredentials(checks.actionRequest, execution, authSession);
    }

    /**
     * Handles a given token using the given token handler. If there is any {@link VerificationException} thrown
     * in the handler, it is handled automatically here to reduce boilerplate code.
     *
     * @param tokenString Original token string
     * @param eventError
     * @param defaultErrorMessage
     * @return
     */
    @Path("action-token")
    @GET
    public Response executeActionToken(@QueryParam("key") String key,
                                       @QueryParam("execution") String execution) {
        return handleActionToken(key, execution);
    }

    protected <T extends DefaultActionToken> Response handleActionToken(String tokenString, String execution) {
        T token;
        ActionTokenHandler<T> handler;
        ActionTokenContext<T> tokenContext;
        String eventError = null;
        String defaultErrorMessage = null;
        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).getCurrentAuthenticationSession(realm);

        event.event(EventType.EXECUTE_ACTION_TOKEN);

        // First resolve action token handler
        try {
            if (tokenString == null) {
                throw new ExplainedTokenVerificationException(null, Errors.NOT_ALLOWED, Messages.INVALID_REQUEST);
            }

            TokenVerifier<DefaultActionToken> tokenVerifier = TokenVerifier.create(tokenString, DefaultActionToken.class);
            DefaultActionToken aToken = tokenVerifier.getToken();

            event
              .detail(Details.TOKEN_ID, aToken.getId())
              .detail(Details.ACTION, aToken.getActionId())
              .user(aToken.getUserId());

            handler = resolveActionTokenHandler(aToken.getActionId());
            eventError = handler.getDefaultEventError();
            defaultErrorMessage = handler.getDefaultErrorMessage();

            if (! realm.isEnabled()) {
                throw new ExplainedTokenVerificationException(aToken, Errors.REALM_DISABLED, Messages.REALM_NOT_ENABLED);
            }
            if (! checkSsl()) {
                throw new ExplainedTokenVerificationException(aToken, Errors.SSL_REQUIRED, Messages.HTTPS_REQUIRED);
            }

            tokenVerifier
              .withChecks(
                // Token introspection checks
                TokenVerifier.IS_ACTIVE,
                new TokenVerifier.RealmUrlCheck(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName())),
                ACTION_TOKEN_BASIC_CHECKS
              )

              .secretKey(session.keys().getActiveHmacKey(realm).getSecretKey())
              .verify();

            // TODO:hmlnarik Optimize
            token = TokenVerifier.create(tokenString, handler.getTokenClass()).getToken();
        } catch (TokenNotActiveException ex) {
            if (authSession != null) {
                event.clone().error(Errors.EXPIRED_CODE);
                String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
                if (flowPath == null) {
                    flowPath = AUTHENTICATE_PATH;
                }
                AuthenticationProcessor.resetFlow(authSession, flowPath);
                return processAuthentication(false, null, authSession, Messages.LOGIN_TIMEOUT);
            }

            return handleActionTokenVerificationException(null, ex, Errors.EXPIRED_CODE, defaultErrorMessage);
        } catch (ExplainedTokenVerificationException ex) {
            return handleActionTokenVerificationException(null, ex, ex.getErrorEvent(), ex.getMessage());
        } catch (VerificationException ex) {
            return handleActionTokenVerificationException(null, ex, eventError, defaultErrorMessage);
        }

        // Now proceed with the verification and handle the token
        tokenContext = new ActionTokenContext(session, realm, uriInfo, clientConnection, request, event, handler);

        try {
            tokenContext.setExecutionId(execution);

            String tokenAuthSessionId = handler.getAuthenticationSessionIdFromToken(token);
            if (authSession == null) {
                if (tokenAuthSessionId != null) {
                    // This can happen if the token contains ID but user opens the link in a new browser
                    LoginActionsServiceChecks.checkNotLoggedInYet(tokenContext, tokenAuthSessionId);
                }

                authSession = handler.startFreshAuthenticationSession(token, tokenContext);
                tokenContext.setAuthenticationSession(authSession, true);

                initLoginEvent(authSession);
                event.event(handler.eventType());
            } else {
                initLoginEvent(authSession);
                event.event(handler.eventType());

                if (tokenAuthSessionId == null) {
                    // There exists an authentication session but no auth session ID was received in the action token
                    logger.debugf("Authentication session exists while reauthentication was requested by using action token %s, restarting.", token.getId());
                    new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authSession, false);

                    authSession = handler.startFreshAuthenticationSession(token, tokenContext);
                    tokenContext.setAuthenticationSession(authSession, true);
                } else {
                    LoginActionsServiceChecks.checkNotLoggedInYet(tokenContext, tokenAuthSessionId);
                    LoginActionsServiceChecks.checkAuthenticationSessionFromCookieMatchesOneFromToken(tokenContext, tokenAuthSessionId);
                }
            }

            LoginActionsServiceChecks.checkIsUserValid(token, tokenContext);
            LoginActionsServiceChecks.checkIsClientValid(token, tokenContext);
            
            session.getContext().setClient(authSession.getClient());

            TokenVerifier.create(token)
              .withChecks(handler.getVerifiers(tokenContext))
              .verify();

            authSession = tokenContext.getAuthenticationSession();
            event = tokenContext.getEvent();

            initLoginEvent(authSession);

            authSession.setAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID, token.getUserId());

            return handler.handleToken(token, tokenContext, this::processFlow);
        } catch (ExplainedTokenVerificationException ex) {
            return handleActionTokenVerificationException(tokenContext, ex, ex.getErrorEvent(), ex.getMessage());
        } catch (RestartFlowException ex) {
            Response response = handler.handleRestartRequest(token, tokenContext, this::processFlow);
            return response == null
              ? handleActionTokenVerificationException(tokenContext, ex, eventError, defaultErrorMessage)
              : response;
        } catch (LoginActionsServiceException ex) {
            Response response = ex.getResponse();
            return response == null
              ? handleActionTokenVerificationException(tokenContext, ex, eventError, defaultErrorMessage)
              : response;
        } catch (VerificationException ex) {
            return handleActionTokenVerificationException(tokenContext, ex, eventError, defaultErrorMessage);
        }
    }

    private <T extends DefaultActionToken> ActionTokenHandler<T> resolveActionTokenHandler(String actionId) throws VerificationException {
        if (actionId == null) {
            throw new VerificationException("Action token operation not set");
        }
        ActionTokenHandler<T> handler = session.getProvider(ActionTokenHandler.class, actionId);

        if (handler == null) {
            throw new VerificationException("Invalid action token operation");
        }
        return handler;
    }

    private Response handleActionTokenVerificationException(ActionTokenContext<?> tokenContext, VerificationException ex, String eventError, String errorMessage) {
        if (tokenContext != null && tokenContext.getAuthenticationSession() != null) {
            new AuthenticationSessionManager(session).removeAuthenticationSession(realm, tokenContext.getAuthenticationSession(), true);
        }

        event
          .detail(Details.REASON, ex == null ? "<unknown>" : ex.getMessage())
          .error(eventError == null ? Errors.INVALID_CODE : eventError);
        return ErrorPage.error(session, errorMessage == null ? Messages.INVALID_CODE : errorMessage);
    }

    protected Response processResetCredentials(boolean actionRequest, String execution, AuthenticationSessionModel authSession) {
        AuthenticationProcessor authProcessor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                boolean firstBrokerLoginInProgress = (authenticationSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
                if (firstBrokerLoginInProgress) {

                    UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, realm, authenticationSession);
                    if (!linkingUser.getId().equals(authenticationSession.getAuthenticatedUser().getId())) {
                        return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_DIFFERENT_USER_MESSAGE, authenticationSession.getAuthenticatedUser().getUsername(), linkingUser.getUsername());
                    }

                    SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                    authSession.setAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS, serializedCtx.getIdentityProviderId());

                    logger.debugf("Forget-password flow finished when authenticated user '%s' after first broker login with identity provider '%s'.",
                            linkingUser.getUsername(), serializedCtx.getIdentityProviderId());

                    return redirectToAfterBrokerLoginEndpoint(authSession, true);
                } else {
                    return super.authenticationComplete();
                }
            }
        };

        return processFlow(actionRequest, execution, authSession, RESET_CREDENTIALS_PATH, realm.getResetCredentialsFlow(), null, authProcessor);
    }


    protected Response processRegistration(boolean action, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        return processFlow(action, execution, authSession, REGISTRATION_PATH, realm.getRegistrationFlow(), errorMessage, new AuthenticationProcessor());
    }


    /**
     * protocol independent registration page entry point
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @GET
    public Response registerPage(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        return registerRequest(code, execution, false);
    }


    /**
     * Registration
     *
     * @param code
     * @return
     */
    @Path(REGISTRATION_PATH)
    @POST
    public Response processRegister(@QueryParam("code") String code,
                                    @QueryParam("execution") String execution) {
        return registerRequest(code, execution, true);
    }


    private Response registerRequest(String code, String execution, boolean isPostRequest) {
        event.event(EventType.REGISTER);
        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorPage.error(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        SessionCodeChecks checks = checksForCode(code, execution, REGISTRATION_PATH);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }

        ClientSessionCode<AuthenticationSessionModel> clientSessionCode = checks.clientCode;
        AuthenticationSessionModel clientSession = clientSessionCode.getClientSession();

        // TODO:mposolda any consequences to do this for POST request too?
        if (!isPostRequest) {
            AuthenticationManager.expireIdentityCookie(realm, uriInfo, clientConnection);
        }

        return processRegistration(checks.actionRequest, execution, clientSession, null);
    }


    @Path(FIRST_BROKER_LOGIN_PATH)
    @GET
    public Response firstBrokerLoginGet(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, FIRST_BROKER_LOGIN_PATH);
    }

    @Path(FIRST_BROKER_LOGIN_PATH)
    @POST
    public Response firstBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, FIRST_BROKER_LOGIN_PATH);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @GET
    public Response postBrokerLoginGet(@QueryParam("code") String code,
                                       @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, POST_BROKER_LOGIN_PATH);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @POST
    public Response postBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, POST_BROKER_LOGIN_PATH);
    }


    protected Response brokerLoginFlow(String code, String execution, String flowPath) {
        boolean firstBrokerLogin = flowPath.equals(FIRST_BROKER_LOGIN_PATH);

        EventType eventType = firstBrokerLogin ? EventType.IDENTITY_PROVIDER_FIRST_LOGIN : EventType.IDENTITY_PROVIDER_POST_LOGIN;
        event.event(eventType);

        SessionCodeChecks checks = checksForCode(code, execution, flowPath);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        final AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        String noteKey = firstBrokerLogin ? AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE : PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT;
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, noteKey);
        if (serializedCtx == null) {
            ServicesLogger.LOGGER.notFoundSerializedCtxInClientSession(noteKey);
            throw new WebApplicationException(ErrorPage.error(session, "Not found serialized context in clientSession."));
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(session, authSession);
        final String identityProviderAlias = brokerContext.getIdpConfig().getAlias();

        String flowId = firstBrokerLogin ? brokerContext.getIdpConfig().getFirstBrokerLoginFlowId() : brokerContext.getIdpConfig().getPostBrokerLoginFlowId();
        if (flowId == null) {
            ServicesLogger.LOGGER.flowNotConfigForIDP(identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, "Flow not configured for identity provider"));
        }
        AuthenticationFlowModel brokerLoginFlow = realm.getAuthenticationFlowById(flowId);
        if (brokerLoginFlow == null) {
            ServicesLogger.LOGGER.flowNotFoundForIDP(flowId, identityProviderAlias);
            throw new WebApplicationException(ErrorPage.error(session, "Flow not found for identity provider"));
        }

        event.detail(Details.IDENTITY_PROVIDER, identityProviderAlias)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, brokerContext.getUsername());


        AuthenticationProcessor processor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                if (firstBrokerLogin) {
                    authSession.setAuthNote(AbstractIdpAuthenticator.FIRST_BROKER_LOGIN_SUCCESS, identityProviderAlias);
                } else {
                    String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + identityProviderAlias;
                    authSession.setAuthNote(authStateNoteKey, "true");
                }

                return redirectToAfterBrokerLoginEndpoint(authSession, firstBrokerLogin);
            }

        };

        return processFlow(checks.actionRequest, execution, authSession, flowPath, brokerLoginFlow, null, processor);
    }

    private Response redirectToAfterBrokerLoginEndpoint(AuthenticationSessionModel authSession, boolean firstBrokerLogin) {
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        authSession.setTimestamp(Time.currentTime());

        URI redirect = firstBrokerLogin ? Urls.identityProviderAfterFirstBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) :
                Urls.identityProviderAfterPostBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) ;
        logger.debugf("Redirecting to '%s' ", redirect);

        return Response.status(302).location(redirect).build();
    }


    /**
     * OAuth grant page.  You should not invoked this directly!
     *
     * @param formData
     * @return
     */
    @Path("consent")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processConsent(final MultivaluedMap<String, String> formData) {
        event.event(EventType.LOGIN);
        String code = formData.getFirst("code");
        SessionCodeChecks checks = checksForCode(code, null, REQUIRED_ACTION);
        if (!checks.verifyRequiredAction(ClientSessionModel.Action.OAUTH_GRANT.name())) {
            return checks.response;
        }

        ClientSessionCode<AuthenticationSessionModel> accessCode = checks.clientCode;
        AuthenticationSessionModel authSession = accessCode.getClientSession();

        initLoginEvent(authSession);

        UserModel user = authSession.getAuthenticatedUser();
        ClientModel client = authSession.getClient();


        if (formData.containsKey("cancel")) {
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authSession.getProtocol());
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo)
                    .setEventBuilder(event);
            Response response = protocol.sendError(authSession, Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        }

        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user.getId(), grantedConsent);
        }
        for (RoleModel role : accessCode.getRequestedRoles()) {
            grantedConsent.addGrantedRole(role);
        }
        for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
            if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                grantedConsent.addGrantedProtocolMapper(protocolMapper);
            }
        }
        session.users().updateConsent(realm, user.getId(), grantedConsent);

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        event.success();

        AuthenticatedClientSessionModel clientSession = AuthenticationProcessor.attachSession(authSession, null, session, realm, clientConnection, event);
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, clientSession.getUserSession(), clientSession, request, uriInfo, clientConnection, event, authSession.getProtocol());
    }

    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("execution") String execution) {
        event.event(EventType.SEND_VERIFY_EMAIL);

        SessionCodeChecks checks = checksForCodeRefreshNotAllowed(code, execution, REQUIRED_ACTION);
        if (!checks.verifyCode(ClientSessionModel.Action.REQUIRED_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
            return checks.response;
        }
        ClientSessionCode accessCode = checks.clientCode;
        AuthenticationSessionModel authSession = checks.getAuthenticationSession();
        initLoginEvent(authSession);

        event.clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, authSession.getAuthenticatedUser().getEmail()).success();

        return VerifyEmail.sendVerifyEmail(session, accessCode.getCode(), authSession.getAuthenticatedUser(), authSession);
    }

    /**
     * Initiated by admin, not the user on login
     *
     * @param key
     * @return
     */
    @Path("execute-actions")
    @GET
    public Response executeActions(@QueryParam("key") String key) {
        // TODO:mposolda
        /*
        event.event(EventType.EXECUTE_ACTIONS);
        if (key != null) {
            SessionCodeChecks checks = checksForCode(key);
            if (!checks.verifyCode(ClientSessionModel.Action.EXECUTE_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                return checks.response;
            }
            ClientSessionModel clientSession = checks.getClientSession();
            // verify user email as we know it is valid as this entry point would never have gotten here.
            clientSession.getUserSession().getUser().setEmailVerified(true);
            clientSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
            clientSession.setNote(ClientSessionModel.Action.EXECUTE_ACTIONS.name(), "true");
            return AuthenticationProcessor.redirectToRequiredActions(session, realm, clientSession, uriInfo);
        } else {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }*/
        return null;
    }

    private void initLoginEvent(AuthenticationSessionModel authSession) {
        String responseType = authSession.getClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType == null) {
            responseType = "code";
        }
        String respMode = authSession.getClientNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        OIDCResponseMode responseMode = OIDCResponseMode.parse(respMode, OIDCResponseType.parse(responseType));

        event.event(EventType.LOGIN).client(authSession.getClient())
                .detail(Details.CODE_ID, authSession.getId())
                .detail(Details.REDIRECT_URI, authSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authSession.getProtocol())
                .detail(Details.RESPONSE_TYPE, responseType)
                .detail(Details.RESPONSE_MODE, responseMode.toString().toLowerCase());

        UserModel authenticatedUser = authSession.getAuthenticatedUser();
        if (authenticatedUser != null) {
            event.user(authenticatedUser)
                    .detail(Details.USERNAME, authenticatedUser.getUsername());
        }

        String attemptedUsername = authSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (attemptedUsername != null) {
            event.detail(Details.USERNAME, attemptedUsername);
        }

        String rememberMe = authSession.getAuthNote(Details.REMEMBER_ME);
        if (rememberMe==null || !rememberMe.equalsIgnoreCase("true")) {
            rememberMe = "false";
        }
        event.detail(Details.REMEMBER_ME, rememberMe);

        // TODO:mposolda Fix if this is called at firstBroker or postBroker login
        /*
                .detail(Details.IDENTITY_PROVIDER, userSession.getNote(Details.IDENTITY_PROVIDER))
                .detail(Details.IDENTITY_PROVIDER_USERNAME, userSession.getNote(Details.IDENTITY_PROVIDER_USERNAME));
                */
    }

    @Path(REQUIRED_ACTION)
    @POST
    public Response requiredActionPOST(@QueryParam("code") final String code,
                                       @QueryParam("execution") String action) {
        return processRequireAction(code, action);
    }

    @Path(REQUIRED_ACTION)
    @GET
    public Response requiredActionGET(@QueryParam("code") final String code,
                                       @QueryParam("execution") String action) {
        return processRequireAction(code, action);
    }

    private Response processRequireAction(final String code, String action) {
        event.event(EventType.CUSTOM_REQUIRED_ACTION);

        SessionCodeChecks checks = checksForCode(code, action, REQUIRED_ACTION);
        if (!checks.verifyRequiredAction(action)) {
            return checks.response;
        }

        AuthenticationSessionModel authSession = checks.getAuthenticationSession();
        if (!checks.actionRequest) {
            initLoginEvent(authSession);
            event.event(EventType.CUSTOM_REQUIRED_ACTION);
            return AuthenticationManager.nextActionAfterAuthentication(session, authSession, clientConnection, request, uriInfo, event);
        }

        initLoginEvent(authSession);
        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail(Details.CUSTOM_REQUIRED_ACTION, action);

        RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, action);
        if (factory == null) {
            ServicesLogger.LOGGER.actionProviderNull();
            event.error(Errors.INVALID_CODE);
            throw new WebApplicationException(ErrorPage.error(session, Messages.INVALID_CODE));
        }
        RequiredActionProvider provider = factory.create(session);

        RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, authSession.getAuthenticatedUser(), factory) {
            @Override
            public void ignore() {
                throw new RuntimeException("Cannot call ignore within processAction()");
            }
        };

        Response response;
        provider.processAction(context);

        authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, action);

        if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().success();
            initLoginEvent(authSession);
            event.event(EventType.LOGIN);
            authSession.removeRequiredAction(factory.getId());
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);

            response = AuthenticationManager.nextActionAfterAuthentication(session, authSession, clientConnection, request, uriInfo, event);
        } else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            response = context.getChallenge();
        } else if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo())
                    .setEventBuilder(event);

            event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
            response = protocol.sendError(authSession, Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
        } else {
            throw new RuntimeException("Unreachable");
        }

        return BrowserHistoryHelper.getInstance().saveResponseAndRedirect(session, authSession, response, true);
    }

    private Response redirectToRequiredActions(String action) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (action != null) {
            uriBuilder.queryParam("execution", action);
        }
        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();
    }

}
