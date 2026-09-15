package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;

public class UserAuthenticatedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserAuthenticatedWorkflowEventProvider(KeycloakSession session, String configParameter, String providerId) {
        super(session, configParameter,  providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(Event event) {
        return EventType.LOGIN.equals(event.getType());
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (super.configParameter != null) {
            // this is the case when the clientId is passed as a parameter to the event provider - like user-logged-in(account-console)
            Event loginEvent = (Event) context.getEvent().getEvent();
            return loginEvent != null && configParameter.equals(loginEvent.getClientId());
        } else {
            // nothing else to check
            return true;
        }
    }
}
