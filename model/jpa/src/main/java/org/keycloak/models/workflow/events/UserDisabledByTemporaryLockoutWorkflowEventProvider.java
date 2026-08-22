package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class UserDisabledByTemporaryLockoutWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserDisabledByTemporaryLockoutWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT.equals(event.getType());
    }

}
