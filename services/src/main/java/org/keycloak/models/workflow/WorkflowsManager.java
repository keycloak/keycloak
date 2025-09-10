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

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;

public class WorkflowsManager {

    private static final Logger log = Logger.getLogger(WorkflowsManager.class);

    private final KeycloakSession session;
    private final WorkflowStateProvider workflowStateProvider;

    public static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Feature.WORKFLOWS);
    }

    public WorkflowsManager(KeycloakSession session) {
        this.session = session;
        this.workflowStateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
    }

    public Workflow addWorkflow(String providerId, Map<String, List<String>> config) {
        return addWorkflow(new Workflow(providerId, config));
    }

    public Workflow addWorkflow(Workflow workflow) {
        RealmModel realm = getRealm();
        ComponentModel model = new ComponentModel();

        model.setParentId(realm.getId());
        model.setProviderId(workflow.getProviderId());
        model.setProviderType(WorkflowProvider.class.getName());

        MultivaluedHashMap<String, String> config = workflow.getConfig();

        if (config != null) {
            model.setConfig(config);
        }

        return new Workflow(realm.addComponentModel(model));
    }

    // This method takes an ordered list of steps. First step in the list has the highest priority, last step has the lowest priority
    public void createSteps(Workflow workflow, List<WorkflowStep> steps) {
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);

            // assign priority based on index.
            step.setPriority(i + 1);

            List<WorkflowStep> subSteps = Optional.ofNullable(step.getSteps()).orElse(List.of());

            // persist the new step component.
            step = addStep(workflow.getId(), step);

            for (int j = 0; j < subSteps.size(); j++) {
                WorkflowStep subStep = subSteps.get(j);
                // assign priority based on index.
                subStep.setPriority(j + 1);
                addStep(step.getId(), subStep);
            }
        }
    }

    private WorkflowStep addStep(String parentId, WorkflowStep step) {
        RealmModel realm = getRealm();
        ComponentModel workflowModel = realm.getComponent(parentId);
        ComponentModel stepModel = new ComponentModel();

        stepModel.setId(step.getId());//need to keep stable UUIDs not to break a link in state table
        stepModel.setParentId(workflowModel.getId());
        stepModel.setProviderId(step.getProviderId());
        stepModel.setProviderType(WorkflowStepProvider.class.getName());
        stepModel.setConfig(step.getConfig());

        return new WorkflowStep(realm.addComponentModel(stepModel));
    }

    public List<Workflow> getWorkflows() {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName())
                .map(Workflow::new).toList();
    }

    public List<WorkflowStep> getSteps(String workflowId) {
        return getStepsStream(workflowId).toList();
    }

    public Stream<WorkflowStep> getStepsStream(String parentId) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(parentId, WorkflowStepProvider.class.getName())
                .map(this::toStep).sorted();
    }

    private WorkflowStep toStep(ComponentModel model) {
        WorkflowStep step = new WorkflowStep(model);

        step.setSteps(getSteps(step.getId()));

        return step;
    }

    public WorkflowStep getStepById(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        ComponentModel component = realm.getComponent(id);

        if (component == null) {
            return null;
        }

        return toStep(component);
    }

    private WorkflowStep getFirstStep(Workflow workflow) {
        WorkflowStep step = getSteps(workflow.getId()).get(0);
        Long notBefore = workflow.getNotBefore();

        if (notBefore != null) {
            step.setAfter(notBefore);
        }

        return step;
    }

    private WorkflowProvider getWorkflowProvider(Workflow workflow) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowProvider.class, workflow.getProviderId());
        return (WorkflowProvider) factory.create(session, getRealm().getComponent(workflow.getId()));
    }

    public WorkflowStepProvider getStepProvider(WorkflowStep step) {
        ComponentFactory<?, ?> stepFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowStepProvider.class, step.getProviderId());
        return (WorkflowStepProvider) stepFactory.create(session, getRealm().getComponent(step.getId()));
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public void removeWorkflows() {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName()).forEach(workflow -> {
            realm.getComponentsStream(workflow.getId(), WorkflowStepProvider.class.getName()).forEach(realm::removeComponent);
            realm.removeComponent(workflow);
        });
    }

    public void scheduleAllEligibleResources(Workflow workflow) {
        if (workflow.isEnabled()) {
            WorkflowProvider provider = getWorkflowProvider(workflow);
            provider.getEligibleResourcesForInitialStep()
                    .forEach(resourceId -> processEvent(List.of(workflow), new AdhocWorkflowEvent(ResourceType.USERS, resourceId)));
        }
    }

    public void processEvent(WorkflowEvent event) {
        processEvent(getWorkflows(), event);
    }

    public void processEvent(List<Workflow> workflows, WorkflowEvent event) {
        List<String> currentlyAssignedWorkflows = workflowStateProvider.getScheduledStepsByResource(event.getResourceId())
                .stream().map(ScheduledStep::workflowId).toList();

        // iterate through the workflows, and for those not yet assigned to the user check if they can be assigned
        workflows.stream()
                .filter(workflow -> workflow.isEnabled() && !getSteps(workflow.getId()).isEmpty())
                .forEach(workflow -> {
                    WorkflowProvider provider = getWorkflowProvider(workflow);
                    try {
                        if (!currentlyAssignedWorkflows.contains(workflow.getId())) {
                            // if workflow is not active for the resource, check if the provider allows activating based on the event
                            if (provider.activateOnEvent(event)) {
                                if (workflow.isScheduled()) {
                                    // workflow is scheduled, so we schedule the first step
                                    log.debugf("Scheduling first step of workflow %s for resource %s based on event %s",
                                            workflow.getId(), event.getResourceId(), event.getOperation());
                                    workflowStateProvider.scheduleStep(workflow, getFirstStep(workflow), event.getResourceId());
                                } else {
                                    // workflow is not scheduled, so we run all steps immediately
                                    log.debugf("Running all steps of workflow %s for resource %s based on event %s",
                                            workflow.getId(), event.getResourceId(), event.getOperation());
                                    KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s ->
                                            getSteps(workflow.getId()).forEach(step -> getStepProvider(step).run(List.of(event.getResourceId())))
                                    );
                                }
                            }
                        } else {
                            if (provider.resetOnEvent(event)) {
                                workflowStateProvider.scheduleStep(workflow, getFirstStep(workflow), event.getResourceId());
                            } else if (provider.deactivateOnEvent(event)) {
                                workflowStateProvider.remove(workflow.getId(), event.getResourceId());
                            }
                        }
                    } catch (WorkflowInvalidStateException e) {
                        workflow.getConfig().putSingle("enabled", "false");
                        workflow.getConfig().putSingle("validation_error", e.getMessage());
                        updateWorkflow(workflow, workflow.getConfig());
                        log.debugf("Workflow %s was disabled due to: %s", workflow.getId(), e.getMessage());
                    }
                });
    }

    public void runScheduledSteps() {
            this.getWorkflows().stream().filter(Workflow::isEnabled).forEach(workflow -> {

            for (ScheduledStep scheduled : workflowStateProvider.getDueScheduledSteps(workflow)) {
                List<WorkflowStep> steps = getSteps(workflow.getId());

                for (int i = 0; i < steps.size(); i++) {
                    WorkflowStep currentStep = steps.get(i);

                    if (currentStep.getId().equals(scheduled.stepId())) {
                        getStepProvider(currentStep).run(List.of(scheduled.resourceId()));

                        if (steps.size() > i + 1) {
                            // schedule the next step using the time offset difference between the steps.
                            WorkflowStep nextStep = steps.get(i + 1);
                            workflowStateProvider.scheduleStep(workflow, nextStep, scheduled.resourceId());
                        } else {
                            // this was the last step, check if the workflow is recurring - i.e. if we need to schedule the first step again
                            if (workflow.isRecurring()) {
                                WorkflowStep firstStep = getFirstStep(workflow);
                                workflowStateProvider.scheduleStep(workflow, firstStep, scheduled.resourceId());
                            } else {
                                // not recurring, remove the state record
                                workflowStateProvider.remove(workflow.getId(), scheduled.resourceId());
                            }
                        }
                    }
                }
            }
        });
    }

    public void removeWorkflow(String id) {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName())
                .filter(workflow -> workflow.getId().equals(id))
                .forEach(workflow -> {
                    realm.getComponentsStream(workflow.getId(), WorkflowStepProvider.class.getName()).forEach(realm::removeComponent);
                    realm.removeComponent(workflow);
                });
        workflowStateProvider.remove(id);
    }

    public Workflow getWorkflow(String id) {
        return new Workflow(getWorkflowComponent(id));
    }

    public void updateWorkflow(Workflow workflow, MultivaluedHashMap<String, String> config) {
        ComponentModel component = getWorkflowComponent(workflow.getId());
        component.setConfig(config);
        getRealm().updateComponent(component);
    }

    private ComponentModel getWorkflowComponent(String id) {
        ComponentModel component = getRealm().getComponent(id);

        if (component == null || !WorkflowProvider.class.getName().equals(component.getProviderType())) {
            throw new BadRequestException("Not a valid resource workflow: " + id);
        }

        return component;
    }

    public WorkflowRepresentation toRepresentation(Workflow workflow) {
        WorkflowRepresentation rep = new WorkflowRepresentation(workflow.getId(), workflow.getProviderId(), workflow.getConfig());

        for (WorkflowStep step : getSteps(workflow.getId())) {
            rep.addStep(toRepresentation(step));
        }

        return rep;
    }

    private WorkflowStepRepresentation toRepresentation(WorkflowStep step) {
        List<WorkflowStepRepresentation> steps = step.getSteps().stream().map(this::toRepresentation).toList();
        return new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig(), steps);
    }

    public Workflow toModel(WorkflowRepresentation rep) {
        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());
        List<WorkflowConditionRepresentation> conditions = ofNullable(rep.getConditions()).orElse(List.of());

        for (WorkflowConditionRepresentation condition : conditions) {
            String conditionProviderId = condition.getProviderId();
            config.computeIfAbsent("conditions", key -> new ArrayList<>()).add(conditionProviderId);

            for (Entry<String, List<String>> configEntry : condition.getConfig().entrySet()) {
                config.put(conditionProviderId + "." + configEntry.getKey(), configEntry.getValue());
            }
        }

        Workflow workflow = addWorkflow(rep.getProviderId(), config);
        List<WorkflowStep> steps = new ArrayList<>();

        for (WorkflowStepRepresentation stepRep : rep.getSteps()) {
            steps.add(toModel(stepRep));
        }

        createSteps(workflow, steps);

        return workflow;
    }

    private WorkflowStep toModel(WorkflowStepRepresentation rep) {
        List<WorkflowStep> subSteps = new ArrayList<>();

        for (WorkflowStepRepresentation subStep : ofNullable(rep.getSteps()).orElse(List.of())) {
            subSteps.add(toModel(subStep));
        }

        return new WorkflowStep(rep.getProviderId(), rep.getConfig(), subSteps);
    }

    public void bind(Workflow workflow, ResourceType type, String resourceId) {
        processEvent(List.of(workflow), new AdhocWorkflowEvent(type, resourceId));
    }

    public Object resolveResource(ResourceType type, String resourceId) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(type, "resourceId");
        return type.resolveResource(session, resourceId);
    }
}
