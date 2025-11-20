package org.keycloak.models.workflow;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.provider.Provider;

public interface WorkflowConditionProvider extends Provider {

    boolean evaluate(WorkflowExecutionContext context);

    default Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> resourceRoot) {
        return null;
    }

    void validate() throws WorkflowInvalidStateException;
}
