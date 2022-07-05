package org.keycloak.config;

import java.util.List;
import java.util.Optional;

public class Option<T> {

    private final Class<T> type;
    private final String key;
    private final OptionCategory category;
    private final boolean hidden;
    private final boolean buildTime;
    private final String description;
    private final Optional<T> defaultValue;
    private final List<String> expectedValues;

    public Option(Class<T> type, String key, OptionCategory category, boolean hidden, boolean buildTime, String description, Optional<T> defaultValue, List<String> expectedValues) {
        this.type = type;
        this.key = key;
        this.category = category;
        this.hidden = hidden;
        this.buildTime = buildTime;
        this.description = getDescriptionByCategorySupportLevel(description);
        this.defaultValue = defaultValue;
        this.expectedValues = expectedValues;
    }

    public Class<T> getType() {
        return type;
    }

    public boolean isHidden() { return hidden; }

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
            this.hidden,
            this.buildTime,
            this.description,
            Optional.ofNullable(defaultValue),
            this.expectedValues
        );
    }

    private String getDescriptionByCategorySupportLevel(String description) {
        if(description == null || description.isBlank()) {
            return description;
        }

        switch(this.getCategory().getSupportLevel()) {
            case PREVIEW:
                description = "Preview: " + description;
                break;
            case EXPERIMENTAL:
                description = "Experimental: " + description;
                break;
            default:
                description = description;
        }

        return description;
    }
}
