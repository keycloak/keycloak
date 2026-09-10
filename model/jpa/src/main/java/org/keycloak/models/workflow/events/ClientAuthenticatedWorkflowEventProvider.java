package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class ClientAuthenticatedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public ClientAuthenticatedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.CLIENT_LOGIN.equals(event.getType());
    }
}
