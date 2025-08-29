package org.keycloak.models.policy;

import java.util.HashMap;
import java.util.Map;

public class ResourcePolicyEvent {

    private final ResourceType type;
    private final ResourceOperationType operation;
    private final String resourceId;
    private final Map<String, String> details = new HashMap<>();

    public ResourcePolicyEvent(ResourceType type, ResourceOperationType operation, String resourceId) {
        this(type, operation, resourceId, null);
    }

    public ResourcePolicyEvent(ResourceType type, ResourceOperationType operation, String resourceId, Map<String, String> details) {
        this.type = type;
        this.operation = operation;
        this.resourceId = resourceId;
        if (details != null) {
            this.details.putAll(details);
        }
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

    public Map<String, String> getDetails() {
        return details;
    }
}
