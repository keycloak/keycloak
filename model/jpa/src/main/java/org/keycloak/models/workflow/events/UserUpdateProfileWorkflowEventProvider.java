package org.keycloak.models.workflow.events;

import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;

public class UserUpdateProfileWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern USER_PATH = Pattern.compile("^users/[^/]+$");

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
        return org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.UPDATE.equals(adminEvent.getOperationType())
                && adminEvent.getResourcePath() != null
                && USER_PATH.matcher(adminEvent.getResourcePath()).matches();
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (configParameter == null || configParameter.isBlank()) {
            // nothing else to check
            return true;
        }
        // this is the case when the attribute name is passed as a parameter to the event provider - like user-update-profile(firstName)
        Object originalEvent = context.getEvent().getEvent();
        Map<String, String> details = null;

        if (originalEvent instanceof Event event) {
            details = event.getDetails();
        } else if (originalEvent instanceof AdminEvent adminEvent) {
            details = adminEvent.getDetails();
        }

        return details != null && details.containsKey(getUpdatedDetail(configParameter));
    }

    /**
     * Returns the event detail that holds the updated value of the given attribute, as set by the user profile when auditing
     * attribute changes.
     */
    private static String getUpdatedDetail(String attributeName) {
        if (UserModel.FIRST_NAME.equalsIgnoreCase(attributeName) || "first_name".equalsIgnoreCase(attributeName)) {
            return Details.UPDATED_FIRST_NAME;
        }
        if (UserModel.LAST_NAME.equalsIgnoreCase(attributeName) || "last_name".equalsIgnoreCase(attributeName)) {
            return Details.UPDATED_LAST_NAME;
        }
        if (UserModel.EMAIL.equalsIgnoreCase(attributeName)) {
            return Details.UPDATED_EMAIL;
        }
        return Details.PREF_UPDATED + attributeName;
    }
}
