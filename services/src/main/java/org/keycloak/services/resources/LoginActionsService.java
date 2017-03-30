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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.TokenVerifier.TokenTypeCheck;
import org.keycloak.authentication.*;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.ObjectUtil;
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
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.keycloak.sessions.CommonClientSessionModel.Action;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.Objects;
import java.util.function.*;
import javax.ws.rs.core.*;
import static org.keycloak.TokenVerifier.optional;
import static org.keycloak.authentication.DefaultActionToken.ACTION_TOKEN_BASIC_CHECKS;
import static org.keycloak.authentication.ResetCredentialsActionToken.RESET_CREDENTIALS_TYPE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginActionsService {

    private static final Logger logger = Logger.getLogger(LoginActionsService.class);

    public static final String ACTION_COOKIE = "KEYCLOAK_ACTION";
    public static final String AUTHENTICATE_PATH = "authenticate";
    public static final String REGISTRATION_PATH = "registration";
    public static final String RESET_CREDENTIALS_PATH = "reset-credentials";
    public static final String REQUIRED_ACTION = "required-action";
    public static final String FIRST_BROKER_LOGIN_PATH = "first-broker-login";
    public static final String POST_BROKER_LOGIN_PATH = "post-broker-login";

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

    private SessionCodeChecks checksForCode(String code, String execution, String flowPath, boolean wantsRestartSession) {
        SessionCodeChecks res = new SessionCodeChecks(code, execution, flowPath, wantsRestartSession);
        res.initialVerifyCode();
        return res;
    }



    private class SessionCodeChecks {
        ClientSessionCode<AuthenticationSessionModel> clientCode;
        Response response;
        ClientSessionCode.ParseResult<AuthenticationSessionModel> result;
        private boolean actionRequest;

        private final String code;
        private final String execution;
        private final String flowPath;
        private final boolean wantsRestartSession;

        public SessionCodeChecks(String code, String execution, String flowPath, boolean wantsRestartSession) {
            this.code = code;
            this.execution = execution;
            this.flowPath = flowPath;
            this.wantsRestartSession = wantsRestartSession;
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


        boolean verifyCode(String requiredAction, ClientSessionCode.ActionType actionType) {
            if (failed()) {
                return false;
            }

            if (!clientCode.isValidAction(requiredAction)) {
                AuthenticationSessionModel authSession = getAuthenticationSession();
                if (ClientSessionModel.Action.REQUIRED_ACTIONS.name().equals(authSession.getAction())) {
                    // TODO:mposolda debug or trace
                    logger.info("Incorrect flow '%s' . User authenticated already. Trying requiredActions now.");
                    response = AuthenticationManager.nextActionAfterAuthentication(session, authSession, clientConnection, request, uriInfo, event);
                    return false;
                } // TODO:mposolda
                /*else if (clientSession.getUserSession() != null && clientSession.getUserSession().getState() == UserSessionModel.State.LOGGED_IN) {
                    response = session.getProvider(LoginFormsProvider.class)
                            .setSuccess(Messages.ALREADY_LOGGED_IN)
                            .createInfoPage();
                    return false;
                }*/
            }

            return isActionActive(actionType);
        }

        private boolean isValidAction(String requiredAction) {
            if (!clientCode.isValidAction(requiredAction)) {
                invalidAction();
                return false;
            }
            return true;
        }

        private void invalidAction() {
            event.client(getAuthenticationSession().getClient());
            event.error(Errors.INVALID_CODE);
            response = ErrorPage.error(session, Messages.INVALID_CODE);
        }

        private boolean isActionActive(ClientSessionCode.ActionType actionType) {
            if (!clientCode.isActionActive(actionType)) {
                event.client(getAuthenticationSession().getClient());
                event.clone().error(Errors.EXPIRED_CODE);
                if (getAuthenticationSession().getAction().equals(ClientSessionModel.Action.AUTHENTICATE.name())) {
                    AuthenticationSessionModel authSession = getAuthenticationSession();
                    AuthenticationProcessor.resetFlow(authSession);
                    response = processAuthentication(false, null, authSession, Messages.LOGIN_TIMEOUT);
                    return false;
                }
                response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                return false;
            }
            return true;
        }

        private boolean initialVerifyCode() {
            // Basic realm checks
            if (!checkSsl()) {
                event.error(Errors.SSL_REQUIRED);
                response = ErrorPage.error(session, Messages.HTTPS_REQUIRED);
                return false;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
                return false;
            }

            // authenticationSession retrieve and check if we need session restart
            AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(code, session, realm, AuthenticationSessionModel.class);
            if (authSession == null) {
                response = restartAuthenticationSession(false);
                return false;
            }
            if (wantsRestartSession) {
                response = restartAuthenticationSession(true);
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
                String lastExecFromSession = authSession.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION);
                String lastFlow = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);

                // Check if we transitted between flows (eg. clicking "register" on login screen)
                if (execution==null && !flowPath.equals(lastFlow)) {
                    logger.infof("Transition between flows! Current flow: %s, Previous flow: %s", flowPath, lastFlow);

                    if (lastFlow == null || isFlowTransitionAllowed(lastFlow)) {
                        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);
                        authSession.removeAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION);
                        lastExecFromSession = null;
                    }
                }

                if (ObjectUtil.isEqualOrBothNull(execution, lastExecFromSession)) {
                    // Allow refresh of previous page
                    clientCode = new ClientSessionCode<>(session, realm, authSession);
                    actionRequest = false;
                    return true;
                } else {
                    logger.info("Redirecting to page expired page.");
                    response = showPageExpired(flowPath, authSession);
                    return false;
                }
            } else {
                result = ClientSessionCode.parseResult(code, session, realm, AuthenticationSessionModel.class);
                clientCode = result.getCode();
                if (clientCode == null) {

                    // In case that is replayed action, but sent to the same FORM like actual FORM, we just re-render the page
                    if (ObjectUtil.isEqualOrBothNull(execution, authSession.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION))) {
                        String latestFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
                        URI redirectUri = getLastExecutionUrl(latestFlowPath, execution);
                        logger.infof("Invalid action code, but execution matches. So just redirecting to %s", redirectUri);
                        response = Response.status(Response.Status.FOUND).location(redirectUri).build();
                    } else {
                        response = showPageExpired(flowPath, authSession);
                    }
                    return false;
                }


                actionRequest = true;
                authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, execution);
                return true;
            }
        }

        private boolean isFlowTransitionAllowed(String lastFlow) {
            if (flowPath.equals(AUTHENTICATE_PATH) && (lastFlow.equals(REGISTRATION_PATH) || lastFlow.equals(RESET_CREDENTIALS_PATH))) {
                return true;
            }

            if (flowPath.equals(REGISTRATION_PATH) && (lastFlow.equals(AUTHENTICATE_PATH))) {
                return true;
            }

            if (flowPath.equals(RESET_CREDENTIALS_PATH) && (lastFlow.equals(AUTHENTICATE_PATH))) {
                return true;
            }

            return false;
        }

        public boolean verifyRequiredAction(String executedAction) {
            if (failed()) {
                return false;
            }

            if (!isValidAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name())) return false;
            if (!isActionActive(ClientSessionCode.ActionType.USER)) return false;

            final AuthenticationSessionModel authSession = getAuthenticationSession();

            if (actionRequest) {
                String currentRequiredAction = authSession.getAuthNote(AuthenticationManager.CURRENT_REQUIRED_ACTION);
                if (executedAction == null || !executedAction.equals(currentRequiredAction)) {
                    logger.debug("required action doesn't match current required action");
                    authSession.removeAuthNote(AuthenticationManager.CURRENT_REQUIRED_ACTION);
                    response = redirectToRequiredActions(currentRequiredAction, authSession);
                    return false;
                }
            }
            return true;
        }
    }


    protected Response restartAuthenticationSession(boolean managedRestart) {
        logger.infof("Login restart requested or authentication session not found. Trying to restart from cookie. Managed restart: %s", managedRestart);
        AuthenticationSessionModel authSession = null;
        try {
            authSession = RestartLoginCookie.restartSession(session, realm);
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToParseRestartLoginCookie(e);
        }

        if (authSession != null) {

            event.clone();

            String warningMessage = null;
            if (managedRestart) {
                event.detail(Details.RESTART_REQUESTED, "true");
            } else {
                event.detail(Details.RESTART_AFTER_TIMEOUT, "true");
                warningMessage = Messages.LOGIN_TIMEOUT;
            }

            event.error(Errors.EXPIRED_CODE);
            return processFlow(false, null, authSession, AUTHENTICATE_PATH, realm.getBrowserFlow(), warningMessage, new AuthenticationProcessor());
        } else {
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }
    }


    protected Response showPageExpired(String flowPath, AuthenticationSessionModel authSession) {
        String executionId = authSession==null ? null : authSession.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION);
        String latestFlowPath = authSession==null ? flowPath : authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
        URI lastStepUrl = getLastExecutionUrl(latestFlowPath, executionId);

        logger.infof("Redirecting to 'page expired' now. Will use URL: %s", lastStepUrl);

        return session.getProvider(LoginFormsProvider.class)
                .setActionUri(lastStepUrl)
                .createLoginExpiredPage();
    }


    protected URI getLastExecutionUrl(String flowPath, String executionId) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(flowPath);

        if (executionId != null) {
            uriBuilder.queryParam("execution", executionId);
        }
        return uriBuilder.build(realm.getName());
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
                                 @QueryParam("execution") String execution,
                                 @QueryParam("restart") String restart) {
        event.event(EventType.LOGIN);

        boolean wantsSessionRestart = Boolean.parseBoolean(restart);

        SessionCodeChecks checks = checksForCode(code, execution, AUTHENTICATE_PATH, wantsSessionRestart);
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
        if (errorMessage != null) processor.setForwardedErrorMessage(new FormMessage(null, errorMessage));

        try {
            if (action) {
                return processor.authenticationAction(execution);
            } else {
                return processor.authenticate();
            }
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }
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
        return authenticate(code, execution, null);
    }

    @Path(RESET_CREDENTIALS_PATH)
    @POST
    public Response resetCredentialsPOST(@QueryParam("code") String code,
                                         @QueryParam("execution") String execution) {
        return resetCredentials(code, execution);
    }

    private Predicate<JsonWebToken> checkThat(BooleanSupplier function, String errorEvent, String errorMessage) {
        return t -> {
            if (! function.getAsBoolean()) {
                event.error(errorEvent);
                throw new LoginActionsServiceException(ErrorPage.error(session, errorMessage));
            }

            return true;
        };
    }

    /**
     * Verifies that the authentication session has not yet been converted to user session, in other words
     * that the user has not yet completed authentication and logged in.
     * @param t token
     */
    private class IsAuthenticationSessionNotConvertedToUserSession<T extends JsonWebToken> implements Predicate<T> {

        private final Function<T, String> getAuthenticationSessionIdFromToken;

        public IsAuthenticationSessionNotConvertedToUserSession(Function<T, String> getAuthenticationSessionIdFromToken) {
            this.getAuthenticationSessionIdFromToken = getAuthenticationSessionIdFromToken;
        }

        @Override
        public boolean test(T t) throws VerificationException {
            String authSessionId = t == null ? null : getAuthenticationSessionIdFromToken.apply(t);
            if (authSessionId == null) {
                return false;
            }

            if (session.sessions().getUserSession(realm, authSessionId) != null) {
                throw new LoginActionsServiceException(
                  session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.ALREADY_LOGGED_IN)
                        .createInfoPage());
            }

            return true;
        }
    }

    /**
     * Verifies whether client stored in the authentication session both exists and is enabled. If yes, it also sets the client
     * into session context.
     * @param <T>
     */
    private class IsClientValid<T extends JsonWebToken> implements Predicate<T> {

        private final Function<T, AuthenticationSessionModel> getAuthenticationSessionFromToken;

        public IsClientValid(Function<T, AuthenticationSessionModel> getAuthenticationSessionFromToken) {
            this.getAuthenticationSessionFromToken = getAuthenticationSessionFromToken;
        }

        @Override
        public boolean test(T t) throws VerificationException {
            AuthenticationSessionModel authenticationSession = getAuthenticationSessionFromToken.apply(t);

            ClientModel client = authenticationSession == null ? null : authenticationSession.getClient();

            if (client == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                session.authenticationSessions().removeAuthenticationSession(realm, authenticationSession);
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER));
            }

            if (! client.isEnabled()) {
                event.error(Errors.CLIENT_NOT_FOUND);
                session.authenticationSessions().removeAuthenticationSession(realm, authenticationSession);
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED));
            }

            session.getContext().setClient(client);

            return true;
        }
    }

    /**
     * This check verifies that:
     * <ul>
     * <li>If authentication session ID is not set in the token, passes.</li>
     * <li>If auth session ID is set in the token, then the corresponding authentication session exists.
     *     Then it is set into the token.</li>
     * </ul>
     *
     * @param <T>
     */
    private class CanResolveAuthenticationSession<T extends JsonWebToken> implements Predicate<T> {

        private final Function<T, String> getAuthenticationSessionIdFromToken;

        private final BiConsumer<T, AuthenticationSessionModel> setAuthenticationSessionToToken;

        public CanResolveAuthenticationSession(Function<T, String> getAuthenticationSessionIdFromToken,
          BiConsumer<T, AuthenticationSessionModel> setAuthenticationSessionToToken) {
            this.getAuthenticationSessionIdFromToken = getAuthenticationSessionIdFromToken;
            this.setAuthenticationSessionToToken = setAuthenticationSessionToToken;
        }

        @Override
        public boolean test(T t) throws VerificationException {
            String authSessionId = t == null ? null : getAuthenticationSessionIdFromToken.apply(t);

            AuthenticationSessionModel authSession;
            if (authSessionId == null) {
                return true;
            } else {
                authSession = session.authenticationSessions().getAuthenticationSession(realm, authSessionId);
            }

            if (authSession == null) { // timeout or logged-already (NOPE - this is handled by IsAuthenticationSessionNotConvertedToUserSession)
                throw new LoginActionsServiceException(restartAuthenticationSession(false));
            }

            event
              .detail(Details.CODE_ID, authSession.getId())
              .client(authSession.getClient());

            setAuthenticationSessionToToken.accept(t, authSession);

            return true;
        }
    }

    /**
     * This check verifies that if the token has not authentication session set, a new authentication session is introduced
     * for the given client and reset-credentials flow is started with this new session.
     * @param <T>
     */
    private class ResetCredsIntroduceAuthenticationSessionIfNotSet implements Predicate<ResetCredentialsActionToken> {

        private final String defaultClientId;

        public ResetCredsIntroduceAuthenticationSessionIfNotSet(String defaultClientId) {
            this.defaultClientId = defaultClientId;
        }

        @Override
        public boolean test(ResetCredentialsActionToken t) throws VerificationException {
            AuthenticationSessionModel authSession = t.getAuthenticationSession();

            if (authSession == null) {
                authSession = createAuthenticationSessionForClient(this.defaultClientId);
                throw new LoginActionsServiceException(processResetCredentials(false, null, authSession, null));
            }

            return true;
        }
    }

    /**
     * Verifies that if authentication session exists and any action is required according to it, then it is
     * the expected one.
     *
     * If there is an action required in the session, furthermore it is not the expected one, and the required
     * action is redirection to "required actions", it throws with response performing the redirect to required
     * actions.
     * @param <T>
     */
    private class IsActionRequired<T extends JsonWebToken> implements Predicate<T> {

        private final ClientSessionModel.Action expectedAction;
        
        private final Function<T, AuthenticationSessionModel> getAuthenticationSessionFromToken;

        public IsActionRequired(Action expectedAction, Function<T, AuthenticationSessionModel> getAuthenticationSessionFromToken) {
            this.expectedAction = expectedAction;
            this.getAuthenticationSessionFromToken = getAuthenticationSessionFromToken;
        }

        @Override
        public boolean test(T t) throws VerificationException {
            AuthenticationSessionModel authSession = getAuthenticationSessionFromToken.apply(t);
            
            if (authSession != null && ! Objects.equals(authSession.getAction(), this.expectedAction.name())) {
                if (ClientSessionModel.Action.REQUIRED_ACTIONS.name().equals(authSession.getAction())) {
                    throw new LoginActionsServiceException(redirectToRequiredActions(null, authSession));
                }
            }

            return true;
        }
    }

    /**
     * Endpoint for executing reset credentials flow.  If code is null, a client session is created with the account
     * service as the client.  Successful reset sends you to the account page.  Note, account service must be enabled.
     *
     * @param code
     * @param execution
     * @return
     */
    @Path(RESET_CREDENTIALS_PATH)
    @GET
    public Response resetCredentialsGET(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution,
                                        @QueryParam(Constants.KEY) String key) {
        if (code != null && key != null) {
            // TODO:mposolda better handling of error
            throw new IllegalStateException("Illegal state");
        }

        AuthenticationSessionModel authSession = session.authenticationSessions().getCurrentAuthenticationSession(realm);

        // we allow applications to link to reset credentials without going through OAuth or SAML handshakes
        if (authSession == null && key == null && code == null) {
            if (!realm.isResetPasswordAllowed()) {
                event.event(EventType.RESET_PASSWORD);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

            }
            authSession = createAuthenticationSessionForClient(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            return processResetCredentials(false, null, authSession, null);
        }

        if (key != null) {
            return resetCredentialsByToken(key, execution);
        }
        
        return resetCredentials(code, execution);
    }

    private AuthenticationSessionModel createAuthenticationSessionForClient(String clientId)
      throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        // set up the account service as the endpoint to call.
        ClientModel client = realm.getClientByClientId(clientId);
        authSession = session.authenticationSessions().createAuthenticationSession(realm, client, true);
        authSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        //authSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String redirectUri = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        authSession.setRedirectUri(redirectUri);
        authSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        return authSession;
    }

    /**
     * @deprecated In favor of {@link #resetCredentialsByToken(String, String)}
     * @param code
     * @param execution
     * @return
     */
    protected Response resetCredentials(String code, String execution) {
        event.event(EventType.RESET_PASSWORD);
        SessionCodeChecks checks = checksForCode(code, execution, RESET_CREDENTIALS_PATH, false);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.USER)) {
            return checks.response;
        }
        final AuthenticationSessionModel authSession = checks.getAuthenticationSession();

        if (!realm.isResetPasswordAllowed()) {
            event.client(authSession.getClient());
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);

        }

        return processResetCredentials(checks.actionRequest, execution, authSession, null);
    }

    protected Response resetCredentialsByToken(String tokenString, String execution) {
        event.event(EventType.RESET_PASSWORD);

        ResetCredentialsActionToken token;
        ResetCredentialsActionTokenChecks singleUseCheck = new ResetCredentialsActionTokenChecks(session, realm, event);
        try {
            token = TokenVerifier.createHollow(tokenString, ResetCredentialsActionToken.class)
              .secretKey(session.keys().getActiveHmacKey(realm).getSecretKey())

              .withChecks(
                new TokenTypeCheck(RESET_CREDENTIALS_TYPE),

                checkThat(realm::isEnabled, Errors.REALM_DISABLED, Messages.REALM_NOT_ENABLED),
                checkThat(realm::isResetPasswordAllowed, Errors.NOT_ALLOWED, Messages.RESET_CREDENTIAL_NOT_ALLOWED),
                checkThat(this::checkSsl, Errors.SSL_REQUIRED, Messages.HTTPS_REQUIRED),

                new IsAuthenticationSessionNotConvertedToUserSession<>(ResetCredentialsActionToken::getAuthenticationSessionId),

                // Authentication session might not be part of the token, hence the following check is optional
                optional(new CanResolveAuthenticationSession<>(ResetCredentialsActionToken::getAuthenticationSessionId, ResetCredentialsActionToken::setAuthenticationSession)),

                // Check for being active has to be after authentication session is resolved so that it can be used in error handling
                TokenVerifier.IS_ACTIVE,

                singleUseCheck, // TODO:hmlnarik make it use a check via generic single-use cache

                new ResetCredsIntroduceAuthenticationSessionIfNotSet(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID),

                new IsActionRequired<>(Action.AUTHENTICATE, ResetCredentialsActionToken::getAuthenticationSession),
                new IsClientValid<>(ResetCredentialsActionToken::getAuthenticationSession)
              )
              .withChecks(ACTION_TOKEN_BASIC_CHECKS)

              .verify()
              .getToken();
        } catch (TokenNotActiveException ex) {
            token = (ResetCredentialsActionToken) ex.getToken();

            if (token != null && token.getAuthenticationSession() != null) {
                event.clone()
                  .client(token.getAuthenticationSession().getClient())
                  .error(Errors.EXPIRED_CODE);
                AuthenticationSessionModel authSession = token.getAuthenticationSession();
                AuthenticationProcessor.resetFlow(authSession);
                return processAuthentication(false, null, authSession, Messages.LOGIN_TIMEOUT);
            }

            event
              .detail(Details.REASON, ex.getMessage())
              .error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);
        } catch (LoginActionsServiceException ex) {
            if (ex.getResponse() == null) {
                event
                  .detail(Details.REASON, ex.getMessage())
                  .error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);
            } else {
                return ex.getResponse();
            }
        } catch (VerificationException ex) {
            event
              .detail(Details.REASON, ex.getMessage())
              .error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED);
        }

        final AuthenticationSessionModel authSession = token.getAuthenticationSession();
        authSession.setAuthNote(ResetCredentialsActionToken.class.getName(), tokenString);

        // Verify if action is processed in same browser.
        if (!isSameBrowser(authSession)) {
            logger.debug("Action request processed in different browser.");

            // TODO:mposolda improve this. The code should be merged with the InfinispanLoginSessionProvider code and rather extracted from the infinispan provider
            setAuthSessionCookie(authSession.getId());

            authSession.setAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        }

        return processResetCredentials(true, execution, authSession, null);
    }


    // Verify if action is processed in same browser.
    private boolean isSameBrowser(AuthenticationSessionModel actionTokenSession) {
        String cookieSessionId = session.authenticationSessions().getCurrentAuthenticationSessionId(realm);

        if (cookieSessionId == null) {
            return false;
        }

        if (actionTokenSession.getId().equals(cookieSessionId)) {
            return true;
        }

        // Chance that cookie session was "forked" in browser from some other session
        AuthenticationSessionModel forkedSession = session.authenticationSessions().getAuthenticationSession(realm, cookieSessionId);
        if (forkedSession == null) {
            return false;
        }

        String parentSessionId = forkedSession.getAuthNote(AuthenticationProcessor.FORKED_FROM);
        if (parentSessionId == null) {
            return false;
        }

        if (actionTokenSession.getId().equals(parentSessionId)) {
            // It's the correct browser. Let's remove forked session as we won't continue from the login form (browser flow) but from the resetCredentialsByToken flow
            session.authenticationSessions().removeAuthenticationSession(realm, forkedSession);
            logger.infof("Removed forked session: %s", forkedSession.getId());

            // Refresh browser cookie
            setAuthSessionCookie(parentSessionId);

            return true;
        } else {
            return false;
        }
    }

    // TODO:mposolda improve this. The code should be merged with the InfinispanLoginSessionProvider code and rather extracted from the infinispan provider
    private void setAuthSessionCookie(String authSessionId) {
        logger.infof("Set browser cookie to %s", authSessionId);

        String cookiePath = CookieHelper.getRealmCookiePath(realm);
        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());
        CookieHelper.addCookie("AUTH_SESSION_ID", authSessionId, cookiePath, null, null, -1, sslRequired, true);
    }



    protected Response processResetCredentials(boolean actionRequest, String execution, AuthenticationSessionModel authSession, String errorMessage) {
        AuthenticationProcessor authProcessor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                boolean firstBrokerLoginInProgress = (authenticationSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
                if (firstBrokerLoginInProgress) {

                    UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, realm, authenticationSession);
                    if (!linkingUser.getId().equals(authenticationSession.getAuthenticatedUser().getId())) {
                        return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_DIFFERENT_USER_MESSAGE, authenticationSession.getAuthenticatedUser().getUsername(), linkingUser.getUsername());
                    }

                    logger.debugf("Forget-password flow finished when authenticated user '%s' after first broker login.", linkingUser.getUsername());

                    // TODO:mposolda Isn't this a bug that we redirect to 'afterBrokerLoginEndpoint' without rather continue with firstBrokerLogin and other authenticators like OTP?
                    //return redirectToAfterBrokerLoginEndpoint(authSession, true);
                    return null;
                } else {
                    return super.authenticationComplete();
                }
            }
        };

        return processFlow(actionRequest, execution, authSession, RESET_CREDENTIALS_PATH, realm.getResetCredentialsFlow(), errorMessage, authProcessor);
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

        SessionCodeChecks checks = checksForCode(code, execution, REGISTRATION_PATH, false);
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

    // TODO:mposolda broker login
