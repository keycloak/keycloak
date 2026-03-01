package org.keycloak.testframework.injection;

public record Dependency(Class<?> valueType, String ref) {

    public Dependency {
        ref = StringUtil.convertEmptyToNull(ref);
    }

    @Override
    public String toString() {
        return valueType.getSimpleName() + ":" + ref;
    }
}
