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

import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.keycloak.models.workflow.Workflow.RECURRING_KEY;
import static org.keycloak.models.workflow.Workflow.SCHEDULED_KEY;
import static org.keycloak.models.workflow.WorkflowStep.AFTER_KEY;

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

            validateStep(workflow, step);

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
        ComponentModel parentModel = realm.getComponent(parentId);

        if (parentModel == null) {
            throw new ModelValidationException("Parent component not found: " + parentId);
        }

        ComponentModel stepModel = new ComponentModel();

        stepModel.setId(step.getId());//need to keep stable UUIDs not to break a link in state table
        stepModel.setParentId(parentModel.getId());
        stepModel.setProviderId(step.getProviderId());
        stepModel.setProviderType(WorkflowStepProvider.class.getName());
        stepModel.setConfig(step.getConfig());

        WorkflowStep persisted = new WorkflowStep(realm.addComponentModel(stepModel));

        persisted.setSteps(step.getSteps());

        return persisted;
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

    public WorkflowStep getStepById(String id) {
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

        validateWorkflow(rep, config);

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

    private void validateWorkflow(WorkflowRepresentation rep, MultivaluedHashMap<String, String> config) {
        // Validations:
        //  workflow cannot be both immediate and recurring
        //  immediate workflow cannot have time conditions
        //  all steps of scheduled workflow must have time condition

        boolean isImmediate = config.containsKey(SCHEDULED_KEY) && !Boolean.parseBoolean(config.getFirst(SCHEDULED_KEY));
        boolean isRecurring = config.containsKey(RECURRING_KEY) && Boolean.parseBoolean(config.getFirst(RECURRING_KEY));
        boolean hasTimeCondition = rep.getSteps().stream().allMatch(step -> step.getConfig() != null
                && step.getConfig().containsKey(AFTER_KEY));
        if (isImmediate && isRecurring) {
            throw new WorkflowInvalidStateException("Workflow cannot be both immediate and recurring.");
        }
        if (isImmediate && hasTimeCondition) {
            throw new WorkflowInvalidStateException("Immediate workflow cannot have steps with time conditions.");
        }
        if (!isImmediate && !hasTimeCondition) {
            throw new WorkflowInvalidStateException("Scheduled workflow cannot have steps without time conditions.");
        }
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

    private void validateStep(Workflow workflow, WorkflowStep step) throws ModelValidationException {
        boolean isAggregatedStep = !step.getSteps().isEmpty();
        boolean isScheduledWorkflow = workflow.isScheduled();

        if (isAggregatedStep) {
            if (!step.getProviderId().equals(AggregatedStepProviderFactory.ID)) {
                // for now, only AggregatedStepProvider supports having sub-steps, but we might want to support
                // in the future more steps from having sub-steps by querying the capability from the provider or via
                // a marker interface
                throw new ModelValidationException("Step provider " + step.getProviderId() + " does not support aggregated steps");
            }

            List<WorkflowStep> subSteps = step.getSteps();
            // for each sub-step (in case it's not aggregated step on its own) check all it's sub-steps do not have
            // time conditions, all its sub-steps are meant to be run at once
            if (subSteps.stream().anyMatch(subStep ->
                    subStep.getConfig().getFirst(AFTER_KEY) != null &&
                            !subStep.getProviderId().equals(AggregatedStepProviderFactory.ID))) {
                throw new ModelValidationException("Sub-steps of aggregated step cannot have time conditions.");
            }
        } else {
            if (isScheduledWorkflow) {
                if (step.getConfig().getFirst(AFTER_KEY) == null || step.getAfter() < 0) {
                    throw new ModelValidationException("All steps of scheduled workflow must have a valid 'after' time condition.");
                }
            } else { // immediate workflow
                if (step.getConfig().getFirst(AFTER_KEY) != null) {
                    throw new ModelValidationException("Immediate workflow step cannot have a time condition.");
                }
            }
        }
    }

    public WorkflowStep addStepToWorkflow(String workflowId, WorkflowStep step, Integer position) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(step, "step cannot be null");

        List<WorkflowStep> existingSteps = getSteps(workflowId);

        int targetPosition = position != null ? position : existingSteps.size();
        if (targetPosition < 0 || targetPosition > existingSteps.size()) {
            throw new BadRequestException("Invalid position: " + targetPosition + ". Must be between 0 and " + existingSteps.size());
        }

        // First, shift existing steps at and after the target position to make room
        shiftStepsForInsertion(targetPosition, existingSteps);

        step.setPriority(targetPosition + 1);
        WorkflowStep addedStep = addStep(workflowId, step);

        updateScheduledStepsAfterStepChange(workflowId);

        log.debugf("Added step %s to workflow %s at position %d", addedStep.getId(), workflowId, targetPosition);
        return addedStep;
    }

    public void removeStepFromWorkflow(String workflowId, String stepId) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(stepId, "stepId cannot be null");

        RealmModel realm = getRealm();
        ComponentModel stepComponent = realm.getComponent(stepId);

        if (stepComponent == null || !stepComponent.getParentId().equals(workflowId)) {
            throw new BadRequestException("Step not found or not part of workflow: " + stepId);
        }

        realm.removeComponent(stepComponent);

        // Reorder remaining steps and update state
        reorderAllSteps(workflowId);
        updateScheduledStepsAfterStepChange(workflowId);

        log.debugf("Removed step %s from workflow %s", stepId, workflowId);
    }

    private void shiftStepsForInsertion(int insertPosition, List<WorkflowStep> existingSteps) {
        RealmModel realm = getRealm();

        // Shift all steps at and after the insertion position by +1 priority
        for (int i = insertPosition; i < existingSteps.size(); i++) {
            WorkflowStep step = existingSteps.get(i);
            step.setPriority(step.getPriority() + 1);
            updateStepComponent(realm, step);
        }
    }

    private void reorderAllSteps(String workflowId) {
        List<WorkflowStep> steps = getSteps(workflowId);
        RealmModel realm = getRealm();

        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            step.setPriority(i + 1);
            updateStepComponent(realm, step);
        }
    }

    private void updateStepComponent(RealmModel realm, WorkflowStep step) {
        ComponentModel component = realm.getComponent(step.getId());
        component.setConfig(step.getConfig());
        realm.updateComponent(component);
    }

    private void updateScheduledStepsAfterStepChange(String workflowId) {
        List<WorkflowStep> steps = getSteps(workflowId);

        if (steps.isEmpty()) {
            workflowStateProvider.remove(workflowId);
            return;
        }

        for (ScheduledStep scheduled : workflowStateProvider.getScheduledStepsByWorkflow(workflowId)) {
            boolean stepStillExists = steps.stream()
                    .anyMatch(step -> step.getId().equals(scheduled.stepId()));

            if (!stepStillExists) {
                Workflow workflow = getWorkflow(workflowId);
                workflowStateProvider.scheduleStep(workflow, steps.get(0), scheduled.resourceId());
            }
        }
    }

    public WorkflowStepRepresentation toStepRepresentation(WorkflowStep step) {
        List<WorkflowStepRepresentation> steps = step.getSteps().stream()
                .map(this::toStepRepresentation)
                .toList();
        return new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig(), steps);
    }

    public WorkflowStep toStepModel(WorkflowStepRepresentation rep) {
        List<WorkflowStep> subSteps = new ArrayList<>();

        for (WorkflowStepRepresentation subStep : ofNullable(rep.getSteps()).orElse(List.of())) {
            subSteps.add(toStepModel(subStep));
        }

        return new WorkflowStep(rep.getProviderId(), rep.getConfig(), subSteps);
    }
}
