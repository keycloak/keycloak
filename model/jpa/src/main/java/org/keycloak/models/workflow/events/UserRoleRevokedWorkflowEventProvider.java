package org.keycloak.models.workflow.events;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleModel.RoleRevokedEvent;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.provider.ProviderEvent;

public class UserRoleRevokedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserRoleRevokedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return providerEvent instanceof RoleRevokedEvent;
    }

    @Override
    protected String resolveResourceId(ProviderEvent providerEvent) {
        if (providerEvent instanceof RoleRevokedEvent rre) {
            return rre.getUser().getId();
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
            if (roleEvent instanceof RoleRevokedEvent roleRevokedEvent) {
                RoleModel expectedRole = resolveRole(configParameter, roleRevokedEvent.getRealm());
                return expectedRole != null && expectedRole.getId().equals(roleRevokedEvent.getRole().getId());
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
