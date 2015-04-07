package org.keycloak.federation.ldap.idm.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.IdentityType;

/**
 * <p>An {@link IdentityQuery} is responsible for querying the underlying identity stores for instances of
 * a given {@link IdentityType}.</p>
 *
 * <p>Instances of this class are obtained using the {@link IdentityQueryBuilder#createIdentityQuery(Class)}
 * method.</p>
 *
 * <pre>
 *      IdentityManager identityManager = getIdentityManager();
 *
 *      // here we get the query builder
 *      IdentityQueryBuilder builder = identityManager.getQueryBuilder();
 *
 *      // create a condition
 *      Condition condition = builder.equal(User.LOGIN_NAME, "john");
 *
 *      // create a query for a specific identity type using the previously created condition
 *      IdentityQuery query = builder.createIdentityQuery(User.class).where(condition);
 *
 *      // execute the query
 *      List<User> result = query.getResultList();
 * </pre>
 *
 * <p>When preparing a query you may want to create conditions to filter its results and configure how they must be retrieved.
 * For that, you can use the {@link IdentityQueryBuilder}, which provides useful methods for creating
 * different expressions and conditions.</p>
 *
 * @author Shane Bryzak
 * @author Pedro Igor
 */
public interface IdentityQuery<T extends IdentityType> {

    /**
     * @see #setPaginationContext(Object object)
     */
    Object getPaginationContext();

    /**
     * Used for pagination models like LDAP when search will return some object (like cookie) for searching on next page
     *
     * @param object to be used for search next page
     *
     * @return this query
     */
    IdentityQuery<T> setPaginationContext(Object object);

    /**
     * @deprecated Will be removed soon.
     *
     * @see #setSortParameters(QueryParameter...)
     */
    @Deprecated
    QueryParameter[] getSortParameters();

    /**
     * Parameters used to sort the results. First parameter has biggest priority. For example: setSortParameter(User.LAST_NAME,
     * User.FIRST_NAME) means that results will be sorted primarily by lastName and firstName will be used to sort only records with
     * same lastName
     *
     * @param sortParameters parameters to specify sort criteria
     *
     * @deprecated Use {@link IdentityQuery#sortBy(Sort...)} instead. Where you can create sort conditions
     * from the {@link IdentityQueryBuilder}.
     *
     * @return this query
     */
    @Deprecated
    IdentityQuery<T> setSortParameters(QueryParameter... sortParameters);

    /**
     * @deprecated Use {@link IdentityQuery#getSorting()} for a list of sorting conditions. Will be removed soon.
     *
     * @return true if sorting will be ascending
     *
     * @see #setSortAscending(boolean)
     */
    @Deprecated
    boolean isSortAscending();

    /**
     * Specify if sorting will be ascending (true) or descending (false)
     *
     * @param sortAscending to specify if sorting will be ascending or descending
     *
     * @deprecated Use {@link IdentityQuery#sortBy(Sort...)} instead. Where you can create sort conditions
     * from the {@link IdentityQueryBuilder}.
     *
     * @return this query
     */
    @Deprecated
    IdentityQuery<T> setSortAscending(boolean sortAscending);

    /**
     * <p>Set a query parameter to this query in order to filter the results.</p>
     *
     * <p>This method always create an equality condition. For more conditions options  take a look at {@link
     * IdentityQueryBuilder} and use the {@link IdentityQuery#where(Condition...)}
     * instead.</p>
     *
     * @param param The query parameter.
     * @param value The value to match for equality.
     *
     * @return
     *
     * @deprecated Use {@link IdentityQuery#where(Condition...)} to specify query conditions.
     */
    @Deprecated
    IdentityQuery<T> setParameter(QueryParameter param, Object... value);

    /**
     * <p>Add to this query the conditions that will be used to filter results.</p>
     *
     * <p>Any condition previously added to this query will be preserved and the new conditions added. If you want to clear the
     * conditions you must create a new query instance.</p>
     *
     * @param condition One or more conditions created from {@link IdentityQueryBuilder}.
     *
     * @return
     */
    IdentityQuery<T> where(Condition... condition);

    /**
     * <p>Add to this query the sorting conditions to be applied to the results.</p>
     *
     * @param sorts The ordering conditions.
     *
     * @return
     */
    IdentityQuery<T> sortBy(Sort... sorts);

    /**
     * <p>The type used to create this query.</p>
     *
     * @return
     */
    Class<T> getIdentityType();

    /**
     * <p>Returns a map with all the parameter set for this query.</p>
     *
     * @return
     *
     * @deprecated Use {@link IdentityQuery#getConditions()} instead. Will be removed.
     */
    @Deprecated
    Map<QueryParameter, Object[]> getParameters();

    /**
     * <p>Returns a set containing all conditions used by this query to filter its results.</p>
     *
     * @return
     */
    Set<Condition> getConditions();

    /**
     * <p>Returns a set containing all sorting conditions used to filter the results.</p>
     *
     * @return
     */
    Set<Sort> getSorting();

    /**
     * <p>Returns the value used to restrict the given query parameter.</p>
     *
     * @param queryParameter
     *
     * @return
     */
    @Deprecated
    Object[] getParameter(QueryParameter queryParameter);

    @Deprecated
    Map<QueryParameter, Object[]> getParameters(Class<?> type);

    int getOffset();

    /**
     * <p>Set the position of the first result to retrieve.</p>
     *
     * @param offset
     *
     * @return
     */
    IdentityQuery<T> setOffset(int offset);

    /**
     * <p>Returns the number of instances to retrieve.</p>
     *
     * @return
     */
    int getLimit();

    /**
     * <p>Set the maximum number of results to retrieve.</p>
     *
     * @param limit the number of instances to retrieve.
     *
     * @return
     */
    IdentityQuery<T> setLimit(int limit);

    /**
     * <p>Execute the query against the underlying identity stores and returns a list containing all instances of
     * the type (defined when creating this query instance) that match the conditions previously specified.</p>
     *
     * @return
     */
    List<T> getResultList();

    /**
     * Count of all query results. It takes into account query parameters, but it doesn't take into account pagination parameter
     * like offset and limit
     *
     * @return count of all query results
     */
    int getResultCount();
}
