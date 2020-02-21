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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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
    public static final String LAST_PROCESSED_EXECUTION = "last.processed.execution";
    public static final String CURRENT_FLOW_PATH = "current.flow.path";
    public static final String FORKED_FROM = "forked.from";

    public static final String BROKER_SESSION_ID = "broker.session.id";
    public static final String BROKER_USER_ID = "broker.user.id";
    public static final String FORWARDED_PASSIVE_LOGIN = "forwarded.passive.login";

    protected static final Logger logger = Logger.getLogger(AuthenticationProcessor.class);
    protected RealmModel realm;
    protected UserSessionModel userSession;
    protected AuthenticationSessionModel authenticationSession;
    protected ClientConnection connection;
    protected UriInfo uriInfo;
    protected KeycloakSession session;
    protected EventBuilder event;
    protected HttpRequest request;
    protected String flowId;
    protected String flowPath;
    protected boolean browserFlow;
    protected BruteForceProtector protector;
    protected Runnable afterResetListener;
    /**
     * This could be an error message forwarded from another authenticator
     */
    protected FormMessage forwardedErrorMessage;

    /**
     * This could be an success message forwarded from another authenticator
     */
    protected FormMessage forwardedSuccessMessage;

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

    public AuthenticationSessionModel getAuthenticationSession() {
        return authenticationSession;
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

    public AuthenticationProcessor setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public AuthenticationProcessor setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
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
        ClientSessionCode accessCode = new ClientSessionCode(session, getRealm(), getAuthenticationSession());
        authenticationSession.getParentSession().setTimestamp(Time.currentTime());
        return accessCode.getOrGenerateCode();
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

    public String getFlowPath() {
        return flowPath;
    }

    public void setAutheticatedUser(UserModel user) {
        UserModel previousUser = getAuthenticationSession().getAuthenticatedUser();
        if (previousUser != null && !user.getId().equals(previousUser.getId()))
            throw new AuthenticationFlowException(AuthenticationFlowError.USER_CONFLICT);
        validateUser(user);
        getAuthenticationSession().setAuthenticatedUser(user);
    }

    public void clearAuthenticatedUser() {
        getAuthenticationSession().setAuthenticatedUser(null);
    }

    public URI getRefreshUrl(boolean authSessionIdParam) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                .path(AuthenticationProcessor.this.flowPath)
                .queryParam(Constants.CLIENT_ID, getAuthenticationSession().getClient().getClientId())
                .queryParam(Constants.TAB_ID, getAuthenticationSession().getTabId());
        if (authSessionIdParam) {
            uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, getAuthenticationSession().getParentSession().getId());
        }
        return uriBuilder
                .build(getRealm().getName());
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
        List<AuthenticationSelectionOption> authenticationSelections;

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
            return getAuthenticationSession().getAuthenticatedUser();
        }

        @Override
        public void setUser(UserModel user) {
            setAutheticatedUser(user);
        }

        @Override
        public List<AuthenticationSelectionOption> getAuthenticationSelections() {
            return authenticationSelections;
        }

        @Override
        public void setAuthenticationSelections(List<AuthenticationSelectionOption> authenticationSelections) {
            this.authenticationSelections = authenticationSelections;
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
        public AuthenticationSessionModel getAuthenticationSession() {
            return AuthenticationProcessor.this.getAuthenticationSession();
        }

        @Override
        public String getFlowPath() {
            return AuthenticationProcessor.this.getFlowPath();
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
                    .setAuthContext(this)
                    .setAuthenticationSession(getAuthenticationSession())
                    .setUser(getUser())
                    .setActionUri(action)
                    .setExecution(getExecution().getId())
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
            UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                    .path(AuthenticationProcessor.this.flowPath)
                    .queryParam(LoginActionsService.SESSION_CODE, code)
                    .queryParam(Constants.EXECUTION, getExecution().getId())
                    .queryParam(Constants.CLIENT_ID, getAuthenticationSession().getClient().getClientId())
                    .queryParam(Constants.TAB_ID, getAuthenticationSession().getTabId());
            if (getUriInfo().getQueryParameters().containsKey(LoginActionsService.AUTH_SESSION_ID)) {
                uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, getAuthenticationSession().getParentSession().getId());
            }
            return uriBuilder
                    .build(getRealm().getName());
        }

        @Override
        public URI getActionTokenUrl(String tokenString) {
            UriBuilder uriBuilder = LoginActionsService.actionTokenProcessor(getUriInfo())
                    .queryParam(Constants.KEY, tokenString)
                    .queryParam(Constants.EXECUTION, getExecution().getId())
                    .queryParam(Constants.CLIENT_ID, getAuthenticationSession().getClient().getClientId())
                    .queryParam(Constants.TAB_ID, getAuthenticationSession().getTabId());
            if (getUriInfo().getQueryParameters().containsKey(LoginActionsService.AUTH_SESSION_ID)) {
                uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, getAuthenticationSession().getParentSession().getId());
            }
            return uriBuilder
                    .build(getRealm().getName());
        }

        @Override
        public URI getActionUrl(String code, boolean authSessionIdParam) {
            UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                    .path(AuthenticationProcessor.this.flowPath)
                    .queryParam(LoginActionsService.SESSION_CODE, code)
                    .queryParam(Constants.EXECUTION, getExecution().getId())
                    .queryParam(Constants.CLIENT_ID, getAuthenticationSession().getClient().getClientId())
                    .queryParam(Constants.TAB_ID, getAuthenticationSession().getTabId());
            if (authSessionIdParam) {
                uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, getAuthenticationSession().getParentSession().getId());
            }
            return uriBuilder
                    .build(getRealm().getName());
        }

        @Override
        public URI getRefreshExecutionUrl() {
            UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(getUriInfo())
                    .path(AuthenticationProcessor.this.flowPath)
                    .queryParam(Constants.EXECUTION, getExecution().getId())
                    .queryParam(Constants.CLIENT_ID, getAuthenticationSession().getClient().getClientId())
                    .queryParam(Constants.TAB_ID, getAuthenticationSession().getTabId());
            if (getUriInfo().getQueryParameters().containsKey(LoginActionsService.AUTH_SESSION_ID)) {
                uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, getAuthenticationSession().getParentSession().getId());
            }
            return uriBuilder
                    .build(getRealm().getName());
        }

        @Override
        public URI getRefreshUrl(boolean authSessionIdParam) {
            return AuthenticationProcessor.this.getRefreshUrl(authSessionIdParam);
        }

        @Override
        public void cancelLogin() {
            getEvent().error(Errors.REJECTED_BY_USER);
            LoginProtocol protocol = getSession().getProvider(LoginProtocol.class, getAuthenticationSession().getProtocol());
            protocol.setRealm(getRealm())
                    .setHttpHeaders(getHttpRequest().getHttpHeaders())
                    .setUriInfo(getUriInfo())
                    .setEventBuilder(event);
            Response response = protocol.sendError(getAuthenticationSession(), Error.CANCELLED_BY_USER);
            forceChallenge(response);
        }

        @Override
        public void resetFlow() {
            this.status = FlowStatus.FLOW_RESET;
        }

        @Override
        public void resetFlow(Runnable afterResetListener) {
            this.status = FlowStatus.FLOW_RESET;
            AuthenticationProcessor.this.afterResetListener = afterResetListener;
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
            String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
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
        AuthenticationSessionModel.ExecutionStatus status = authenticationSession.getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.SUCCESS;
    }

    public boolean isEvaluatedTrue(AuthenticationExecutionModel model) {
        AuthenticationSessionModel.ExecutionStatus status = authenticationSession.getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.EVALUATED_TRUE;
    }

    public boolean isEvaluatedFalse(AuthenticationExecutionModel model) {
        AuthenticationSessionModel.ExecutionStatus status = authenticationSession.getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.EVALUATED_FALSE;
    }

    public Response handleBrowserExceptionList(AuthenticationFlowException e) {
        LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authenticationSession);
        ServicesLogger.LOGGER.failedAuthentication(e);
        forms.addError(new FormMessage(Messages.UNEXPECTED_ERROR_HANDLING_REQUEST));
        for (AuthenticationFlowException afe : e.getAfeList()) {
            ServicesLogger.LOGGER.failedAuthentication(afe);
            switch (afe.getError()){
                case INVALID_USER:
                    event.error(Errors.USER_NOT_FOUND);
                    forms.addError(new FormMessage(Messages.INVALID_USER));
                    break;
                case USER_DISABLED:
                    event.error(Errors.USER_DISABLED);
                    forms.addError(new FormMessage(Messages.ACCOUNT_DISABLED));
                    break;
                case USER_TEMPORARILY_DISABLED:
                    event.error(Errors.USER_TEMPORARILY_DISABLED);
                    forms.addError(new FormMessage(Messages.INVALID_USER));
                    break;
                case INVALID_CLIENT_SESSION:
                    event.error(Errors.INVALID_CODE);
                    forms.addError(new FormMessage(Messages.INVALID_CODE));
                    break;
                case EXPIRED_CODE:
                    event.error(Errors.EXPIRED_CODE);
                    forms.addError(new FormMessage(Messages.EXPIRED_CODE));
                    break;
                case DISPLAY_NOT_SUPPORTED:
                    event.error(Errors.DISPLAY_UNSUPPORTED);
                    forms.addError(new FormMessage(Messages.DISPLAY_UNSUPPORTED));
                    break;
                case CREDENTIAL_SETUP_REQUIRED:
                    event.error(Errors.INVALID_USER_CREDENTIALS);
                    forms.addError(new FormMessage(Messages.CREDENTIAL_SETUP_REQUIRED));
                    break;
            }
        }
        return forms.createErrorPage(Response.Status.BAD_REQUEST);
    }

    public Response handleBrowserException(Exception failure) {
        if (failure instanceof AuthenticationFlowException) {
            AuthenticationFlowException e = (AuthenticationFlowException) failure;
            if (e.getAfeList() != null && !e.getAfeList().isEmpty()){
                return handleBrowserExceptionList(e);
            }

            if (e.getError() == AuthenticationFlowError.INVALID_USER) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.USER_NOT_FOUND);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.INVALID_USER);
            } else if (e.getError() == AuthenticationFlowError.USER_DISABLED) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.USER_DISABLED);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session,authenticationSession, Response.Status.BAD_REQUEST, Messages.ACCOUNT_DISABLED);
            } else if (e.getError() == AuthenticationFlowError.USER_TEMPORARILY_DISABLED) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session,authenticationSession, Response.Status.BAD_REQUEST, Messages.INVALID_USER);

            } else if (e.getError() == AuthenticationFlowError.INVALID_CLIENT_SESSION) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.INVALID_CODE);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.INVALID_CODE);

            } else if (e.getError() == AuthenticationFlowError.EXPIRED_CODE) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.EXPIRED_CODE);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);

            } else if (e.getError() == AuthenticationFlowError.FORK_FLOW) {
                ForkFlowException reset = (ForkFlowException)e;

                AuthenticationSessionModel clone = clone(session, authenticationSession);

                clone.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
                setAuthenticationSession(clone);
                session.getProvider(LoginFormsProvider.class).setAuthenticationSession(clone);

                AuthenticationProcessor processor = new AuthenticationProcessor();
                processor.setAuthenticationSession(clone)
                        .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                        .setFlowId(AuthenticationFlowResolver.resolveBrowserFlow(clone).getId())
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

            } else if (e.getError() == AuthenticationFlowError.DISPLAY_NOT_SUPPORTED) {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.DISPLAY_UNSUPPORTED);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.DISPLAY_UNSUPPORTED);
            } else if (e.getError() == AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED){
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.INVALID_USER_CREDENTIALS);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.CREDENTIAL_SETUP_REQUIRED);
            } else {
                ServicesLogger.LOGGER.failedAuthentication(e);
                event.error(Errors.INVALID_USER_CREDENTIALS);
                if (e.getResponse() != null) return e.getResponse();
                return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.INVALID_USER);
            }

        } else {
            ServicesLogger.LOGGER.failedAuthentication(failure);
            event.error(Errors.INVALID_USER_CREDENTIALS);
            return ErrorPage.error(session, authenticationSession, Response.Status.BAD_REQUEST, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
        }

    }

    public Response handleClientAuthException(Exception failure) {
        if (failure instanceof AuthenticationFlowException) {
            AuthenticationFlowException e = (AuthenticationFlowException) failure;
            ServicesLogger.LOGGER.failedClientAuthentication(e);
            if (e.getError() == AuthenticationFlowError.CLIENT_NOT_FOUND) {
                event.error(Errors.CLIENT_NOT_FOUND);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Invalid client credentials");
            } else if (e.getError() == AuthenticationFlowError.CLIENT_DISABLED) {
                event.error(Errors.CLIENT_DISABLED);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Invalid client credentials");
            } else if (e.getError() == AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED) {
                event.error(Errors.INVALID_CLIENT_CREDENTIALS);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", e.getMessage());
            } else {
                event.error(Errors.INVALID_CLIENT_CREDENTIALS);
                return ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", e.getError().toString() + ": " + e.getMessage());
            }
        } else {
            ServicesLogger.LOGGER.errorAuthenticatingClient(failure);
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
            if (challenge != null) return challenge;
            if (!authenticationFlow.isSuccessful()) {
                throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
            }
            return null;
        } catch (Exception e) {
            return handleClientAuthException(e);
        }
    }


    public Response redirectToFlow() {
        URI redirect = new AuthenticationFlowURLHelper(session, realm, uriInfo).getLastExecutionUrl(authenticationSession);

        logger.debug("Redirecting to URL: " + redirect.toString());

        return Response.status(302).location(redirect).build();

    }

    public void resetFlow() {
        resetFlow(authenticationSession, flowPath);

        if (afterResetListener != null) {
            afterResetListener.run();
        }
    }

    public static void resetFlow(AuthenticationSessionModel authSession, String flowPath) {
        logger.debug("RESET FLOW");
        authSession.getParentSession().setTimestamp(Time.currentTime());
        authSession.setAuthenticatedUser(null);
        authSession.clearExecutionStatus();
        authSession.clearUserSessionNotes();
        authSession.clearAuthNotes();

        authSession.setAction(CommonClientSessionModel.Action.AUTHENTICATE.name());

        authSession.setAuthNote(CURRENT_FLOW_PATH, flowPath);
    }


    // Clone new authentication session from the given authSession. New authenticationSession will have same parent (rootSession) and will use same client
    public static AuthenticationSessionModel clone(KeycloakSession session, AuthenticationSessionModel authSession) {
        AuthenticationSessionModel clone = authSession.getParentSession().createAuthenticationSession(authSession.getClient());

        clone.setRedirectUri(authSession.getRedirectUri());
        clone.setProtocol(authSession.getProtocol());

        for (Map.Entry<String, String> clientNote : authSession.getClientNotes().entrySet()) {
            clone.setClientNote(clientNote.getKey(), clientNote.getValue());
        }

        clone.setAuthNote(FORKED_FROM, authSession.getTabId());

        logger.debugf("Forked authSession %s from authSession %s . Client: %s, Root session: %s",
                clone.getTabId(), authSession.getTabId(), authSession.getClient().getClientId(), authSession.getParentSession().getId());

        return clone;
    }


    public Response authenticationAction(String execution) {
        logger.debug("authenticationAction");
        checkClientSession(true);
        String current = authenticationSession.getAuthNote(CURRENT_AUTHENTICATION_EXECUTION);
        if (execution == null || !execution.equals(current)) {
            logger.debug("Current execution does not equal executed execution.  Might be a page refresh");
            return new AuthenticationFlowURLHelper(session, realm, uriInfo).showPageExpired(authenticationSession);
        }
        UserModel authUser = authenticationSession.getAuthenticatedUser();
        validateUser(authUser);
        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            logger.debug("Cannot find execution, reseting flow");
            logFailure();
            resetFlow();
            return authenticate();
        }
        event.client(authenticationSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, authenticationSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authenticationSession.getProtocol());
        String authType = authenticationSession.getAuthNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }

        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, model);
        Response challenge = authenticationFlow.processAction(execution);
        if (challenge != null) return challenge;
        if (authenticationSession.getAuthenticatedUser() == null) {
            throw new AuthenticationFlowException(AuthenticationFlowError.UNKNOWN_USER);
        }
        if (!authenticationFlow.isSuccessful()) {
            throw new AuthenticationFlowException(authenticationFlow.getFlowExceptions());
        }
        return authenticationComplete();
    }

    private void checkClientSession(boolean checkAction) {
        ClientSessionCode code = new ClientSessionCode(session, realm, authenticationSession);

        if (checkAction) {
            String action = AuthenticationSessionModel.Action.AUTHENTICATE.name();
            if (!code.isValidAction(action)) {
                throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_CLIENT_SESSION);
            }
        }
        if (!code.isActionActive(ClientSessionCode.ActionType.LOGIN)) {
            throw new AuthenticationFlowException(AuthenticationFlowError.EXPIRED_CODE);
        }

        authenticationSession.getParentSession().setTimestamp(Time.currentTime());
    }

    public Response authenticateOnly() throws AuthenticationFlowException {
        logger.debug("AUTHENTICATE ONLY");
        checkClientSession(false);
        event.client(authenticationSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, authenticationSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authenticationSession.getProtocol());
        String authType = authenticationSession.getAuthNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }
        UserModel authUser = authenticationSession.getAuthenticatedUser();
        validateUser(authUser);
        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, null);
        Response challenge = authenticationFlow.processFlow();
        if (challenge != null) return challenge;
        if (authenticationSession.getAuthenticatedUser() == null) {
            throw new AuthenticationFlowException(AuthenticationFlowError.UNKNOWN_USER);
        }
        if (!authenticationFlow.isSuccessful()) {
            throw new AuthenticationFlowException(authenticationFlow.getFlowExceptions());
        }
        return null;
    }

    // May create userSession too
    public ClientSessionContext attachSession() {
        ClientSessionContext clientSessionCtx = attachSession(authenticationSession, userSession, session, realm, connection, event);

        if (userSession == null) {
            userSession = clientSessionCtx.getClientSession().getUserSession();
        }

        return clientSessionCtx;
    }

    // May create new userSession too (if userSession argument is null)
    public static ClientSessionContext attachSession(AuthenticationSessionModel authSession, UserSessionModel userSession, KeycloakSession session, RealmModel realm, ClientConnection connection, EventBuilder event) {
        String username = authSession.getAuthenticatedUser().getUsername();
        String attemptedUsername = authSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (attemptedUsername != null) username = attemptedUsername;
        String rememberMe = authSession.getAuthNote(Details.REMEMBER_ME);
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("true");
        String brokerSessionId = authSession.getAuthNote(BROKER_SESSION_ID);
        String brokerUserId = authSession.getAuthNote(BROKER_USER_ID);

        if (userSession == null) { // if no authenticator attached a usersession

            userSession = session.sessions().getUserSession(realm, authSession.getParentSession().getId());
            if (userSession == null) {
                userSession = session.sessions().createUserSession(authSession.getParentSession().getId(), realm, authSession.getAuthenticatedUser(), username, connection.getRemoteAddr(), authSession.getProtocol()
                        , remember, brokerSessionId, brokerUserId);
            } else if (userSession.getUser() == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
                userSession.restartSession(realm, authSession.getAuthenticatedUser(), username, connection.getRemoteAddr(), authSession.getProtocol()
                        , remember, brokerSessionId, brokerUserId);
            } else {
                // We have existing userSession even if it wasn't attached to authenticator. Could happen if SSO authentication was ignored (eg. prompt=login) and in some other cases.
                // We need to handle case when different user was used
                logger.debugf("No SSO login, but found existing userSession with ID '%s' after finished authentication.", userSession.getId());
                if (!authSession.getAuthenticatedUser().equals(userSession.getUser())) {
                    event.detail(Details.EXISTING_USER, userSession.getUser().getId());
                    event.error(Errors.DIFFERENT_USER_AUTHENTICATED);
                    throw new ErrorPageException(session, authSession, Response.Status.INTERNAL_SERVER_ERROR, Messages.DIFFERENT_USER_AUTHENTICATED, userSession.getUser().getUsername());
                }
            }
            userSession.setState(UserSessionModel.State.LOGGED_IN);
        }

        if (remember) {
            event.detail(Details.REMEMBER_ME, "true");
        }

        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

        event.user(userSession.getUser())
                .detail(Details.USERNAME, username)
                .session(userSession);

        return clientSessionCtx;
    }

    public void evaluateRequiredActionTriggers() {
        AuthenticationManager.evaluateRequiredActionTriggers(session, authenticationSession, connection, request, uriInfo, event, realm, authenticationSession.getAuthenticatedUser());
    }

    public Response finishAuthentication(LoginProtocol protocol) {
        event.success();
        RealmModel realm = authenticationSession.getRealm();
        ClientSessionContext clientSessionCtx = attachSession();
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, connection, event, authenticationSession, protocol);

    }

    public void validateUser(UserModel authenticatedUser) {
        if (authenticatedUser == null) return;
        if (!authenticatedUser.isEnabled()) throw new AuthenticationFlowException(AuthenticationFlowError.USER_DISABLED);
    }
    
    protected Response authenticationComplete() {
        // attachSession(); // Session will be attached after requiredActions + consents are finished.
        AuthenticationManager.setClientScopesInSession(authenticationSession);

        String nextRequiredAction = nextRequiredAction();
        if (nextRequiredAction != null) {
            return AuthenticationManager.redirectToRequiredActions(session, realm, authenticationSession, uriInfo, nextRequiredAction);
        } else {
            event.detail(Details.CODE_ID, authenticationSession.getParentSession().getId());  // todo This should be set elsewhere.  find out why tests fail.  Don't know where this is supposed to be set
            return AuthenticationManager.finishedRequiredActions(session, authenticationSession, userSession, connection, request, uriInfo, event);
        }
    }

    public String nextRequiredAction() {
        return AuthenticationManager.nextRequiredAction(session, authenticationSession, connection, request, uriInfo, event);
    }

    public AuthenticationProcessor.Result createAuthenticatorContext(AuthenticationExecutionModel model, Authenticator authenticator, List<AuthenticationExecutionModel> executions) {
        return new Result(model, authenticator, executions);
    }

    public AuthenticationProcessor.Result createClientAuthenticatorContext(AuthenticationExecutionModel model, ClientAuthenticator clientAuthenticator, List<AuthenticationExecutionModel> executions) {
        return new Result(model, clientAuthenticator, executions);
    }




}
