package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Utility class for creating and publishing workflow provider events.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
final class WorkflowProviderEvents {

    private WorkflowProviderEvents() {
    }

    static void fireWorkflowActivatedEvent(KeycloakSession session, Workflow workflow, String resourceId,
                                          String executionId, String triggerEventType) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowActivatedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public String getTriggerEventType() {
                return triggerEventType;
            }
        });
    }

    static void fireWorkflowDeactivatedEvent(KeycloakSession session, Workflow workflow, String resourceId,
                                            String executionId, String reason) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowDeactivatedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public String getReason() {
                return reason;
            }
        });
    }

    static void fireWorkflowRestartedEvent(KeycloakSession session, Workflow workflow, String resourceId,
                                          String executionId) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowRestartedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }
        });
    }

    static void fireWorkflowStepScheduledEvent(KeycloakSession session, Workflow workflow, WorkflowStep step,
                                              String resourceId, String executionId, long scheduledTime, String delay) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowStepScheduledEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public String getStepId() {
                return step.getId();
            }

            @Override
            public String getStepProviderId() {
                return step.getProviderId();
            }

            @Override
            public long getScheduledTime() {
                return scheduledTime;
            }

            @Override
            public String getDelay() {
                return delay;
            }
        });
    }

    static void fireWorkflowStepExecutedEvent(KeycloakSession session, Workflow workflow, WorkflowStep step,
                                             String resourceId, String executionId) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowStepExecutedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public String getStepId() {
                return step.getId();
            }

            @Override
            public String getStepProviderId() {
                return step.getProviderId();
            }
        });
    }

    static void fireWorkflowStepFailedEvent(KeycloakSession session, Workflow workflow, WorkflowStep step,
                                           String resourceId, String executionId, String errorMessage) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowStepFailedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public String getStepId() {
                return step.getId();
            }

            @Override
            public String getStepProviderId() {
                return step.getProviderId();
            }

            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        });
    }

    static void fireWorkflowResourceMigratedEvent(KeycloakSession session, Workflow sourceWorkflow, Workflow destWorkflow,
                                                 WorkflowStep sourceStep, WorkflowStep destStep, String resourceId,
                                                 String oldExecutionId, String newExecutionId) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowResourceMigratedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return destWorkflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return destWorkflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return destWorkflow.getSupportedType();
            }

            @Override
            public String getSourceWorkflowId() {
                return sourceWorkflow.getId();
            }

            @Override
            public String getSourceWorkflowName() {
                return sourceWorkflow.getName();
            }

            @Override
            public String getDestinationWorkflowId() {
                return destWorkflow.getId();
            }

            @Override
            public String getDestinationWorkflowName() {
                return destWorkflow.getName();
            }

            @Override
            public String getSourceStepId() {
                return sourceStep.getId();
            }

            @Override
            public String getSourceStepProviderId() {
                return sourceStep.getProviderId();
            }

            @Override
            public String getDestinationStepId() {
                return destStep.getId();
            }

            @Override
            public String getDestinationStepProviderId() {
                return destStep.getProviderId();
            }

            @Override
            public String getOldExecutionId() {
                return oldExecutionId;
            }

            @Override
            public String getNewExecutionId() {
                return newExecutionId;
            }
        });
    }

    static void fireWorkflowCompletedEvent(KeycloakSession session, Workflow workflow, String resourceId, String executionId) {
        session.getKeycloakSessionFactory().publish(new WorkflowProviderEvent.WorkflowCompletedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return session.getContext().getRealm();
            }

            @Override
            public String getWorkflowId() {
                return workflow.getId();
            }

            @Override
            public String getWorkflowName() {
                return workflow.getName();
            }

            @Override
            public String getResourceId() {
                return resourceId;
            }

            @Override
            public ResourceType getResourceType() {
                return workflow.getSupportedType();
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }
        });
    }
}
