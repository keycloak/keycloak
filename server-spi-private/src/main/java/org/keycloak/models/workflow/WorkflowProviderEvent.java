package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;

/**
 * Base interface for workflow-related provider events.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface WorkflowProviderEvent extends ProviderEvent {

    /**
     * Gets the Keycloak session associated with this event.
     *
     * @return the {@link KeycloakSession}
     */
    KeycloakSession getKeycloakSession();

    /**
     * Gets the realm where the workflow event occurred.
     *
     * @return the {@link RealmModel}
     */
    RealmModel getRealm();

    /**
     * Gets the workflow ID.
     *
     * @return the workflow ID
     */
    String getWorkflowId();

    /**
     * Gets the workflow name.
     *
     * @return the workflow name
     */
    String getWorkflowName();

    /**
     * Gets the resource ID associated with this workflow event.
     *
     * @return the resource ID
     */
    String getResourceId();

    /**
     * Gets the resource type associated with this workflow event.
     *
     * @return the {@link ResourceType}
     */
    ResourceType getResourceType();


    /**
     * Gets the execution ID for this workflow activation.
     *
     * @return the execution ID
     */
    String getExecutionId();

    /**
     * Event fired when a workflow is activated for a resource.
     */
    interface WorkflowActivatedEvent extends WorkflowProviderEvent {

        /**
         * Gets the trigger event type that activated the workflow.
         *
         * @return the event provider ID (e.g., "scheduled", "user-created", etc.)
         */
        String getTriggerEventType();
    }

    /**
     * Event fired when a workflow is deactivated for a resource.
     */
    interface WorkflowDeactivatedEvent extends WorkflowProviderEvent {

        /**
         * Gets the reason for deactivation.
         *
         * @return the deactivation reason
         */
        String getReason();
    }

    /**
     * Event fired when a workflow is restarted for a resource.
     */
    interface WorkflowRestartedEvent extends WorkflowProviderEvent {
    }

    /**
     * Event fired when a workflow step is scheduled.
     */
    interface WorkflowStepScheduledEvent extends WorkflowProviderEvent {

        /**
         * Gets the step ID.
         *
         * @return the step ID
         */
        String getStepId();

        /**
         * Gets the step provider ID.
         *
         * @return the step provider ID
         */
        String getStepProviderId();

        /**
         * Gets the scheduled time for the step.
         *
         * @return the scheduled time in milliseconds since epoch
         */
        long getScheduledTime();

        /**
         * Gets the delay duration before the step should be executed.
         *
         * @return the delay as a duration string (e.g., "PT5M" for 5 minutes)
         */
        String getDelay();
    }

    /**
     * Event fired when a workflow step is executed successfully.
     */
    interface WorkflowStepExecutedEvent extends WorkflowProviderEvent {

        /**
         * Gets the step ID.
         *
         * @return the step ID
         */
        String getStepId();

        /**
         * Gets the step provider ID.
         *
         * @return the step provider ID
         */
        String getStepProviderId();
    }

    /**
     * Event fired when a workflow step execution fails.
     */
    interface WorkflowStepFailedEvent extends WorkflowProviderEvent {

        /**
         * Gets the step ID.
         *
         * @return the step ID
         */
        String getStepId();

        /**
         * Gets the step provider ID.
         *
         * @return the step provider ID
         */
        String getStepProviderId();

        /**
         * Gets the error message.
         *
         * @return the error message
         */
        String getErrorMessage();
    }

    /**
     * Event fired when workflow resources are migrated from one step/workflow to another.
     */
    interface WorkflowResourceMigratedEvent extends WorkflowProviderEvent {

        @Override
        default String getExecutionId() {
            return getOldExecutionId();
        }

        /**
         * Gets the source workflow ID.
         *
         * @return the source workflow ID
         */
        String getSourceWorkflowId();

        /**
         * Gets the source workflow name.
         *
         * @return the source workflow name
         */
        String getSourceWorkflowName();

        /**
         * Gets the destination workflow ID.
         *
         * @return the destination workflow ID
         */
        String getDestinationWorkflowId();

        /**
         * Gets the destination workflow name.
         *
         * @return the destination workflow name
         */
        String getDestinationWorkflowName();

        /**
         * Gets the source step ID.
         *
         * @return the source step ID
         */
        String getSourceStepId();

        /**
         * Gets the source step provider ID.
         *
         * @return the source step provider ID
         */
        String getSourceStepProviderId();

        /**
         * Gets the destination step ID.
         *
         * @return the destination step ID
         */
        String getDestinationStepId();

        /**
         * Gets the destination step provider ID.
         *
         * @return the destination step provider ID
         */
        String getDestinationStepProviderId();

        /**
         * Gets the old execution ID (from the source workflow).
         *
         * @return the old execution ID
         */
        String getOldExecutionId();

        /**
         * Gets the new execution ID (for the destination workflow).
         *
         * @return the new execution ID
         */
        String getNewExecutionId();
    }

    /**
     * Event fired when a workflow completes successfully.
     */
    interface WorkflowCompletedEvent extends WorkflowProviderEvent {
    }
}
