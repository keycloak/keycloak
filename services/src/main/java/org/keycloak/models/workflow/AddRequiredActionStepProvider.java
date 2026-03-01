package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;


public class AddRequiredActionStepProvider implements WorkflowStepProvider {

    public static String REQUIRED_ACTION_KEY = "action";

    private final KeycloakSession session;
    private final ComponentModel stepModel;
    private final Logger log = Logger.getLogger(AddRequiredActionStepProvider.class);

    public AddRequiredActionStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user != null) {
            String configuredAction = stepModel.getConfig().getFirst(REQUIRED_ACTION_KEY);
            if (configuredAction == null) {
                log.warnv("Missing required configuration option '{0}' in {1}", REQUIRED_ACTION_KEY, AddRequiredActionStepProviderFactory.ID);
                return;
            }
            try {
                // Convert hyphens to underscores and uppercase to match enum naming
                configuredAction = configuredAction.replace("-", "_").toUpperCase();
                UserModel.RequiredAction action = UserModel.RequiredAction.valueOf(configuredAction);
                if (!realm.getRequiredActionProviderByAlias(action.name()).isEnabled()) {
                    log.warnv("Required action {0} is not enabled in realm {1}", action, realm.getName());
                    return;
                }
                log.debugv("Adding required action {0} to user {1})", action, user.getId());
                user.addRequiredAction(action);
            } catch (IllegalArgumentException e) {
                log.warnv("Invalid required action {0} configured in {1}", stepModel.getConfig().getFirst(REQUIRED_ACTION_KEY),
                        AddRequiredActionStepProviderFactory.ID);
            }
        }
    }

    @Override
    public void close() {
    }
}
