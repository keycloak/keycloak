package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.config.Option.WILDCARD_PLACEHOLDER_PATTERN;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class WildcardPropertyMapper<T> extends PropertyMapper<T> {

    private final Matcher fromWildcardMatcher;
    private final Pattern fromWildcardPattern;
    private final Pattern envVarNameWildcardPattern;
    private Matcher toWildcardMatcher;
    private Pattern toWildcardPattern;
    private final Function<Set<String>, Set<String>> wildcardKeysTransformer;
    private final ValueMapper wildcardMapFrom;

    public WildcardPropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
            BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen, Matcher fromWildcardMatcher, Function<Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom) {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null);
        this.wildcardMapFrom = wildcardMapFrom;
        this.fromWildcardMatcher = fromWildcardMatcher;
        // Includes handling for both "--" prefix for CLI options and "kc." prefix
        this.fromWildcardPattern = Pattern.compile("(?:" + ARG_PREFIX + "|kc\\.)" + fromWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));

        // Not using toEnvVarFormat because it would process the whole string incl the <...> wildcard.
        Matcher envVarMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey().toUpperCase().replace("-", "_"));
        this.envVarNameWildcardPattern = Pattern.compile("KC_" + envVarMatcher.replaceFirst("([_A-Z0-9]+)"));

        if (to != null) {
            toWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(to);
            if (!toWildcardMatcher.find()) {
                throw new IllegalArgumentException("Attempted to map a wildcard option to a non-wildcard option");
            }

            this.toWildcardPattern = Pattern.compile(toWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));
        }

        this.wildcardKeysTransformer = wildcardKeysTransformer;
    }

    @Override
    public boolean hasWildcard() {
        return true;
    }

    String getTo(String wildcardKey) {
        return toWildcardMatcher.replaceFirst(wildcardKey);
    }

    String getFrom(String wildcardKey) {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + fromWildcardMatcher.replaceFirst(wildcardKey);
    }

    @Override
    public List<ConfigValue> getKcConfigValues() {
        return this.getWildcardKeys().stream().map(v -> Configuration.getConfigValue(getFrom(v))).toList();
    }

    public Set<String> getWildcardKeys() {
        // this is not optimal
        // TODO find an efficient way to get all values that match the wildcard
        Set<String> values = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false)
                .map(n -> getMappedKey(n, false))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (wildcardKeysTransformer != null) {
            return wildcardKeysTransformer.apply(values);
        }

        return values;
    }

    /**
     * Returns a mapped key for the given option name if a relevant mapping is available, or empty otherwise.
     * Currently, it only attempts to extract the wildcard key from the given option name.
     * E.g. for the option "log-level-<category>" and the option name "log-level-io.quarkus",
     * the wildcard value would be "io.quarkus".
     */
    private Optional<String> getMappedKey(String originalKey, boolean tryTo) {
        Matcher matcher = fromWildcardPattern.matcher(originalKey);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }

        if (tryTo && toWildcardPattern != null) {
            matcher = toWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }

        return Optional.empty();
    }

    public Set<String> getToWithWildcards() {
        if (toWildcardMatcher == null) {
            return Set.of();
        }

        return getWildcardKeys().stream()
                .map(v -> toWildcardMatcher.replaceFirst(v))
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        return fromWildcardPattern.matcher(name).matches() || envVarNameWildcardPattern.matcher(name).matches()
                || (toWildcardPattern != null && toWildcardPattern.matcher(name).matches());
    }

    @Override
    public PropertyMapper<?> forEnvKey(String key) {
        Matcher matcher = envVarNameWildcardPattern.matcher(key);
        if (!matcher.matches()) {
            throw new IllegalStateException("Env var '" + key + "' does not match the expected pattern '" + envVarNameWildcardPattern + "'");
        }
        String value = matcher.group(1);
        final String wildcardValue = value.toLowerCase().replace("_", "."); // we opiniotatedly convert env var names to CLI format with dots
        return forWildcardValue(wildcardValue);
    }

    private PropertyMapper<?> forWildcardValue(final String wildcardValue) {
        String to = getTo(wildcardValue);
        String from = getFrom(wildcardValue);
        return new PropertyMapper<T>(this, from, to, wildcardMapFrom == null ? null : (v, context) -> wildcardMapFrom.map(wildcardValue, v, context));
    }

    @Override
    public PropertyMapper<?> forKey(String key) {
        final String wildcardValue = getMappedKey(key, true).orElseThrow();
        return forWildcardValue(wildcardValue);
    }

}
