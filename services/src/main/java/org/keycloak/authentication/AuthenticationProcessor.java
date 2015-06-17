package org.keycloak.authentication;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationProcessor {
    protected static Logger logger = Logger.getLogger(AuthenticationProcessor.class);
    protected RealmModel realm;
    protected UserSessionModel userSession;
    protected ClientSessionModel clientSession;
    protected ClientConnection connection;
    protected UriInfo uriInfo;
    protected KeycloakSession session;
    protected BruteForceProtector protector;
    protected EventBuilder event;
    protected HttpRequest request;
    protected String flowId;
    protected String action;
    /**
     * This could be an error message forwarded from brokering when the broker failed authentication
     * and we want to continue authentication locally.  forwardedErrorMessage can then be displayed by
     * whatever form is challenging.
     */
    protected String forwardedErrorMessage;
    protected boolean userSessionCreated;


    public static enum Status {
        SUCCESS,
        CHALLENGE,
        FORCE_CHALLENGE,
        FAILURE_CHALLENGE,
        FAILED,
        ATTEMPTED

    }
    public static enum Error {
        EXPIRED_CODE,
        INVALID_CLIENT_SESSION,
        INVALID_USER,
        INVALID_CREDENTIALS,
        CREDENTIAL_SETUP_REQUIRED,
        USER_DISABLED,
        USER_CONFLICT,
        USER_TEMPORARILY_DISABLED,
        INTERNAL_ERROR,
        UNKNOWN_USER
    }

    public RealmModel getRealm() {
        return realm;
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

    public AuthenticationProcessor setProtector(BruteForceProtector protector) {
        this.protector = protector;
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

    public AuthenticationProcessor setAction(String action) {
        this.action = action;
        return this;
    }

    public AuthenticationProcessor setForwardedErrorMessage(String forwardedErrorMessage) {
        this.forwardedErrorMessage = forwardedErrorMessage;
        return this;
    }

    private class Result implements AuthenticatorContext {
        AuthenticatorModel model;
        AuthenticationExecutionModel execution;
        Authenticator authenticator;
        Status status;
        Response challenge;
        Error error;

        private Result(AuthenticationExecutionModel execution, AuthenticatorModel model, Authenticator authenticator) {
            this.execution = execution;
            this.model = model;
            this.authenticator = authenticator;
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return execution;
        }

        @Override
        public void setExecution(AuthenticationExecutionModel execution) {
            this.execution = execution;
        }

        @Override
        public AuthenticatorModel getAuthenticatorModel() {
            return model;
        }

        @Override
        public void setAuthenticatorModel(AuthenticatorModel model) {
            this.model = model;
        }

        @Override
        public String getAction() {
            return AuthenticationProcessor.this.action;
        }

        @Override
        public Authenticator getAuthenticator() {
            return authenticator;
        }

        @Override
        public void setAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public void success() {
            this.status = Status.SUCCESS;
        }

        @Override
        public void failure(Error error) {
            status = Status.FAILED;
            this.error = error;

        }

        @Override
        public void challenge(Response challenge) {
            this.status = Status.CHALLENGE;
            this.challenge = challenge;

        }
        @Override
        public void forceChallenge(Response challenge) {
            this.status = Status.FORCE_CHALLENGE;
            this.challenge = challenge;

        }
        @Override
        public void failureChallenge(Error error, Response challenge) {
            this.error = error;
            this.status = Status.FAILURE_CHALLENGE;
            this.challenge = challenge;

        }
        @Override
        public void failure(Error error, Response challenge) {
            this.error = error;
            this.status = Status.FAILED;
            this.challenge = challenge;

        }

        @Override
        public void attempted() {
            this.status = Status.ATTEMPTED;

        }

        @Override
        public UserModel getUser() {
            return getClientSession().getAuthenticatedUser();
        }

        @Override
        public void setUser(UserModel user) {
            UserModel previousUser = getUser();
            if (previousUser != null && !user.getId().equals(previousUser.getId())) throw new AuthException(Error.USER_CONFLICT);
            validateUser(user);
            getClientSession().setAuthenticatedUser(user);
        }

        @Override
        public RealmModel getRealm() {
            return AuthenticationProcessor.this.getRealm();
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
            return AuthenticationProcessor.this.protector;
        }

        @Override
        public EventBuilder getEvent() {
            return AuthenticationProcessor.this.event;
        }

        @Override
        public String getForwardedErrorMessage() {
            return AuthenticationProcessor.this.forwardedErrorMessage;
        }

        @Override
        public String generateAccessCode() {
            ClientSessionCode accessCode = new ClientSessionCode(getRealm(), getClientSession());
            accessCode.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
            return accessCode.getCode();
        }
    }

    public static class AuthException extends RuntimeException {
        private Error error;

        public AuthException(Error error) {
            this.error = error;
        }

        public AuthException(String message, Error error) {
            super(message);
            this.error = error;
        }

        public AuthException(String message, Throwable cause, Error error) {
            super(message, cause);
            this.error = error;
        }

        public AuthException(Throwable cause, Error error) {
            super(cause);
            this.error = error;
        }

        public AuthException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Error error) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.error = error;
        }

        public Error getError() {
            return error;
        }
    }

    public void logUserFailure() {

    }

    protected boolean isProcessed(AuthenticationExecutionModel model) {
        if (model.isDisabled()) return true;
        UserSessionModel.AuthenticatorStatus status = clientSession.getAuthenticators().get(model.getId());
        if (status == null) return false;
        return status == UserSessionModel.AuthenticatorStatus.SUCCESS || status == UserSessionModel.AuthenticatorStatus.SKIPPED
                || status == UserSessionModel.AuthenticatorStatus.ATTEMPTED
                || status == UserSessionModel.AuthenticatorStatus.SETUP_REQUIRED;
    }

    public boolean isSuccessful(AuthenticationExecutionModel model) {
        UserSessionModel.AuthenticatorStatus status = clientSession.getAuthenticators().get(model.getId());
        if (status == null) return false;
        return status == UserSessionModel.AuthenticatorStatus.SUCCESS;
    }

    public Response handleBrowserException(Exception failure) {
        if (failure instanceof AuthException) {
            AuthException e = (AuthException)failure;
            logger.error("failed authentication: " + e.getError().toString(), e);
            if (e.getError() == AuthenticationProcessor.Error.INVALID_USER) {
                event.error(Errors.USER_NOT_FOUND);
                return ErrorPage.error(session, Messages.INVALID_USER);
            } else if (e.getError() == AuthenticationProcessor.Error.USER_DISABLED) {
                event.error(Errors.USER_DISABLED);
                return ErrorPage.error(session, Messages.ACCOUNT_DISABLED);
            } else if (e.getError() == AuthenticationProcessor.Error.USER_TEMPORARILY_DISABLED) {
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return ErrorPage.error(session, Messages.ACCOUNT_TEMPORARILY_DISABLED);

            } else if (e.getError() == Error.INVALID_CLIENT_SESSION) {
                event.error(Errors.INVALID_CODE);
                return ErrorPage.error(session, Messages.INVALID_CODE);

            }  else if (e.getError() == Error.EXPIRED_CODE) {
                event.error(Errors.EXPIRED_CODE);
                return ErrorPage.error(session, Messages.EXPIRED_CODE);

            }else {
                event.error(Errors.INVALID_USER_CREDENTIALS);
                return ErrorPage.error(session, Messages.INVALID_USER);
            }

        } else {
            logger.error("failed authentication", failure);
            event.error(Errors.INVALID_USER_CREDENTIALS);
            return ErrorPage.error(session, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
        }

    }


    public Response authenticate() throws AuthException {
        checkClientSession();
        logger.debug("AUTHENTICATE");
        event.event(EventType.LOGIN);
        event.client(clientSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, clientSession.getAuthMethod());
        String authType = clientSession.getNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        Response challenge = processFlow(flowId);
        if (challenge != null) return challenge;
        if (clientSession.getAuthenticatedUser() == null) {
            throw new AuthException(Error.UNKNOWN_USER);
        }
        return authenticationComplete();
    }

    public void checkClientSession() {
        ClientSessionCode code = new ClientSessionCode(realm, clientSession);
        if (!code.isValidAction(ClientSessionModel.Action.AUTHENTICATE.name())) {
            throw new AuthException(Error.INVALID_CLIENT_SESSION);
        }
        if (!code.isActionActive(ClientSessionModel.Action.AUTHENTICATE.name())) {
            throw new AuthException(Error.EXPIRED_CODE);
        }
    }

    public Response authenticateOnly() throws AuthException {
        checkClientSession();
        event.event(EventType.LOGIN);
        event.client(clientSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, clientSession.getAuthMethod());
        String authType = clientSession.getNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        Response challenge = processFlow(flowId);
        if (challenge != null) return challenge;

        String username = clientSession.getAuthenticatedUser().getUsername();
        if (userSession == null) { // if no authenticator attached a usersession
            userSession = session.sessions().createUserSession(realm, clientSession.getAuthenticatedUser(), username, connection.getRemoteAddr(), "form", false, null, null);
            userSession.setState(UserSessionModel.State.LOGGING_IN);
            userSessionCreated = true;
        }
        TokenManager.attachClientSession(userSession, clientSession);
        event.user(userSession.getUser())
                .detail(Details.USERNAME, username)
                .session(userSession);

        return AuthenticationManager.actionRequired(session, userSession, clientSession, connection, request, uriInfo, event);
    }

    public Response finishAuthentication() {
        event.success();
        RealmModel realm = clientSession.getRealm();
        return AuthenticationManager.redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, connection);

    }

    public Response processFlow(String flowId) {
        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(flowId);
        if (flow == null) {
            logger.error("Unknown flow to execute with");
            throw new AuthException(Error.INTERNAL_ERROR);
        }
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(flowId);
        if (executions == null) return null;
        Response alternativeChallenge = null;
        AuthenticationExecutionModel challengedAlternativeExecution = null;
        boolean alternativeSuccessful = false;
        for (AuthenticationExecutionModel model : executions) {
            if (isProcessed(model)) {
                logger.debug("execution is processed");
                if (!alternativeSuccessful && model.isAlternative() && isSuccessful(model)) alternativeSuccessful = true;
                continue;
            }
            Result context = null;
            if (model.isAlternative() && alternativeSuccessful) {
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SKIPPED);
                continue;
            }
            if (model.isAutheticatorFlow()) {
                Response flowResponse = processFlow(model.getAuthenticator());
                if (flowResponse == null) {
                    clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SUCCESS);
                    if (model.isAlternative()) alternativeSuccessful = true;
                    continue;
                } else {
                    return flowResponse;
                }

            }

            AuthenticatorModel authenticatorModel = realm.getAuthenticatorById(model.getAuthenticator());
            AuthenticatorFactory factory = (AuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, authenticatorModel.getProviderId());
            Authenticator authenticator = factory.create(authenticatorModel);
            logger.debugv("authenticator: {0}", authenticatorModel.getProviderId());
            UserModel authUser = clientSession.getAuthenticatedUser();

            if (authenticator.requiresUser() && authUser == null){
                if (alternativeChallenge != null) {
                    clientSession.setAuthenticatorStatus(challengedAlternativeExecution.getId(), UserSessionModel.AuthenticatorStatus.CHALLENGED);
                    return alternativeChallenge;
                }
                throw new AuthException("authenticator: " + authenticatorModel.getProviderId(), Error.UNKNOWN_USER);
            }
            boolean configuredFor = false;
            if (authenticator.requiresUser() && authUser != null) {
                configuredFor = authenticator.configuredFor(session, realm, authUser);
                if (!configuredFor) {
                    if (model.isRequired()) {
                        if (model.isUserSetupAllowed()) {
                            logger.debugv("authenticator SETUP_REQUIRED: {0}", authenticatorModel.getProviderId());
                            clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SETUP_REQUIRED);
                            authenticator.setRequiredActions(session, realm, clientSession.getAuthenticatedUser());
                            continue;
                        } else {
                            throw new AuthException(Error.CREDENTIAL_SETUP_REQUIRED);
                        }
                    } else if (model.isOptional()) {
                        clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SKIPPED);
                        continue;
                    }
                }
            }
            context = new Result(model, authenticatorModel, authenticator);
            authenticator.authenticate(context);
            Status result = context.getStatus();
            if (result == Status.SUCCESS){
                logger.debugv("authenticator SUCCESS: {0}", authenticatorModel.getProviderId());
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SUCCESS);
                if (model.isAlternative()) alternativeSuccessful = true;
                continue;
            } else if (result == Status.FAILED) {
                logger.debugv("authenticator FAILED: {0}", authenticatorModel.getProviderId());
                logUserFailure();
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.FAILED);
                if (context.challenge != null) return context.challenge;
                throw new AuthException(context.error);
            } else if (result == Status.FORCE_CHALLENGE) {
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.CHALLENGED);
                return context.challenge;
            } else if (result == Status.CHALLENGE) {
                logger.debugv("authenticator CHALLENGE: {0}", authenticatorModel.getProviderId());
                if (model.isRequired() || (model.isOptional() && configuredFor)) {
                    clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.CHALLENGED);
                    return context.challenge;
                }
                else if (model.isAlternative()) {
                    alternativeChallenge = context.challenge;
                    challengedAlternativeExecution = model;
                } else {
                    clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SKIPPED);
                }
                continue;
            } else if (result == Status.FAILURE_CHALLENGE) {
                logger.debugv("authenticator FAILURE_CHALLENGE: {0}", authenticatorModel.getProviderId());
                logUserFailure();
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.CHALLENGED);
                return context.challenge;
            } else if (result == Status.ATTEMPTED) {
                logger.debugv("authenticator ATTEMPTED: {0}", authenticatorModel.getProviderId());
                if (model.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    throw new AuthException(Error.INVALID_CREDENTIALS);
                }
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.ATTEMPTED);
                continue;
            } else {
                logger.debugv("authenticator INTERNAL_ERROR: {0}", authenticatorModel.getProviderId());
                logger.error("Unknown result status");
                throw new AuthException(Error.INTERNAL_ERROR);
            }
        }
        return null;
    }



    public void validateUser(UserModel authenticatedUser) {
        if (authenticatedUser != null) {
            if (!authenticatedUser.isEnabled()) throw new AuthException(Error.USER_DISABLED);
        }
        if (realm.isBruteForceProtected()) {
            if (protector.isTemporarilyDisabled(session, realm, authenticatedUser.getUsername())) {
                throw new AuthException(Error.USER_TEMPORARILY_DISABLED);
            }
        }
    }

    protected Response authenticationComplete() {
        String username = clientSession.getAuthenticatedUser().getUsername();
        String rememberMe = clientSession.getNote(Details.REMEMBER_ME);
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("true");
        if (userSession == null) { // if no authenticator attached a usersession
            userSession = session.sessions().createUserSession(realm, clientSession.getAuthenticatedUser(), username, connection.getRemoteAddr(), clientSession.getAuthMethod(), remember, null, null);
            userSession.setState(UserSessionModel.State.LOGGING_IN);
        }
        if (remember) {
            event.detail(Details.REMEMBER_ME, "true");
        }
        TokenManager.attachClientSession(userSession, clientSession);
        event.user(userSession.getUser())
             .detail(Details.USERNAME, username)
             .session(userSession);

        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, connection, request, uriInfo, event);

    }


}
