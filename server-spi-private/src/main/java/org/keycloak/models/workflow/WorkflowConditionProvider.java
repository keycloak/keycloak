package org.keycloak.models.workflow;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.provider.Provider;

import java.util.Set;

public interface WorkflowConditionProvider extends Provider {

    Set<ResourceType> supportedTypes();

    boolean evaluate(WorkflowExecutionContext context);

    default Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> resourceRoot) {
        return null;
    }

    void validate() throws WorkflowInvalidStateException;
}
