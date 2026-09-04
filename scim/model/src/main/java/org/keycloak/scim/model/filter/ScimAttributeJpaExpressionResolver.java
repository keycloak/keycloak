package org.keycloak.scim.model.filter;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.common.util.TriFunction;
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

    /**
     * Optionally creates a complete {@link Predicate} for the given attribute when a single expression is not sufficient
     * (for example when the attribute value is sourced from multiple joins that must be OR-ed together).
     *
     * <p>Returning {@code null} falls back to {@link #getAttributeExpression} and the default operator application.
     *
     * @param attribute the SCIM attribute for which to create the predicate
     * @param operation the comparison operator (eq, ne, pr, gt, ge, lt, le, co, sw, ew)
     * @param value the normalized value to compare against, or {@code null} for presence checks
     * @param cb the criteria builder
     * @param root the root of the query
     * @param joinResolver a function that resolves a join for a given class
     * @param operator applies the comparison operator to an expression and value
     * @return a custom predicate, or {@code null} to use the default expression-based predicate
     */
    default Predicate createAttributePredicate(Attribute<?, ?> attribute, String operation, Object value,
            CriteriaBuilder cb, Root<?> root,
            BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver,
            TriFunction<CriteriaBuilder, Expression, Object, Predicate> operator) {
        return null;
    }
}
