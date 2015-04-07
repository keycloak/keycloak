package org.keycloak.models.utils.reflection;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A criteria that matches a property based on name
 *
 * @see PropertyCriteria
 */
public class NamedPropertyCriteria implements PropertyCriteria {
    private final String[] propertyNames;

    public NamedPropertyCriteria(String... propertyNames) {
        this.propertyNames = propertyNames;
    }

    public boolean fieldMatches(Field f) {
        for (String propertyName : propertyNames) {
            if (propertyName.equals(f.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean methodMatches(Method m) {
        String[] validPrefix = {"get", "is"};
        for (String propertyName : propertyNames) {
            for (String prefix : validPrefix) {
                if (m.getName().startsWith(prefix) &&
                        Introspector.decapitalize(m.getName().substring(prefix.length())).equals(propertyName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
