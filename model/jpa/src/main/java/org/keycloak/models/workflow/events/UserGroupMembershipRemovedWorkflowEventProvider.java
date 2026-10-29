package org.keycloak.models.workflow.events;

import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupMemberLeaveEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.provider.ProviderEvent;

import static org.keycloak.models.utils.KeycloakModelUtils.GROUP_PATH_SEPARATOR;

public class UserGroupMembershipRemovedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserGroupMembershipRemovedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return providerEvent instanceof GroupMemberLeaveEvent;
    }

    @Override
    protected String resolveResourceId(ProviderEvent providerEvent) {
        if (providerEvent instanceof GroupMemberLeaveEvent gme) {
            return gme.getUser().getId();
        }
        return null;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (super.configParameter != null) {
            String groupName = configParameter;
            if (!groupName.startsWith(GROUP_PATH_SEPARATOR))
                groupName = GROUP_PATH_SEPARATOR + groupName;
            ProviderEvent groupEvent = (ProviderEvent) context.getEvent().getEvent();
            if (groupEvent instanceof GroupMemberLeaveEvent leaveEvent) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(session, leaveEvent.getRealm(), groupName);
                return group != null && group.getId().equals(leaveEvent.getGroup().getId());
            } else {
                return false;
            }
        } else {
            // nothing else to check
            return true;
        }
    }
}
