package org.keycloak.models.workflow;

import java.util.Locale;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
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
            RequiredActionProviderModel provider =
                    realm.getRequiredActionProviderByAlias(configuredAction);

            if (provider == null) {
                String normalized = configuredAction.replace("-", "_").toUpperCase(Locale.ROOT);
                provider = realm.getRequiredActionProviderByAlias(normalized);
            }

            if (provider == null) {
                log.warnv("Required action {0} is not configured in realm {1}",
                        configuredAction, realm.getName());
                return;
            }

            if (!provider.isEnabled()) {
                log.warnv("Required action {0} is not enabled in realm {1}",
                        configuredAction, realm.getName());
                return;
            }

            log.debugv("Adding required action {0} to user {1}", provider.getAlias(), user.getId());

            user.addRequiredAction(provider.getAlias());
        }
    }

    @Override
    public void close() {
    }
}
