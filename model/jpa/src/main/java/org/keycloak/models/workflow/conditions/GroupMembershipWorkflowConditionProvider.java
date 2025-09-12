package org.keycloak.models.workflow.conditions;

import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.models.workflow.ResourceType;

public class GroupMembershipWorkflowConditionProvider implements WorkflowConditionProvider {

    private final List<String> expectedGroups;
    private final KeycloakSession session;

    public GroupMembershipWorkflowConditionProvider(KeycloakSession session, List<String> expectedGroups) {
        this.session = session;
        this.expectedGroups = expectedGroups;;
    }

    @Override
    public boolean evaluate(WorkflowEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        validate();

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);

        for (String expectedGroup : expectedGroups) {
            GroupModel group = session.groups().getGroupById(realm, expectedGroup);

            if (user.isMemberOf(group)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void validate() {
        expectedGroups.forEach(id -> {
            if (session.groups().getGroupById(session.getContext().getRealm(), id) == null) {
                throw new WorkflowInvalidStateException(String.format("Group with id %s does not exist.", id));
            }
        });
    }

    @Override
    public void close() {

    }
}
