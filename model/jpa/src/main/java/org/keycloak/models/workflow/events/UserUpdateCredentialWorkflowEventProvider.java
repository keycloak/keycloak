package org.keycloak.models.workflow.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowExecutionContext;

public class UserUpdateCredentialWorkflowEventProvider extends AbstractWorkflowEventProvider {

    private static final Pattern RESET_PASSWORD_PATH = Pattern.compile("^users/([^/]+)/reset-password$");
    private static final Pattern CREDENTIALS_PATH = Pattern.compile("^users/([^/]+)/credentials/([^/]+)$");

    public UserUpdateCredentialWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.UPDATE_CREDENTIAL.equals(event.getType()) || EventType.RESET_PASSWORD.equals(event.getType());
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
        return RESET_PASSWORD_PATH.matcher(path).matches() 
            || (CREDENTIALS_PATH.matcher(path).matches() && !OperationType.DELETE.equals(adminEvent.getOperationType()));
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
        Matcher resetMatcher = RESET_PASSWORD_PATH.matcher(path);
        if (resetMatcher.matches()) {
            return resetMatcher.group(1);
        }
        Matcher credsMatcher = CREDENTIALS_PATH.matcher(path);
        if (credsMatcher.matches()) {
            return credsMatcher.group(1);
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
        String credentialType = null;
        
        if (originalEvent instanceof Event event) {
            if (EventType.RESET_PASSWORD.equals(event.getType())) {
                credentialType = "password";
            } else if (event.getDetails() != null) {
                credentialType = event.getDetails().get(Details.CREDENTIAL_TYPE);
            }
        } else if (originalEvent instanceof AdminEvent adminEvent) {
            credentialType = getCredentialType(adminEvent);
        }
        
        return configParameter.equalsIgnoreCase(credentialType);
    }

    private String getCredentialType(AdminEvent adminEvent) {
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return null;
        }
        Matcher resetMatcher = RESET_PASSWORD_PATH.matcher(path);
        if (resetMatcher.matches()) {
            return "password";
        }
        Matcher credsMatcher = CREDENTIALS_PATH.matcher(path);
        if (credsMatcher.matches()) {
            String userId = credsMatcher.group(1);
            String credentialId = credsMatcher.group(2);
            RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
            if (realm != null) {
                UserModel user = session.users().getUserById(realm, userId);
                if (user != null) {
                    org.keycloak.credential.CredentialModel stored = user.credentialManager().getStoredCredentialById(credentialId);
                    if (stored != null) {
                        return stored.getType();
                    }
                }
            }
            if (adminEvent.getDetails() != null) {
                return adminEvent.getDetails().get(Details.CREDENTIAL_TYPE);
            }
        }
        return null;
    }
}
