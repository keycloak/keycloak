package org.keycloak.testframework.injection;

import java.util.LinkedList;
import java.util.List;

public class RequiredDependencies {

    private static final RequiredDependencies NONE = new RequiredDependencies();

    private List<RequiredDependency> dependencies;

    public static RequiredDependencies none() {
        return NONE;
    }

    public static RequiredDependencies create(Class<?> valueType) {
        return new RequiredDependencies().add(valueType);
    }

    public static RequiredDependencies create(Class<?> valueType, String ref) {
        return new RequiredDependencies().add(valueType, ref);
    }

    public RequiredDependencies add(Class<?> valueType) {
        dependencies.add(new RequiredDependency(valueType, null));
        return this;
    }

    public RequiredDependencies add(Class<?> valueType, String ref) {
        dependencies.add(new RequiredDependency(valueType, ref));
        return this;
    }

    public RequiredDependencies() {
        this.dependencies = new LinkedList<>();
    }

    List<RequiredDependency> getList() {
        return dependencies;
    }

    public record RequiredDependency(Class<?> valueType, String ref) {
    }

}
