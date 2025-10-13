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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RECURRING;

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

    private Workflow addWorkflow(Workflow workflow) {
        RealmModel realm = getRealm();
        ComponentModel model = new ComponentModel();

        model.setParentId(realm.getId());
        model.setProviderId(ofNullable(workflow.getProviderId()).orElse(WorkflowConstants.DEFAULT_WORKFLOW));
        model.setProviderType(WorkflowProvider.class.getName());

        MultivaluedHashMap<String, String> config = workflow.getConfig();

        if (config != null) {
            model.setConfig(config);
        }

        return new Workflow(realm.addComponentModel(model));
    }

    // This method takes an ordered list of steps. First step in the list has the highest priority, last step has the lowest priority
    private void addSteps(Workflow workflow, List<WorkflowStep> steps) {
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);

            // assign priority based on index.
            step.setPriority(i + 1);

            // persist the new step component.
            addStep(workflow, step);
        }
    }

    private WorkflowStep addStep(Workflow workflow, WorkflowStep step) {
        RealmModel realm = getRealm();
        ComponentModel workflowModel = realm.getComponent(workflow.getId());

        if (workflowModel == null) {
            throw new ModelValidationException("Workflow with id '%s' not found.".formatted(workflow.getId()));
        }

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
        return new WorkflowStep(model);
    }

    public WorkflowStep getStepById(String id) {
        RealmModel realm = session.getContext().getRealm();
        ComponentModel component = realm.getComponent(id);

        if (component == null) {
            return null;
        }

        return toStep(component);
    }

    private WorkflowProvider getWorkflowProvider(Workflow workflow) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowProvider.class, workflow.getProviderId());
        return (WorkflowProvider) factory.create(session, getRealm().getComponent(workflow.getId()));
    }

    public WorkflowStepProvider getStepProvider(WorkflowStep step) {
        return (WorkflowStepProvider) getStepProviderFactory(step).create(session, getRealm().getComponent(step.getId()));
    }

    private ComponentFactory<?, ?> getStepProviderFactory(WorkflowStep step) {
        ComponentFactory<?, ?> stepFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowStepProvider.class, step.getProviderId());

        if (stepFactory == null) {
            throw new WorkflowInvalidStateException("Step not found: " + step.getProviderId());
        }

        return stepFactory;
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
        Map<String, ScheduledStep> scheduledSteps = workflowStateProvider.getScheduledStepsByResource(event.getResourceId())
                .stream().collect(HashMap::new, (m, v) -> m.put(v.workflowId(), v), HashMap::putAll);

        // iterate through the workflows, and for those not yet assigned to the user check if they can be assigned
        workflows.stream()
                .filter(workflow -> workflow.isEnabled() && !getSteps(workflow.getId()).isEmpty())
                .forEach(workflow -> {
                    WorkflowProvider provider = getWorkflowProvider(workflow);
                    try {
                        // if workflow is not active for the resource, check if the provider allows activating based on the event
                        if (!scheduledSteps.containsKey(workflow.getId())) {
                            if (provider.activateOnEvent(event)) {
                                WorkflowExecutionContext context = buildAndInitContext(workflow, event.getResourceId());
                                // If the workflow has a notBefore set, schedule the first step with it
                                if (context.hasNextStep() && workflow.getNotBefore() != null && workflow.getNotBefore() > 0) {
                                    WorkflowStep step = context.getNextStep();
                                    log.debugf("Scheduling first step '%s' of workflow '%s' for resource %s based on on event %s with notBefore %d",
                                            step.getProviderId(), workflow.getName(), event.getResourceId(), event.getOperation(), workflow.getNotBefore());
                                    Long originalAfter = step.getAfter();
                                    try {
                                        step.setAfter(workflow.getNotBefore());
                                        workflowStateProvider.scheduleStep(workflow, step, event.getResourceId(), context.getExecutionId());
                                    } finally {
                                        // restore the original after value
                                        step.setAfter(originalAfter);
                                    }
                                }
                                else {
                                    // process the workflow steps, scheduling or running them as needed
                                    processWorkflow(workflow, context, event.getResourceId());
                                }
                            }
                        } else {
                            // workflow is active for the resource, check if the provider wants to reset or deactivate it based on the event
                            WorkflowExecutionContext context = buildFromScheduledStep(scheduledSteps.get(workflow.getId()));
                            if (provider.resetOnEvent(event)) {
                                context.restart();
                                processWorkflow(workflow, context, event.getResourceId());
                            } else if (provider.deactivateOnEvent(event)) {
                                context.cancel();
                                workflowStateProvider.remove(context.getExecutionId());
                            }
                        }
                    } catch (WorkflowInvalidStateException e) {
                        workflow.setEnabled(false);
                        workflow.setError(e.getMessage());
                        updateWorkflowConfig(workflow, workflow.getConfig());
                        log.warnf("Workflow %s was disabled due to: %s", workflow.getId(), e.getMessage());
                    }
                });
    }

    public void runScheduledSteps() {
        this.getWorkflows().stream().filter(Workflow::isEnabled).forEach(workflow -> {

            for (ScheduledStep scheduled : workflowStateProvider.getDueScheduledSteps(workflow)) {
                WorkflowExecutionContext context = buildFromScheduledStep(scheduled);
                if (!context.hasNextStep()) {
                    log.warnf("Could not find step %s in workflow %s for resource %s. Removing the workflow state.",
                            scheduled.stepId(), scheduled.workflowId(), scheduled.resourceId());
                    workflowStateProvider.remove(scheduled.executionId());
                    continue;
                }
                // run the scheduled step that is due
                this.runWorkflowStep(context, context.getNextStep(), scheduled.resourceId());

                // now process the subsequent steps, scheduling or running them as needed
                processWorkflow(workflow, context, scheduled.resourceId());
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
        workflowStateProvider.removeByWorkflow(id);
    }

    public Workflow getWorkflow(String id) {
        return new Workflow(getWorkflowComponent(id));
    }

    public void updateWorkflow(Workflow workflow, WorkflowRepresentation representation) {

        WorkflowRepresentation currentRep = toRepresentation(workflow);

        // we compare the representation, removing first the entries we allow updating. If anything else changes, we throw a validation exception
        String currentName = currentRep.getName(); currentRep.getConfig().remove(CONFIG_NAME);
        String newName = representation.getName(); representation.getConfig().remove(CONFIG_NAME);
        Boolean currentEnabled = currentRep.getEnabled(); currentRep.getConfig().remove(CONFIG_ENABLED);
        Boolean newEnabled = representation.getEnabled(); representation.getConfig().remove(CONFIG_ENABLED);

        if (!currentRep.equals(representation)) {
            throw new ModelValidationException("Workflow update can only change 'name' and 'enabled' config entries.");
        }

        if (!Objects.equals(currentName, newName) || !Objects.equals(currentEnabled, newEnabled)) {
            // only update component if something changed
            representation.setName(newName);
            representation.setEnabled(newEnabled);
            this.updateWorkflowConfig(workflow, representation.getConfig());
        }
    }

    private void updateWorkflowConfig(Workflow workflow, MultivaluedHashMap<String, String> config) {
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
        List<WorkflowConditionRepresentation> conditions = toConditionRepresentation(workflow);
        List<WorkflowStepRepresentation> steps = toRepresentation(getSteps(workflow.getId()));

        return new WorkflowRepresentation(workflow.getId(), workflow.getProviderId(), workflow.getConfig(), conditions, steps);
    }

    private List<WorkflowConditionRepresentation> toConditionRepresentation(Workflow workflow) {
        MultivaluedHashMap<String, String> workflowConfig = ofNullable(workflow.getConfig()).orElse(new MultivaluedHashMap<>());
        List<String> ids = workflowConfig.getOrDefault(CONFIG_CONDITIONS, List.of());

        if (ids.isEmpty()) {
            return null;
        }

        List<WorkflowConditionRepresentation> conditions = new ArrayList<>();

        for (String id : ids) {
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

            for (Entry<String, List<String>> configEntry : workflowConfig.entrySet()) {
                String key = configEntry.getKey();
                if (key.startsWith(id + ".")) {
                    config.put(key.substring(id.length() + 1), configEntry.getValue());
                }
            }

            conditions.add(new WorkflowConditionRepresentation(id, config));
        }

        return conditions;
    }

    private List<WorkflowStepRepresentation> toRepresentation(List<WorkflowStep> existingSteps) {
        if (existingSteps == null || existingSteps.isEmpty()) {
            return null;
        }

        List<WorkflowStepRepresentation> steps = new ArrayList<>();

        for (WorkflowStep step : existingSteps) {
            steps.add(toRepresentation(step));
        }

        return steps;
    }

    public WorkflowStepRepresentation toRepresentation(WorkflowStep step) {
        return new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig());
    }

    public Workflow toModel(WorkflowRepresentation rep) {
        validateWorkflow(rep);

        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());
        List<WorkflowConditionRepresentation> conditions = ofNullable(rep.getConditions()).orElse(List.of());

        for (WorkflowConditionRepresentation condition : conditions) {
            String conditionProviderId = condition.getUses();
            getConditionProviderFactory(conditionProviderId);
            config.computeIfAbsent(CONFIG_CONDITIONS, key -> new ArrayList<>()).add(conditionProviderId);

            for (Entry<String, List<String>> configEntry : condition.getConfig().entrySet()) {
                config.put(conditionProviderId + "." + configEntry.getKey(), configEntry.getValue());
            }
        }

        Workflow workflow = addWorkflow(rep.getUses(), config);

        List<WorkflowStep> steps = rep.getSteps().stream().map(this::toModel).toList();

        addSteps(workflow, steps);

        return workflow;
    }

    private void validateWorkflow(WorkflowRepresentation rep) {
        validateEvents(rep.getOnValues());
        validateEvents(rep.getOnEventsReset());
        // a recurring workflow must have at least one scheduled step to prevent an infinite loop of immediate executions
        if (rep.getConfig() != null && Boolean.parseBoolean(rep.getConfig().getFirstOrDefault(CONFIG_RECURRING, "false"))) {
            boolean hasScheduledStep = ofNullable(rep.getSteps()).orElse(List.of()).stream()
                    .anyMatch(step -> Integer.parseInt(ofNullable(step.getAfter()).orElse("0")) > 0);
            if (!hasScheduledStep) {
                throw new WorkflowInvalidStateException("A recurring workflow must have at least one step with a time delay.");
            }
        }
    }

    private static void validateEvents(List<String> events) {
        for (String event : ofNullable(events).orElse(List.of())) {
            try {
                ResourceOperationType.valueOf(event.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new WorkflowInvalidStateException("Invalid event type: " + event);
            }
        }
    }

    public void bind(Workflow workflow, ResourceType type, String resourceId) {
        processEvent(List.of(workflow), new AdhocWorkflowEvent(type, resourceId));
    }

    public Object resolveResource(ResourceType type, String resourceId) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(type, "resourceId");
        return type.resolveResource(session, resourceId);
    }

    private void validateStep(WorkflowStep step) throws ModelValidationException {
        if (step.getAfter() < 0) {
            throw new ModelValidationException("Step 'after' time condition cannot be negative.");
        }
        // verify the step does have valid provider
        getStepProviderFactory(step);
    }

    public WorkflowStep addStepToWorkflow(Workflow workflow, WorkflowStep step, Integer position) {
        Objects.requireNonNull(workflow, "workflow cannot be null");
        Objects.requireNonNull(step, "step cannot be null");

        List<WorkflowStep> existingSteps = getSteps(workflow.getId());

        int targetPosition = position != null ? position : existingSteps.size();
        if (targetPosition < 0 || targetPosition > existingSteps.size()) {
            throw new BadRequestException("Invalid position: " + targetPosition + ". Must be between 0 and " + existingSteps.size());
        }

        // First, shift existing steps at and after the target position to make room
        shiftStepsForInsertion(targetPosition, existingSteps);

        step.setPriority(targetPosition + 1);
        WorkflowStep addedStep = addStep(workflow, step);

        log.debugf("Added step %s to workflow %s at position %d", addedStep.getId(), workflow.getId(), targetPosition);
        return addedStep;
    }

    public void removeStepFromWorkflow(Workflow workflow, String stepId) {
        Objects.requireNonNull(workflow, "workflow cannot be null");
        Objects.requireNonNull(stepId, "stepId cannot be null");

        RealmModel realm = getRealm();
        ComponentModel stepComponent = realm.getComponent(stepId);

        if (stepComponent == null || !stepComponent.getParentId().equals(workflow.getId())) {
            throw new BadRequestException("Step not found or not part of workflow: " + stepId);
        }

        realm.removeComponent(stepComponent);

        // Reorder remaining steps and update state
        reorderAllSteps(workflow.getId());
        updateScheduledStepsAfterStepChange(workflow, stepId);

        log.debugf("Removed step %s from workflow %s", stepId, workflow.getId());
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

    private void updateScheduledStepsAfterStepChange(Workflow workflow, String stepId) {

        for (ScheduledStep scheduled : workflowStateProvider.getScheduledStepsByStep(stepId)) {
            WorkflowExecutionContext context = buildFromScheduledStep(scheduled);
            context.restart();
            workflowStateProvider.scheduleStep(workflow, context.getNextStep(), scheduled.resourceId(), context.getExecutionId());
        }
    }

    public WorkflowStep toModel(WorkflowStepRepresentation rep) {
        WorkflowStep step = new WorkflowStep(rep.getUses(), rep.getConfig());
        validateStep(step);
        return step;
    }

    public WorkflowConditionProvider getConditionProvider(String providerId, MultivaluedHashMap<String, String> modelConfig) {
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = getConditionProviderFactory(providerId);
        Map<String, List<String>> config = new HashMap<>();

        for (Entry<String, List<String>> configEntry : modelConfig.entrySet()) {
            if (configEntry.getKey().startsWith(providerId)) {
                config.put(configEntry.getKey().substring(providerId.length() + 1), configEntry.getValue());
            }
        }

        WorkflowConditionProvider condition = providerFactory.create(session, config);

        if (condition == null) {
            throw new IllegalStateException("Factory " + providerFactory.getClass() + " returned a null provider");
        }

        return condition;
    }

    public WorkflowConditionProviderFactory<WorkflowConditionProvider> getConditionProviderFactory(String providerId) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = (WorkflowConditionProviderFactory<WorkflowConditionProvider>) sessionFactory.getProviderFactory(WorkflowConditionProvider.class, providerId);

        if (providerFactory == null) {
            throw new WorkflowInvalidStateException("Could not find condition provider: " + providerId);
        }

        return providerFactory;
    }

    private WorkflowExecutionContext buildAndInitContext(Workflow workflow, String resourceId) {
        WorkflowExecutionContext context = new WorkflowExecutionContext(workflow, getSteps(workflow.getId()), resourceId);
        context.init();
        return context;
    }

    private WorkflowExecutionContext buildFromScheduledStep(ScheduledStep scheduledStep) {
        return new WorkflowExecutionContext(
                getWorkflow(scheduledStep.workflowId()),
                getSteps(scheduledStep.workflowId()),
                scheduledStep.resourceId(),
                scheduledStep.stepId(),
                scheduledStep.executionId()
        );
    }

    private void processWorkflow(Workflow workflow, WorkflowExecutionContext context, String resourceId) {
        while (context.hasNextStep()) {
            WorkflowStep step = context.getNextStep();
            if (step.getAfter() > 0) {
                // If a step has a time defined, schedule it and stop processing the other steps of workflow
                log.debugf("Scheduling step %s to run in %d ms for resource %s (execution id: %s)",
                        step.getProviderId(), step.getAfter(), resourceId, context.getExecutionId());
                workflowStateProvider.scheduleStep(workflow, step, resourceId, context.getExecutionId());
                return;
            } else {
                // Otherwise run the step right away

                runWorkflowStep(context, step, resourceId);
            }
        }

        // if we've reached the end of the workflow, check if it is recurring or if we can mark it as completed
        if (workflow.isRecurring()) {
            // if the workflow is recurring, restart it
            context.restart();
            processWorkflow(workflow, context, resourceId);
        } else {
            // not recurring, remove the state record
            context.complete();
            workflowStateProvider.remove(context.getExecutionId());
        }
    }

    private void runWorkflowStep(WorkflowExecutionContext context, WorkflowStep step, String resourceId) {
        log.debugf("Running step %s on resource %s (execution id: %s)", step.getProviderId(), resourceId, context.getExecutionId());
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            try {
                getStepProvider(step).run(List.of(resourceId));
                context.success(step);
            } catch(WorkflowExecutionException e) {
                context.fail(step, e.getMessage());
                throw e;
            }
        });
    }
}
