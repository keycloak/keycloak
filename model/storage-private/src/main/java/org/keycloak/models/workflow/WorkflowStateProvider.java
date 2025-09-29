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

import org.keycloak.provider.Provider;

import java.util.List;

/**
 * Interface serves as state check for workflow actions.
 */
public interface WorkflowStateProvider extends Provider {

    /**
     * Deletes the state records associated with the given {@code resourceId}.
     *
     * @param resourceId the id of the resource.
     */
    void removeByResource(String resourceId);

    /**
     * Removes any record identified by the specified {@code workflowId}.
     * @param workflowId the id of the workflow.
     */
    void removeByWorkflow(String workflowId);

    /**
     * Removes the record identified by the specified {@code executionId}.
     */
    void remove(String executionId);

    /**
     * Deletes all state records associated with the current realm bound to the session.
     */
    void removeAll();

    void scheduleStep(Workflow workflow, WorkflowStep step, String resourceId, String executionId);

    ScheduledStep getScheduledStep(String workflowId, String resourceId);

    List<ScheduledStep> getScheduledStepsByResource(String resourceId);

    List<ScheduledStep> getScheduledStepsByWorkflow(String workflowId);

    List<ScheduledStep> getScheduledStepsByStep(String stepId);

    default List<ScheduledStep> getScheduledStepsByWorkflow(Workflow workflow) {
        if (workflow == null) {
            return List.of();
        }

        return getScheduledStepsByWorkflow(workflow.getId());
    }

    List<ScheduledStep> getDueScheduledSteps(Workflow workflow);

    record ScheduledStep(String workflowId, String stepId, String resourceId, String executionId) {}
}
