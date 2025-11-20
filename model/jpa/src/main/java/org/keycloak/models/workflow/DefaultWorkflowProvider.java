package org.keycloak.models.workflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import org.jboss.logging.Logger;

import static java.util.Optional.ofNullable;

public class DefaultWorkflowProvider implements WorkflowProvider {

    private static final Logger log = Logger.getLogger(DefaultWorkflowProvider.class);

    private final KeycloakSession session;
    private final WorkflowStateProvider stateProvider;
    private final WorkflowExecutor executor;
    private final KeycloakSessionFactory sessionFactory;
    private final RealmModel realm;

    DefaultWorkflowProvider(KeycloakSession session, WorkflowExecutor executor) {
        this.session = session;
        this.executor = executor;
        this.sessionFactory = session.getKeycloakSessionFactory();
        this.stateProvider = sessionFactory.getProviderFactory(WorkflowStateProvider.class).create(session);
        this.realm = session.getContext().getRealm();
    }

    @Override
    public ResourceTypeSelector getResourceTypeSelector(ResourceType type) {
        Objects.requireNonNull(type, "type");

        return switch (type) {
            case USERS -> new UserResourceTypeWorkflowProvider(session);
        };
    }

    @Override
    public void updateWorkflow(Workflow workflow, WorkflowRepresentation representation) {
        // first step - ensure the updated workflow is valid
        WorkflowValidator.validateWorkflow(session, representation);

        // check if there are scheduled steps for this workflow - if there aren't, we can update freely
        if (!stateProvider.hasScheduledSteps(workflow.getId())) {
            // simply delete and re-create the workflow, ensuring the id remains the same
            removeWorkflow(workflow);
            representation.setId(workflow.getId());
            toModel(representation);
        } else {
            // if there are scheduled steps, we don't allow to update the workflow's 'on' config
            WorkflowRepresentation currentRepresentation = toRepresentation(workflow);
            if (!Objects.equals(currentRepresentation.getOn(), representation.getOn())) {
                throw new ModelValidationException("Cannot update 'on' configuration when there are scheduled resources for the workflow.");
            }

            // we also need to guarantee the steps remain the same - that is, in the same order with the same 'uses' property.
            // each step can have its config updated, but the steps themselves cannot be changed.
            List<WorkflowStepRepresentation> currentSteps = currentRepresentation.getSteps();
            List<WorkflowStepRepresentation> newSteps = ofNullable(representation.getSteps()).orElse(List.of());
            if (currentSteps.size() != newSteps.size()) {
                throw new ModelValidationException("Cannot change the number or order of steps when there are scheduled resources for the workflow.");
            }
            for (int i = 0; i < currentSteps.size(); i++) {
                WorkflowStepRepresentation currentStep = currentSteps.get(i);
                WorkflowStepRepresentation newStep = newSteps.get(i);
                if (!Objects.equals(currentStep.getUses(), newStep.getUses())) {
                    throw new ModelValidationException("Cannot change the number or order of steps when there are scheduled resources for the workflow.");
                }
                // set the id of the step to match the existing one, so we can update the config
                newStep.setId(currentStep.getId());
            }

            // finally, update the workflow's config along with the steps' configs
            workflow.updateConfig(representation.getConfig(), newSteps);
        }
    }

    @Override
    public void removeWorkflow(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow");
        ComponentModel component = getWorkflowComponent(workflow.getId());
        realm.getComponentsStream(workflow.getId(), WorkflowStepProvider.class.getName()).forEach(realm::removeComponent);
        realm.removeComponent(component);
        stateProvider.removeByWorkflow(workflow.getId());
    }

    @Override
    public Workflow getWorkflow(String id) {
        return new Workflow(session, getWorkflowComponent(id));
    }

