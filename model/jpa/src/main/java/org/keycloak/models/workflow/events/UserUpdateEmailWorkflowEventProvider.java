package org.keycloak.models.workflow.events;

import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;

public class UserUpdateEmailWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern USER_PATH = Pattern.compile("^users/[^/]+$");

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
        // admin user updates only carry the email details when the email has actually changed, and only the previous email
        // when it has been cleared
        Map<String, String> details = adminEvent.getDetails();
        return org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.UPDATE.equals(adminEvent.getOperationType())
                && adminEvent.getResourcePath() != null
                && USER_PATH.matcher(adminEvent.getResourcePath()).matches()
                && details != null
                && (details.containsKey(Details.UPDATED_EMAIL) || details.containsKey(Details.PREVIOUS_EMAIL));
    }
}
