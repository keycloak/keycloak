package org.keycloak.testframework.injection;

import java.util.HashMap;
import java.util.Map;

public class ValueTypeAlias {

    private final Map<Class<?>, String> aliases = new HashMap<>();

    public void addAll(Map<Class<?>, String> aliases) {
        this.aliases.putAll(aliases);
    }

    public String getAlias(Class<?> clazz) {
        String alias = aliases.get(clazz);
        if (alias == null) {
            alias = clazz.getSimpleName();
        }
        return alias;
    }

}
