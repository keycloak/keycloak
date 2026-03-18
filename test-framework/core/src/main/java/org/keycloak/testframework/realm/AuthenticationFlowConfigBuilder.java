package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testframework.util.Collections;

public class AuthenticationFlowConfigBuilder {

    private final AuthenticationFlowRepresentation rep;

    private AuthenticationFlowConfigBuilder(AuthenticationFlowRepresentation rep) {
        this.rep = rep;
    }

    public static AuthenticationFlowConfigBuilder create() {
        AuthenticationFlowRepresentation rep = new AuthenticationFlowRepresentation();
        return new AuthenticationFlowConfigBuilder(rep);
    }

    public static AuthenticationFlowConfigBuilder update(AuthenticationFlowRepresentation rep) {
        return new AuthenticationFlowConfigBuilder(rep);
    }

    public AuthenticationFlowConfigBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public AuthenticationFlowConfigBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public AuthenticationFlowConfigBuilder providerId(String providerId) {
        rep.setProviderId(providerId);
        return this;
    }

    public AuthenticationFlowConfigBuilder topLevel(boolean enabled) {
        rep.setTopLevel(enabled);
        return this;
    }

    public AuthenticationFlowConfigBuilder builtIn(boolean enabled) {
        rep.setBuiltIn(enabled);
        return this;
    }

    public AuthenticationExecutionExportConfigBuilder addAuthenticationExecutionWithAuthenticator(String authenticator, String requirement, Integer priority, boolean  userSetupAllowed) {
        AuthenticationExecutionExportRepresentation exec = new AuthenticationExecutionExportRepresentation();
        rep.setAuthenticationExecutions(Collections.combine(rep.getAuthenticationExecutions(), exec));

        return AuthenticationExecutionExportConfigBuilder.update(exec).authenticator(authenticator).requirement(requirement)
                .priority(priority).userSetupAllowed(userSetupAllowed).authenticatorFlow(false);
    }

    public AuthenticationExecutionExportConfigBuilder addAuthenticationExecutionWithAliasFlow(String flowAlias, String requirement, Integer priority, boolean  userSetupAllowed) {
        AuthenticationExecutionExportRepresentation exec = new AuthenticationExecutionExportRepresentation();
        rep.setAuthenticationExecutions(Collections.combine(rep.getAuthenticationExecutions(), exec));

        return AuthenticationExecutionExportConfigBuilder.update(exec).flowAlias(flowAlias).requirement(requirement)
                .priority(priority).userSetupAllowed(userSetupAllowed).authenticatorFlow(true);
    }

    public AuthenticationFlowRepresentation build() {
        return rep;
    }
}
