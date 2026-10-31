package org.keycloak.models.workflow.events;

import java.util.Map;
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

public class UserRemoveCredentialWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern CREDENTIALS_PATH = Pattern.compile("^users/([^/]+)/credentials/[^/]+$");

    public UserRemoveCredentialWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.REMOVE_CREDENTIAL.equals(event.getType());
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        // removing a credential through the admin API is reported as an ACTION on the credential path
        return org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.ACTION.equals(adminEvent.getOperationType())
                && adminEvent.getResourcePath() != null
                && CREDENTIALS_PATH.matcher(adminEvent.getResourcePath()).matches();
    }

    @Override
    public WorkflowEvent create(AdminEvent adminEvent) {
        if (supports(adminEvent)) {
            // the user id is not the last segment of the resource path, so it can't be resolved by the default implementation
            Matcher matcher = CREDENTIALS_PATH.matcher(adminEvent.getResourcePath());
            return matcher.matches() ? new WorkflowEvent(getSupportedResourceType(), matcher.group(1), adminEvent, providerId) : null;
        }
        return null;
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
        // this is the case when the credential type is passed as a parameter to the event provider - like user-remove-credential(otp)
        Object originalEvent = context.getEvent().getEvent();
        Map<String, String> details = null;

        if (originalEvent instanceof Event event) {
            details = event.getDetails();
        } else if (originalEvent instanceof AdminEvent adminEvent) {
            details = adminEvent.getDetails();
        }

        return details != null && configParameter.equalsIgnoreCase(details.get(Details.CREDENTIAL_TYPE));
    }
}
