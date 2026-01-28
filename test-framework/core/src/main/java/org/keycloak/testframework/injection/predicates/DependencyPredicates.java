package org.keycloak.testframework.injection.predicates;

import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.testframework.injection.Dependency;

public interface DependencyPredicates {

    static Predicate<Dependency> matches(Class<?> typeClass, String ref) {
        return d -> d.valueType().equals(typeClass) && Objects.equals(d.ref(), ref);
    }

}
