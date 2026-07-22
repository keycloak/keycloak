package org.keycloak.models.workflow.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

public class UserUpdateEmailWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern USER_PATH = Pattern.compile("^users/([^/]+)$");

    public UserUpdateEmailWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.UPDATE_EMAIL.equals(event.getType());
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
        if (!USER_PATH.matcher(path).matches()) {
            return false;
        }
        if (adminEvent.getDetails() != null) {
            return adminEvent.getDetails().containsKey(org.keycloak.events.Details.UPDATED_EMAIL);
        }
        return false;
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
}
