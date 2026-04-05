/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.workflow;

import java.util.stream.Stream;

import org.keycloak.provider.Provider;

/**
 * Defines a provider interface for managing workflow execution state and scheduled steps.
 * </p>
 * Implementations of this interface are responsible for persisting and retrieving workflow
 * state information, including scheduled steps that need to be executed at specific times.
 * This provider acts as the state management layer for workflow executions within a realm.
 */
public interface WorkflowStateProvider extends Provider {

    /**
     * Deletes all state records associated with the given resource ID.
     * </p>
     * This method removes all workflow state information for the specified resource,
     * regardless of which workflows the resource is associated with.
     *
     * @param resourceId the ID of the resource whose state records should be deleted
     */
    void removeByResource(String resourceId);

    /**
     * Deletes the state record associated with a specific workflow and resource combination.
     * </p>
     * This method removes only the state information for the specified resource within
     * the context of the specified workflow, leaving state records for other workflows intact.
     *
     * @param workflowId the ID of the workflow
     * @param resourceId the ID of the resource
     */
    void removeByWorkflowAndResource(String workflowId, String resourceId);

    /**
     * Removes all state records associated with the specified workflow.
     * </p>
     * This method deletes all state information for the given workflow across all resources.
     *
     * @param workflowId the ID of the workflow whose state records should be deleted
     */
    void removeByWorkflow(String workflowId);

    /**
     * Removes the state record identified by the specified execution ID.
     * </p>
     * This method deletes a specific workflow execution state record.
     *
     * @param executionId the ID of the execution whose state record should be deleted
     */
    void remove(String executionId);

    /**
     * Deletes all workflow state records associated with the current realm.
     * </p>
     * This method removes all workflow state information for the realm bound to the current session.
     */
    void removeAll();

    /**
     * Checks whether there are any scheduled steps for the given workflow.
     *
     * @param workflowId the ID of the workflow to check
     * @return {@code true} if there are scheduled steps for this workflow, {@code false} otherwise
     */
    boolean hasScheduledSteps(String workflowId);

    /**
     * Schedules a workflow step for future execution.
     * </p>
     * This method persists the scheduling information for a step that should be executed
     * at a later time, typically based on the step's configuration (e.g., delay settings).
     *
     * @param workflow the workflow containing the step
     * @param step the workflow step to schedule
     * @param resourceId the ID of the resource associated with this scheduled step
     * @param executionId the ID of the workflow execution
     * @return {@code CREATED} if the execution was created (when the workflow first activates for a resource), {@code UPDATED) if it was just updated.
     */
    ScheduleResult scheduleStep(Workflow workflow, WorkflowStep step, String resourceId, String executionId);

    /**
     * Retrieves the scheduled step for a specific workflow and resource combination.
     *
     * @param workflowId the ID of the workflow
     * @param resourceId the ID of the resource
     * @return the scheduled step, or {@code null} if no step is scheduled for this combination
     */
    ScheduledStep getScheduledStep(String workflowId, String resourceId);

    /**
     * Retrieves all scheduled steps associated with the specified resource.
     *
     * @param resourceId the ID of the resource
     * @return a stream of scheduled steps for the given resource
     */
    Stream<ScheduledStep> getScheduledStepsByResource(String resourceId);

    /**
     * Retrieves all scheduled steps associated with the specified workflow.
     *
     * @param workflowId the ID of the workflow
     * @return a stream of scheduled steps for the given workflow
     */
    Stream<ScheduledStep> getScheduledStepsByWorkflow(String workflowId);

    /**
     * Retrieves all scheduled steps for a specific step within a workflow.
     *
     * @param workflowId the ID of the workflow
     * @param stepId the ID of the step
     * @return a stream of scheduled steps matching the workflow and step IDs
     */
    Stream<ScheduledStep> getScheduledStepsByStep(String workflowId, String stepId);

    /**
     * Retrieves all scheduled steps that are due for execution for the given workflow.
     * </p>
     * This method returns steps whose scheduled execution time has been reached or passed.
     *
     * @param workflow the workflow to check for due steps
     * @return a stream of scheduled steps that are due for execution
     */
    Stream<ScheduledStep> getDueScheduledSteps(Workflow workflow);

    /**
     * Represents a scheduled workflow step with its associated metadata.
     *
     * @param workflowId the ID of the workflow containing this step
     * @param stepId the ID of the workflow step
     * @param resourceId the ID of the resource associated with this scheduled step
     * @param executionId the ID of the workflow execution
     * @param scheduledAt the timestamp (in milliseconds) when this step was scheduled
     */
    record ScheduledStep(String workflowId, String stepId, String resourceId, String executionId, long scheduledAt) {}

    enum ScheduleResult {
        CREATED,
        UPDATED
    }
}
