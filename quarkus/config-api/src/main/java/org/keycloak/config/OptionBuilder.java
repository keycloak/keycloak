package org.keycloak.config;

import io.smallrye.common.constraint.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType", "rawtypes"})
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
    private List<String> expectedValues;
    // Denotes whether a custom value can be provided among the expected values
    private boolean strictExpectedValues;
    private boolean caseInsensitiveExpectedValues;
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
        Assert.assertNotNull(expected);
        this.expectedValues = expected;
        return this;
    }

    public OptionBuilder<T> expectedValues(Class<? extends Enum> expected) {
        return expectedValues(Stream.of(expected.getEnumConstants()).map(Object::toString).collect(Collectors.toList()));
    }

    public OptionBuilder<T> expectedValues(T ... expected) {
        return expectedValues(Stream.of(expected).map(Object::toString).collect(Collectors.toList()));
    }

    public OptionBuilder<T> strictExpectedValues(boolean strictExpectedValues) {
        this.strictExpectedValues = strictExpectedValues;
        return this;
    }

    public OptionBuilder<T> caseInsensitiveExpectedValues(boolean caseInsensitiveExpectedValues) {
        this.caseInsensitiveExpectedValues = caseInsensitiveExpectedValues;
        return this;
    }

    public OptionBuilder<T> deprecated() {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateOption(null);
        return this;
    }

    public OptionBuilder<T> deprecatedMetadata(DeprecatedMetadata deprecatedMetadata) {
        this.deprecatedMetadata = deprecatedMetadata;
        return this;
    }

    public OptionBuilder<T> deprecatedValues(String note, T... values) {
        this.deprecatedMetadata = DeprecatedMetadata.deprecateValues(note, Stream.of(values).map(Object::toString).toArray(String[]::new));
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

        if (expectedValues == null) {
            if (Boolean.class.equals(expected)) {
                expectedValues(BOOLEAN_TYPE_VALUES);
            } else if (Enum.class.isAssignableFrom(expected)) {
                expectedValues((Class<? extends Enum>) expected);
            } else {
                expectedValues = List.of();
            }
        }

        if (defaultValue.isEmpty() && Boolean.class.equals(expected)) {
            defaultValue = Optional.of((T) Boolean.FALSE);
        }

        return new Option<T>(type, key, category, hidden, build, description, defaultValue, expectedValues, strictExpectedValues, caseInsensitiveExpectedValues, deprecatedMetadata);
    }

}
