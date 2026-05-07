package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;

public class AuthenticationExecutionExportBuilder extends Builder<AuthenticationExecutionExportRepresentation> {

    private AuthenticationExecutionExportBuilder(AuthenticationExecutionExportRepresentation rep) {
        super(rep);
    }

    public static AuthenticationExecutionExportBuilder create() {
        return new AuthenticationExecutionExportBuilder(new AuthenticationExecutionExportRepresentation());
    }

    public static AuthenticationExecutionExportBuilder authenticator(String authenticator, String requirement, Integer priority, boolean  userSetupAllowed) {
        return create().authenticatorFlow(false).authenticator(authenticator).requirement(requirement).priority(priority);
    }

    public static AuthenticationExecutionExportBuilder alias(String flowAlias, String requirement, Integer priority, boolean  userSetupAllowed) {
        return create().authenticatorFlow(true).flowAlias(flowAlias).requirement(requirement).priority(priority);
    }

    public static AuthenticationExecutionExportBuilder update(AuthenticationExecutionExportRepresentation rep) {
        return new AuthenticationExecutionExportBuilder(rep);
    }

    public AuthenticationExecutionExportBuilder authenticator(String authenticator) {
        rep.setAuthenticator(authenticator);
        return this;
    }

    public AuthenticationExecutionExportBuilder flowAlias(String flowAlias) {
        rep.setFlowAlias(flowAlias);
        return this;
    }

    public AuthenticationExecutionExportBuilder requirement(String requirement) {
        rep.setRequirement(requirement);
        return this;
    }

    public AuthenticationExecutionExportBuilder priority(Integer priority) {
        rep.setPriority(priority);
        return this;
    }

    public AuthenticationExecutionExportBuilder authenticatorFlow(boolean authenticatorFlow) {
        rep.setAuthenticatorFlow(authenticatorFlow);
        return this;
    }

    public AuthenticationExecutionExportBuilder userSetupAllowed(boolean userSetupAllowed) {
        rep.setUserSetupAllowed(userSetupAllowed);
        return this;
    }

}
