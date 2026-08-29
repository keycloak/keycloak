package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class ClientCreatedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public ClientCreatedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.CLIENT_REGISTER.equals(event.getType());
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        return org.keycloak.events.admin.ResourceType.CLIENT.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType());
    }
}
