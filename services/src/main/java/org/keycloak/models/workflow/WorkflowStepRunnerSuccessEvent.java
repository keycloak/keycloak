package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

public record WorkflowStepRunnerSuccessEvent(KeycloakSession session) implements ProviderEvent {
}
