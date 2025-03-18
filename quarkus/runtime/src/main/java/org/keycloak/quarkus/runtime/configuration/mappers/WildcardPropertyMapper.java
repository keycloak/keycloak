package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.keycloak.config.Option;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.common.utils.StringUtil;

public class WildcardPropertyMapper<T> extends PropertyMapper<T> {

    public static final String WILDCARD_FROM_START = "<";

    private final BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer;
    private final ValueMapper wildcardMapFrom;

    private final String fromPrefix;
    private String toPrefix;
    private String toSuffix;

    private Map<String, String> canonicalEnvMapping = new HashMap<String, String>();

    public WildcardPropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
            BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen, BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom) {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null);
        this.wildcardMapFrom = wildcardMapFrom;

        this.fromPrefix = getFrom().substring(0, getFrom().indexOf(WILDCARD_FROM_START));
        if (!getFrom().endsWith(">")) {
            throw new IllegalArgumentException("Invalid wildcard form format");
        }

        if (to != null) {
            this.toPrefix = to.substring(0, to.indexOf('"') + 1);
            this.toSuffix = to.substring(to.lastIndexOf('"'), to.length());
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
        String wildcardValue = extractWildcardValue(key);
        String to = getTo(wildcardValue);
        String from = getFrom(wildcardValue);
        return new PropertyMapper<T>(this, from, to,
                wildcardMapFrom == null ? null : (v, context) -> wildcardMapFrom.map(wildcardValue, v, context));
    }

    private String extractWildcardValue(String key) {
        if (key.startsWith(fromPrefix)) {
            return key.substring(fromPrefix.length());
        } else if (key.startsWith(toPrefix) && key.endsWith(toSuffix)) {
            // TODO: this presumes that the quarkus value is quoted
            return key.substring(toPrefix.length(), key.length() - toSuffix.length());
        }
        throw new IllegalArgumentException();
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        try {
            extractWildcardValue(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void addCanonicalEnv(String key) {
        this.canonicalEnvMapping.put(StringUtil.replaceNonAlphanumericByUnderscores(key.toUpperCase()), key);
    }

    public String getKcKeyForEnvKey(String envKey, String transformedKey) {
        String from = this.canonicalEnvMapping.get(envKey);
        if (from != null) {
            return from;
        }
        if (transformedKey.startsWith(fromPrefix)) {
            return getFrom(envKey.substring(fromPrefix.length()).toLowerCase().replace("_", "."));
        }
        throw new IllegalArgumentException();
    }

}
