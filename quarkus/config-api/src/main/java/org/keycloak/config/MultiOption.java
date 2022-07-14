package org.keycloak.config;

import java.util.List;
import java.util.Optional;

public class MultiOption<T> extends Option<T> {

    private final Class auxiliaryType;

    public MultiOption(Class type, Class auxiliaryType, String key, OptionCategory category, boolean hidden, boolean buildTime, String description, Optional defaultValue, List expectedValues) {
        super(type, key, category, hidden, buildTime, description, defaultValue, expectedValues);
        this.auxiliaryType = auxiliaryType;
    }

    public Class<?> getAuxiliaryType() {
        return auxiliaryType;
    }
}
