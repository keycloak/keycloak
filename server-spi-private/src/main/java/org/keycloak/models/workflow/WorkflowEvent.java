package org.keycloak.models.workflow;

public class WorkflowEvent {

    private final ResourceType type;
    private final ResourceOperationType operation;
    private final String resourceId;
    private final Object event;

    public WorkflowEvent(ResourceType type, ResourceOperationType operation, String resourceId, Object event) {
        this.type = type;
        this.operation = operation;
        this.resourceId = resourceId;
        this.event = event;
    }

    public ResourceType getResourceType() {
        return type;
    }

    public ResourceOperationType getOperation() {
        return operation;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Object getEvent() {
        return event;
    }
}
