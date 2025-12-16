package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;

public class AuthenticationExecutionExportConfigBuilder {

    private final AuthenticationExecutionExportRepresentation rep;

    private AuthenticationExecutionExportConfigBuilder(AuthenticationExecutionExportRepresentation rep) {
        this.rep = rep;
    }

    public static AuthenticationExecutionExportConfigBuilder create() {
        AuthenticationExecutionExportRepresentation rep = new AuthenticationExecutionExportRepresentation();
        return new AuthenticationExecutionExportConfigBuilder(rep);
    }

    public static AuthenticationExecutionExportConfigBuilder update(AuthenticationExecutionExportRepresentation rep) {
        return new AuthenticationExecutionExportConfigBuilder(rep);
    }

    public AuthenticationExecutionExportConfigBuilder authenticator(String authenticator) {
        rep.setAuthenticator(authenticator);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder flowAlias(String flowAlias) {
        rep.setFlowAlias(flowAlias);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder requirement(String requirement) {
        rep.setRequirement(requirement);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder priority(Integer priority) {
        rep.setPriority(priority);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder userSetupAllowed(boolean allowed) {
        rep.setUserSetupAllowed(allowed);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder authenticatorFlow(boolean enabled) {
        rep.setAuthenticatorFlow(enabled);
        return this;
    }

    public AuthenticationExecutionExportRepresentation build() {
        return rep;
    }
}
