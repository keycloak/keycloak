package org.keycloak.models.workflow.conditions;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.utils.StringUtil;

public class GroupMembershipWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedGroup;
    private final KeycloakSession session;

    public GroupMembershipWorkflowConditionProvider(KeycloakSession session,String expectedGroup) {
        this.session = session;
        this.expectedGroup = expectedGroup;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());
        if (user == null) {
            return false;
        }

        GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, expectedGroup);
        return user.isMemberOf(group);
    }

    @Override
    public void validate() {
        if (StringUtil.isBlank(this.expectedGroup)) {
            throw new WorkflowInvalidStateException("Expected group path not set.");
        }
        if (KeycloakModelUtils.findGroupByPath(session, session.getContext().getRealm(), expectedGroup) == null) {
            throw new WorkflowInvalidStateException(String.format("Group with name %s does not exist.", expectedGroup));
        }
    }

    @Override
    public void close() {

    }
}
