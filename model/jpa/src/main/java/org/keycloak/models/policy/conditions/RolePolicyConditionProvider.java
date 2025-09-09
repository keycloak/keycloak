package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.ResourcePolicyConditionProvider;
import org.keycloak.models.policy.ResourcePolicyEvent;
import org.keycloak.models.policy.ResourceType;
import org.keycloak.models.utils.RoleUtils;

public class RolePolicyConditionProvider implements ResourcePolicyConditionProvider {

    private final List<String> expectedRoles;
    private final KeycloakSession session;

    public RolePolicyConditionProvider(KeycloakSession session, List<String> expectedRoles) {
        this.session = session;
        this.expectedRoles = expectedRoles;
    }

    @Override
    public boolean evaluate(ResourcePolicyEvent event) {
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
