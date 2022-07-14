package org.keycloak.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OptionBuilder<T> {
    private final Class<T> type;
    private final Class<T> auxiliaryType;
    private final String key;
    private OptionCategory category;
    private boolean hidden;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private List<String> expectedValues;

    public OptionBuilder(String key, Class<T> type) {
        this.type = type;
        this.auxiliaryType = null;
        this.key = key;
        category = OptionCategory.GENERAL;
        hidden = false;
        build = false;
        description = null;
        defaultValue = Boolean.class.equals(type) ? Optional.of((T) Boolean.FALSE) : Optional.empty();
        expectedValues = new ArrayList<>();
        if (Boolean.class.equals(type)) {
            expectedStringValues(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        }
    }

    public OptionBuilder(String key, Class<T> type, Class<T> auxiliaryType) {
        this.type = type;
        this.auxiliaryType = auxiliaryType;
        this.key = key;
        category = OptionCategory.GENERAL;
        hidden = false;
        build = false;
        description = null;
        defaultValue = Boolean.class.equals(type) ? Optional.of((T) Boolean.FALSE) : Optional.empty();
        expectedValues = new ArrayList<>();
        if (Boolean.class.equals(type)) {
            expectedStringValues(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        }
    }

    public OptionBuilder<T> category(OptionCategory category) {
        this.category = category;
        return this;
    }

    public OptionBuilder<T> hidden() {
        this.hidden = true;
        return this;
    }

    public OptionBuilder<T> buildTime(boolean build) {
        this.build = build;
        return this;
    }

    public OptionBuilder<T> description(String description) {
        this.description = description;
        return this;
    }

    public OptionBuilder<T> defaultValue(Optional<T> defaultV) {
        this.defaultValue = defaultV;
        return this;
    }

    public OptionBuilder<T> defaultValue(T defaultV) {
        this.defaultValue = Optional.ofNullable(defaultV);
        return this;
    }

    public OptionBuilder<T> expectedStringValues(List<String> expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(expected);
        return this;
    }

    public OptionBuilder<T> expectedStringValues(String ... expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(Arrays.asList(expected));
        return this;
    }

    public OptionBuilder<T> expectedValues(List<T> expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(expected.stream().map(v -> v.toString()).collect(Collectors.toList()));
        return this;
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(Arrays.asList(expected).stream().map(v -> v.toString()).collect(Collectors.toList()));
        return this;
    }


    public Option<T> build() {
        if (auxiliaryType != null) {
            return new MultiOption<T>(type, auxiliaryType, key, category, hidden, build, description, defaultValue, expectedValues);
        } else {
            return new Option<T>(type, key, category, hidden, build, description, defaultValue, expectedValues);
        }
    }

}
