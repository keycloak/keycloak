package org.keycloak.config;

/**
 * Utility class for working with configuration options that use wildcards.
 * <p>
 * Wildcard options are configuration keys that contain a variable segment enclosed between {@link #WILDCARD_START} and {@link #WILDCARD_END} characters.
 * <p>
 * Wildcard options in Keycloak always <strong>end</strong> with the variable segment.
 */
public class WildcardOptionsUtil {

    /**
     * Marker indicating the start of a wildcard segment in a configuration key.
     */
    public static final String WILDCARD_START = "<";

    /**
     * Marker indicating the end of a wildcard segment in a configuration key.
     */
    public static final String WILDCARD_END = ">";

        /**
     * Determines whether the given configuration key represents a wildcard option.
     * <p>
     * Examples:
     * <pre>{@code
     * isWildcardOption("tracing-header-<header>")      → "true"
     * isWildcardOption("tracing-header-<headxxx")      → "false"
     * isWildcardOption("tracing-header-headxxx>")      → "false"
     * isWildcardOption("db-kind-<datasource>")         → "true"
     * isWildcardOption("http-port")                    → "false"
     * isWildcardOption("quarkus.<sth>.end")            → "false"
     * }</pre>
     *
     * @param key the configuration key to check
     * @return {@code true} if the key represents a Keycloak wildcard option
     */
    public static boolean isWildcardOption(String key) {
        return key != null && key.contains(WILDCARD_START) && key.endsWith(WILDCARD_END);
    }

    /**
     * Determines whether the given configuration key represents an external wildcard option (variable segment does not have to be at the end)
     * <p>
     * Examples:
     * <pre>{@code
     * isExternalWildcardOption("tracing-header-<header>")      → "true"
     * isExternalWildcardOption("tracing-header-<headxxx")      → "false"
     * isExternalWildcardOption("tracing-header-headxxx>")      → "false"
     * isExternalWildcardOption("db-kind-<datasource>")         → "true"
     * isExternalWildcardOption("http-port")                    → "false"
     * isExternalWildcardOption("quarkus.<sth>.end")            → "true"
     * }</pre>
     *
     * @param key the configuration key to check
     * @return {@code true} if the key represents an external wildcard option
     */
    public static boolean isExternalWildcardOption(String key) {
        return key != null && key.contains(WILDCARD_START) && key.contains(WILDCARD_END);
    }

    /**
     * Extracts the prefix part of a wildcard key.
     * You should always check the presence of the wildcard via the {@link #isWildcardOption(String)}.
     * <p>
     * Examples:
     * <pre>{@code
     * getWildcardPrefix("tracing-header-<header>")       → "tracing-header-"
     * getWildcardPrefix("db-kind-<datasource>")         → "db-kind-"
     * }</pre>
     *
     * @param wildcardKey a configuration key that includes a wildcard segment
     * @return the prefix before the wildcard marker, otherwise {@code null}
     */
    public static String getWildcardPrefix(String wildcardKey) {
        return wildcardKey != null ? wildcardKey.substring(0, wildcardKey.indexOf(WILDCARD_START)) : null;
    }

    /**
     * Generates a concrete configuration key by replacing the wildcard placeholder with a specific value.
     * You should always check the presence of the wildcard via the {@link #isWildcardOption(String)}.
     * <p>
     * Examples:
     * <pre>{@code
     * getWildcardNamedKey("tracing-header-<header>", "Authorization")  → "tracing-header-Authorization"
     * getWildcardNamedKey("db-kind-<datasource>", "user-store") → "db-kind-user-store"
     * }</pre>
     *
     * @param wildcardKey a configuration key that includes a wildcard segment
     * @param value       the value to replace the wildcard with
     * @return the resolved key, otherwise {@code null}
     */
    public static String getWildcardNamedKey(String wildcardKey, String value) {
        var prefix = getWildcardPrefix(wildcardKey);
        return prefix != null ? prefix.concat(value) : null;
    }

    /**
     * Extracts the name that replaces the wildcard placeholder from a fully qualified configuration key.
     * <p>
     * Examples:
     * <pre>{@code
     * getWildcardName(TracingOptions.TRACING_HEADER, "tracing-header-Authorization") → "Authorization"
     * getWildcardName(DatabaseOptions.DB_ENABLE, "db-enable-my-store") → "my-store"
     * getWildcardName(DatabaseOptions.DB_ENABLE, "kc.db-enable-my-store") → "my-store"
     * }</pre>
     *
     * @param option   the option containing a wildcard key
     * @param namedKey the fully qualified (resolved) configuration key
     * @return the part of {@code namedKey} that replaces the wildcard in {@code option.getKey()}, otherwise {@code null}
     */
    public static String getWildcardName(Option<?> option, String namedKey) {
        if (option == null || namedKey == null) {
            return null;
        }

        String key = namedKey.startsWith("kc.") ? namedKey.substring("kc.".length()) : namedKey;
        String prefix = getWildcardPrefix(option.getKey());
        return key.substring(prefix.length());
    }
}
