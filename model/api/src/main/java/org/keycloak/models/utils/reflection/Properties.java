package org.keycloak.models.utils.reflection;

import java.lang.reflect.Method;

/**
 * Utility class for working with JavaBean style properties
 *
 * @see Property
 */
public class Properties {

    private Properties() {
    }

    /**
     * Create a JavaBean style property from the specified method
     *
     * @param <V>
     * @param method
     *
     * @return
     *
     * @throws IllegalArgumentException if the method does not match JavaBean conventions
     * @see http://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
     */
    public static <V> MethodProperty<V> createProperty(Method method) {
        return new MethodPropertyImpl<V>(method);
    }

    /**
     * Indicates whether this method is a valid property method.
     */
    public static <V> boolean isProperty(Method method) {
        try {
            new MethodPropertyImpl<V>(method);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

