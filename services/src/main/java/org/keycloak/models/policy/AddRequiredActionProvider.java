package org.keycloak.models.policy;


import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.keycloak.models.policy.ResourceAction.AFTER_KEY;

public class AddRequiredActionProvider implements ResourceActionProvider {

    public static String REQUIRED_ACTION_KEY = "action";

    private final KeycloakSession session;
    private final ComponentModel actionModel;
    private final Logger log = Logger.getLogger(AddRequiredActionProvider.class);

    public AddRequiredActionProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.actionModel = model;
    }

    @Override
    public void run(List<String> userIds) {
        RealmModel realm = session.getContext().getRealm();

        for (String id : userIds) {
            UserModel user = session.users().getUserById(realm, id);

            if (user != null) {
                try {
                    UserModel.RequiredAction action = UserModel.RequiredAction.valueOf(actionModel.getConfig().getFirst(REQUIRED_ACTION_KEY));
                    log.debugv("Adding required action {0} to user {1})", action, user.getId());
                    user.addRequiredAction(action);
                } catch (IllegalArgumentException e) {
                    log.warnv("Invalid required action {0} configured in AddRequiredActionProvider", actionModel.getConfig().getFirst(REQUIRED_ACTION_KEY));
                }
            }
        }
    }

    @Override
    public boolean isRunnable() {
        return actionModel.get(AFTER_KEY) != null;
    }

    @Override
    public void close() {
    }
}
