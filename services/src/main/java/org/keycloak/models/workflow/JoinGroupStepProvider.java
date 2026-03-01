package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.ModelToRepresentation.buildGroupPath;

public class JoinGroupStepProvider extends GroupBasedStepProvider {

    private final Logger log = Logger.getLogger(JoinGroupStepProvider.class);

    protected JoinGroupStepProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    protected void run(UserModel user, GroupModel group) {
        log.debugv("Adding user %s to group %s)", user.getId(), buildGroupPath(group));
        user.joinGroup(group);
    }
}
