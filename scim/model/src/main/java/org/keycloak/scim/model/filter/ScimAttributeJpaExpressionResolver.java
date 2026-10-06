package org.keycloak.scim.model.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * <p>A functional interface that defines a method for resolving JPA expressions based on SCIM attributes.
 *
 * <p>This interface is used to map SCIM attributes to their corresponding JPA expressions, allowing for dynamic query
 * construction based on SCIM filters.
 */
public interface ScimAttributeJpaExpressionResolver {

    /**
     * Resolves a {@link Expression} for the given {@code attribute} using the provided {@code CriteriaBuilder} and
     * {@code Root}, within the given correlated {@code subquery}.
     *
     * <p>Implementations that need to reach into a relation collection (e.g. group memberships, role mappings) must
     * add a {@code Root} for the relation entity to the {@code subquery} via {@link Subquery#from(Class)} and
     * correlate it to the outer {@code root} with an explicit {@code subquery.where(...)} predicate (comparing a
     * foreign-key attribute on the relation entity to {@code root.get(...)}), rather than joining off of {@code root}
     * directly. The caller combines this correlation predicate with the operator predicate before finalizing the
     * subquery, so that the resulting {@code EXISTS} check is evaluated per-resource - this is what allows
     * conjunction across independent values and negation of multivalued attributes to be expressed correctly (see
     * <a href="https://github.com/keycloak/keycloak/issues/51805">#51805</a>). Adding a {@code Root} without setting
     * this correlation is a bug: the caller cannot invent the correlation on your behalf and will throw rather than
     * silently execute an uncorrelated {@code EXISTS} that matches every resource. Implementations that instead
     * return a computed expression evaluated directly against {@code root} (e.g. a derived field, not a collection)
     * should leave the subquery untouched - the caller detects this and skips the {@code EXISTS} wrapping entirely.
     *
     * @param attribute the SCIM attribute for which to resolve the JPA expression
     * @param cb the criteria builder
     * @param root the root of the enclosing query
     * @param subquery the correlated subquery that will be used to build the {@code EXISTS} predicate for this attribute
     * @return the expression corresponding to the given attribute, or {@code null} if this resolver does not handle it
     */
    Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root, Subquery<?> subquery);
}
