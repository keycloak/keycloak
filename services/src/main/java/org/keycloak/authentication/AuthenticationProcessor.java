package org.keycloak.authentication;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

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
    protected EventBuilder eventBuilder;
    protected HttpRequest request;
    protected String flowId;


    public static enum Status {
        SUCCESS,
        CHALLENGE,
        FAILURE_CHALLENGE,
        FAILED,
        ATTEMPTED

    }
    public static enum Error {
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
        this.eventBuilder = eventBuilder;
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

    private class Result implements AuthenticatorContext {
        AuthenticatorModel model;
        Authenticator authenticator;
        Status status;
        Response challenge;
        Error error;

        private Result(AuthenticatorModel model, Authenticator authenticator) {
            this.model = model;
            this.authenticator = authenticator;
        }

        @Override
        public AuthenticatorModel getModel() {
            return model;
        }

        @Override
        public void setModel(AuthenticatorModel model) {
            this.model = model;
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

    public Response authenticate() throws AuthException {
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        Response challenge = processFlow(flowId);
        if (challenge != null) return challenge;
        if (clientSession.getAuthenticatedUser() == null) {
            throw new AuthException(Error.UNKNOWN_USER);
        }
        return authenticationComplete();

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
        boolean alternativeSuccessful = false;
        for (AuthenticationExecutionModel model : executions) {
            if (isProcessed(model)) {
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
            UserModel authUser = clientSession.getAuthenticatedUser();

            if (authenticator.requiresUser() && authUser == null){
                if (alternativeChallenge != null) return alternativeChallenge;
                throw new AuthException(Error.UNKNOWN_USER);
            }

            if (authenticator.requiresUser() && authUser != null && !authenticator.configuredFor(authUser)) {
                if (model.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    if (model.isUserSetupAllowed()) {
                        clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SETUP_REQUIRED);
                        authUser.addRequiredAction(authenticator.getRequiredAction());

                    } else {
                        throw new AuthException(Error.CREDENTIAL_SETUP_REQUIRED);
                    }
                }
                continue;
            }
            context = new Result(authenticatorModel, authenticator);
            authenticator.authenticate(context);
            Status result = context.getStatus();
            if (result == Status.SUCCESS){
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SUCCESS);
                if (model.isAlternative()) alternativeSuccessful = true;
                continue;
            } else if (result == Status.FAILED) {
                logUserFailure();
                if (context.challenge != null) return context.challenge;
                throw new AuthException(context.error);
            } else if (result == Status.CHALLENGE) {
                if (model.isRequired()) return context.challenge;
                else if (model.isAlternative()) alternativeChallenge = context.challenge;
                else clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.SKIPPED);
                continue;
            } else if (result == Status.FAILURE_CHALLENGE) {
                logUserFailure();
                return context.challenge;
            } else if (result == Status.ATTEMPTED) {
                if (model.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) throw new AuthException(Error.INVALID_CREDENTIALS);
                clientSession.setAuthenticatorStatus(model.getId(), UserSessionModel.AuthenticatorStatus.ATTEMPTED);
                continue;
            } else {
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
        if (userSession == null) { // if no authenticator attached a usersession
            userSession = session.sessions().createUserSession(realm, clientSession.getAuthenticatedUser(), clientSession.getAuthenticatedUser().getUsername(), connection.getRemoteAddr(), "form", false, null, null);
            userSession.setState(UserSessionModel.State.LOGGING_IN);
        }
        TokenManager.attachClientSession(userSession, clientSession);
        return processRequiredActions();

    }

    public Response processRequiredActions() {
        return AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, connection, request, uriInfo, eventBuilder);

    }


}
