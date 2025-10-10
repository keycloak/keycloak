package org.keycloak.models.workflow;

final class AdhocWorkflowEvent extends WorkflowEvent {

    AdhocWorkflowEvent(ResourceType type, String resourceId) {
        super(type, ResourceOperationType.AD_HOC, resourceId, null);
    }
}
