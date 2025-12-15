package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

public class GrantRoleStepProvider extends RoleBasedStepProvider {

    private final Logger log = Logger.getLogger(GrantRoleStepProvider.class);

    protected GrantRoleStepProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    protected void run(UserModel user, RoleModel role) {
        log.debugv("Granting role %s to user %s)", role.getName(), user.getId());
        user.grantRole(role);
    }
}
