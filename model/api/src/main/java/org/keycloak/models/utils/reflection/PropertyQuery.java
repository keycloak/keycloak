package org.keycloak.models.utils.reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p> Queries a target class for properties that match certain criteria. A property may either be a private or public
 * field, declared by the target class or inherited from a superclass, or a public method declared by the target class
 * or inherited from any of its superclasses. For properties that are exposed via a method, the property must be a
 * JavaBean style property, i.e. it must provide both an accessor and mutator method according to the JavaBean
 * specification. </p> <p/> <p> This class is not thread-safe, however the result returned by the getResultList() method
 * is. </p>
 *
 * @see PropertyQueries
 * @see PropertyCriteria
 */
public class PropertyQuery<V> {
    private final Class<?> targetClass;
    private final List<PropertyCriteria> criteria;

    PropertyQuery(Class<?> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("targetClass parameter may not be null");
        }

        this.targetClass = targetClass;
        this.criteria = new ArrayList<PropertyCriteria>();
    }

    /**
     * Add a criteria to query
     *
     * @param criteria the criteria to add
     */
    public PropertyQuery<V> addCriteria(PropertyCriteria criteria) {
        this.criteria.add(criteria);
        return this;
    }

    /**
     * Get the first result from the query, causing the query to be run.
     *
     * @return the first result, or null if there are no results
     */
    public Property<V> getFirstResult() {
        Map<String, Property<V>> results = getResultList();
        return results.isEmpty() ? null : results.values().iterator().next();
    }

    /**
     * Get the first result from the query that is not marked as read only, causing the query to be run.
     *
     * @return the first writable result, or null if there are no results
     */
    public Property<V> getFirstWritableResult() {
        Map<String, Property<V>>  results = getWritableResultList();
        return results.isEmpty() ? null : results.values().iterator().next();
    }

    /**
     * Get a single result from the query, causing the query to be run. An exception is thrown if the query does not
     * return exactly one result.
     *
     * @return the single result
     *
     * @throws RuntimeException if the query does not return exactly one result
     */
    public Property<V> getSingleResult() {
        Map<String, Property<V>> results = getResultList();
        if (results.size() == 1) {
            return results.values().iterator().next();
        } else if (results.isEmpty()) {
            throw new RuntimeException(
                    "Expected one property match, but the criteria did not match any properties on " +
                            targetClass.getName());
        } else {
            throw new RuntimeException("Expected one property match, but the criteria matched " + results.size() +
                    " properties on " + targetClass.getName());
        }
    }

    /**
     * Get a single result from the query that is not marked as read only, causing the query to be run. An exception is
     * thrown if the query does not return exactly one result.
     *
     * @return the single writable result
     *
     * @throws RuntimeException if the query does not return exactly one result
     */
    public Property<V> getWritableSingleResult() {
        Map<String, Property<V>> results = getWritableResultList();
        if (results.size() == 1) {
            return results.values().iterator().next();
        } else if (results.isEmpty()) {
            throw new RuntimeException(
                    "Expected one property match, but the criteria did not match any properties on " +
                            targetClass.getName());
        } else {
            throw new RuntimeException("Expected one property match, but the criteria matched " +
                    results.size() + " properties on " + targetClass.getName());
        }
    }

    /**
     * Get the result from the query, causing the query to be run.
     *
     * @return the results, or an empty list if there are no results
     */
    public Map<String, Property<V>> getResultList() {
        return getResultList(false);
    }

    /**
     * Get the non read only results from the query, causing the query to be run.
     *
     * @return the results, or an empty list if there are no results
     */
    public Map<String, Property<V>> getWritableResultList() {
        return getResultList(true);
    }

    /**
     * Get the result from the query, causing the query to be run.
     *
     * @param writable if this query should only return properties that are not read only
     *
     * @return the results, or an empty list if there are no results
     */
    private Map<String, Property<V>> getResultList(boolean writable) {
        Map<String, Property<V>> properties = new HashMap<String, Property<V>>();

        // First check public accessor methods (we ignore private methods)
        for (Method method : targetClass.getMethods()) {
            if (!(method.getName().startsWith("is") || method.getName().startsWith("get"))) {
                continue;
            }

            boolean match = true;
            for (PropertyCriteria c : criteria) {
                if (!c.methodMatches(method)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                MethodProperty<V> property = Properties.<V>createProperty(method);

                if (!writable || !property.isReadOnly()) {
                    properties.put(property.getName(), property);
                }
            }
        }

        return Collections.unmodifiableMap(properties);
    }

}
