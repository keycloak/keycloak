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

import java.util.List;
import org.keycloak.provider.Provider;

public interface ResourcePolicyProvider extends Provider {

    /**
     * Finds all resources that are eligible for the first action of a policy.
     *
     * @param time The time delay for the first action.
     * @return A list of eligible resource IDs.
     */
    List<String> getEligibleResourcesForInitialAction(long time);

    /** 
     * This method checks a list of candidates and returns only those that are eligible based on time.
     */ 
    List<String> filterEligibleResources(List<String> candidateResourceIds, long time);

    /**
     * Checks if the provider supports resources of the specified type.
     *
     * @param type the resource type.
     * @return {@code true} if the provider supports the specified type; {@code false} otherwise.
     */
    boolean supports(ResourceType type);

    /**
     * Indicates whether the policy supports being assigned to a resource based on the event or not. If {@code true}, the
     * policy's first action will be scheduled for the resource.
     *
     * At the very least, implementations should validate the event's resource type and operation to ensure the policy will
     * only be assigned on expected operations being performed on the expected type.
     *
     * @param event a {@link ResourcePolicyEvent} containing details of the event that was triggered such as operation
     *              (CREATE, LOGIN, etc.), the resource type, and the resource id.
     * @return {@code true} if the policy allows for the setup of the first action based on the received event; {@code false}
     *              otherwise.
     */
    boolean scheduleOnEvent(ResourcePolicyEvent event);

    /**
     * Indicates whether the policy supports being reset (i.e. go back to the first action) based on the event received or not.
     * By default, this method returns false as most policies won't support this kind of flow, but specific policies such
     * as one based on a resource's last updated time, or last used time, can signal that they expect the process to start
     * over once the timestamp they are based on is updated.
     *
     * At the very least, implementations should validate the event's resource type and operation to ensure the policy will
     * only be reset on expected operations being performed on the expected type.
     *
     * @param event a {@link ResourcePolicyEvent} containing details of the event that was triggered such as operation
     *              (CREATE, LOGIN, etc.), the resource type, and the resource id.
     * @return {@code true} if the policy supports resetting the flow based on the received event; {@code false} otherwise.
     */
    boolean resetOnEvent(ResourcePolicyEvent event);
}
