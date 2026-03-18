package org.keycloak.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;

@SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType", "rawtypes"})
public class OptionBuilder<T> {

    private static final List<String> BOOLEAN_TYPE_VALUES = List.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());

    private final Class<T> type;
    private final Class<?> auxiliaryType;
    private final Set<String> connectedOptions = new HashSet<>();

    private String key;
    private OptionCategory category;
    private boolean hidden;
    private boolean build;
    private String description;
    private Optional<T> defaultValue;
    private List<String> expectedValues;
    private boolean transformEnumValues;
    // Denotes whether a custom value can be provided among the expected values
    private boolean strictExpectedValues;
    private boolean caseInsensitiveExpectedValues;
    private DeprecatedMetadata deprecatedMetadata;
    private String wildcardKey;

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
        strictExpectedValues = true;
    }

    OptionBuilder<T> key(String key) {
        this.key = key;
        return this;
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

    /**
     * For more details, see the {@link Option#transformEnumValue(String)}
     */
    public OptionBuilder<T> transformEnumValues(boolean transform) {
        this.transformEnumValues = transform;
        return this;
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

    /**
     * For more details, see the {@link Option#getConnectedOptions()}
     */
    public OptionBuilder<T> connectedOptions(Option<?>... connectedOptions) {
        this.connectedOptions.addAll(Arrays.stream(connectedOptions).map(Option::getKey).collect(Collectors.toSet()));
        return this;
    }

    /**
     * For more details, see the {@link Option#getWildcardKey()}
     */
    public OptionBuilder<T> wildcardKey(String wildcardKey) {
        this.wildcardKey = wildcardKey;
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

        boolean isEnumType = Enum.class.isAssignableFrom(expected);

        if (expectedValues == null) {
            if (Boolean.class.equals(expected)) {
                expectedValues(BOOLEAN_TYPE_VALUES);
            } else if (isEnumType) {
                expectedValues((Class<? extends Enum>) expected);
            } else {
                expectedValues = List.of();
            }
        }

        if (defaultValue == null) {
            if (Boolean.class.equals(expected)) {
                defaultValue = Optional.of((T) Boolean.FALSE);
            } else {
                defaultValue = Optional.empty();
            }
        }

        if (transformEnumValues) {
            if (isEnumType) {
                expectedValues(expectedValues.stream().map(Option::transformEnumValue).toList());
                defaultValue.ifPresent(t -> defaultValue(Optional.of((T) Option.transformEnumValue(t.toString()))));
            } else {
                throw new IllegalArgumentException("You can use 'transformEnumValues' only for Enum types");
            }
        }

        return new Option<T>(type, key, category, hidden, build, description, defaultValue, expectedValues, strictExpectedValues, caseInsensitiveExpectedValues, deprecatedMetadata, connectedOptions, wildcardKey, expected);
    }

}
