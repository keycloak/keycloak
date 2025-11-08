package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.ScheduledTask;

import org.jboss.logging.Logger;

/**
 * A {@link ScheduledTask} that runs all the scheduled steps for resources on a per-realm basis.
 */
final class WorkflowRunnerScheduledTask implements ScheduledTask {

    private final Logger logger = Logger.getLogger(WorkflowRunnerScheduledTask.class);

    private final KeycloakSessionFactory sessionFactory;

    WorkflowRunnerScheduledTask(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void run(KeycloakSession session) {
        // TODO: Depending on how many realms and the steps in use, this task can consume a lot of gears (e.g.: cpu, memory, and network)
        // we need a smarter mechanism that process realms in batches with some window interval
        session.realms().getRealmsStream().map(RealmModel::getId).forEach(this::runScheduledTasksOnRealm);
    }

    private void runScheduledTasksOnRealm(String id) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
            try {
                KeycloakContext context = session.getContext();
                RealmModel realm = session.realms().getRealm(id);

                context.setRealm(realm);
                session.getProvider(WorkflowProvider.class).runScheduledSteps();

                sessionFactory.publish(new WorkflowStepRunnerSuccessEvent(session));
            } catch (Exception e) {
                logger.errorf(e, "Failed to run workflow steps on realm with id '%s'", id);
            }
        });
    }

    @Override
    public String getTaskName() {
        return "workflow-runner-task";
    }
}
