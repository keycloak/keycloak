package org.keycloak.federation.ldap.idm.query;

import org.keycloak.federation.ldap.idm.model.IdentityType;

/**
 * <p>The {@link IdentityQueryBuilder} is responsible for creating {@link IdentityQuery} instances and also
 * provide methods to create conditions, orderings, sorting, etc.</p>
 *
 * @author Pedro Igor
 */
public interface IdentityQueryBuilder {

    /**
     * <p>Create a condition for testing the whether the query parameter satisfies the given pattern..</p>
     *
     * @param parameter The query parameter.
     * @param pattern The pattern to match.
     *
     * @return
     */
    Condition like(QueryParameter parameter, String pattern);

    /**
     * <p>Create a condition for testing the arguments for equality.</p>
     *
     * @param parameter The query parameter.
     * @param value The value to compare.
     *
     * @return
     */
    Condition equal(QueryParameter parameter, Object value);

    /**
     * <p>Create a condition for testing whether the query parameter is grater than the given value..</p>
     *
     * @param parameter The query parameter.
     * @param x The value to compare.
     *
     * @return
     */
    Condition greaterThan(QueryParameter parameter, Object x);

    /**
     * <p>Create a condition for testing whether the query parameter is grater than or equal to the given value..</p>
     *
     * @param parameter The query parameter.
     * @param x The value to compare.
     *
     * @return
     */
    Condition greaterThanOrEqualTo(QueryParameter parameter, Object x);

    /**
     * <p>Create a condition for testing whether the query parameter is less than the given value..</p>
     *
     * @param parameter The query parameter.
     * @param x The value to compare.
     *
     * @return
     */
    Condition lessThan(QueryParameter parameter, Object x);

    /**
     * <p>Create a condition for testing whether the query parameter is less than or equal to the given value..</p>
     *
     * @param parameter The query parameter.
     * @param x The value to compare.
     *
     * @return
     */
    Condition lessThanOrEqualTo(QueryParameter parameter, Object x);

    /**
     * <p>Create a condition for testing whether the query parameter is between the given values.</p>
     *
     * @param parameter The query parameter.
     * @param x The first value.
     * @param x The second value.
     *
     * @return
     */
    Condition between(QueryParameter parameter, Object x, Object y);

    /**
     * <p>Create a condition for testing whether the query parameter is contained in a list of values.</p>
     *
     * @param parameter The query parameter.
     * @param values A list of values.
     *
     * @return
     */
    Condition in(QueryParameter parameter, Object... values);

    /**
     * <p>Create an ascending order for the given <code>parameter</code>. Once created, you can use it to sort the results of a
     * query.</p>
     *
     * @param parameter The query parameter to sort.
     *
     * @return
     */
    Sort asc(QueryParameter parameter);

    /**
     * <p>Create an descending order for the given <code>parameter</code>. Once created, you can use it to sort the results of a
     * query.</p>
     *
     * @param parameter The query parameter to sort.
     *
     * @return
     */
    Sort desc(QueryParameter parameter);

    /**
     * <p> Create an {@link IdentityQuery} that can be used to query for {@link
     * IdentityType} instances of a the given <code>identityType</code>. </p>
     *
     * @param identityType The type to search. If you provide the {@link IdentityType}
     * base interface any of its sub-types will be returned.
     *
     * @return
     */
    <T extends IdentityType> IdentityQuery<T> createIdentityQuery(Class<T> identityType);
}
