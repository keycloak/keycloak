package org.keycloak.models.workflow.expression;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowConditionProvider;

import static org.keycloak.models.workflow.Workflows.getConditionProvider;
import static org.keycloak.models.workflow.expression.ConditionParserUtil.extractParameter;

/**
 * This visitor traverses the entire parse tree and collects the supported types of all conditionCalls.
 */
public class ConditionTypeCollector extends BooleanConditionParserBaseVisitor<Void> {

    private final KeycloakSession session;

    // A list to store the types we find.
    private Set<ResourceType> resourceTypes;

    public ConditionTypeCollector(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Returns the set of all collected condition supported types.
     */
    public Set<ResourceType> getConditionTypes() {
        if (resourceTypes == null) {
            throw new IllegalStateException("ConditionTypeCollector has not been initialized");
        }
        return Collections.unmodifiableSet(resourceTypes);
    }

    // --- The Collector Method ---

    @Override
    public Void visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        if (resourceTypes == null) {
            resourceTypes = EnumSet.allOf(ResourceType.class);
        }

        String conditionName = ctx.Identifier().getText();
        WorkflowConditionProvider conditionProvider = getConditionProvider(session, conditionName, extractParameter(ctx.parameter()));
        resourceTypes.retainAll(List.of(conditionProvider.getSupportedResourceType()));

        // We don't need to visit children (like 'parameter')
        return null;
    }

}
