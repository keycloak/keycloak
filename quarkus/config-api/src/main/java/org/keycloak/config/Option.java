package org.keycloak.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Option<T> {

    public enum Runtime {
        QUARKUS,
        DOCS,
        OPERATOR;
    }

    private final Class<T> type;
    private final String key;
    private final OptionCategory category;
    private final Set<Runtime> supportedRuntimes;
    private final boolean buildTime;
    private final String description;
    private final Optional<T> defaultValue;
    private final List<String> expectedValues;

    public Option(Class<T> type, String key, OptionCategory category, Set<Runtime> supportedRuntimes, boolean buildTime, String description, Optional<T> defaultValue, List<String> expectedValues) {
        this.type = type;
        this.key = key;
        this.category = category;
        this.supportedRuntimes = supportedRuntimes;
        this.buildTime = buildTime;
        this.description = description;
        this.defaultValue = defaultValue;
        this.expectedValues = expectedValues;
    }

    public Class<T> getType() {
        return type;
    }

    public Set<Runtime> getSupportedRuntimes() {
        return supportedRuntimes;
    }

    public boolean isBuildTime() {
        return buildTime;
    }

    public String getKey() {
        return key;
    }

    public OptionCategory getCategory() {
        return category;
    }

    public String getDescription() { return description; }

    public Optional<T> getDefaultValue() {
        return defaultValue;
    }

    public List<String> getExpectedValues() {
        return expectedValues;
    }

    public Option<T> withRuntimeSpecificDefault(T defaultValue) {
        return new Option<T>(
            this.type,
            this.key,
            this.category,
            this.supportedRuntimes,
            this.buildTime,
            this.description,
            Optional.ofNullable(defaultValue),
            this.expectedValues
        );
    }

}
