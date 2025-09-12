package org.keycloak.models.workflow;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class AggregatedActionProvider implements WorkflowActionProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final Logger log = Logger.getLogger(AggregatedActionProvider.class);

    public AggregatedActionProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(List<String> userIds) {
        WorkflowsManager manager = new WorkflowsManager(session);
        List<WorkflowActionProvider> actions = manager.getActionById(session, model.getId())
                .getActions().stream()
                .map(manager::getActionProvider)
                .toList();

        for (String userId : userIds) {
            for (WorkflowActionProvider action : actions) {
                try {
                    action.run(List.of(userId));
                } catch (Exception e) {
                    log.errorf(e, "Failed to execute action %s for user %s", model.getProviderId(), userId);
                }
            }
        }
    }

    @Override
    public boolean isRunnable() {
        return true;
    }
}
