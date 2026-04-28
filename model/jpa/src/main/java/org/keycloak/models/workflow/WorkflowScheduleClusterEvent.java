package org.keycloak.models.workflow;

import java.util.Objects;

import org.keycloak.cluster.ClusterEvent;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(65621)
public class WorkflowScheduleClusterEvent implements ClusterEvent {

    private String realmId;
    private String workflowId;
    private boolean removed;
    private int intervalSecs;
    private int lastScheduleRun;

    @ProtoField(1)
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @ProtoField(2)
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @ProtoField(3)
    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    @ProtoField(value = 4, defaultValue = "0")
    public int getIntervalSecs() {
        return intervalSecs;
    }

    public void setIntervalSecs(int intervalSecs) {
        this.intervalSecs = intervalSecs;
    }

    @ProtoField(value = 5, defaultValue = "0")
    public int getLastScheduleRun() {
        return lastScheduleRun;
    }

    public void setLastScheduleRun(int lastScheduleRun) {
        this.lastScheduleRun = lastScheduleRun;
    }

    public static WorkflowScheduleClusterEvent create(String realmId, String workflowId, boolean removed,
            int intervalSecs, int lastScheduleRun) {
        WorkflowScheduleClusterEvent event = new WorkflowScheduleClusterEvent();
        event.setRealmId(realmId);
        event.setWorkflowId(workflowId);
        event.setRemoved(removed);
        event.setIntervalSecs(intervalSecs);
        event.setLastScheduleRun(lastScheduleRun);
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowScheduleClusterEvent that = (WorkflowScheduleClusterEvent) o;
        return removed == that.removed && intervalSecs == that.intervalSecs && lastScheduleRun == that.lastScheduleRun
                && Objects.equals(realmId, that.realmId) && Objects.equals(workflowId, that.workflowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realmId, workflowId, removed, intervalSecs, lastScheduleRun);
    }
}
