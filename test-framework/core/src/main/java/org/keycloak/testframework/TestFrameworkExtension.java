package org.keycloak.testframework;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.testframework.injection.Supplier;

public interface TestFrameworkExtension {

    List<Supplier<?, ?>> suppliers();

    default List<Class<?>> alwaysEnabledValueTypes() {
        return Collections.emptyList();
    }

    default Map<Class<?>, String> valueTypeAliases() {
        return Collections.emptyMap();
    }

}
