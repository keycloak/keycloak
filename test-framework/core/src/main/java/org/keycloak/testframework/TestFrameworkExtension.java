package org.keycloak.testframework;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.testframework.injection.Supplier;

/**
 * Test framework extensions allows adding additional suppliers to the test framework
 */
public interface TestFrameworkExtension {

    /**
     * List of suppliers provided by the extension
     * @return supplier list
     */
    List<Supplier<?, ?>> suppliers();

    /**
     * List of value types that are always created when running tests. Extensions usually does not need to implement
     * this method
     * @return the list of value types that are always requested for tests
     */
    default List<Class<?>> alwaysEnabledValueTypes() {
        return Collections.emptyList();
    }

    /**
     * List of aliases for value types. By default, {@code getSimpleName} is used as the name for a value type, implementing
     * this method allows setting custom aliases for the value type. For example the core extension has the alias
     * {@code server} for the value type {@code KeycloakServer}
     * @return map where key is the value type and value is the alias
     */
    default Map<Class<?>, String> valueTypeAliases() {
        return Collections.emptyMap();
    }

}