    @Override
    public Stream<Workflow> getWorkflows() {
        return realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName())
                .map(c -> new Workflow(session, c));
    }

    @Override
    public void submit(WorkflowEvent event) {
        processEvent(getWorkflows(), event);
    }

    @Override
    public void runScheduledSteps() {
        getWorkflows().forEach((workflow) -> {
            if (!workflow.isEnabled()) {
                log.debugf("Skipping workflow %s as it is disabled", workflow.getName());
                return;
            }
            for (ScheduledStep scheduled : stateProvider.getDueScheduledSteps(workflow)) {
                // check if the resource is still passes the workflow's resource conditions
                DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, workflow, scheduled);
                EventBasedWorkflow provider = new EventBasedWorkflow(session, getWorkflowComponent(workflow.getId()));
                if (!provider.validateResourceConditions(context)) {
                    log.debugf("Resource %s is no longer eligible for workflow %s. Cancelling execution of the workflow.",
                            scheduled.resourceId(), scheduled.workflowId());
                    stateProvider.remove(scheduled.executionId());
                } else {
                    WorkflowStep step = context.getCurrentStep();
                    if (step == null) {
                        log.warnf("Could not find step %s in workflow %s for resource %s. Cancelling execution of the workflow.",
                                scheduled.stepId(), scheduled.workflowId(), scheduled.resourceId());
                        stateProvider.remove(scheduled.executionId());
                    } else {
                        runWorkflow(context);
                    }
                }
            }
        });
    }

    @Override
    public void activate(Workflow workflow, ResourceType type, String resourceId) {
        processEvent(Stream.of(workflow), new AdhocWorkflowEvent(type, resourceId));
    }

    @Override
    public void deactivate(Workflow workflow, String resourceId) {
        stateProvider.removeByWorkflowAndResource(workflow.getId(),  resourceId);
    }

    @Override
    public void activateForAllEligibleResources(Workflow workflow) {
        if (workflow.isEnabled()) {
            WorkflowProvider provider = getWorkflowProvider(workflow);
            ResourceTypeSelector selector = provider.getResourceTypeSelector(ResourceType.USERS);
            selector.getResourceIds(workflow)
                    .forEach(resourceId -> processEvent(Stream.of(workflow), new AdhocWorkflowEvent(ResourceType.USERS, resourceId)));
        }
    }

    @Override
    public WorkflowRepresentation toRepresentation(Workflow workflow) {
        List<WorkflowStepRepresentation> steps = workflow.getSteps().map(this::toRepresentation).toList();
        return new WorkflowRepresentation(workflow.getId(), workflow.getName(), workflow.getConfig(), steps);
    }

    @Override
    public Workflow toModel(WorkflowRepresentation rep) {
        WorkflowValidator.validateWorkflow(session, rep);

        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());
        if (rep.isCancelIfRunning()) {
            config.putSingle(WorkflowConstants.CONFIG_CANCEL_IF_RUNNING, "true");
        }

        Workflow workflow = addWorkflow(new Workflow(session, rep.getId(), config));
        workflow.addSteps(rep.getSteps());
        return workflow;
    }

    @Override
    public void close() {
    }

    WorkflowStepProvider getStepProvider(WorkflowStep step) {
        return getStepProviderFactory(step).create(session, realm.getComponent(step.getId()));
    }

    private ComponentModel getWorkflowComponent(String id) {
        ComponentModel component = realm.getComponent(id);

        if (component == null || !WorkflowProvider.class.getName().equals(component.getProviderType())) {
            throw new BadRequestException("Not a valid resource workflow: " + id);
        }

        return component;
    }

    /* ================================= Workflows component providers and factories ================================= */

    private WorkflowProvider getWorkflowProvider(Workflow workflow) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) sessionFactory
                .getProviderFactory(WorkflowProvider.class, DefaultWorkflowProviderFactory.ID);
        return (WorkflowProvider) factory.create(session, realm.getComponent(workflow.getId()));
    }

    private WorkflowStepProviderFactory<WorkflowStepProvider> getStepProviderFactory(WorkflowStep step) {
        WorkflowStepProviderFactory<WorkflowStepProvider> factory = (WorkflowStepProviderFactory<WorkflowStepProvider>) session
                .getKeycloakSessionFactory().getProviderFactory(WorkflowStepProvider.class, step.getProviderId());

        if (factory == null) {
            throw new WorkflowInvalidStateException("Step not found: " + step.getProviderId());
        }

        return factory;
    }

    private void processEvent(Stream<Workflow> workflows, WorkflowEvent event) {
        Map<String, ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByResource(event.getResourceId())
                .stream().collect(Collectors.toMap(ScheduledStep::workflowId, Function.identity()));

        workflows.forEach(workflow -> {
            if (!workflow.isEnabled()) {
                log.debugf("Skipping workflow %s as it is disabled or has no steps", workflow.getName());
                return;
            }

            EventBasedWorkflow provider = new EventBasedWorkflow(session, getWorkflowComponent(workflow.getId()));

            try {
                ScheduledStep scheduledStep = scheduledSteps.get(workflow.getId());
                DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, workflow, event);

                // if workflow is not active for the resource, check if the provider allows activating based on the event
                if (scheduledStep == null) {
                    if (provider.activate(context)) {
                        if (isAlreadyScheduledInSession(event, workflow)) {
                            return;
                        }
                        // If the workflow has a positive notBefore set, schedule the first step with it
                        if (DurationConverter.isPositiveDuration(workflow.getNotBefore())) {
                            scheduleWorkflow(context);
                        } else {
                            // process the workflow steps, scheduling or running them as needed
                            runWorkflow(context);
                        }
                    }
                } else {
                    // workflow is active for the resource, check if the provider wants to reset or deactivate it based on the event
                    String executionId = scheduledStep.executionId();
                    String resourceId = scheduledStep.resourceId();
                    if (provider.reset(context)) {
                        new DefaultWorkflowExecutionContext(session, workflow, event, scheduledStep).restart();
                    } else if (provider.deactivate(context)) {
                        log.debugf("Workflow '%s' cancelled for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
                        stateProvider.remove(executionId);
                    }
                }
            } catch (WorkflowInvalidStateException e) {
                workflow.setEnabled(false);
                workflow.setError(e.getMessage());
                workflow.updateConfig(workflow.getConfig(), null);
                log.warnf("Workflow %s was disabled due to: %s", workflow.getId(), e.getMessage());
            }
        });
    }

    private boolean isAlreadyScheduledInSession(WorkflowEvent event, Workflow workflow) {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> scheduled = (Map<String, Set<String>>) session.getAttribute("kc.workflow.scheduled");

        if (scheduled == null) {
            scheduled = new HashMap<>();
            session.setAttribute("kc.workflow.scheduled", scheduled);
        }

        String resourceId = event.getResourceId();

        boolean isAlreadyScheduled = !scheduled.computeIfAbsent(resourceId, k -> new HashSet<>()).add(workflow.getId());

        if (isAlreadyScheduled) {
            log.debugf("Event %s for workflow %s and resource %s was previously processed for the resource", workflow.getName(), resourceId);
        }

        return isAlreadyScheduled;
    }

    private void scheduleWorkflow(WorkflowExecutionContext context) {
        executor.runTask(session, new ScheduleWorkflowTask((DefaultWorkflowExecutionContext) context));
    }

    private void runWorkflow(DefaultWorkflowExecutionContext context) {
        executor.runTask(session, new RunWorkflowTask(context));
    }

    private WorkflowStepRepresentation toRepresentation(WorkflowStep step) {
        return new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig());
    }

    private Workflow addWorkflow(Workflow workflow) {
        ComponentModel model = new ComponentModel();

        model.setId(workflow.getId());
        model.setParentId(realm.getId());
        model.setProviderId(DefaultWorkflowProviderFactory.ID);
        model.setProviderType(WorkflowProvider.class.getName());

        MultivaluedHashMap<String, String> config = workflow.getConfig();

        if (config != null) {
            model.setConfig(config);
        }

        return new Workflow(session, realm.addComponentModel(model));
    }
}
