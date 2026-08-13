package org.keycloak.models.workflow;

import org.keycloak.representations.workflows.WorkflowConstants;

final class AdhocWorkflowEvent extends WorkflowEvent {

    AdhocWorkflowEvent(ResourceType type, String resourceId) {
        super(type, resourceId, null, WorkflowConstants.AD_HOC);
    }
}
