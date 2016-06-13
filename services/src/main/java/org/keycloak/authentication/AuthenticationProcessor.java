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

package org.keycloak.authentication;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.common.util.Time;
import org.keycloak.services.util.CacheControlUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationProcessor {
    public static final String CURRENT_AUTHENTICATION_EXECUTION = "current.authentication.execution";
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    protected RealmModel realm;
    protected UserSessionModel userSession;
    protected ClientSessionModel clientSession;
    protected ClientConnection connection;
    protected UriInfo uriInfo;
    protected KeycloakSession session;
    protected EventBuilder event;
    protected HttpRequest request;
    protected String flowId;
    protected String flowPath;
    protected boolean browserFlow;
    protected BruteForceProtector protector;
    protected boolean oneActionWasSuccessful;
    /**
     * This could be an error message forwarded from another authenticator
     */
    protected FormMessage forwardedErrorMessage;

    /**
     * This could be an success message forwarded from another authenticator
     */
    protected FormMessage forwardedSuccessMessage;
    protected boolean userSessionCreated;

    // Used for client authentication
    protected ClientModel client;
    protected Map<String, String> clientAuthAttributes = new HashMap<>();

    public AuthenticationProcessor() {
    }

    public boolean isBrowserFlow() {
        return browserFlow;
    }

    public AuthenticationProcessor setBrowserFlow(boolean browserFlow) {
        this.browserFlow = browserFlow;
        return this;
    }

    public BruteForceProtector getBruteForceProtector() {
        if (protector == null) {
            protector = session.getProvider(BruteForceProtector.class);
        }
        return protector;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public ClientModel getClient() {
        return client;
    }

    public void setClient(ClientModel client) {
        this.client = client;
    }

    public Map<String, String> getClientAuthAttributes() {
        return clientAuthAttributes;
    }

    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public UserSessionModel getUserSession() {
        return userSession;
    }

    public boolean isUserSessionCreated() {
        return userSessionCreated;
    }

    public AuthenticationProcessor setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public AuthenticationProcessor setClientSession(ClientSessionModel clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    public AuthenticationProcessor setConnection(ClientConnection connection) {
        this.connection = connection;
        return this;
    }

    public AuthenticationProcessor setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    public AuthenticationProcessor setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    public AuthenticationProcessor setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    public AuthenticationProcessor setRequest(HttpRequest request) {
        this.request = request;
        return this;
    }

    public AuthenticationProcessor setFlowId(String flowId) {
        this.flowId = flowId;
        return this;
    }

    /**
     * This is the path segment to append when generating an action URL.
     *
     * @param flowPath
     */
    public AuthenticationProcessor setFlowPath(String flowPath) {
        this.flowPath = flowPath;
        return this;
    }

    public AuthenticationProcessor setForwardedErrorMessage(FormMessage forwardedErrorMessage) {
        this.forwardedErrorMessage = forwardedErrorMessage;
        return this;
    }

    public AuthenticationProcessor setForwardedSuccessMessage(FormMessage forwardedSuccessMessage) {
        this.forwardedSuccessMessage = forwardedSuccessMessage;
        return this;
    }

    public String generateCode() {
        ClientSessionCode accessCode = new ClientSessionCode(getRealm(), getClientSession());
        clientSession.setTimestamp(Time.currentTime());
        return accessCode.getCode();
    }

    public EventBuilder newEvent() {
        this.event = new EventBuilder(realm, session, connection);
        return this.event;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setAutheticatedUser(UserModel user) {
        UserModel previousUser = clientSession.getAuthenticatedUser();
        if (previousUser != null && !user.getId().equals(previousUser.getId()))
            throw new AuthenticationFlowException(AuthenticationFlowError.USER_CONFLICT);
        validateUser(user);
        getClientSession().setAuthenticatedUser(user);
    }

    public void clearAuthenticatedUser() {
        getClientSession().setAuthenticatedUser(null);
    }

    public class Result implements AuthenticationFlowContext, ClientAuthenticationFlowContext {
        AuthenticatorConfigModel authenticatorConfig;
        AuthenticationExecutionModel execution;
        Authenticator authenticator;
        FlowStatus status;
        ClientAuthenticator clientAuthenticator;
        Response challenge;
        AuthenticationFlowError error;
        List<AuthenticationExecutionModel> currentExecutions;
        FormMessage errorMessage;
        FormMessage successMessage;

        private Result(AuthenticationExecutionModel execution, Authenticator authenticator, List<AuthenticationExecutionModel> currentExecutions) {
            this.execution = execution;
            this.authenticator = authenticator;
            this.currentExecutions = currentExecutions;
        }

        private Result(AuthenticationExecutionModel execution, ClientAuthenticator clientAuthenticator, List<AuthenticationExecutionModel> currentExecutions) {
            this.execution = execution;
            this.clientAuthenticator = clientAuthenticator;
            this.currentExecutions = currentExecutions;
        }

        @Override
        public EventBuilder newEvent() {
            return AuthenticationProcessor.this.newEvent();
        }

        @Override
        public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory) {
            List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(execution.getParentFlow());
            for (AuthenticationExecutionModel exe : executions) {
                AuthenticatorFactory factory = (AuthenticatorFactory) getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, exe.getAuthenticator());
                if (factory != null && factory.getReferenceCategory().equals(authenticatorCategory)) {
                    return exe.getRequirement();
                }

            }
            return null;
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return execution;
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfig() {
            if (execution.getAuthenticatorConfig() == null) return null;
            if (authenticatorConfig != null) return authenticatorConfig;
            authenticatorConfig = realm.getAuthenticatorConfigById(execution.getAuthenticatorConfig());
            return authenticatorConfig;
        }

        public Authenticator getAuthenticator() {
            return authenticator;
        }

        @Override
        public FlowStatus getStatus() {
            return status;
        }

        public ClientAuthenticator getClientAuthenticator() {
            return clientAuthenticator;
        }

        @Override
        public void success() {
            this.status = FlowStatus.SUCCESS;
        }

        @Override
        public void failure(AuthenticationFlowError error) {
            status = FlowStatus.FAILED;
            this.error = error;

        }

        @Override
        public void challenge(Response challenge) {
            this.status = FlowStatus.CHALLENGE;
            this.challenge = challenge;

        }

        @Override
        public void forceChallenge(Response challenge) {
            this.status = FlowStatus.FORCE_CHALLENGE;
            this.challenge = challenge;

        }

        @Override
        public void failureChallenge(AuthenticationFlowError error, Response challenge) {
            this.error = error;
            this.status = FlowStatus.FAILURE_CHALLENGE;
            this.challenge = challenge;

        }

        @Override
        public void failure(AuthenticationFlowError error, Response challenge) {
            this.error = error;
            this.status = FlowStatus.FAILED;
            this.challenge = challenge;

        }

        @Override
        public void attempted() {
            this.status = FlowStatus.ATTEMPTED;

        }



        @Override
        public UserModel getUser() {
            return getClientSession().getAuthenticatedUser();
        }

        @Override
        public void setUser(UserModel user) {
            setAutheticatedUser(user);
        }

        @Override
        public void clearUser() {
            clearAuthenticatedUser();
        }

        @Override
        public RealmModel getRealm() {
            return AuthenticationProcessor.this.getRealm();
        }

        @Override
        public ClientModel getClient() {
            return AuthenticationProcessor.this.getClient();
        }

        @Override
        public void setClient(ClientModel client) {
            AuthenticationProcessor.this.setClient(client);
        }

        @Override
        public Map<String, String> getClientAuthAttributes() {
            return AuthenticationProcessor.this.getClientAuthAttributes();
        }

        @Override
        public ClientSessionModel getClientSession() {
            return AuthenticationProcessor.this.getClientSession();
        }

        @Override
        public ClientConnection getConnection() {
            return AuthenticationProcessor.this.getConnection();
        }

        @Override
        public UriInfo getUriInfo() {
            return AuthenticationProcessor.this.getUriInfo();
        }

        @Override
        public KeycloakSession getSession() {
            return AuthenticationProcessor.this.getSession();
        }

        @Override
        public HttpRequest getHttpRequest() {
            return AuthenticationProcessor.this.request;
        }

        @Override
        public void attachUserSession(UserSessionModel userSession) {
            AuthenticationProcessor.this.userSession = userSession;
        }

        @Override
        public BruteForceProtector getProtector() {
            return AuthenticationProcessor.this.getBruteForceProtector();
        }

        @Override
        public EventBuilder getEvent() {
            return AuthenticationProcessor.this.event;
        }

        @Override
        public FormMessage getForwardedErrorMessage() {
            return AuthenticationProcessor.this.forwardedErrorMessage;
        }

        @Override
        public String generateAccessCode() {
            return generateCode();
        }


        public Response getChallenge() {
            return challenge;
        }

        @Override
        public AuthenticationFlowError getError() {
            return error;
        }

        @Override
        public LoginFormsProvider form() {
            String accessCode = generateAccessCode();
            URI action = getActionUrl(accessCode);
            LoginFormsProvider provider = getSession().getProvider(LoginFormsProvider.class)
                    .setUser(getUser())
                    .setActionUri(action)
                    .setFormData(request.getDecodedFormParameters())
                    .setClientSessionCode(accessCode);
            if (getForwardedErrorMessage() != null) {
                provider.addError(getForwardedErrorMessage());
            } else if (getForwardedSuccessMessage() != null) {
                provider.addSuccess(getForwardedSuccessMessage());
            }
            return provider;
        }

        @Override
        public URI getActionUrl(String code) {
            return LoginActionsService.loginActionsBaseUrl(getUriInfo())
                    .path(AuthenticationProcessor.this.flowPath)
                    .queryParam(OAuth2Constants.CODE, code)
                    .queryParam("execution", getExecution().getId())
                    .build(getRealm().getName());
        }

        @Override
        public URI getActionUrl() {
            return getActionUrl(generateAccessCode());
        }

        @Override
        public void cancelLogin() {
            getEvent().error(Errors.REJECTED_BY_USER);
            LoginProtocol protocol = getSession().getProvider(LoginProtocol.class, getClientSession().getAuthMethod());
            protocol.setRealm(getRealm())
                    .setHttpHeaders(getHttpRequest().getHttpHeaders())
                    .setUriInfo(getUriInfo())
                    .setEventBuilder(event);
            Response response = protocol.sendError(getClientSession(), Error.CANCELLED_BY_USER);
            forceChallenge(response);
        }

        @Override
        public void resetFlow() {
            this.status = FlowStatus.FLOW_RESET;
        }

        @Override
        public void fork() {
            this.status = FlowStatus.FORK;
        }

        @Override
        public void forkWithSuccessMessage(FormMessage message) {
            this.status = FlowStatus.FORK;
            this.successMessage = message;

        }

        @Override
        public void forkWithErrorMessage(FormMessage message) {
            this.status = FlowStatus.FORK;
            this.errorMessage = message;

        }

        @Override
        public FormMessage getForwardedSuccessMessage() {
            return AuthenticationProcessor.this.forwardedSuccessMessage;
        }

        public FormMessage getErrorMessage() {
            return errorMessage;
        }

        public FormMessage getSuccessMessage() {
            return successMessage;
        }
    }

    public void logFailure() {
        if (realm.isBruteForceProtected()) {
            String username = clientSession.getNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
            // todo need to handle non form failures
            if (username == null) {

            } else {
                UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
                if (user != null) {
                    getBruteForceProtector().failedLogin(realm, user, connection);
                }
            }
        }
    }

    public boolean isSuccessful(AuthenticationExecutionModel model) {
        ClientSessionModel.ExecutionStatus status = clientSession.getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == ClientSessionModel.ExecutionStatus.SUCCESS;
    }

    public Response handleBrowserException(Exception failure) {
        if (failure instanceof AuthenticationFlowException) {
            AuthenticationFlowException e = (AuthenticationFlowException) failure;
            if (e.getError() == AuthenticationFlowError.INVALID_USER) {
                logger.failedAuthentication(e);
                event.error(Errors.USER_NOT_FOUND);
                return ErrorPage.error(session, Messages.INVALID_USER);
            } else if (e.getError() == AuthenticationFlowError.USER_DISABLED) {
                logger.failedAuthentication(e);
                event.error(Errors.USER_DISABLED);
                return ErrorPage.error(session, Messages.ACCOUNT_DISABLED);
            } else if (e.getError() == AuthenticationFlowError.USER_TEMPORARILY_DISABLED) {
                logger.failedAuthentication(e);
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return ErrorPage.error(session, Messages.INVALID_USER);

            } else if (e.getError() == AuthenticationFlowError.INVALID_CLIENT_SESSION) {
                logger.failedAuthentication(e);
                event.error(Errors.INVALID_CODE);
                return ErrorPage.error(session, Messages.INVALID_CODE);

            } else if (e.getError() == AuthenticationFlowError.EXPIRED_CODE) {
                logger.failedAuthentication(e);
                event.error(Errors.EXPIRED_CODE);
                return ErrorPage.error(session, Messages.EXPIRED_CODE);

            } else if (e.getError() == AuthenticationFlowError.FORK_FLOW) {
                ForkFlowException reset = (ForkFlowException)e;
                ClientSessionModel clone = clone(session, clientSession);
                clone.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
                AuthenticationProcessor processor = new AuthenticationProcessor();
                processor.setClientSession(clone)
                        .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                        .setFlowId(realm.getBrowserFlow().getId())
                        .setForwardedErrorMessage(reset.getErrorMessage())
                        .setForwardedSuccessMessage(reset.getSuccessMessage())
                        .setConnection(connection)
                        .setEventBuilder(event)
                        .setRealm(realm)
                        .setBrowserFlow(isBrowserFlow())
                        .setSession(session)
                        .setUriInfo(uriInfo)
                        .setRequest(request);
                CacheControlUtil.noBackButtonCacheControlHeader();
                return processor.authenticate();

            } else {
                logger.failedAuthentication(e);
                event.error(Errors.INVALID_USER_CREDENTIALS);
                return ErrorPage.error(session, Messages.INVALID_USER);
            }

        } else {
            logger.failedAuthentication(failure);
            event.error(Errors.INVALID_USER_CREDENTIALS);
            return ErrorPage.error(session, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
        }

    }

    public Response handleClientAuthException(Exception failure) {
        if (failure instanceof AuthenticationFlowException) {
            AuthenticationFlowException e = (AuthenticationFlowException) failure;
            logger.failedClientAuthentication(e);
            if (e.getError() == AuthenticationFlowError.CLIENT_NOT_FOUND) {
                event.error(Errors.CLIENT_NOT_FOUND);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Could not find client");
            } else if (e.getError() == AuthenticationFlowError.CLIENT_DISABLED) {
                event.error(Errors.CLIENT_DISABLED);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Client is not enabled");
            } else if (e.getError() == AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED) {
                event.error(Errors.INVALID_CLIENT_CREDENTIALS);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", e.getMessage());
            } else {
                event.error(Errors.INVALID_CLIENT_CREDENTIALS);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", e.getError().toString() + ": " + e.getMessage());
            }
        } else {
            logger.errorAuthenticatingClient(failure);
            event.error(Errors.INVALID_CLIENT_CREDENTIALS);
            return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Unexpected error when authenticating client: " + failure.getMessage());
        }
    }

    public AuthenticationFlow createFlowExecution(String flowId, AuthenticationExecutionModel execution) {
        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(flowId);
        if (flow == null) {
            logger.error("Unknown flow to execute with");
            throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }
        if (flow.getProviderId() == null || flow.getProviderId().equals(AuthenticationFlow.BASIC_FLOW)) {
            DefaultAuthenticationFlow flowExecution = new DefaultAuthenticationFlow(this, flow);
            return flowExecution;

        } else if (flow.getProviderId().equals(AuthenticationFlow.FORM_FLOW)) {
            FormAuthenticationFlow flowExecution = new FormAuthenticationFlow(this, execution);
            return flowExecution;
        } else if (flow.getProviderId().equals(AuthenticationFlow.CLIENT_FLOW)) {
            ClientAuthenticationFlow flowExecution = new ClientAuthenticationFlow(this, flow);
            return flowExecution;
        }
        throw new AuthenticationFlowException("Unknown flow provider type", AuthenticationFlowError.INTERNAL_ERROR);
    }

    public Response authenticate() throws AuthenticationFlowException {
        logger.debug("AUTHENTICATE");
        Response challenge = authenticateOnly();
        if (challenge != null) return challenge;
        return authenticationComplete();
    }

    public Response authenticateClient() throws AuthenticationFlowException {
        logger.debug("AUTHENTICATE CLIENT");
        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, null);
        try {
            Response challenge = authenticationFlow.processFlow();
            return challenge;
        } catch (Exception e) {
            return handleClientAuthException(e);
        }
    }


    public Response redirectToFlow() {
        String code = generateCode();

        URI redirect = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                .path(flowPath)
                .queryParam(OAuth2Constants.CODE, code).build(getRealm().getName());
        return Response.status(302).location(redirect).build();

    }

    public static Response redirectToRequiredActions(RealmModel realm, ClientSessionModel clientSession, UriInfo uriInfo) {

        // redirect to non-action url so browser refresh button works without reposting past data
        ClientSessionCode accessCode = new ClientSessionCode(realm, clientSession);
        accessCode.setAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name());
        clientSession.setTimestamp(Time.currentTime());

        URI redirect = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION)
                .queryParam(OAuth2Constants.CODE, accessCode.getCode()).build(realm.getName());
        return Response.status(302).location(redirect).build();

    }

    public static void resetFlow(ClientSessionModel clientSession) {
        logger.debug("RESET FLOW");
        clientSession.setTimestamp(Time.currentTime());
        clientSession.setAuthenticatedUser(null);
        clientSession.clearExecutionStatus();
        clientSession.clearUserSessionNotes();
        clientSession.removeNote(CURRENT_AUTHENTICATION_EXECUTION);
    }

    public static ClientSessionModel clone(KeycloakSession session, ClientSessionModel clientSession) {
        ClientSessionModel clone = session.sessions().createClientSession(clientSession.getRealm(), clientSession.getClient());
        for (Map.Entry<String, String> entry : clientSession.getNotes().entrySet()) {
            clone.setNote(entry.getKey(), entry.getValue());
        }
        clone.setRedirectUri(clientSession.getRedirectUri());
        clone.setAuthMethod(clientSession.getAuthMethod());
        clone.setTimestamp(Time.currentTime());
        clone.removeNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
        return clone;

    }


    public Response authenticationAction(String execution) {
        logger.debug("authenticationAction");
        checkClientSession();
        String current = clientSession.getNote(CURRENT_AUTHENTICATION_EXECUTION);
        if (!execution.equals(current)) {
            logger.debug("Current execution does not equal executed execution.  Might be a page refresh");
            //logFailure();
            //resetFlow(clientSession);
            return authenticate();
        }
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            logger.debug("Cannot find execution, reseting flow");
            logFailure();
            resetFlow(clientSession);
            return authenticate();
        }
        event.client(clientSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, clientSession.getAuthMethod());
        String authType = clientSession.getNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }

        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, model);
        Response challenge = authenticationFlow.processAction(execution);
        if (challenge != null) return challenge;
        if (clientSession.getAuthenticatedUser() == null) {
            throw new AuthenticationFlowException(AuthenticationFlowError.UNKNOWN_USER);
        }
        return authenticationComplete();
    }

    public void checkClientSession() {
        ClientSessionCode code = new ClientSessionCode(realm, clientSession);
        String action = ClientSessionModel.Action.AUTHENTICATE.name();
        if (!code.isValidAction(action)) {
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        }
        if (!code.isActionActive(ClientSessionCode.ActionType.LOGIN)) {
            throw new AuthenticationFlowException(AuthenticationFlowError.EXPIRED_CODE);
        }
        clientSession.setTimestamp(Time.currentTime());
    }

    public Response authenticateOnly() throws AuthenticationFlowException {
        logger.debug("AUTHENTICATE ONLY");
        checkClientSession();
        event.client(clientSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, clientSession.getAuthMethod());
        String authType = clientSession.getNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, null);
        Response challenge = authenticationFlow.processFlow();
        if (challenge != null) return challenge;
        if (clientSession.getAuthenticatedUser() == null) {
            throw new AuthenticationFlowException(AuthenticationFlowError.UNKNOWN_USER);
        }
        return challenge;
    }

    /**
     * Marks that at least one action was successful
     *
     */
    public void setActionSuccessful() {
        oneActionWasSuccessful = true;
    }

    public Response checkWasSuccessfulBrowserAction() {
        if (oneActionWasSuccessful && isBrowserFlow()) {
            // redirect to non-action url so browser refresh button works without reposting past data
            String code = generateCode();

            URI redirect = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                    .path(flowPath)
                    .queryParam(OAuth2Constants.CODE, code).build(getRealm().getName());
            return Response.status(302).location(redirect).build();
        } else {
            return null;
        }
    }

    public void attachSession() {
        String username = clientSession.getAuthenticatedUser().getUsername();
        String attemptedUsername = clientSession.getNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (attemptedUsername != null) username = attemptedUsername;
        String rememberMe = clientSession.getNote(Details.REMEMBER_ME);
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("true");
        if (userSession == null) { // if no authenticator attached a usersession
            userSession = session.sessions().createUserSession(realm, clientSession.getAuthenticatedUser(), username, connection.getRemoteAddr(), clientSession.getAuthMethod(), remember, null, null);
            userSession.setState(UserSessionModel.State.LOGGING_IN);
            userSessionCreated = true;
        }
        if (remember) {
            event.detail(Details.REMEMBER_ME, "true");
        }
        TokenManager.attachClientSession(userSession, clientSession);
        event.user(userSession.getUser())
                .detail(Details.USERNAME, username)
                .session(userSession);
    }

    public void evaluateRequiredActionTriggers() {
        AuthenticationManager.evaluateRequiredActionTriggers(session, userSession, clientSession, connection, request, uriInfo, event, realm, clientSession.getAuthenticatedUser());
    }

    public Response finishAuthentication(LoginProtocol protocol) {
        event.success();
        RealmModel realm = clientSession.getRealm();
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, connection, event, protocol);

    }

    public void validateUser(UserModel authenticatedUser) {
        if (authenticatedUser == null) return;
        if (!authenticatedUser.isEnabled()) throw new AuthenticationFlowException(AuthenticationFlowError.USER_DISABLED);
        if (realm.isBruteForceProtected()) {
            if (getBruteForceProtector().isTemporarilyDisabled(session, realm, authenticatedUser)) {
                throw new AuthenticationFlowException(AuthenticationFlowError.USER_TEMPORARILY_DISABLED);
            }
        }
    }

    protected Response authenticationComplete() {
        attachSession();
        if (isActionRequired()) {
            return redirectToRequiredActions(realm, clientSession, uriInfo);
        } else {
            event.detail(Details.CODE_ID, clientSession.getId());  // todo This should be set elsewhere.  find out why tests fail.  Don't know where this is supposed to be set
            return AuthenticationManager.finishedRequiredActions(session,  userSession, clientSession, connection, request, uriInfo, event);
        }
    }

    public boolean isActionRequired() {
        return AuthenticationManager.isActionRequired(session, userSession, clientSession, connection, request, uriInfo, event);
    }

    public AuthenticationProcessor.Result createAuthenticatorContext(AuthenticationExecutionModel model, Authenticator authenticator, List<AuthenticationExecutionModel> executions) {
        return new Result(model, authenticator, executions);
    }

    public AuthenticationProcessor.Result createClientAuthenticatorContext(AuthenticationExecutionModel model, ClientAuthenticator clientAuthenticator, List<AuthenticationExecutionModel> executions) {
        return new Result(model, clientAuthenticator, executions);
    }




}
