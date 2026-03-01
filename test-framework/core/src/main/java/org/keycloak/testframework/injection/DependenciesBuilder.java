package org.keycloak.testframework.injection;

import java.util.LinkedList;
import java.util.List;

public class DependenciesBuilder {

    public static DependenciesBuilder create(Class<?> valueType) {
        return new DependenciesBuilder().add(valueType);
    }

    public static DependenciesBuilder create(Class<?> valueType, String ref) {
        return new DependenciesBuilder().add(valueType, ref);
    }

    private final List<Dependency> dependencies;

    public DependenciesBuilder add(Class<?> valueType) {
        dependencies.add(new Dependency(valueType, null));
        return this;
    }

    public DependenciesBuilder add(Class<?> valueType, String ref) {
        dependencies.add(new Dependency(valueType, ref));
        return this;
    }

    public DependenciesBuilder() {
        this.dependencies = new LinkedList<>();
    }

    public List<Dependency> build() {
        return dependencies;
    }

}
