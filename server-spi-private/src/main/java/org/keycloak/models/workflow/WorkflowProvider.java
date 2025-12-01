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

import java.util.Comparator;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.utils.StringUtil;

public interface WorkflowProvider extends Provider {

    /**
     * Returns a {@link ResourceTypeSelector} for the specified resource type.
     *
     * @param type     the resource type.
     * @return the corresponding {@link ResourceTypeSelector}.
     */
    ResourceTypeSelector getResourceTypeSelector(ResourceType type);

    Workflow toModel(WorkflowRepresentation representation);

    Workflow getWorkflow(String id);

    void removeWorkflow(Workflow workflow);

    Stream<Workflow> getWorkflows();

    default Stream<Workflow> getWorkflows(String search, Boolean exact, Integer first, Integer max) {
        return getWorkflows().sorted(Comparator.comparing(Workflow::getName))
                .filter(workflow -> {
                    if (StringUtil.isBlank(search)) {
                        return true;
                    }
                    return Boolean.TRUE.equals(exact) ? workflow.getName().equals(search) : workflow.getName().toLowerCase().contains(search.toLowerCase());
                })
                .skip(first).limit(max);
    }

    WorkflowRepresentation toRepresentation(Workflow workflow);

    void updateWorkflow(Workflow workflow, WorkflowRepresentation rep);

    void activate(Workflow workflow, ResourceType type, String resourceId);

    void deactivate(Workflow workflow, String resourceId);

    void submit(WorkflowEvent event);

    void runScheduledSteps();

    void activateForAllEligibleResources(Workflow workflow);
}
