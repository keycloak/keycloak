package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

public final class WorkflowActionRunnerSuccessEvent implements ProviderEvent {

    private final KeycloakSession session;

    public WorkflowActionRunnerSuccessEvent(KeycloakSession session) {
        this.session = session;
    }

    public KeycloakSession getSession() {
        return session;
    }
}
