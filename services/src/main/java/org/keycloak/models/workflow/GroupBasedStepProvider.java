package org.keycloak.models.workflow;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

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
        return model.getConfig().getOrDefault(CONFIG_GROUP, List.of()).stream().map(this::getGroup);
    }

    private GroupModel getGroup(String name) {
        GroupProvider groups = session.groups();
        String[] paths = name.split("/");
        RealmModel realm = getRealm();
        GroupModel group = null;

        for (String part : paths) {
            if (part.isEmpty()) {
                continue;
            }
            group = groups.getGroupByName(realm, group, part);
        }

        if (group == null) {
            throw new IllegalStateException("Could not find group for name or path: " + name);
        }

        return group;
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
