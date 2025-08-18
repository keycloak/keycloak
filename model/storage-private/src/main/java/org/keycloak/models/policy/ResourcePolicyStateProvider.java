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

package org.keycloak.models.policy;

import org.keycloak.provider.Provider;
import java.util.List;
import java.util.Set;

/**
 * Interface serves as state check for policy actions.
 */
public interface ResourcePolicyStateProvider extends Provider {

    /**
     * Finds resource IDs scheduled to run the specified action within a policy.
     */
    List<String> findResourceIdsByScheduledAction(String policyId, String scheduledActionId);

    /**
     * Updates the state for a list of resources that have just completed a new action.
     * This will perform an update for existing states or an insert for new states.
     */
    void update(String policyId, String policyProviderId, List<String> resourceIds, String newLastCompletedActionId);

    /**
     * Deletes the orphaned state records.
     */
    void removeByCompletedActions(String policyId, Set<String> deletedActionIds);

    /**
     * Deletes the state records associated with the given {@code resourceId}.
     *
     * @param resourceId the id of the resource.
     */
    void removeByResource(String resourceId);

    /**
     * Removes the record identified by the specified {@code policyId} and {@code resourceId}.
     * @param policyId the id of the policy.
     * @param resourceId the id of the resource.
     */
    void remove(String policyId, String resourceId);

    /**
     * Deletes all state records associated with the current realm bound to the session.
     */
    void removeAll();

    default void scheduleAction(ResourcePolicy policy, ResourceAction action, String resourceId) {
        this.scheduleAction(policy, action, action.getAfter(), resourceId);
    }

    void scheduleAction(ResourcePolicy policy, ResourceAction action, long scheduledTimeOffset, String resourceId);

    ScheduledAction getScheduledAction(String policyId, String resourceId);

    List<ScheduledAction> getScheduledActionsByResource(String resourceId);

    List<ScheduledAction> getDueScheduledActions(ResourcePolicy policy);

    record ScheduledAction (String policyId, String actionId, String resourceId) {};
}
