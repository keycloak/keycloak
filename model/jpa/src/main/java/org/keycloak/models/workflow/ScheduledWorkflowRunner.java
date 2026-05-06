package org.keycloak.models.workflow;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.DurationConverter;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class ScheduledWorkflowRunner implements ScheduledTask {

    private static final Logger log = Logger.getLogger("org.keycloak.workflow.schedule");

    private static final int MIN_LOCK_TIMEOUT_SECS = 30;

    private final String workflowId;
    private final String realmId;
    private final int intervalSecs;

    public ScheduledWorkflowRunner(String workflowId, String realmId, int intervalSecs) {
        this.workflowId = workflowId;
        this.realmId = realmId;
        this.intervalSecs = intervalSecs;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);

        if (realm == null) {
            log.warnf("Realm %s for scheduled workflow %s not found, cancelling task", realmId, workflowId);
            cancelTask(session);
            return;
        }

        session.getContext().setRealm(realm);
        WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
        Workflow workflow;

        try {
            workflow = provider.getWorkflow(workflowId);
        } catch (BadRequestException e) {
            log.warnf("Scheduled workflow %s in realm %s not found, cancelling task", workflowId, realmId);
            cancelTask(session);
            return;
        }

        if (!workflow.isEnabled()) {
            log.debugf("Workflow '%s' in realm %s is disabled, cancelling scheduled task", workflow.getName(), realm.getName());
            cancelTask(session);
            return;
        }

        String currentSchedule = workflow.getConfig().getFirst(WorkflowConstants.CONFIG_SCHEDULE_AFTER);
        if (currentSchedule == null) {
            log.debugf("Workflow '%s' in realm %s no longer has a schedule, cancelling task", workflow.getName(), realm.getName());
            cancelTask(session);
            return;
        }

        int currentIntervalSecs = (int) DurationConverter.parseDuration(currentSchedule).toSeconds();
        if (currentIntervalSecs != intervalSecs) {
            log.debugf("Schedule interval for workflow '%s' in realm %s changed from %d to %d s, rescheduling",
                    workflow.getName(), realm.getName(), intervalSecs, currentIntervalSecs);
            cancelTask(session);
            scheduleAligned(session, workflow, currentIntervalSecs);
            return;
        }

        if (!isSchedulePeriod(workflow)) {
            log.debugf("Skipping scheduled workflow '%s' in realm %s, too soon since last run", workflow.getName(), realm.getName());
            return;
        }

        ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
        String taskKey = workflowId + "::schedule";
        int lockTimeout = Math.max(MIN_LOCK_TIMEOUT_SECS, intervalSecs);

        ExecutionResult<Void> result = clusterProvider.executeIfNotExecuted(taskKey, lockTimeout, () -> {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
                RealmModel r = s.realms().getRealm(realmId);
                s.getContext().setRealm(r);
                updateLastScheduleRun(s);
            });

            log.debugf("Executing scheduled workflow '%s' in realm %s", workflow.getName(), realm.getName());

            try {
                provider.activateForAllEligibleResources(workflow);
            } catch (Exception e) {
                log.errorf(e, "Error while executing scheduled workflow %s in realm %s", workflow.getName(), realm.getName());
            }

            log.debugf("Finished executing scheduled workflow '%s' in realm %s", workflow.getName(), realm.getName());
            return null;
        });

        if (!result.isExecuted()) {
            log.debugf("Skipping scheduled workflow '%s' in realm %s, already in progress on another node", workflow.getName(), realm.getName());
        }
    }

    private boolean isSchedulePeriod(Workflow workflow) {
        int lastRun = getLastScheduleRun(workflow);

        if (lastRun <= 0) {
            return true;
        }

        int elapsed = Time.currentTime() - lastRun;
        return elapsed >= (intervalSecs - 1);
    }

    private void updateLastScheduleRun(KeycloakSession session) {
        ComponentModel component = session.getContext().getRealm().getComponent(workflowId);
        component.put(WorkflowConstants.CONFIG_LAST_SCHEDULE_RUN, String.valueOf(Time.currentTime()));
        session.getContext().getRealm().updateComponent(component);
    }

    private void cancelTask(KeycloakSession session) {
        session.getProvider(TimerProvider.class).cancelTask(getTaskName());
    }

    private void scheduleAligned(KeycloakSession session, Workflow workflow, int newIntervalSecs) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        ScheduledWorkflowRunner newRunner = new ScheduledWorkflowRunner(workflowId, realmId, newIntervalSecs);
        long initialDelayMillis = computeInitialDelay(workflow, newIntervalSecs) * 1000L;
        timer.scheduleTask(newRunner, initialDelayMillis, newIntervalSecs * 1000L);
    }

    @Override
    public String getTaskName() {
        return taskName(workflowId);
    }

    static String taskName(String workflowId) {
        return "workflow-" + workflowId;
    }

    static int getLastScheduleRun(Workflow workflow) {
        String val = workflow.getConfig().getFirst(WorkflowConstants.CONFIG_LAST_SCHEDULE_RUN);
        return val == null ? 0 : Integer.parseInt(val);
    }

    static int computeInitialDelay(Workflow workflow, int intervalSecs) {
        return computeInitialDelay(getLastScheduleRun(workflow), intervalSecs);
    }

    static int computeInitialDelay(int lastRunSecs, int intervalSecs) {
        if (lastRunSecs <= 0) {
            return intervalSecs;
        }

        int nextFireTime = lastRunSecs + intervalSecs;
        int delay = nextFireTime - Time.currentTime();
        return Math.max(0, delay);
    }
}
