package org.keycloak.test.framework;

import org.keycloak.test.framework.injection.Supplier;

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
