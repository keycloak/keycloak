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
import java.util.concurrent.atomic.AtomicBoolean;
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

    /* ========================================= Workflows CRUD operations =========================================== */

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

    private void addStep(Workflow workflow, WorkflowStep step) {
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
        realm.addComponentModel(stepModel);
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

    WorkflowStep getStepById(Workflow workflow, String id) {
        return getSteps(workflow.getId()).filter(s -> s.getId().equals(id)).findAny().orElse(null);
    }

    private ComponentModel getWorkflowComponent(String id) {
        ComponentModel component = getRealm().getComponent(id);

        if (component == null || !WorkflowProvider.class.getName().equals(component.getProviderType())) {
            throw new BadRequestException("Not a valid resource workflow: " + id);
        }

        return component;
    }

    public Stream<Workflow> getWorkflows() {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName()).map(Workflow::new);
    }

    public Stream<WorkflowStep> getSteps(String workflowId) {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(workflowId, WorkflowStepProvider.class.getName())
                .map(WorkflowStep::new).sorted();
    }

    /* ================================= Workflows component providers and factories ================================= */

    private WorkflowProvider getWorkflowProvider(Workflow workflow) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowProvider.class, workflow.getProviderId());
        return (WorkflowProvider) factory.create(session, getRealm().getComponent(workflow.getId()));
    }

    public WorkflowStepProvider getStepProvider(WorkflowStep step) {
        return getStepProviderFactory(step).create(session, getRealm().getComponent(step.getId()));
    }

    private WorkflowStepProviderFactory<WorkflowStepProvider> getStepProviderFactory(WorkflowStep step) {
        WorkflowStepProviderFactory<WorkflowStepProvider> factory = (WorkflowStepProviderFactory<WorkflowStepProvider>) session
                .getKeycloakSessionFactory().getProviderFactory(WorkflowStepProvider.class, step.getProviderId());

        if (factory == null) {
            throw new WorkflowInvalidStateException("Step not found: " + step.getProviderId());
        }

        return factory;
    }

    public WorkflowConditionProvider getConditionProvider(String providerId, MultivaluedHashMap<String, String> modelConfig) {
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = getConditionProviderFactory(providerId);
        Map<String, List<String>> config = new HashMap<>();

        for (Entry<String, List<String>> configEntry : modelConfig.entrySet()) {
            if (configEntry.getKey().startsWith(providerId)) {
                config.put(configEntry.getKey().substring(providerId.length() + 1), configEntry.getValue());
            }
        }

        return providerFactory.create(session, config);
    }

    public WorkflowConditionProviderFactory<WorkflowConditionProvider> getConditionProviderFactory(String providerId) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = (WorkflowConditionProviderFactory<WorkflowConditionProvider>) sessionFactory.getProviderFactory(WorkflowConditionProvider.class, providerId);

        if (providerFactory == null) {
            throw new WorkflowInvalidStateException("Could not find condition provider: " + providerId);
        }

        return providerFactory;
    }


    /* =================== Workflows execution methods (bind, process events, run scheduled steps) =================== */

    public void processEvent(WorkflowEvent event) {
        processEvent(getWorkflows(), event);
    }

    private void processEvent(Stream<Workflow> workflows, WorkflowEvent event) {
        Map<String, ScheduledStep> scheduledSteps = workflowStateProvider.getScheduledStepsByResource(event.getResourceId())
                .stream().collect(HashMap::new, (m, v) -> m.put(v.workflowId(), v), HashMap::putAll);

        workflows.forEach(workflow -> {
            if (!workflow.isEnabled()) {
                log.debugf("Skipping workflow %s as it is disabled or has no steps", workflow.getName());
                return;
            }

            WorkflowProvider provider = getWorkflowProvider(workflow);

            try {
                ScheduledStep scheduledStep = scheduledSteps.get(workflow.getId());

                // if workflow is not active for the resource, check if the provider allows activating based on the event
                if (scheduledStep == null) {
                    if (provider.activateOnEvent(event)) {
                        DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(workflow, event);
                        // If the workflow has a notBefore set, schedule the first step with it
                        if (workflow.getNotBefore() != null && workflow.getNotBefore() > 0) {
                            WorkflowStep firstStep = getSteps(workflow.getId()).findFirst().orElseThrow(() -> new WorkflowInvalidStateException("No steps found for workflow " + workflow.getName()));
                            log.debugf("Scheduling first step '%s' of workflow '%s' for resource %s based on on event %s with notBefore %d",
                                    firstStep.getProviderId(), workflow.getName(), event.getResourceId(), event.getOperation(), workflow.getNotBefore());
                            Long originalAfter = firstStep.getAfter();
                            try {
                                firstStep.setAfter(workflow.getNotBefore());
                                workflowStateProvider.scheduleStep(workflow, firstStep, event.getResourceId(), context.getExecutionId());
                            } finally {
                                // restore the original after value
                                firstStep.setAfter(originalAfter);
                            }
                        } else {
                            // process the workflow steps, scheduling or running them as needed
                            runWorkflowNextSteps(context);
                        }
                    }
                } else {
                    // workflow is active for the resource, check if the provider wants to reset or deactivate it based on the event
                    String executionId = scheduledStep.executionId();
                    String resourceId = scheduledStep.resourceId();
                    if (provider.resetOnEvent(event)) {
                        restartWorkflow(new DefaultWorkflowExecutionContext(workflow, event, scheduledStep));
                    } else if (provider.deactivateOnEvent(event)) {
                        log.debugf("Workflow '%s' cancelled for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
                        workflowStateProvider.remove(executionId);
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
        getWorkflows().forEach((workflow) -> {
            if (!workflow.isEnabled()) {
                log.debugf("Skipping workflow %s as it is disabled", workflow.getName());
                return;
            }
            for (ScheduledStep scheduled : workflowStateProvider.getDueScheduledSteps(workflow)) {
                DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(this, workflow, scheduled);
                WorkflowStep step = context.getCurrentStep();

                if (step == null) {
                    log.warnf("Could not find step %s in workflow %s for resource %s. Removing the workflow state.",
                            scheduled.stepId(), scheduled.workflowId(), scheduled.resourceId());
                    workflowStateProvider.remove(scheduled.executionId());
                    continue;
                }

                // run the scheduled step that is due
                runWorkflowStep(context);

                // now process the subsequent steps, scheduling or running them as needed
                runWorkflowNextSteps(context);
            }
        });
    }

    public void bind(Workflow workflow, ResourceType type, String resourceId) {
        processEvent(Stream.of(workflow), new AdhocWorkflowEvent(type, resourceId));
    }

    public void bindToAllEligibleResources(Workflow workflow) {
        if (workflow.isEnabled()) {
            WorkflowProvider provider = getWorkflowProvider(workflow);
            provider.getEligibleResourcesForInitialStep()
                    .forEach(resourceId -> processEvent(Stream.of(workflow), new AdhocWorkflowEvent(ResourceType.USERS, resourceId)));
        }
    }

    private void restartWorkflow(DefaultWorkflowExecutionContext context) {
        Workflow workflow = context.getWorkflow();
        String resourceId = context.getResourceId();
        String executionId = context.getExecutionId();
        context.setCurrentStep(null);
        log.debugf("Restarted workflow '%s' for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
        runWorkflowNextSteps(context);
    }

    private void runWorkflowNextSteps(DefaultWorkflowExecutionContext context) {
        String executionId = context.getExecutionId();
        Workflow workflow = context.getWorkflow();
        Stream<WorkflowStep> steps = getSteps(workflow.getId());
        WorkflowStep currentStep = context.getCurrentStep();

        if (currentStep != null) {
            steps = steps.skip(currentStep.getPriority());
        }

        String resourceId = context.getResourceId();
        AtomicBoolean scheduled = new AtomicBoolean(false);

        steps.forEach(step -> {
            if (scheduled.get()) {
                // if we already scheduled a step, we skip processing the other steps of the workflow
                return;
            }
            if (step.getAfter() > 0) {
                // If a step has a time defined, schedule it and stop processing the other steps of workflow
                log.debugf("Scheduling step %s to run in %d ms for resource %s (execution id: %s)",
                        step.getProviderId(), step.getAfter(), resourceId, executionId);
                workflowStateProvider.scheduleStep(workflow, step, resourceId, executionId);
                scheduled.set(true);
            } else {
                // Otherwise, run the step right away
                context.setCurrentStep(step);
                runWorkflowStep(context);
            }
        });

        if (scheduled.get()) {
            // if we scheduled a step, we stop processing the workflow here
            return;
        }

        // if we've reached the end of the workflow, check if it is recurring or if we can mark it as completed
        if (workflow.isRecurring()) {
            // if the workflow is recurring, restart it
            restartWorkflow(context);
        } else {
            // not recurring, remove the state record
            log.debugf("Workflow '%s' completed for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
            workflowStateProvider.remove(executionId);
        }
    }

    private void runWorkflowStep(DefaultWorkflowExecutionContext context) {
        String executionId = context.getExecutionId();
        WorkflowStep step = context.getCurrentStep();
        String resourceId = context.getResourceId();
        log.debugf("Running step %s on resource %s (execution id: %s)", step.getProviderId(), resourceId, executionId);
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            try {
                getStepProvider(step).run(context);
                log.debugf("Step %s completed successfully (execution id: %s)", step.getProviderId(), executionId);
            } catch(WorkflowExecutionException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Step %s failed (execution id: %s)");
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    sb.append(" - error message: %s");
                    log.debugf(sb.toString(), step.getProviderId(), executionId, errorMessage);
                }
                else {
                    log.debugf(sb.toString(), step.getProviderId(), executionId);
                }
                throw e;
            }
        });
    }

    /* ======================= Workflows representation <-> model conversions and validations ======================== */

    public WorkflowRepresentation toRepresentation(Workflow workflow) {
        List<WorkflowConditionRepresentation> conditions = toConditionRepresentation(workflow);
        List<WorkflowStepRepresentation> steps = getSteps(workflow.getId()).map(this::toRepresentation).toList();

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

    private WorkflowStepRepresentation toRepresentation(WorkflowStep step) {
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

        Workflow workflow = addWorkflow(new Workflow(rep.getUses(), config));

        List<WorkflowStep> steps = rep.getSteps().stream().map(this::toModel).toList();

        addSteps(workflow, steps);

        return workflow;
    }

    public WorkflowStep toModel(WorkflowStepRepresentation rep) {
        WorkflowStep step = new WorkflowStep(rep.getUses(), rep.getConfig());
        validateStep(step);
        return step;
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

    private void validateStep(WorkflowStep step) throws ModelValidationException {
        if (step.getAfter() < 0) {
            throw new ModelValidationException("Step 'after' time condition cannot be negative.");
        }
        // verify the step does have valid provider
        getStepProviderFactory(step);
    }

    /* ============================================ Other utility methods ============================================ */

    public Object resolveResource(ResourceType type, String resourceId) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(type, "resourceId");
        return type.resolveResource(session, resourceId);
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
