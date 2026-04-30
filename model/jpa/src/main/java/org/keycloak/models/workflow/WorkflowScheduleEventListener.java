package org.keycloak.models.workflow;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

public final class WorkflowScheduleEventListener implements ClusterListener, ProviderEventListener {

    private static final Logger logger = Logger.getLogger("org.keycloak.workflow.schedule");
    static final String WORKFLOW_SCHEDULE_TASK_KEY = "workflow-schedule";

    private final KeycloakSessionFactory sessionFactory;

    public WorkflowScheduleEventListener(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void eventReceived(ClusterEvent event) {
        WorkflowScheduleClusterEvent workflowEvent = (WorkflowScheduleClusterEvent) event;
        logger.debugf("Received workflow schedule cluster event for workflow %s (removed=%s, interval=%d s)",
                workflowEvent.getWorkflowId(), workflowEvent.isRemoved(), workflowEvent.getIntervalSecs());

        runJobInTransaction(sessionFactory, session -> {
            RealmModel realm = session.realms().getRealm(workflowEvent.getRealmId());

            if (realm == null) {
                logger.debugf("Realm %s not found, ignoring workflow schedule cluster event", workflowEvent.getRealmId());
                return;
            }

            session.getContext().setRealm(realm);
            rescheduleWorkflow(session, workflowEvent);
        });
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent ev) {
            runJobInTransaction(ev.getFactory(), session -> {
                ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);

                if (clusterProvider != null) {
                    clusterProvider.registerListener(WORKFLOW_SCHEDULE_TASK_KEY, this);
                }
            });
        }
    }

    private void rescheduleWorkflow(KeycloakSession session, WorkflowScheduleClusterEvent workflowEvent) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        String workflowId = workflowEvent.getWorkflowId();
        String taskName = ScheduledWorkflowRunner.taskName(workflowId);

        timer.cancelTask(taskName);

        if (workflowEvent.isRemoved() || workflowEvent.getIntervalSecs() <= 0) {
            logger.debugf("Cancelled scheduled workflow task %s", workflowId);
            return;
        }

        int intervalSecs = workflowEvent.getIntervalSecs();
        int lastScheduleRun = workflowEvent.getLastScheduleRun();
        int initialDelaySecs = ScheduledWorkflowRunner.computeInitialDelay(lastScheduleRun, intervalSecs);
        String realmId = session.getContext().getRealm().getId();
        ScheduledWorkflowRunner runner = new ScheduledWorkflowRunner(workflowId, realmId, intervalSecs);
        timer.scheduleTask(runner, initialDelaySecs * 1000L, intervalSecs * 1000L);
        logger.debugf("Rescheduled workflow %s with interval %d s, initial delay %d s (cluster event)", workflowId, intervalSecs, initialDelaySecs);
    }

    void notifyCluster(KeycloakSession session, String realmId, String workflowId, boolean removed,
            int intervalSecs, int lastScheduleRun) {
        ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);

        if (clusterProvider != null) {
            WorkflowScheduleClusterEvent event = WorkflowScheduleClusterEvent.create(
                    realmId, workflowId, removed, intervalSecs, lastScheduleRun);
            clusterProvider.notify(WORKFLOW_SCHEDULE_TASK_KEY, event, true);
        }
    }
}
