package org.keycloak.config;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OptionBuilder<T> {

    private  static final Supplier<List<String>> EMPTY_VALUES_SUPPLIER = List::of;
    private  static final Supplier<List<String>> BOOLEAN_TYPE_VALUES = new Supplier<List<String>>() {
        List<String> values = List.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());

        @Override 
        public List<String> get() {
            return values;
        }
    };

    private final Class<T> type;
    private final Class<T> auxiliaryType;
    private final String key;
    private OptionCategory category;
    private boolean hidden;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private Supplier<List<String>> expectedValues;

    public OptionBuilder(String key, Class<T> type) {
        this(key, type, null);
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
        expectedValues = EMPTY_VALUES_SUPPLIER;
        if (Boolean.class.equals(type)) {
            expectedValues(BOOLEAN_TYPE_VALUES);
        }
        if (Enum.class.isAssignableFrom(type)) {
            expectedValues((Class<? extends Enum>) type);
        }
        if (auxiliaryType != null && Enum.class.isAssignableFrom(auxiliaryType)) {
            expectedValues((Class<? extends Enum>) auxiliaryType);
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

    public OptionBuilder<T> expectedValues(Supplier<List<String>> expected) {
        this.expectedValues = expected;
        return this;
    }

    public OptionBuilder<T> expectedValues(Class<? extends Enum> expected) {
        this.expectedValues = new Supplier<>() {
            List<String> values = List.of(expected.getEnumConstants()).stream().map(Object::toString).collect(Collectors.toList());

            @Override
            public List<String> get() {
                return values;
            }
        };
        return this;
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        this.expectedValues = new Supplier<>() {
            List<String> values = List.of(expected).stream().map(v -> v.toString()).collect(Collectors.toList());

            @Override
            public List<String> get() {
                return values;
            }
        };
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
