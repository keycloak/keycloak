package org.keycloak.models.workflow;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.provider.Provider;

/**
 * Defines a provider interface for evaluating conditions within workflow executions.
 * </p>
 * Implementations of this interface are responsible for determining whether specific
 * conditions are met based on the context of a workflow execution, as well as providing
 * JPA Criteria API predicates for querying resources based on these conditions.
 */
public interface WorkflowConditionProvider extends Provider {

    /**
     * Returns the {@link }ResourceType} that this condition is capable of evaluating.
     *
     * @return the supported ResourceType for this condition implementation
     */
    ResourceType getSupportedResourceType();

    /**
     * Evaluates the condition against the given workflow execution context.
     * </p>
     * Implementations should inspect the provided {@code context} and return {@code true}
     * when the condition is satisfied and {@code false} otherwise. Typically, implementations
     * use the resource found in the context to test if the condition holds or not, but sometimes
     * the condition may depend on other aspects, such as the current time or other environmental
     * conditions not directly related to the resource.
     *
     * @param context the execution context for the workflow evaluation
     * @return {@code true} if the condition is met, {@code false} otherwise
     */
    boolean evaluate(WorkflowExecutionContext context);

    /**
     * Creates a JPA Criteria API {@link Predicate} representing this condition for use in queries.
     * </p>
     * Implementations should construct and return a Predicate that can be applied to a query
     * that targets the underlying resource. The method receives a {@link CriteriaBuilder},
     * the {@link CriteriaQuery} being built and the query {@link Root} corresponding to the
     * resource being filtered.
     *
     * @param cb the CriteriaBuilder used to construct predicates
     * @param query the CriteriaQuery being constructed
     * @param resourceRoot the Root representing the resource entity in the query
     * @return a Predicate representing this condition for use in a CriteriaQuery
     */
    Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> resourceRoot);

    /**
     * Validates the internal configuration/state of this condition provider.
     * </p>
     * Implementations should perform any necessary self-checks and throw a
     * {@link WorkflowInvalidStateException} if the provider is not correctly configured
     * or cannot operate safely.
     *
     * @throws WorkflowInvalidStateException if the provider is in an invalid state
     */
    void validate() throws WorkflowInvalidStateException;
}
