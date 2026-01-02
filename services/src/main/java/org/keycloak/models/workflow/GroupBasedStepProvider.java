package org.keycloak.models.workflow;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

public abstract class GroupBasedStepProvider implements WorkflowStepProvider {

    private final Logger log = Logger.getLogger(GroupBasedStepProvider.class);
    public static final String CONFIG_GROUP = "group";

    private final KeycloakSession session;
    private final ComponentModel model;

    public GroupBasedStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        UserModel user = session.users().getUserById(getRealm(), context.getResourceId());

        if (user != null) {
            try {
                getGroups().forEach(group -> run(user, group));
            } catch (Exception e) {
                log.errorf(e, "Failed to manage group membership for user %s", user.getId());
            }
        }
    }

    protected abstract void run(UserModel user, GroupModel group);

    @Override
    public void close() {
    }

    private Stream<GroupModel> getGroups() {
        return model.getConfig().getOrDefault(CONFIG_GROUP, List.of()).stream()
                .map(name -> KeycloakModelUtils.findGroupByPath(session, getRealm(), name));
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
