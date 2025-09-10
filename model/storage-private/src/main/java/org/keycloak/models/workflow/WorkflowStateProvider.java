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
     * Removes the record identified by the specified {@code workflowId} and {@code resourceId}.
     * @param workflowId the id of the workflow.
     * @param resourceId the id of the resource.
     */
    void remove(String workflowId, String resourceId);

    /**
     * Removes any record identified by the specified {@code policyId}.
     * @param workflowId the id of the policy.
     */
    void remove(String workflowId);

    /**
     * Deletes all state records associated with the current realm bound to the session.
     */
    void removeAll();

    default void scheduleAction(Workflow workflow, WorkflowAction action, String resourceId) {
        this.scheduleAction(workflow, action, action.getAfter(), resourceId);
    }

    void scheduleAction(Workflow workflow, WorkflowAction action, long scheduledTimeOffset, String resourceId);

    ScheduledAction getScheduledAction(String workflowId, String resourceId);

    List<ScheduledAction> getScheduledActionsByResource(String resourceId);

    List<ScheduledAction> getScheduledActionsByWorkflow(String workflowId);

    default List<ScheduledAction> getScheduledActionsByWorkflow(Workflow workflow) {
        if (workflow == null) {
            return List.of();
        }

        return getScheduledActionsByWorkflow(workflow.getId());
    }
    List<ScheduledAction> getDueScheduledActions(Workflow workflow);

    record ScheduledAction (String workflowId, String actionId, String resourceId) {};
}