/*
    @Path(FIRST_BROKER_LOGIN_PATH)
    @GET
    public Response firstBrokerLoginGet(@QueryParam("code") String code,
                                 @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, true);
    }

    @Path(FIRST_BROKER_LOGIN_PATH)
    @POST
    public Response firstBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, true);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @GET
    public Response postBrokerLoginGet(@QueryParam("code") String code,
                                       @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, false);
    }

    @Path(POST_BROKER_LOGIN_PATH)
    @POST
    public Response postBrokerLoginPost(@QueryParam("code") String code,
                                        @QueryParam("execution") String execution) {
        return brokerLoginFlow(code, execution, false);
    }


    protected Response brokerLoginFlow(String code, String execution, final boolean firstBrokerLogin) {
        EventType eventType = firstBrokerLogin ? EventType.IDENTITY_PROVIDER_FIRST_LOGIN : EventType.IDENTITY_PROVIDER_POST_LOGIN;
        event.event(eventType);

        SessionCodeChecks checks = checksForCode(code);
        if (!checks.verifyCode(ClientSessionModel.Action.AUTHENTICATE.name(), ClientSessionCode.ActionType.LOGIN)) {
            return checks.response;
        }
        event.detail(Details.CODE_ID, code);
        final ClientSessionModel clientSessionn = checks.getClientSession();

        String noteKey = firstBrokerLogin ? AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE : PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT;
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(clientSessionn, noteKey);
        if (serializedCtx == null) {
            ServicesLogger.LOGGER.notFoundSerializedCtxInClientSession(noteKey);
            throw new WebApplicationException(ErrorPage.error(session, "Not found serialized context in clientSession."));
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(session, clientSessionn);
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
                if (!firstBrokerLogin) {
                    String authStateNoteKey = PostBrokerLoginConstants.PBL_AUTH_STATE_PREFIX + identityProviderAlias;
                    clientSessionn.setNote(authStateNoteKey, "true");
                }

                return redirectToAfterBrokerLoginEndpoint(clientSession, firstBrokerLogin);
            }

        };

        String flowPath = firstBrokerLogin ? FIRST_BROKER_LOGIN_PATH : POST_BROKER_LOGIN_PATH;
        return processFlow(execution, clientSessionn, flowPath, brokerLoginFlow, null, processor);
    }

    private Response redirectToAfterBrokerLoginEndpoint(ClientSessionModel clientSession, boolean firstBrokerLogin) {
        ClientSessionCode accessCode = new ClientSessionCode(session, realm, clientSession);
        clientSession.setTimestamp(Time.currentTime());

        URI redirect = firstBrokerLogin ? Urls.identityProviderAfterFirstBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) :
                Urls.identityProviderAfterPostBrokerLogin(uriInfo.getBaseUri(), realm.getName(), accessCode.getCode()) ;
        logger.debugf("Redirecting to '%s' ", redirect);

        return Response.status(302).location(redirect).build();
    }
*/

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
        SessionCodeChecks checks = checksForCode(code, null, REQUIRED_ACTION, false);
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

        // TODO:mposolda So assume that requiredActions were already done in this stage. Doublecheck...
        AuthenticatedClientSessionModel clientSession = AuthenticationProcessor.attachSession(authSession, null, session, realm, clientConnection, event);
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, clientSession.getUserSession(), clientSession, request, uriInfo, clientConnection, event, authSession.getProtocol());
    }

    @Path("email-verification")
    @GET
    public Response emailVerification(@QueryParam("code") String code, @QueryParam("key") String key) {
        // TODO:mposolda
        /*
        event.event(EventType.VERIFY_EMAIL);
        if (key != null) {
            ClientSessionModel clientSession = null;
            String keyFromSession = null;
            if (code != null) {
                clientSession = ClientSessionCode.getClientSession(code, session, realm);
                keyFromSession = clientSession != null ? clientSession.getNote(Constants.VERIFY_EMAIL_KEY) : null;
            }

            if (!key.equals(keyFromSession)) {
                ServicesLogger.LOGGER.invalidKeyForEmailVerification();
                event.error(Errors.INVALID_CODE);
                throw new WebApplicationException(ErrorPage.error(session, Messages.STALE_VERIFY_EMAIL_LINK));
            }

            clientSession.removeNote(Constants.VERIFY_EMAIL_KEY);

            SessionCodeChecks checks = checksForCode(code);
            if (!checks.verifyCode(ClientSessionModel.Action.REQUIRED_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                if (checks.clientCode == null && checks.result.isClientSessionNotFound() || checks.result.isIllegalHash()) {
                   return ErrorPage.error(session, Messages.STALE_VERIFY_EMAIL_LINK);
                }
                return checks.response;
            }

            clientSession = checks.getClientSession();
            if (!ClientSessionModel.Action.VERIFY_EMAIL.name().equals(clientSession.getNote(AuthenticationManager.CURRENT_REQUIRED_ACTION))) {
                ServicesLogger.LOGGER.reqdActionDoesNotMatch();
                event.error(Errors.INVALID_CODE);
                throw new WebApplicationException(ErrorPage.error(session, Messages.STALE_VERIFY_EMAIL_LINK));
            }

            UserSessionModel userSession = clientSession.getUserSession();
            UserModel user = userSession.getUser();
            initEvent(clientSession);
            event.event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail());

            user.setEmailVerified(true);

            user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

            event.success();

            String actionCookieValue = getActionCookie();
            if (actionCookieValue == null || !actionCookieValue.equals(userSession.getId())) {
                session.sessions().removeClientSession(realm, clientSession);
                return session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.EMAIL_VERIFIED)
                        .createInfoPage();
            }

            event = event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN);

            return AuthenticationProcessor.redirectToRequiredActions(session, realm, clientSession, uriInfo);
        } else {
            SessionCodeChecks checks = checksForCode(code);
            if (!checks.verifyCode(ClientSessionModel.Action.REQUIRED_ACTIONS.name(), ClientSessionCode.ActionType.USER)) {
                return checks.response;
            }
            ClientSessionCode accessCode = checks.clientCode;
            ClientSessionModel clientSession = checks.getClientSession();
            UserSessionModel userSession = clientSession.getUserSession();
            initEvent(clientSession);

            createActionCookie(realm, uriInfo, clientConnection, userSession.getId());

            VerifyEmail.setupKey(clientSession);

            return session.getProvider(LoginFormsProvider.class)
                    .setClientSessionCode(accessCode.getCode())
                    .setAuthenticationSession(clientSession)
                    .setUser(userSession.getUser())
                    .createResponse(RequiredAction.VERIFY_EMAIL);
        }*/
        return null;
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

    private String getActionCookie() {
        return getActionCookie(headers, realm, uriInfo, clientConnection);
    }

    public static String getActionCookie(HttpHeaders headers, RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection) {
        Cookie cookie = headers.getCookies().get(ACTION_COOKIE);
        AuthenticationManager.expireCookie(realm, ACTION_COOKIE, AuthenticationManager.getRealmCookiePath(realm, uriInfo), realm.getSslRequired().isRequired(clientConnection), clientConnection);
        return cookie != null ? cookie.getValue() : null;
    }

    // TODO: Remove this method. We will be able to use login-session-cookie
    public static void createActionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, String sessionId) {
        CookieHelper.addCookie(ACTION_COOKIE, sessionId, AuthenticationManager.getRealmCookiePath(realm, uriInfo), null, null, -1, realm.getSslRequired().isRequired(clientConnection), true);
    }

    private void initLoginEvent(AuthenticationSessionModel authSession) {
        String responseType = authSession.getNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType == null) {
            responseType = "code";
        }
        String respMode = authSession.getNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
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
        SessionCodeChecks checks = checksForCode(code, action, REQUIRED_ACTION, false);
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
        provider.processAction(context);
        if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().success();
            initLoginEvent(authSession);
            event.event(EventType.LOGIN);
            authSession.removeRequiredAction(factory.getId());
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeAuthNote(AuthenticationManager.CURRENT_REQUIRED_ACTION);

            return redirectToRequiredActions(action, authSession);
        }
        if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            return context.getChallenge();
        }
        if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo())
                    .setEventBuilder(event);

            event.detail(Details.CUSTOM_REQUIRED_ACTION, action);
            Response response = protocol.sendError(authSession, Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;

        }

        throw new RuntimeException("Unreachable");
    }

    private Response redirectToRequiredActions(String action, AuthenticationSessionModel authSession) {
        authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, action);

        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (action != null) {
            uriBuilder.queryParam("execution", action);
        }
        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();
    }

}
