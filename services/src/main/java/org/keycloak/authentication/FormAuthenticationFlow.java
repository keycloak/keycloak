package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class FormAuthenticationFlow implements AuthenticationFlow {
    AuthenticationProcessor processor;
    AuthenticationExecutionModel execution;


    public FormAuthenticationFlow(AuthenticationProcessor processor, AuthenticationExecutionModel execution) {
        this.processor = processor;
        this.execution = execution;
    }

    private static class FormActionResult implements FormContext {
        AuthenticatorContext delegate;
        FormAuthenticator authenticator;

        FormActionResult(AuthenticatorContext delegate, FormAuthenticator authenticator) {
            this.delegate = delegate;
            this.authenticator = authenticator;
        }

        @Override
        public FormAuthenticator getFormAuthenticator() {
            return authenticator;
        }

        @Override
        public EventBuilder getEvent() {
            return delegate.getEvent();
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return delegate.getExecution();
        }

        @Override
        public void setExecution(AuthenticationExecutionModel execution) {
            delegate.setExecution(execution);
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfig() {
            return delegate.getAuthenticatorConfig();
        }

        @Override
        public String getAction() {
            return delegate.getAction();
        }

        @Override
        public Authenticator getAuthenticator() {
            return delegate.getAuthenticator();
        }

        @Override
        public void setAuthenticator(Authenticator authenticator) {
            delegate.setAuthenticator(authenticator);
        }

        @Override
        public AuthenticationProcessor.Status getStatus() {
            return delegate.getStatus();
        }

        @Override
        public UserModel getUser() {
            return delegate.getUser();
        }

        @Override
        public void setUser(UserModel user) {
            delegate.setUser(user);
        }

        @Override
        public RealmModel getRealm() {
            return delegate.getRealm();
        }

        @Override
        public ClientSessionModel getClientSession() {
            return delegate.getClientSession();
        }

        @Override
        public void attachUserSession(UserSessionModel userSession) {
            delegate.attachUserSession(userSession);
        }

        @Override
        public ClientConnection getConnection() {
            return delegate.getConnection();
        }

        @Override
        public UriInfo getUriInfo() {
            return delegate.getUriInfo();
        }

        @Override
        public KeycloakSession getSession() {
            return delegate.getSession();
        }

        @Override
        public HttpRequest getHttpRequest() {
            return delegate.getHttpRequest();
        }

        @Override
        public BruteForceProtector getProtector() {
            return delegate.getProtector();
        }

        @Override
        public void success() {
            delegate.success();
        }

        @Override
        public void failure(AuthenticationProcessor.Error error) {
            delegate.failure(error);
        }

        @Override
        public void failure(AuthenticationProcessor.Error error, Response response) {
            delegate.failure(error, response);
        }

        @Override
        public void challenge(Response challenge) {
            delegate.challenge(challenge);
        }

        @Override
        public void forceChallenge(Response challenge) {
            delegate.forceChallenge(challenge);
        }

        @Override
        public void failureChallenge(AuthenticationProcessor.Error error, Response challenge) {
            delegate.failureChallenge(error, challenge);
        }

        @Override
        public void attempted() {
            delegate.attempted();
        }

        @Override
        public String getForwardedErrorMessage() {
            return delegate.getForwardedErrorMessage();
        }

        @Override
        public String generateAccessCode() {
            return delegate.generateAccessCode();
        }

        @Override
        public Response getChallenge() {
            return delegate.getChallenge();
        }

        @Override
        public AuthenticationProcessor.Error getError() {
            return delegate.getError();
        }
    }


    @Override
    public Response processAction(String actionExecution) {
        if (!actionExecution.equals(execution.getId())) {
            throw new AuthenticationProcessor.AuthException("action is not current execution", AuthenticationProcessor.Error.INTERNAL_ERROR);
        }
        FormAuthenticator authenticator = processor.getSession().getProvider(FormAuthenticator.class, execution.getAuthenticator());
        for (AuthenticationExecutionModel formActionExecution : processor.getRealm().getAuthenticationExecutions(execution.getFlowId())) {
            FormAction action = processor.getSession().getProvider(FormAction.class, execution.getAuthenticator());

            UserModel authUser = processor.getClientSession().getAuthenticatedUser();
            if (action.requiresUser() && authUser == null) {
                throw new AuthenticationProcessor.AuthException("form action: " + execution.getAuthenticator() + " requires user", AuthenticationProcessor.Error.UNKNOWN_USER);
            }
            boolean configuredFor = false;
            if (action.requiresUser() && authUser != null) {
                configuredFor = action.configuredFor(processor.getSession(), processor.getRealm(), authUser);
                if (!configuredFor) {
                    if (formActionExecution.isRequired()) {
                        if (formActionExecution.isUserSetupAllowed()) {
                            AuthenticationProcessor.logger.debugv("authenticator SETUP_REQUIRED: {0}",  execution.getAuthenticator());
                            processor.getClientSession().setExecutionStatus(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SETUP_REQUIRED);
                            action.setRequiredActions(processor.getSession(), processor.getRealm(), authUser);
                            continue;
                        } else {
                            throw new AuthenticationProcessor.AuthException(AuthenticationProcessor.Error.CREDENTIAL_SETUP_REQUIRED);
                        }
                    } else if (formActionExecution.isOptional()) {
                        processor.getClientSession().setExecutionStatus(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SKIPPED);
                        continue;
                    }
                }
            }

            FormActionResult result = new FormActionResult(processor.createAuthenticatorContext(formActionExecution, null), authenticator);
            action.authenticate(result);
            return processResult(result, formActionExecution);

        }
        return null;

    }

    @Override
    public Response processFlow() {
        FormAuthenticator authenticator = processor.getSession().getProvider(FormAuthenticator.class, execution.getAuthenticator());
        AuthenticatorContext context = processor.createAuthenticatorContext(execution, null);
        authenticator.authenticate(context);
        return processResult(context, execution);
    }


    public Response processResult(AuthenticatorContext result, AuthenticationExecutionModel execution) {
        AuthenticationProcessor.Status status = result.getStatus();
        if (status == AuthenticationProcessor.Status.SUCCESS) {
            return null;
        } else if (status == AuthenticationProcessor.Status.FAILED) {
            AuthenticationProcessor.logger.debugv("authenticator FAILED: {0}", execution.getAuthenticator());
            processor.logFailure();
            processor.getClientSession().setExecutionStatus(execution.getId(), ClientSessionModel.ExecutionStatus.FAILED);
            if (result.getChallenge() != null) {
                return sendChallenge(result, execution);
            }
            throw new AuthenticationProcessor.AuthException(result.getError());
        } else if (status == AuthenticationProcessor.Status.FORCE_CHALLENGE) {
            processor.getClientSession().setExecutionStatus(execution.getId(), ClientSessionModel.ExecutionStatus.CHALLENGED);
            return sendChallenge(result, execution);
        } else if (status == AuthenticationProcessor.Status.CHALLENGE) {
            processor.getClientSession().setExecutionStatus(execution.getId(), ClientSessionModel.ExecutionStatus.CHALLENGED);
            return sendChallenge(result, execution);
        } else if (status == AuthenticationProcessor.Status.FAILURE_CHALLENGE) {
            AuthenticationProcessor.logger.debugv("authenticator FAILURE_CHALLENGE: {0}", execution.getAuthenticator());
            processor.logFailure();
            processor.getClientSession().setExecutionStatus(execution.getId(), ClientSessionModel.ExecutionStatus.CHALLENGED);
            return sendChallenge(result, execution);
        } else if (status == AuthenticationProcessor.Status.ATTEMPTED) {
            AuthenticationProcessor.logger.debugv("authenticator ATTEMPTED: {0}", execution.getAuthenticator());
            if (execution.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                throw new AuthenticationProcessor.AuthException(AuthenticationProcessor.Error.INVALID_CREDENTIALS);
            }
            processor.getClientSession().setExecutionStatus(execution.getId(), ClientSessionModel.ExecutionStatus.ATTEMPTED);
            return null;
        } else {
            AuthenticationProcessor.logger.debugv("authenticator INTERNAL_ERROR: {0}", execution.getAuthenticator());
            AuthenticationProcessor.logger.error("Unknown result status");
            throw new AuthenticationProcessor.AuthException(AuthenticationProcessor.Error.INTERNAL_ERROR);
        }

    }

    public Response sendChallenge(AuthenticatorContext result, AuthenticationExecutionModel execution) {
        processor.getClientSession().setNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
        return result.getChallenge();
    }


}
