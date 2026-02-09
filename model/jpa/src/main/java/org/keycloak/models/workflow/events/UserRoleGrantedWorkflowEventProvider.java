package org.keycloak.models.workflow.events;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel.RoleGrantedEvent;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.provider.ProviderEvent;

public class UserRoleGrantedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserRoleGrantedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return providerEvent instanceof RoleGrantedEvent;
    }

    @Override
    protected String resolveResourceId(ProviderEvent providerEvent) {
        if (providerEvent instanceof RoleGrantedEvent rge) {
            return rge.getUser().getId();
        }
        return null;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (super.configParameter != null) {
            // this is the case when the role name is passed as a parameter to the event provider - like user-role-granted(myrole)
            ProviderEvent roleEvent = (ProviderEvent) context.getEvent().getEvent();
            if (roleEvent instanceof RoleGrantedEvent roleGrantedEvent) {
                return configParameter.equals(roleGrantedEvent.getRole().getName());
            } else {
                return false;
            }
        } else {
            // nothing else to check
            return true;
        }
    }
}
