package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
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

//
// setup
// cookie: master, alternative
// CERT_AUTH: alternative
// UserPassword: alternative
//    OTP: optional
//    CAPTHA: required
//
// scenario: username password
// * cookie, attempted
// * cert, attempated
// * usernamepassord, doesn't see form, sets challenge to form
//
//
//
//
//
//
//



public class AuthenticationProcessor {
    protected RealmModel realm;
    protected UserSessionModel userSession;
    protected ClientSessionModel clientSession;
    protected ClientConnection connection;
    protected UriInfo uriInfo;
    protected KeycloakSession session;
    protected List<AuthenticatorModel> authenticators;
    protected BruteForceProtector protector;
    protected EventBuilder eventBuilder;
    protected HttpRequest request;


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
    }

    public void logUserFailure() {

    }

    protected boolean isProcessed(UserSessionModel.AuthenticatorStatus status) {
        return status == UserSessionModel.AuthenticatorStatus.SUCCESS || status == UserSessionModel.AuthenticatorStatus.SKIPPED
                || status == UserSessionModel.AuthenticatorStatus.ATTEMPTED
                || status == UserSessionModel.AuthenticatorStatus.SETUP_REQUIRED;
    }

    public Response authenticate() {
        UserModel authUser = clientSession.getAuthenticatedUser();
        validateUser(authUser);
        Response challenge = null;
        Map<String, UserSessionModel.AuthenticatorStatus> previousAttempts = clientSession.getAuthenticators();
        for (AuthenticatorModel model : authenticators) {
            UserSessionModel.AuthenticatorStatus oldStatus = previousAttempts.get(model.getAlias());
            if (isProcessed(oldStatus)) continue;

            AuthenticatorFactory factory = (AuthenticatorFactory)session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getProviderId());
            Authenticator authenticator = factory.create(model);
            if (authenticator.requiresUser() && authUser == null){
                if ( authenticator.requiresUser()) {
                    if (challenge != null) return challenge;
                    throw new AuthException(Error.UNKNOWN_USER);
                }
            }
            if (authUser != null && model.getRequirement() == AuthenticatorModel.Requirement.ALTERNATIVE) {
                clientSession.setAuthenticatorStatus(model.getAlias(), UserSessionModel.AuthenticatorStatus.SKIPPED);
                continue;
            }
            authUser = clientSession.getAuthenticatedUser();

            if (authenticator.requiresUser() && authUser != null && !authenticator.configuredFor(authUser)) {
                if (model.getRequirement() == AuthenticatorModel.Requirement.REQUIRED) {
                    if (model.isUserSetupAllowed()) {
                        clientSession.setAuthenticatorStatus(model.getAlias(), UserSessionModel.AuthenticatorStatus.SETUP_REQUIRED);
                        authUser.addRequiredAction(authenticator.getRequiredAction());

                    } else {
                        throw new AuthException(Error.CREDENTIAL_SETUP_REQUIRED);
                    }
                }
                continue;
            }
            Result context = new Result(model, authenticator);
            authenticator.authenticate(context);
            Status result = context.getStatus();
            if (result == Status.SUCCESS){
                clientSession.setAuthenticatorStatus(model.getAlias(), UserSessionModel.AuthenticatorStatus.SUCCESS);
                if (model.isMasterAuthenticator()) return authenticationComplete();
                continue;
            } else if (result == Status.FAILED) {
                throw new AuthException(context.error);
            } else if (result == Status.CHALLENGE) {
                if (model.getRequirement() == AuthenticatorModel.Requirement.REQUIRED) return context.challenge;
                if (challenge != null) challenge = context.challenge;
                continue;
            } else if (result == Status.FAILURE_CHALLENGE) {
                logUserFailure();
                return context.challenge;
            } else if (result == Status.ATTEMPTED) {
                if (model.getRequirement() == AuthenticatorModel.Requirement.REQUIRED) throw new AuthException(Error.INVALID_CREDENTIALS);
                clientSession.setAuthenticatorStatus(model.getAlias(), UserSessionModel.AuthenticatorStatus.ATTEMPTED);
                continue;
            }
        }

        if (authUser == null) {
            if (challenge != null) return challenge;
            throw new AuthException(Error.UNKNOWN_USER);
        }


        return authenticationComplete();
    }



    public void validateUser(UserModel authenticatedUser) {
        if (authenticatedUser != null) {
            if (!clientSession.getAuthenticatedUser().isEnabled()) throw new AuthException(Error.USER_DISABLED);
        }
        if (realm.isBruteForceProtected()) {
            if (protector.isTemporarilyDisabled(session, realm, authenticatedUser.getUsername())) {
                throw new AuthException(Error.USER_TEMPORARILY_DISABLED);
            }
        }
    }

    protected  Response authenticationComplete() {
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
