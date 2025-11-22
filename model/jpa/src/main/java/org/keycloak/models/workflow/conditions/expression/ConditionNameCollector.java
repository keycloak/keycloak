package org.keycloak.models.workflow.conditions.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor traverses the entire parse tree and collects the names of all conditionCalls.
 */
public class ConditionNameCollector extends AbstractConditionCollector {

    // 1. A list to store the names we find.
    private final List<String> conditionNames = new ArrayList<>();

    /**
     * Returns the list of all collected condition call names.
     */
    public List<String> getConditionNames() {
        return conditionNames;
    }

    // --- The Collector Method ---

    @Override
    public Void visitConditionCall(BooleanConditionParser.ConditionCallContext ctx) {
        String conditionName = ctx.Identifier().getText();
        conditionNames.add(conditionName);

        // We don't need to visit children (like 'parameter')
        return null;
    }
}
