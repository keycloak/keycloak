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

import java.util.List;
import org.keycloak.provider.Provider;

public interface WorkflowProvider extends Provider {

    /**
     * Finds all resources that are eligible for the first action of a workflow.
     *
     * @return A list of eligible resource IDs.
     */
    List<String> getEligibleResourcesForInitialStep();

    /**
     * Checks if the provider supports resources of the specified type.
     *
     * @param type the resource type.
     * @return {@code true} if the provider supports the specified type; {@code false} otherwise.
     */
    boolean supports(ResourceType type);

    /**
     * Indicates whether the workflow supports being activated for a resource based on the event or not. If {@code true}, the
     * workflow will be activated for the resource. For scheduled workflows, this means the first action will be scheduled. For
     * immediate workflows, this means all actions will be executed right away.
     *
     * At the very least, implementations should validate the event's resource type and operation to ensure the workflow will
     * only be activated on expected operations being performed on the expected type.
     *
     * @param event a {@link WorkflowEvent} containing details of the event that was triggered such as operation
     *              (CREATE, LOGIN, etc.), the resource type, and the resource id.
     * @return {@code true} if the workflow can be activated based on the received event; {@code false} otherwise.
     */
    boolean activateOnEvent(WorkflowEvent event);

    /**
     * Indicates whether the workflow supports being reset (i.e. go back to the first action) based on the event received or not.
     * By default, this method returns false as most workflows won't support this kind of flow, but specific workflows such
     * as one based on a resource's last updated time, or last used time, can signal that they expect the process to start
     * over once the timestamp they are based on is updated.
     *
     * At the very least, implementations should validate the event's resource type and operation to ensure the workflow will
     * only be reset on expected operations being performed on the expected type.
     *
     * @param event a {@link WorkflowEvent} containing details of the event that was triggered such as operation
     *              (CREATE, LOGIN, etc.), the resource type, and the resource id.
     * @return {@code true} if the workflow supports resetting the flow based on the received event; {@code false} otherwise.
     */
    boolean resetOnEvent(WorkflowEvent event);

    /**
     * Indicates whether the workflow supports being deactivated for a resource based on the event or not. If {@code true}, the
     * workflow will be deactivated for the resource, meaning any existing scheduled actions will be removed and no further
     * actions will be executed.
     *
     * At the very least, implementations should validate the event's resource type and operation to ensure the workflow will
     * only be deactivated on expected operations being performed on the expected type.
     *
     * @param event a {@link WorkflowEvent} containing details of the event that was triggered such as operation
     *              (CREATE, LOGIN, etc.), the resource type, and the resource id.
     * @return {@code true} if the workflow can be deactivated based on the received event; {@code false} otherwise.
     */
    boolean deactivateOnEvent(WorkflowEvent event);
}
