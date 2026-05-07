package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

public class AuthenticationFlowBuilder extends Builder<AuthenticationFlowRepresentation> {

    private AuthenticationFlowBuilder(AuthenticationFlowRepresentation rep) {
        super(rep);
    }

    public static AuthenticationFlowBuilder create() {
        return new AuthenticationFlowBuilder(new AuthenticationFlowRepresentation());
    }

    public static AuthenticationFlowBuilder create(String alias, String description, String providerId, boolean topLevel, boolean builtIn) {
        return create().alias(alias).description(description).providerId(providerId).topLevel(topLevel).builtIn(builtIn);
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

    public AuthenticationFlowBuilder authenticationExecutions(AuthenticationExecutionExportBuilder... authenticationExecutions) {
        rep.setAuthenticationExecutions(combine(rep.getAuthenticationExecutions(), authenticationExecutions));
        return this;
    }

}
