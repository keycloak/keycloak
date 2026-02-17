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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import static java.util.Optional.ofNullable;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ERROR;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_SUPPORTS;

public class Workflow {

    private final String id;
    private final RealmModel realm;
    private final KeycloakSession session;
    private MultivaluedHashMap<String, String> config;
    private String notBefore;

    public Workflow(KeycloakSession session, ComponentModel c) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.id = c.getId();
        this.config = c.getConfig();
    }

    public Workflow(KeycloakSession session, String id, Map<String, List<String>> config) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.id = id;
        MultivaluedHashMap<String, String> c = new MultivaluedHashMap<>();
        config.forEach(c::addAll);
        this.config = c;
    }

    /**
     * Create a new workflow instance based on the provided workflow but bound to a new session.
     *
     * @param workflow the workflow to copy
     */
    public Workflow(KeycloakSession session, Workflow workflow) {
        this(session, workflow.getId(), workflow.getConfig());
        this.notBefore = workflow.getNotBefore();
    }


    public String getId() {
        return id;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public String getName() {
        return config != null ? config.getFirst(CONFIG_NAME) : null;
    }

    public boolean isEnabled() {
        return config != null && Boolean.parseBoolean(config.getFirstOrDefault(CONFIG_ENABLED, "true"));
    }

    public String getNotBefore() {
        return notBefore;
    }

    public ResourceType getSupportedType() {
        return Optional.ofNullable(config).map(c -> c.getFirst(CONFIG_SUPPORTS))
                .map(ResourceType::valueOf)
                .orElse(null);
    }

    public String getCondition() {
        return config != null ? config.getFirst(CONFIG_CONDITIONS) : null;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    public void setEnabled(boolean enabled) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        config.putSingle(CONFIG_ENABLED, String.valueOf(enabled));
    }

    public void setError(String message) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        config.putSingle(CONFIG_ERROR, message);
    }

    public void setSupportedType(ResourceType resourceType) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        config.putSingle(CONFIG_SUPPORTS, resourceType.name());
    }

    public void updateConfig(MultivaluedHashMap<String, String> config, List<WorkflowStepRepresentation> steps) {
        ComponentModel component = getWorkflowComponent(this.id, WorkflowProvider.class.getName());
        component.setConfig(config);
        realm.updateComponent(component);

        // check if there are steps to be updated as well
        if (steps != null) {
            steps.forEach(step -> {
                ComponentModel stepComponent = getWorkflowComponent(step.getId(), WorkflowStepProvider.class.getName());
                stepComponent.setConfig(step.getConfig());
                realm.updateComponent(stepComponent);
            });
        }
    }

    public Stream<WorkflowStep> getSteps() {
        return realm.getComponentsStream(getId(), WorkflowStepProvider.class.getName())
                .map((c) -> new WorkflowStep(session, c)).sorted();
    }

    /**
     * Get steps starting from the specified stepId (inclusive)
     *
     * @param stepId the step id to start from
     * @return the stream of workflow steps
     */
    public Stream<WorkflowStep> getSteps(String stepId) {
        boolean[] startAdding = {stepId == null};
        return getSteps().filter(step -> {
            if (startAdding[0]) {
                return true;
            }
            if (step.getId().equals(stepId)) {
                startAdding[0] = true;
                return true;
            }
            return false;
        });
    }

    public WorkflowStep getStepById(String id) {
        return getSteps().filter(s -> s.getId().equals(id)).findAny().orElse(null);
    }

    public void addSteps(List<WorkflowStepRepresentation> steps) {
        Set<ResourceType> allowedTypes = EnumSet.allOf(ResourceType.class);

        steps = ofNullable(steps).orElse(List.of());
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = toModel(steps.get(i));

            // assign priority based on index.
            step.setPriority(i + 1);

            // persist the new step component.
            addStep(step);

            // update allowed types
            WorkflowStepProviderFactory<WorkflowStepProvider> stepProvider = Workflows.getStepProviderFactory(session, step);
            allowedTypes.retainAll(stepProvider.getSupportedResourceTypes());
        }

        if (allowedTypes.isEmpty()) {
            throw new ModelValidationException("Steps provided are not compatible with each other.");
        }
        else if (allowedTypes.size() > 1) {
            String formattedTypes = allowedTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
            throw new ModelValidationException("Steps provided should support a single type, actual: " + formattedTypes);
        }

        ResourceType supported = allowedTypes.stream().findFirst().orElseThrow();
        setSupportedType(supported);
        updateConfig(config, null);
    }

    private void addStep(WorkflowStep step) {
        ComponentModel workflowModel = realm.getComponent(getId());

        if (workflowModel == null) {
            throw new ModelValidationException("Workflow with id '%s' not found.".formatted(getId()));
        }

        ComponentModel stepModel = new ComponentModel();
        stepModel.setId(step.getId());//need to keep stable UUIDs not to break a link in state table
        stepModel.setParentId(workflowModel.getId());
        stepModel.setProviderId(step.getProviderId());
        stepModel.setProviderType(WorkflowStepProvider.class.getName());
        stepModel.setConfig(step.getConfig());
        realm.addComponentModel(stepModel);
    }

    private WorkflowStep toModel(WorkflowStepRepresentation rep) {
        return new WorkflowStep(rep.getUses(), rep.getConfig());
    }

    private ComponentModel getWorkflowComponent(String id, String providerType) {
        ComponentModel component = realm.getComponent(id);

        if (component == null || !Objects.equals(providerType, component.getProviderType())) {
            throw new BadRequestException("Not a valid workflow resource: " + id);
        }
        return component;
    }
}
