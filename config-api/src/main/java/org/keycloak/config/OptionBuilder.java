package org.keycloak.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OptionBuilder<T> {
    private Class<T> type;
    private String key;
    private OptionCategory category;
    private Set<Option.Runtime> supportedRuntimes;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private List<T> expectedValues;

    public OptionBuilder(String key, Class<T> type) {
        this.type = type;
        this.key = key;
        category = OptionCategory.GENERAL;
        supportedRuntimes = Arrays.stream(Option.Runtime.values()).collect(Collectors.toSet());
        build = false;
        description = "";
        defaultValue = Optional.empty();
        expectedValues = new ArrayList<>();
    }

    public OptionBuilder<T> category(OptionCategory category) {
        this.category = category;
        return this;
    }

    public OptionBuilder<T> runtimes(Option.Runtime ... runtimes) {
        this.supportedRuntimes.clear();
        this.supportedRuntimes.addAll(Arrays.asList(runtimes));
        return this;
    }

    public OptionBuilder<T> runtimes(Set<Option.Runtime> runtimes) {
        this.supportedRuntimes.clear();
        this.supportedRuntimes.addAll(runtimes);
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

    public OptionBuilder<T> expectedValues(List<T> expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(expected);
        return this;
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        this.expectedValues.clear();
        this.expectedValues.addAll(Arrays.asList(expected));
        return this;
    }

    public Option<T> build() {
        return new Option<T>(type, key, category, supportedRuntimes, build, description, defaultValue, expectedValues);
    }

}
