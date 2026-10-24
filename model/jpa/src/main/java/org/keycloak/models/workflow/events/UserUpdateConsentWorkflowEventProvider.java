package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class UserUpdateConsentWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserUpdateConsentWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        // admin events are not supported: the only consent operation in the admin API revokes the consent, which is the
        // equivalent of a REVOKE_GRANT user event rather than a consent update
        return EventType.UPDATE_CONSENT.equals(event.getType());
    }
}
