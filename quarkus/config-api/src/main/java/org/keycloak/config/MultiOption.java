package org.keycloak.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MultiOption<T> extends Option<T> {

    private final Class auxiliaryType;

    public MultiOption(Class type, Class auxiliaryType, String key, OptionCategory category, Set supportedRuntimes, boolean buildTime, String description, Optional defaultValue, List expectedValues) {
        super(type, key, category, supportedRuntimes, buildTime, description, defaultValue, expectedValues);
        this.auxiliaryType = auxiliaryType;
    }

    public Class<?> getAuxiliaryType() {
        return auxiliaryType;
    }
}
