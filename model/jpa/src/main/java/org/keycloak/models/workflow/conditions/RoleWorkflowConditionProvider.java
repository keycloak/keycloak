package org.keycloak.models.workflow.conditions;

import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.utils.StringUtil;

public class RoleWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedRole;
    private final KeycloakSession session;

    public RoleWorkflowConditionProvider(KeycloakSession session, String expectedRole) {
        this.session = session;
        this.expectedRole = expectedRole;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user == null) {
            return false;
        }

        Set<RoleModel> roles = user.getRoleMappingsStream().collect(Collectors.toSet());
        RoleModel role = getRole(expectedRole, realm);
        return role != null && RoleUtils.hasRole(roles, role);
    }

    @Override
    public void validate() throws WorkflowInvalidStateException {
        if (StringUtil.isBlank(expectedRole)) {
            throw new WorkflowInvalidStateException("Expected role name not set.");
        }
        if (getRole(expectedRole, session.getContext().getRealm()) == null) {
            throw new WorkflowInvalidStateException(String.format("Role with name %s does not exist.", expectedRole));
        }
    }

    private RoleModel getRole(String expectedRole, RealmModel realm) {
        boolean isClientRole = expectedRole.indexOf('/') != -1;

        if (isClientRole) {
            String[] parts = expectedRole.split("/");

            if (parts.length != 2) {
                return null;
            }

            String clientId = parts[0];
            String roleName = parts[1];

            ClientModel client = session.clients().getClientByClientId(realm, clientId);

            if (client == null) {
                return null;
            }

            return client.getRole(roleName);
        }

        return realm.getRole(expectedRole);
    }

    @Override
    public void close() {

    }
}
