package org.keycloak.models.workflow;

import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.provider.Provider;

public interface WorkflowConditionProvider extends Provider {

    Set<ResourceType> supportedTypes();

    boolean evaluate(WorkflowExecutionContext context);

    Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> resourceRoot);

    void validate() throws WorkflowInvalidStateException;
}
