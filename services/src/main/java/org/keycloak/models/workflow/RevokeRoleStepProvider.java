package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

public class RevokeRoleStepProvider extends RoleBasedStepProvider {

    private final Logger log = Logger.getLogger(RevokeRoleStepProvider.class);

    protected RevokeRoleStepProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    protected void run(UserModel user, RoleModel role) {
        log.debugv("Revoking role %s from user %s)", role.getName(), user.getId());
        user.deleteRoleMapping(role);
    }
}
