package org.keycloak.models.workflow.events;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
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
            ProviderEvent roleEvent = (ProviderEvent) context.getEvent().getEvent();
            if (roleEvent instanceof RoleGrantedEvent roleGrantedEvent) {
                RoleModel expectedRole = resolveRole(configParameter, roleGrantedEvent.getRealm());
                return expectedRole != null && expectedRole.getId().equals(roleGrantedEvent.getRole().getId());
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private RoleModel resolveRole(String roleName, RealmModel realm) {
        int slashIndex = roleName.indexOf('/');
        if (slashIndex != -1) {
            String clientId = roleName.substring(0, slashIndex);
            String name = roleName.substring(slashIndex + 1);
            ClientModel client = session.clients().getClientByClientId(realm, clientId);
            return client != null ? client.getRole(name) : null;
        }
        return realm.getRole(roleName);
    }
}
