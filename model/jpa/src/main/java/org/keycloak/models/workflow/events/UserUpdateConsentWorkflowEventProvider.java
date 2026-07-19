package org.keycloak.models.workflow.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowEvent;

public class UserUpdateConsentWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern CONSENTS_PATH = Pattern.compile("^users/([^/]+)/consents(?:/.*)?$");

    public UserUpdateConsentWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.UPDATE_CONSENT.equals(event.getType()) || EventType.GRANT_CONSENT.equals(event.getType());
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        if (!org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())) {
            return false;
        }
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return false;
        }
        return CONSENTS_PATH.matcher(path).matches();
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
        Matcher matcher = CONSENTS_PATH.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
