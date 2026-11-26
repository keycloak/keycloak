package org.keycloak.services.resources.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorFactory;

public class AdminRoleTokenPostProcessorFactory implements TokenPostProcessorFactory {

    @Override
    public TokenPostProcessor create(KeycloakSession session) {
        return new AdminRoleTokenPostProcessor(session);
    }

    @Override
    public String getId() {
        return "admin-role-token-post-processor";
    }
}
