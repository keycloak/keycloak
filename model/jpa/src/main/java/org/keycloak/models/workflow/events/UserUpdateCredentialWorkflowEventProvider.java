package org.keycloak.models.workflow.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowExecutionContext;

public class UserUpdateCredentialWorkflowEventProvider extends AbstractWorkflowEventProvider {

    // resetting the password is the only admin operation that updates a credential of a user
    private static final Pattern RESET_PASSWORD_PATH = Pattern.compile("^users/([^/]+)/reset-password$");

    public UserUpdateCredentialWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.UPDATE_CREDENTIAL.equals(event.getType());
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        return org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.ACTION.equals(adminEvent.getOperationType())
                && adminEvent.getResourcePath() != null
                && RESET_PASSWORD_PATH.matcher(adminEvent.getResourcePath()).matches();
    }

    @Override
    public WorkflowEvent create(AdminEvent adminEvent) {
        if (supports(adminEvent)) {
            // the user id is not the last segment of the resource path, so it can't be resolved by the default implementation
            Matcher matcher = RESET_PASSWORD_PATH.matcher(adminEvent.getResourcePath());
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
        // this is the case when the credential type is passed as a parameter to the event provider - like user-update-credential(password)
        Object originalEvent = context.getEvent().getEvent();
        String credentialType = null;

        if (originalEvent instanceof Event event) {
            credentialType = event.getDetails() != null ? event.getDetails().get(Details.CREDENTIAL_TYPE) : null;
        } else if (originalEvent instanceof AdminEvent) {
            credentialType = PasswordCredentialModel.TYPE;
        }

        return configParameter.equalsIgnoreCase(credentialType);
    }
}
