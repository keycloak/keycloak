package org.keycloak.models.policy;

public class ResourcePolicyEvent {

    private final ResourceType type;
    private final ResourceOperationType operation;
    private final String resourceId;

    public ResourcePolicyEvent(ResourceType type, ResourceOperationType operation, String resourceId) {
        this.type = type;
        this.operation = operation;
        this.resourceId = resourceId;
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
}
