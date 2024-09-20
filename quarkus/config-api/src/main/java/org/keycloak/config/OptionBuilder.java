package org.keycloak.config;

import org.keycloak.common.util.CollectionUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionBuilder<T> {

    private static final List<String> BOOLEAN_TYPE_VALUES = List.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());

    private final Class<T> type;
    private final Class<?> auxiliaryType;
    private final String key;
    private OptionCategory category;
    private boolean hidden;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private List<String> expectedValues = List.of();
    // Denotes whether a custom value can be provided among the expected values
    private boolean strictExpectedValues;
    private DeprecatedMetadata deprecatedMetadata;

    public static <A> OptionBuilder<List<A>> listOptionBuilder(String key, Class<A> type) {
        return new OptionBuilder(key, List.class, type);
    }

    public OptionBuilder(String key, Class<T> type) {
        this(key, type, null);
    }

    private OptionBuilder(String key, Class<T> type, Class<?> auxiliaryType) {
        this.type = type;
        this.auxiliaryType = auxiliaryType;
        if (type.isArray() || ((Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) && type != java.util.List.class)) {
            throw new IllegalArgumentException("Non-List multi-valued options are not yet supported");
        }
        this.key = key;
        category = OptionCategory.GENERAL;
        hidden = false;
        build = false;
        description = null;
        defaultValue = Optional.empty();
        strictExpectedValues = true;
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
        return expectedValues(true, expected);
    }

    /**
     * @param strict   if only expected values are allowed, or some other custom value can be specified
     * @param expected expected values
     */
    public OptionBuilder<T> expectedValues(boolean strict, List<String> expected) {
        this.strictExpectedValues = strict;
        this.expectedValues = expected;
        return this;
    }

    public OptionBuilder<T> expectedValues(Class<? extends Enum> expected) {
        return expectedValues(true, expected);
    }

    public OptionBuilder<T> expectedValues(boolean strict, Class<? extends Enum> expected) {
        this.strictExpectedValues = strict;
        this.expectedValues = Stream.of(expected.getEnumConstants()).map(Object::toString).collect(Collectors.toList());
        return this;
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        return expectedValues(true, expected);
    }

    /**
     * @param strict   if only expected values are allowed, or some other custom value can be specified
     * @param expected expected values - if empty and the {@link #type} or {@link #auxiliaryType} is enum, values are inferred
     */
    public OptionBuilder<T> expectedValues(boolean strict, T... expected) {
        this.strictExpectedValues = strict;
        this.expectedValues = Stream.of(expected).map(Object::toString).collect(Collectors.toList());
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

        Class<?> expected = type;
        if (auxiliaryType != null) {
            expected = auxiliaryType;
        }

        if (CollectionUtil.isEmpty(expectedValues)) {
            if (Boolean.class.equals(expected)) {
                expectedValues(strictExpectedValues, BOOLEAN_TYPE_VALUES);
            }

            if (Enum.class.isAssignableFrom(expected)) {
                expectedValues(strictExpectedValues, (Class<? extends Enum>) expected);
            }
        }

        if (defaultValue.isEmpty() && Boolean.class.equals(expected)) {
            defaultValue = Optional.of((T) Boolean.FALSE);
        }

        return new Option<T>(type, key, category, hidden, build, description, defaultValue, expectedValues, strictExpectedValues, deprecatedMetadata);
    }

}
