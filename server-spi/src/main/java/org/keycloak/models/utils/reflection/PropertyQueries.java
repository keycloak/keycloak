package org.keycloak.models.utils.reflection;

/**
 * Utilities for working with property queries
 *
 * @see PropertyQuery
 */
public class PropertyQueries {

    private PropertyQueries() {
    }

    /**
     * Create a new {@link PropertyQuery}
     *
     * @param <V>
     * @param targetClass
     *
     * @return
     */
    public static <V> PropertyQuery<V> createQuery(Class<?> targetClass) {
        return new PropertyQuery<V>(targetClass);
    }

}
