package org.keycloak.models.workflow.conditions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.utils.RoleUtils;

public class RoleWorkflowConditionProvider implements WorkflowConditionProvider {

    private final List<String> expectedRoles;
    private final KeycloakSession session;

    public RoleWorkflowConditionProvider(KeycloakSession session, List<String> expectedRoles) {
        this.session = session;
        this.expectedRoles = expectedRoles;
    }

    @Override
    public boolean evaluate(WorkflowEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);

        if (user == null) {
            return false;
        }

        Set<RoleModel> roles = user.getRoleMappingsStream().collect(Collectors.toSet());

        for (String name : expectedRoles) {
            RoleModel expectedRole = getRole(name, realm);

            if (expectedRole == null || !RoleUtils.hasRole(roles, expectedRole)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void validate() throws WorkflowInvalidStateException {
        expectedRoles.forEach(id -> {
            if (session.roles().getRoleById(session.getContext().getRealm(), id) == null) {
                throw new WorkflowInvalidStateException(String.format("Role with id %s does not exist.", id));
            }
        });
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
