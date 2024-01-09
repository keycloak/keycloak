package org.keycloak.config;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class Option<T> {

    private final Class<T> type;
    private final String key;
    private final OptionCategory category;
    private final boolean hidden;
    private final boolean buildTime;
    private final String description;
    private final Optional<T> defaultValue;
    private final Supplier<List<String>> expectedValues;
    private final DeprecatedMetadata deprecatedMetadata;

    public Option(Class<T> type, String key, OptionCategory category, boolean hidden, boolean buildTime, String description, Optional<T> defaultValue, Supplier<List<String>> expectedValues, DeprecatedMetadata deprecatedMetadata) {
        this.type = type;
        this.key = key;
        this.category = category;
        this.hidden = hidden;
        this.buildTime = buildTime;
        this.description = getDescriptionByCategorySupportLevel(description, category);
        this.defaultValue = defaultValue;
        this.expectedValues = expectedValues;
        this.deprecatedMetadata = deprecatedMetadata;
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
        return expectedValues.get();
    }

    public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
        return Optional.ofNullable(deprecatedMetadata);
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
            this.expectedValues,
            this.deprecatedMetadata
        );
    }

    private static String getDescriptionByCategorySupportLevel(String description, OptionCategory category) {
        if (description != null && !description.isBlank()) {
            switch (category.getSupportLevel()) {
            case PREVIEW:
                description = "Preview: " + description;
                break;
            case EXPERIMENTAL:
                description = "Experimental: " + description;
                break;
            default:
                break;
            }
        }

        return description;
    }
}
