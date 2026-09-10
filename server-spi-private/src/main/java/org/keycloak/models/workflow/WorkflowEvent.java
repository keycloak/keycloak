package org.keycloak.models.workflow;

public class WorkflowEvent {

    private final ResourceType type;
    private final String resourceId;
    private final Object event;
    private final String eventProviderId;

    public WorkflowEvent(ResourceType type, String resourceId, Object event, String eventProviderId) {
        this.type = type;
        this.resourceId = resourceId;
        this.event = event;
        this.eventProviderId = eventProviderId;
    }

    public ResourceType getResourceType() {
        return type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getEventProviderId() {
        return eventProviderId;
    }

    public Object getEvent() {
        return event;
    }
}
