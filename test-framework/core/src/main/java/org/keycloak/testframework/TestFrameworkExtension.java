package org.keycloak.testframework;

import org.keycloak.testframework.injection.Supplier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TestFrameworkExtension {

    List<Supplier<?, ?>> suppliers();

    default List<Class<?>> alwaysEnabledValueTypes() {
        return Collections.emptyList();
    }

    default Map<Class<?>, String> valueTypeAliases() {
        return Collections.emptyMap();
    }

}
