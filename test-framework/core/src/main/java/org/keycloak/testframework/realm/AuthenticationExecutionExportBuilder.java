package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;

public class AuthenticationExecutionExportBuilder {

    private final AuthenticationExecutionExportRepresentation rep;

    private AuthenticationExecutionExportBuilder(AuthenticationExecutionExportRepresentation rep) {
        this.rep = rep;
    }

    public static AuthenticationExecutionExportBuilder create() {
        AuthenticationExecutionExportRepresentation rep = new AuthenticationExecutionExportRepresentation();
        return new AuthenticationExecutionExportBuilder(rep);
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

    public AuthenticationExecutionExportBuilder userSetupAllowed(boolean allowed) {
        rep.setUserSetupAllowed(allowed);
        return this;
    }

    public AuthenticationExecutionExportBuilder authenticatorFlow(boolean enabled) {
        rep.setAuthenticatorFlow(enabled);
        return this;
    }

    public AuthenticationExecutionExportRepresentation build() {
        return rep;
    }
}
