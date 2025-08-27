package org.keycloak.models.policy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

public final class ResourcePolicyActionRunnerSuccessEvent implements ProviderEvent {

    private final KeycloakSession session;

    public ResourcePolicyActionRunnerSuccessEvent(KeycloakSession session) {
        this.session = session;
    }

    public KeycloakSession getSession() {
        return session;
    }
}
