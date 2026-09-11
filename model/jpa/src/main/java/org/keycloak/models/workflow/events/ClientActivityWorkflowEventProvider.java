package org.keycloak.models.workflow.events;

import java.util.EnumSet;
import java.util.Set;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class ClientActivityWorkflowEventProvider extends AbstractWorkflowEventProvider {

    // User events that indicate a client is actively being used on behalf of a user. Unlike CLIENT_LOGIN,
    // these events are also sent for public clients, which cannot authenticate on their own.
    private static final Set<EventType> ACTIVITY_EVENT_TYPES = EnumSet.of(EventType.LOGIN, EventType.CODE_TO_TOKEN, EventType.REFRESH_TOKEN);

    public ClientActivityWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean supports(Event event) {
        return ACTIVITY_EVENT_TYPES.contains(event.getType());
    }
}
