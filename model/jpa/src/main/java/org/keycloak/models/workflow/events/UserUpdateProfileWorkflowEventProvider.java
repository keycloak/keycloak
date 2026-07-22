package org.keycloak.models.workflow.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

public class UserUpdateProfileWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern USER_PATH = Pattern.compile("^users/([^/]+)$");

    public UserUpdateProfileWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.UPDATE_PROFILE.equals(event.getType());
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        if (!org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())) {
            return false;
        }
        if (!OperationType.UPDATE.equals(adminEvent.getOperationType())) {
            return false;
        }
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return false;
        }
        return USER_PATH.matcher(path).matches();
    }

    @Override
    public WorkflowEvent create(AdminEvent adminEvent) {
        if (supports(adminEvent)) {
            String userId = getUserIdFromPath(adminEvent);
            return userId != null ? new WorkflowEvent(getSupportedResourceType(), userId, adminEvent, providerId) : null;
        }
        return null;
    }

    private String getUserIdFromPath(AdminEvent adminEvent) {
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return null;
        }
        Matcher matcher = USER_PATH.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (configParameter == null || configParameter.isBlank()) {
            return true;
        }
        
        Object originalEvent = context.getEvent().getEvent();
        
        if (originalEvent instanceof Event event) {
            if (event.getDetails() == null) {
                return false;
            }
            if ("email".equalsIgnoreCase(configParameter)) {
                return event.getDetails().containsKey(Details.UPDATED_EMAIL);
            } else if ("firstName".equalsIgnoreCase(configParameter) || "first_name".equalsIgnoreCase(configParameter)) {
                return event.getDetails().containsKey("updated_first_name");
            } else if ("lastName".equalsIgnoreCase(configParameter) || "last_name".equalsIgnoreCase(configParameter)) {
                return event.getDetails().containsKey("updated_last_name");
            } else {
                return event.getDetails().containsKey("updated_" + configParameter);
            }
        } else if (originalEvent instanceof AdminEvent adminEvent) {
            String representation = adminEvent.getRepresentation();
            if (representation == null || representation.isBlank()) {
                // If representation is not enabled, we cannot filter by attribute, so we fallback to true
                return true;
            }
            try {
                UserRepresentation rep = JsonSerialization.readValue(representation, UserRepresentation.class);
                if (rep == null) {
                    return true;
                }
                if ("email".equalsIgnoreCase(configParameter)) {
                    return rep.getEmail() != null;
                } else if ("firstName".equalsIgnoreCase(configParameter) || "first_name".equalsIgnoreCase(configParameter)) {
                    return rep.getFirstName() != null;
                } else if ("lastName".equalsIgnoreCase(configParameter) || "last_name".equalsIgnoreCase(configParameter)) {
                    return rep.getLastName() != null;
                } else {
                    return rep.getAttributes() != null && rep.getAttributes().containsKey(configParameter);
                }
            } catch (Exception e) {
                return true;
            }
        }
        
        return false;
    }
}
