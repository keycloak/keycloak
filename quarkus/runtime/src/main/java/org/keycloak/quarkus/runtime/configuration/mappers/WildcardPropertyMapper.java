package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class WildcardPropertyMapper<T> extends PropertyMapper<T> {

    public static final String WILDCARD_FROM_START = "<";

    private static final Pattern valueValidator = Pattern.compile("[\\[\\]\\$\\-._a-zA-Z0-9]+");

    private final BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer;
    private final ValueMapper wildcardMapFrom;

    private final String fromPrefix;
    private String toPrefix;
    private String toSuffix;

    public WildcardPropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
            BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen, BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom) {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null);
        this.wildcardMapFrom = wildcardMapFrom;

        this.fromPrefix = getFrom().substring(0, getFrom().indexOf(WILDCARD_FROM_START));
        if (!getFrom().endsWith(">")) {
            throw new IllegalArgumentException("Invalid wildcard from format. Wildcard must be at the end of the option.");
        }

        if (to != null) {
            if (!to.startsWith(MicroProfileConfigProvider.NS_QUARKUS_PREFIX)) {
                throw new IllegalArgumentException("Wildcards should map to quarkus options. If not, PropertyMappers logic will need adjusted");
            }
            this.toPrefix = to.substring(0, to.indexOf(WILDCARD_FROM_START));
            int lastIndexOf = to.lastIndexOf(">");
            if (lastIndexOf == -1) {
                throw new IllegalArgumentException("Invalid wildcard map to.");
            }
            this.toSuffix = to.substring(lastIndexOf + 1, to.length());
        }

        this.wildcardKeysTransformer = wildcardKeysTransformer;
    }

    @Override
    public boolean hasWildcard() {
        return true;
    }

    String getTo(String wildcardKey) {
        return toPrefix + wildcardKey + toSuffix;
    }

    String getFrom(String wildcardKey) {
        return fromPrefix + wildcardKey;
    }

    public Stream<String> getToFromWildcardTransformer(String value) {
        if (wildcardKeysTransformer == null) {
            return Stream.empty();
        }
        return wildcardKeysTransformer.apply(value, new HashSet<String>()).stream().map(this::getTo);
    }

    @Override
    public PropertyMapper<?> forKey(String key) {
        String wildcardValue = extractWildcardValue(key).orElseThrow();
        String to = getTo(wildcardValue);
        String from = getFrom(wildcardValue);
        return new PropertyMapper<T>(this, from, to,
                wildcardMapFrom == null ? null : (v, context) -> wildcardMapFrom.map(wildcardValue, v, context));
    }

    private Optional<String> extractWildcardValue(String key) {
        String result = null;
        if (key.startsWith(fromPrefix)) {
            result = key.substring(fromPrefix.length());
        } else if (key.startsWith(toPrefix) && key.endsWith(toSuffix)) {
            // TODO: this presumes that the quarkus value is quoted
            result = key.substring(toPrefix.length(), key.length() - toSuffix.length());
        }
        // TODO: it would be nice to warn the user for property file or env entries that look
        // like they should be wildcards, but aren't allowed
        return Optional.ofNullable(result).filter(WildcardPropertyMapper::isValidWildcardValue);
    }

    public static boolean isValidWildcardValue(String result) {
        return valueValidator.matcher(result).matches();
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        return extractWildcardValue(name).isPresent();
    }

    public Optional<String> getKcKeyForEnvKey(String envKey, String transformedKey) {
        if (transformedKey.startsWith(fromPrefix)) {
            return Optional.ofNullable(getFrom(envKey.substring(fromPrefix.length()).toLowerCase().replace("_", ".")));
        }
        return Optional.empty();
    }

}
