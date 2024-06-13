package org.keycloak.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OptionBuilder<T> {

    private static final List<String> BOOLEAN_TYPE_VALUES = List.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());

    private final Class<T> type;
    private final String key;
    private OptionCategory category;
    private boolean hidden;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private List<String> expectedValues = List.of();
    private DeprecatedMetadata deprecatedMetadata;

    public static <A> OptionBuilder<List<A>> listOptionBuilder(String key, Class<A> type) {
        return new OptionBuilder(key, List.class, type);
    }

    public OptionBuilder(String key, Class<T> type) {
        this(key, type, null);
    }

    private OptionBuilder(String key, Class<T> type, Class<?> auxiliaryType) {
        this.type = type;
        if (type.isArray() || ((Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) && type != java.util.List.class)) {
            throw new IllegalArgumentException("Non-List multi-valued options are not yet supported");
        }
        this.key = key;
        category = OptionCategory.GENERAL;
        hidden = false;
        build = false;
        description = null;
        Class<?> expected = type;
        if (auxiliaryType != null) {
            expected = auxiliaryType;
        }
        defaultValue = Boolean.class.equals(expected) ? Optional.of((T) Boolean.FALSE) : Optional.empty();
        if (Boolean.class.equals(expected)) {
            expectedValues(BOOLEAN_TYPE_VALUES);
        }
        if (Enum.class.isAssignableFrom(expected)) {
            expectedValues((Class<? extends Enum>) expected);
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

    public OptionBuilder<T> expectedValues(List<String> expected) {
        this.expectedValues = expected;
        return this;
    }

    public OptionBuilder<T> expectedValues(Class<? extends Enum> expected) {
        this.expectedValues = List.of(expected.getEnumConstants()).stream().map(Object::toString).collect(Collectors.toList());
        return this;
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        this.expectedValues = List.of(expected).stream().map(v -> v.toString()).collect(Collectors.toList());
        return this;
    }

    public OptionBuilder<T> deprecated() {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateOption(null, null);
        return this;
    }

    public OptionBuilder<T> deprecated(String note) {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateOption(note, null);
        return this;
    }

    public OptionBuilder<T> deprecated(Set<String> newOptionsKeys) {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateOption(null, newOptionsKeys);
        return this;
    }

    public OptionBuilder<T> deprecated(String note, Set<String> newOptionsKeys) {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateOption(note, newOptionsKeys);
        return this;
    }

    public OptionBuilder<T> deprecatedValues(Set<String> values, String note) {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateValues(values, note);
        return this;
    }


    public Option<T> build() {
        if (deprecatedMetadata == null && category.getSupportLevel() == ConfigSupportLevel.DEPRECATED) {
            deprecated();
        }

        return new Option<T>(type, key, category, hidden, build, description, defaultValue, expectedValues, deprecatedMetadata);
    }

}
