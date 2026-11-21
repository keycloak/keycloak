package org.keycloak.models.workflow.events;

import java.util.Arrays;
import java.util.Map;
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

    private static final Pattern RESET_PASSWORD_PATH = Pattern.compile("^users/([^/]+)/reset-password$");
    private static final Pattern USER_PATH = Pattern.compile("^users/[^/]+$");

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
        if (!org.keycloak.events.admin.ResourceType.USER.equals(adminEvent.getResourceType()) || adminEvent.getResourcePath() == null) {
            return false;
        }
        if (OperationType.ACTION.equals(adminEvent.getOperationType())) {
            return RESET_PASSWORD_PATH.matcher(adminEvent.getResourcePath()).matches();
        }
        // admin user updates only carry the credential type detail when credentials were set from the user representation
        return OperationType.UPDATE.equals(adminEvent.getOperationType())
                && USER_PATH.matcher(adminEvent.getResourcePath()).matches()
                && adminEvent.getDetails() != null
                && adminEvent.getDetails().containsKey(Details.CREDENTIAL_TYPE);
    }

    @Override
    public WorkflowEvent create(AdminEvent adminEvent) {
        if (!supports(adminEvent)) {
            return null;
        }
        Matcher matcher = RESET_PASSWORD_PATH.matcher(adminEvent.getResourcePath());
        if (matcher.matches()) {
            // the user id is not the last segment of the reset password path, so it can't be resolved by the default implementation
            return new WorkflowEvent(getSupportedResourceType(), matcher.group(1), adminEvent, providerId);
        }
        return super.create(adminEvent);
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
        Map<String, String> details = null;

        if (originalEvent instanceof Event event) {
            details = event.getDetails();
        } else if (originalEvent instanceof AdminEvent adminEvent) {
            details = adminEvent.getDetails();
        }

        String credentialType = details != null ? details.get(Details.CREDENTIAL_TYPE) : null;

        if (credentialType == null && originalEvent instanceof AdminEvent) {
            // resetting the password is the only admin operation that doesn't carry the credential type
            credentialType = PasswordCredentialModel.TYPE;
        }

        // admin user updates can set several credentials at once, in which case their types are joined
        return credentialType != null && Arrays.stream(credentialType.split("::")).anyMatch(configParameter::equalsIgnoreCase);
    }
}
