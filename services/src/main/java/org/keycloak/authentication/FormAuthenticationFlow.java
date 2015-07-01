package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class FormAuthenticationFlow implements AuthenticationFlow {
    AuthenticationProcessor processor;
    AuthenticationExecutionModel formExecution;
    private final List<AuthenticationExecutionModel> formActionExecutions;
    private final FormAuthenticator formAuthenticator;


    public FormAuthenticationFlow(AuthenticationProcessor processor, AuthenticationExecutionModel execution) {
        this.processor = processor;
        this.formExecution = execution;
        formActionExecutions = processor.getRealm().getAuthenticationExecutions(execution.getFlowId());
        formAuthenticator = processor.getSession().getProvider(FormAuthenticator.class, execution.getAuthenticator());
    }

    private class FormContextImpl implements FormContext {
        AuthenticationExecutionModel executionModel;
        AuthenticatorConfigModel authenticatorConfig;

        private FormContextImpl(AuthenticationExecutionModel executionModel) {
            this.executionModel = executionModel;
        }

        @Override
        public EventBuilder newEvent() {
            return processor.newEvent();
        }

       @Override
        public EventBuilder getEvent() {
            return processor.getEvent();
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return executionModel;
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfig() {
            if (executionModel.getAuthenticatorConfig() == null) return null;
            if (authenticatorConfig != null) return authenticatorConfig;
            authenticatorConfig = getRealm().getAuthenticatorConfigById(executionModel.getAuthenticatorConfig());
            return authenticatorConfig;
        }

        @Override
        public UserModel getUser() {
            return getClientSession().getAuthenticatedUser();
        }

        @Override
        public void setUser(UserModel user) {
            processor.setAutheticatedUser(user);
        }

        @Override
        public RealmModel getRealm() {
            return processor.getRealm();
        }

        @Override
        public ClientSessionModel getClientSession() {
            return processor.getClientSession();
        }

        @Override
        public ClientConnection getConnection() {
            return processor.getConnection();
        }

        @Override
        public UriInfo getUriInfo() {
            return processor.getUriInfo();
        }

        @Override
        public KeycloakSession getSession() {
            return processor.getSession();
        }

        @Override
        public HttpRequest getHttpRequest() {
            return processor.getRequest();
        }

    }

    private class ValidationContextImpl extends FormContextImpl implements ValidationContext {
        FormAction action;

        private ValidationContextImpl(AuthenticationExecutionModel executionModel, FormAction action) {
            super(executionModel);
            this.action = action;
        }

        boolean success;
        List<FormMessage> errors = null;
        MultivaluedMap<String, String> formData = null;
        @Override
        public void validationError(MultivaluedMap<String, String> formData, List<FormMessage> errors) {
            this.errors = errors;
            this.formData = formData;
        }

        @Override
        public void success() {
           success = true;
        }
    }

    @Override
    public Response processAction(String actionExecution) {
        if (!actionExecution.equals(formExecution.getId())) {
            throw new AuthenticationProcessor.AuthException("action is not current execution", AuthenticationProcessor.Error.INTERNAL_ERROR);
        }
        Map<String, ClientSessionModel.ExecutionStatus> executionStatus = new HashMap<>();
        List<FormAction> requiredActions = new LinkedList<>();
        List<ValidationContextImpl> successes = new LinkedList<>();
        for (AuthenticationExecutionModel formActionExecution : formActionExecutions) {
            if (!formActionExecution.isEnabled()) {
                executionStatus.put(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SKIPPED);
                continue;
            }
            FormAction action = processor.getSession().getProvider(FormAction.class, formActionExecution.getAuthenticator());

            UserModel authUser = processor.getClientSession().getAuthenticatedUser();
            if (action.requiresUser() && authUser == null) {
                throw new AuthenticationProcessor.AuthException("form action: " + formExecution.getAuthenticator() + " requires user", AuthenticationProcessor.Error.UNKNOWN_USER);
            }
            boolean configuredFor = false;
            if (action.requiresUser() && authUser != null) {
                configuredFor = action.configuredFor(processor.getSession(), processor.getRealm(), authUser);
                if (!configuredFor) {
                    if (formActionExecution.isRequired()) {
                        if (formActionExecution.isUserSetupAllowed()) {
                            AuthenticationProcessor.logger.debugv("authenticator SETUP_REQUIRED: {0}", formExecution.getAuthenticator());
                            executionStatus.put(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SETUP_REQUIRED);
                            requiredActions.add(action);
                            continue;
                        } else {
                            throw new AuthenticationProcessor.AuthException(AuthenticationProcessor.Error.CREDENTIAL_SETUP_REQUIRED);
                        }
                    } else if (formActionExecution.isOptional()) {
                        executionStatus.put(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SKIPPED);
                        continue;
                    }
                }
            }

            ValidationContextImpl result = new ValidationContextImpl(formActionExecution, action);
            action.validate(result);
            if (result.success) {
                executionStatus.put(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.SUCCESS);
                successes.add(result);
            } else {
                processor.logFailure();
                executionStatus.put(formActionExecution.getId(), ClientSessionModel.ExecutionStatus.CHALLENGED);
                return renderForm(result.formData, result.errors);
            }
        }

        for (ValidationContextImpl context : successes) {
            context.action.success(context);
        }
        // set status and required actions only if form is fully successful
        for (Map.Entry<String, ClientSessionModel.ExecutionStatus> entry : executionStatus.entrySet()) {
            processor.getClientSession().setExecutionStatus(entry.getKey(), entry.getValue());
        }
        for (FormAction action : requiredActions) {
            action.setRequiredActions(processor.getSession(), processor.getRealm(), processor.getClientSession().getAuthenticatedUser());

        }
        return null;

    }

    public URI getActionUrl(String executionId, String code) {
        return LoginActionsService.registrationFormProcessor(processor.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam("execution", executionId)
                .build(processor.getRealm().getName());
    }


    @Override
    public Response processFlow() {
        return renderForm(null, null);
    }

    public Response renderForm(MultivaluedMap<String, String> formData, List<FormMessage> errors) {
        String executionId = formExecution.getId();
        processor.getClientSession().setNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, executionId);
        String code = processor.generateCode();
        URI actionUrl = getActionUrl(executionId, code);
        LoginFormsProvider form = processor.getSession().getProvider(LoginFormsProvider.class)
                .setActionUri(actionUrl)
                .setClientSessionCode(code)
                .setFormData(formData)
                .setErrors(errors);
        for (AuthenticationExecutionModel formActionExecution : formActionExecutions) {
            if (!formActionExecution.isEnabled()) continue;
            FormAction action = processor.getSession().getProvider(FormAction.class, formActionExecution.getAuthenticator());
            FormContext result = new FormContextImpl(formActionExecution);
            action.buildPage(result, form);
        }
        FormContext context = new FormContextImpl(formExecution);
        return formAuthenticator.render(context, form);
    }
}
