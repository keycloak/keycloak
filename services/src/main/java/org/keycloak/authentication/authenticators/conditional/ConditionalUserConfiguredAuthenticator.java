package org.keycloak.authentication.authenticators.conditional;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class ConditionalUserConfiguredAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalUserConfiguredAuthenticator SINGLETON = new ConditionalUserConfiguredAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        return matchConditionInFlow(context, context.getExecution().getParentFlow());
    }

    private boolean matchConditionInFlow(AuthenticationFlowContext context, String flowId) {
        List<AuthenticationExecutionModel> executions = context.getRealm().getAuthenticationExecutions(flowId);
        if (executions==null) {
            return true;
        }
        List<AuthenticationExecutionModel> requiredExecutions = new ArrayList<>();
        List<AuthenticationExecutionModel> alternativeExecutions = new ArrayList<>();
        executions.forEach(e -> {
            //Check if the execution's authenticator is a conditional authenticator, as they must not be evaluated here.
            boolean isConditionalAuthenticator = false;
            try {
                AuthenticatorFactory factory = (AuthenticatorFactory) context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, e.getAuthenticator());
                if (factory != null) {
                    Authenticator auth = factory.create(context.getSession());
                    if (auth instanceof ConditionalAuthenticator) {
                        isConditionalAuthenticator = true;
                    }
                }
            } catch (Exception exception) {
                //don't need to catch this
            }
            if (!context.getExecution().getId().equals(e.getId()) && !e.isAuthenticatorFlow() && !isConditionalAuthenticator) {
                if (e.isRequired()) {
                    requiredExecutions.add(e);
                } else if (e.isAlternative()) {
                    alternativeExecutions.add(e);
                }
            }
        });
        if (!requiredExecutions.isEmpty()) {
            return requiredExecutions.stream().allMatch(e -> isConfiguredFor(e, context));
        } else  if (!alternativeExecutions.isEmpty()) {
            return alternativeExecutions.stream().anyMatch(e -> isConfiguredFor(e, context));
        }
        return true;
    }

    private boolean isConfiguredFor(AuthenticationExecutionModel model, AuthenticationFlowContext context) {
        if (model.isAuthenticatorFlow()) {
            return matchConditionInFlow(context, model.getId());
        }
        AuthenticatorFactory factory = (AuthenticatorFactory) context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
        Authenticator authenticator = factory.create(context.getSession());
        return authenticator.configuredFor(context.getSession(), context.getRealm(), context.getUser());
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used
    }

    @Override
    public void close() {
        // Does nothing
    }
}
