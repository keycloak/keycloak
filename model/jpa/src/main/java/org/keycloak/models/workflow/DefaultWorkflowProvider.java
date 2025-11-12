package org.keycloak.models.workflow;

import static java.util.Optional.ofNullable;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;

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
import org.jboss.logging.Logger;
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
import org.keycloak.utils.StringUtil;

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
                DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, workflow, scheduled);
                WorkflowStep step = context.getCurrentStep();

                if (step == null) {
                    log.warnf("Could not find step %s in workflow %s for resource %s. Removing the workflow state.",
                            scheduled.stepId(), scheduled.workflowId(), scheduled.resourceId());
                    stateProvider.remove(scheduled.executionId());
                    continue;
                }

                runWorkflow(context);
            }
        });
    }

    @Override
    public void bind(Workflow workflow, ResourceType type, String resourceId) {
        processEvent(Stream.of(workflow), new AdhocWorkflowEvent(type, resourceId));
    }

    @Override
    public void deactivate(Workflow workflow, String resourceId) {
        stateProvider.removeByWorkflowAndResource(workflow.getId(),  resourceId);
    }

    @Override
    public void bindToAllEligibleResources(Workflow workflow) {
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
        validateWorkflow(rep);

        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());
        if (rep.isCancelIfRunning()) {
            config.putSingle(WorkflowConstants.CONFIG_CANCEL_IF_RUNNING, "true");
        }

        Workflow workflow = addWorkflow(new Workflow(session, config));

        workflow.addSteps(rep.getSteps());

        return workflow;
    }

    @Override
    public void close() {
    }

    WorkflowStepProvider getStepProvider(WorkflowStep step) {
        return getStepProviderFactory(step).create(session, realm.getComponent(step.getId()));
    }

    private void updateWorkflowConfig(Workflow workflow, MultivaluedHashMap<String, String> config) {
        ComponentModel component = getWorkflowComponent(workflow.getId());
        component.setConfig(config);
        realm.updateComponent(component);
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

                // if workflow is not active for the resource, check if the provider allows activating based on the event
                if (scheduledStep == null) {
                    if (provider.activateOnEvent(event)) {
                        if (isAlreadyScheduledInSession(event, workflow)) {
                            return;
                        }
                        // If the workflow has a positive notBefore set, schedule the first step with it
                        if (DurationConverter.isPositiveDuration(workflow.getNotBefore())) {
                            scheduleWorkflow(event, workflow);
                        } else {
                            DefaultWorkflowExecutionContext context = new DefaultWorkflowExecutionContext(session, workflow, event);
                            // process the workflow steps, scheduling or running them as needed
                            runWorkflow(context);
                        }
                    }
                } else {
                    // workflow is active for the resource, check if the provider wants to reset or deactivate it based on the event
                    String executionId = scheduledStep.executionId();
                    String resourceId = scheduledStep.resourceId();
                    if (provider.resetOnEvent(event)) {
                        new DefaultWorkflowExecutionContext(session, workflow, event, scheduledStep).restart();
                    } else if (provider.deactivateOnEvent(event)) {
                        log.debugf("Workflow '%s' cancelled for resource %s (execution id: %s)", workflow.getName(), resourceId, executionId);
                        stateProvider.remove(executionId);
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

    private void scheduleWorkflow(WorkflowEvent event, Workflow workflow) {
        executor.runTask(session, new ScheduleWorkflowTask(new DefaultWorkflowExecutionContext(session, workflow, event)));
    }

    private void runWorkflow(DefaultWorkflowExecutionContext context) {
        executor.runTask(session, new RunWorkflowTask(context));
    }

    private WorkflowStepRepresentation toRepresentation(WorkflowStep step) {
        return new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig());
    }

    private void validateWorkflow(WorkflowRepresentation rep) {
        validateField(rep, "name", rep.getName());
        //TODO: validate event and resource conditions (`on` and `if` properties) using the providers with a custom evaluator that calls validate on
        // each condition provider used in the expression.

        // if a workflow has a restart step, at least one of the previous steps must be scheduled to prevent an infinite loop of immediate executions
        List<WorkflowStepRepresentation> steps = ofNullable(rep.getSteps()).orElse(List.of());

        if (steps.isEmpty()) {
            return;
        }

        steps.forEach(step -> validateField(step, "uses", step.getUses()));

        List<WorkflowStepRepresentation> restartSteps = steps.stream()
                .filter(step -> Objects.equals("restart", step.getUses()))
                .toList();

        if (!restartSteps.isEmpty()) {
            if (restartSteps.size() > 1) {
                throw new WorkflowInvalidStateException("Workflow can have only one restart step.");
            }
            WorkflowStepRepresentation restartStep = restartSteps.get(0);
            if (steps.indexOf(restartStep) != steps.size() - 1) {
                throw new WorkflowInvalidStateException("Workflow restart step must be the last step.");
            }
            boolean hasScheduledStep = steps.stream()
                    .anyMatch(step -> DurationConverter.isPositiveDuration(step.getAfter()));
            if (!hasScheduledStep) {
                throw new WorkflowInvalidStateException("A workflow with a restart step must have at least one step with a time delay.");
            }
        }
    }

    private void validateField(Object obj, String fieldName, String value) {
        if (StringUtil.isBlank(value)) {
            throw new ModelValidationException("%s field '%s' cannot be null or empty.".formatted(obj.getClass().getCanonicalName(), fieldName));
        }
    }

    private Workflow addWorkflow(Workflow workflow) {
        ComponentModel model = new ComponentModel();

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
