package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testframework.util.Collections;

public class AuthenticationFlowBuilder {

    private final AuthenticationFlowRepresentation rep;

    private AuthenticationFlowBuilder(AuthenticationFlowRepresentation rep) {
        this.rep = rep;
    }

    public static AuthenticationFlowBuilder create() {
        AuthenticationFlowRepresentation rep = new AuthenticationFlowRepresentation();
        return new AuthenticationFlowBuilder(rep);
    }

    public static AuthenticationFlowBuilder update(AuthenticationFlowRepresentation rep) {
        return new AuthenticationFlowBuilder(rep);
    }

    public AuthenticationFlowBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public AuthenticationFlowBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public AuthenticationFlowBuilder providerId(String providerId) {
        rep.setProviderId(providerId);
        return this;
    }

    public AuthenticationFlowBuilder topLevel(boolean enabled) {
        rep.setTopLevel(enabled);
        return this;
    }

    public AuthenticationFlowBuilder builtIn(boolean enabled) {
        rep.setBuiltIn(enabled);
        return this;
    }

    public AuthenticationExecutionExportBuilder addAuthenticationExecutionWithAuthenticator(String authenticator, String requirement, Integer priority, boolean  userSetupAllowed) {
        AuthenticationExecutionExportRepresentation exec = new AuthenticationExecutionExportRepresentation();
        rep.setAuthenticationExecutions(Collections.combine(rep.getAuthenticationExecutions(), exec));

        return AuthenticationExecutionExportBuilder.update(exec).authenticator(authenticator).requirement(requirement)
                .priority(priority).userSetupAllowed(userSetupAllowed).authenticatorFlow(false);
    }

    public AuthenticationExecutionExportBuilder addAuthenticationExecutionWithAliasFlow(String flowAlias, String requirement, Integer priority, boolean  userSetupAllowed) {
        AuthenticationExecutionExportRepresentation exec = new AuthenticationExecutionExportRepresentation();
        rep.setAuthenticationExecutions(Collections.combine(rep.getAuthenticationExecutions(), exec));

        return AuthenticationExecutionExportBuilder.update(exec).flowAlias(flowAlias).requirement(requirement)
                .priority(priority).userSetupAllowed(userSetupAllowed).authenticatorFlow(true);
    }

    public AuthenticationFlowRepresentation build() {
        return rep;
    }
}
