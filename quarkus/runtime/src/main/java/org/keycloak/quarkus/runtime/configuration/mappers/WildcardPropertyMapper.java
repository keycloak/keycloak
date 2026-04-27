package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.config.LoggingOptions;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;

import io.smallrye.config.ConfigValue;

import static org.keycloak.config.WildcardOptionsUtil.WILDCARD_END;
import static org.keycloak.config.WildcardOptionsUtil.getWildcardNamedKey;
import static org.keycloak.config.WildcardOptionsUtil.getWildcardPrefix;
import static org.keycloak.config.WildcardOptionsUtil.isWildcardOption;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_QUARKUS_PREFIX;

public class WildcardPropertyMapper<T> extends PropertyMapper<T> {

    private static final Pattern valueValidator = Pattern.compile("[\\[\\]\\$\\-._a-zA-Z0-9]+");

    private final BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer;
    private final ValueMapper wildcardMapFrom;

    private final String fromPrefix;
    private String toPrefix;
    private String toSuffix;
    private Character replacementChar = null;

    public WildcardPropertyMapper(Option<T> option, String to, Function<AbstractCommand, Boolean> enabled, String enabledWhen, ValueMapper mapper, String mapFrom, ValueMapper parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen, BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom) {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null, null);
        this.wildcardMapFrom = wildcardMapFrom;

        if (!isWildcardOption(getFrom()) || !getFrom().endsWith(WILDCARD_END)) {
            throw new IllegalArgumentException("Invalid wildcard from format. Wildcard must be at the end of the option.");
        }
        this.fromPrefix = getWildcardPrefix(getFrom());

        if (option == LoggingOptions.LOG_LEVEL_CATEGORY) {
            replacementChar = '.';
        }

        if (getTo() != null) {
            if (!getTo().startsWith(NS_QUARKUS_PREFIX) && !getTo().startsWith(NS_KEYCLOAK_PREFIX)) {
                throw new IllegalArgumentException("Wildcards should map to Quarkus or Keycloak options (option '%s' mapped to '%s'). If not, PropertyMappers logic will need adjusted".formatted(option.getKey(), getTo()));
            }
            if (!isWildcardOption(getTo())) {
                throw new IllegalArgumentException("Invalid wildcard map to.");
            }
            this.toPrefix = getWildcardPrefix(getTo());
            this.toSuffix = getTo().substring(getTo().lastIndexOf(WILDCARD_END) + 1);
        }

        this.wildcardKeysTransformer = wildcardKeysTransformer;
    }

    @Override
    public boolean hasWildcard() {
        return true;
    }

    public String getTo(String wildcardKey) {
        return toPrefix + wildcardKey + toSuffix;
    }

    public String getFrom(String wildcardKey) {
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
        String wildcardValue = extractWildcardValue(key).orElseThrow(() -> new IllegalArgumentException("Invalid wildcard value"));
        String to = getTo(wildcardValue);
        String from = getFrom(wildcardValue);
        String mapFrom = getMapFrom();
        // resolve even the mapFrom() value
        if (isWildcardOption(mapFrom)) {
            mapFrom = getWildcardNamedKey(mapFrom, wildcardValue);
        }

        return new PropertyMapper<T>(this, from, to, mapFrom, wildcardValue,
                wildcardMapFrom == null ? null : (name, v, context) -> wildcardMapFrom.map(wildcardValue, v, context));
    }

    /**
     * Get connected options mapped to use the wildcard value
     */
    public Set<String> getConnectedOptions(String key) {
        return option.getConnectedOptions().stream()
                .map(option -> isWildcardOption(option) ? getWildcardNamedKey(option, key) : option)
                .collect(Collectors.toSet());
    }

    public Optional<String> extractWildcardValue(String key) {
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
            if (replacementChar != null) {
                return Optional.ofNullable(getFrom(envKey.substring(fromPrefix.length()).toLowerCase().replace('_', replacementChar)));
            }
            return Optional.of(transformedKey);
        }
        return Optional.empty();
    }

}
