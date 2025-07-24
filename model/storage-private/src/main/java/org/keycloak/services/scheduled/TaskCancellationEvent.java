package org.keycloak.services.scheduled;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.cluster.ClusterEvent;

@ProtoTypeId(65616)
public class TaskCancellationEvent implements ClusterEvent {

    public static final String CANCEL_TASK = "cancelTask";
    private final String taskName;

    @ProtoFactory
    public TaskCancellationEvent(String taskName) {
        this.taskName = taskName;
    }

    @ProtoField(1)
    public String getTaskName() {
        return taskName;
    }
}
