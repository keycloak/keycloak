package org.keycloak.models.workflow;

import java.util.Arrays;
import java.util.Optional;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;

import static org.keycloak.models.workflow.ResourceOperationType.toOperationType;

public final class WorkflowEventConverter {

    static Optional<WorkflowEvent> fromEvent(KeycloakSession session, Event event) {
        return resourceOfEvent(event)
            .map(r -> {
                ResourceOperationType resourceOperationType = toOperationType(event.getType());
                String resourceId = switch (r) {
                    case USERS -> event.getUserId();
                    case CLIENTS -> findClientResourceId(session, event.getClientId());
                };
                if (resourceOperationType != null && resourceId != null) {
                    return new WorkflowEvent(r, resourceOperationType, resourceId, event);
                }
                return null;
            });
    }

    static Optional<WorkflowEvent> fromEvent(AdminEvent event) {
        return resourceOfEvent(event)
            .map(r -> {
                ResourceOperationType resourceOperationType = toOperationType(r, event.getOperationType());
                if (resourceOperationType != null) {
                    return new WorkflowEvent(r, resourceOperationType, event.getResourceId(), event);
                }
                return null;
            });
    }

    static Optional<WorkflowEvent> fromEvent(ProviderEvent event) {
        ResourceOperationType resourceOperationType = toOperationType(event.getClass());
        if (resourceOperationType == null) {
            return Optional.empty();
        }

        String resourceId = resourceOperationType.getResourceId(event);
        if (resourceId == null) {
            return Optional.empty();
        }

        WorkflowEvent workflowEvent = new WorkflowEvent(resourceOperationType.getResourceType(), resourceOperationType, resourceId, event);
        return Optional.of(workflowEvent);
    }


    private static Optional<ResourceType> resourceOfEvent(Event event) {
        // Is it possible for an event to have multiple resources, thus triggering multiple workflows?
        return Arrays.stream(ResourceType.values())
            .filter(r -> r.supportsEvent(event.getType()))
            .findFirst();
    }

    private static Optional<ResourceType> resourceOfEvent(AdminEvent event) {
        return Arrays.stream(ResourceType.values())
                     .filter(r -> r.supportsAdminEvent(event.getResourceType(), event.getOperationType()))
                     .findFirst();
    }

    private static String findClientResourceId(KeycloakSession session, String clientClientId) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }

        ClientModel client = realm.getClientByClientId(clientClientId);
        if (client == null) {
            return null;
        }

        return client.getId();
    }
}
