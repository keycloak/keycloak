package org.keycloak.models.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.conditions.expression.BooleanConditionParser;
import org.keycloak.models.workflow.conditions.expression.ConditionNameCollector;
import org.keycloak.models.workflow.conditions.expression.EvaluatorUtils;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.utils.StringUtil;

import static java.util.Optional.ofNullable;

public class WorkflowValidator {

    public static void validateWorkflow(KeycloakSession session, WorkflowRepresentation rep) throws WorkflowInvalidStateException {
        validateField(rep, "name", rep.getName());
        //TODO: validate event and resource conditions (`on` and `if` properties) using the providers with a custom evaluator that calls validate on
        // each condition provider used in the expression once we have the event condition providers implemented
        if (StringUtil.isNotBlank(rep.getOn())) {
            validateConditionExpression(session, rep.getOn(), "on");
        }
        if (StringUtil.isNotBlank(rep.getConditions())) {
            validateConditionExpression(session, rep.getConditions(), "if");
        }

        // if a workflow has a restart step, at least one of the previous steps must be scheduled to prevent an infinite loop of immediate executions
        List<WorkflowStepRepresentation> steps = ofNullable(rep.getSteps()).orElse(List.of());
        if (steps.isEmpty()) {
            return;
        }
        steps.forEach(step -> validateStep(session, step));

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

    private static void validateStep(KeycloakSession session, WorkflowStepRepresentation step) throws WorkflowInvalidStateException {

        // validate the step rep has 'uses' defined
        if (StringUtil.isBlank(step.getUses())) {
            throw new WorkflowInvalidStateException("Step 'uses' cannot be null or empty.");
        }

        // validate the after time, if present
        try {
            Duration duration = DurationConverter.parseDuration(step.getAfter());
            if (duration != null && duration.isNegative()) { // duration can only be null if the config is not set
                throw new WorkflowInvalidStateException("Step 'after' configuration cannot be negative.");
            }
        } catch (IllegalArgumentException e) {
            throw new WorkflowInvalidStateException("Step 'after' configuration is not valid: " + step.getAfter());
        }

        // verify the step does have valid provider
        WorkflowStepProviderFactory<WorkflowStepProvider> factory = (WorkflowStepProviderFactory<WorkflowStepProvider>) session
                .getKeycloakSessionFactory().getProviderFactory(WorkflowStepProvider.class, step.getUses());

        if (factory == null) {
            throw new WorkflowInvalidStateException("Could not find step provider: " + step.getUses());
        }
    }

    private static void validateConditionExpression(KeycloakSession session, String expression, String fieldName) throws WorkflowInvalidStateException {
        BooleanConditionParser.EvaluatorContext context = EvaluatorUtils.createEvaluatorContext(expression);
        ConditionNameCollector collector = new ConditionNameCollector();
        collector.visit(context);

        // check if there are providers for the conditions used in the expression
        if ("on".equals(fieldName)) {
            // check if we can get a ResourceOperationType for the events in the expression
            for (String name : collector.getConditionNames()) {
                try {
                    ResourceOperationType.valueOf(name.replace("-", "_").toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new WorkflowInvalidStateException("Could not find event: " + name);
                }
            }
        } else if ("if".equals(fieldName)) {
            // try to get an instance of the provider -> method throws a WorkflowInvalidStateException if provider is not found
            collector.getConditionNames().forEach(name -> Workflows.getConditionProvider(session, name, expression));
        }
    }

    private static void validateField(Object obj, String fieldName, String value) throws WorkflowInvalidStateException {
        if (StringUtil.isBlank(value)) {
            throw new WorkflowInvalidStateException("%s field '%s' cannot be null or empty.".formatted(obj.getClass().getCanonicalName(), fieldName));
        }
    }
}
