package org.keycloak.scim.model.filter;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * <p>A functional interface that defines a method for resolving JPA expressions based on SCIM attributes.
 *
 * <p>This interface is used to map SCIM attributes to their corresponding JPA expressions, allowing for dynamic query
 * construction based on SCIM filters.
 */
public interface ScimAttributeJpaExpressionResolver {

    /**
     * Resolves a {@link Expression} for the given {@code attribute} using the provided {@code CriteriaBuilder}, {@code Root}, and a {@code joinResolver} function.
     *
     * @param attribute the SCIM attribute for which to resolve the JPA expression
     * @param cb the criteria builder
     * @param root the root of the query
     * @param joinResolver a function that resolves a join for a given class. If the join does not exist, the function should create it using the provided supplier
     * @return the expression corresponding to the given attribute
     */
    Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root, BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver);
}
