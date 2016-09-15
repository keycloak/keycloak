package org.keycloak.protocol.oidc.utils;

import java.util.function.Predicate;

/**
 * Attempts to determine whether the given wildcard url string matches an input string assumed to be a normal url.  Note that
 * this *only* currently evaluates protocol, host, port, and path.  Query parameters and fragments are not evaluated.
 *
 * E.X.
 * new WildcardUrlStringMatch(http://www.redhat.com/*).test("http://www.redhat.com/foo.html?lang=en) = true
 */
public class WildcardUrlStringMatches implements Predicate<String> {

    private final UrlString wildcardUrlString;

    public WildcardUrlStringMatches(final String wildcardString) {
        this.wildcardUrlString = new UrlString(wildcardString);
    }

    @Override
    public boolean test(final String regularString) {
        final UrlString regularUrlString = new UrlString(regularString);

        // first check on the protocol, host, and port
        boolean matches = regularUrlString.getProtocol().matches(toRegex(wildcardUrlString.getProtocol())) &&
                regularUrlString.getHost().matches(toRegex(wildcardUrlString.getHost())) &&
                regularUrlString.getPort().orElse("").matches(toRegex(wildcardUrlString.getPort().orElse("")));

        // Special Snowflake Case = when the path ends in 'foo/*', then 'foo' must also be accepted as valid
        final String wildcardPathString = wildcardUrlString.getPath().orElse("");
        final boolean specialSnowflakeCase = wildcardPathString.endsWith("/*");
        if (matches) {
            final String regularUrlPathString = regularUrlString.getPath().orElse("");
            matches = regularUrlPathString.matches(toRegex(wildcardPathString)) ||
                    (specialSnowflakeCase && regularUrlPathString.matches(toRegex(wildcardPathString.substring(0, wildcardPathString.length() - 2))));
        }

        // Now perform a naive check on the query params (if applicable)
        if (matches && !specialSnowflakeCase) {
            matches = regularUrlString.getQueryParams().orElse("").equals(wildcardUrlString.getQueryParams().orElse(""));
        }

        return matches;
    }

    /**
     * Converts a wildcard string to a regex string by escaping all of Java's special chars, the subsituting
     * '.*' wildcards for '*'.
     *
     * @param wildcardString Input string that may or may not contain wildcard characters
     * @return regex pattern string
     */
    public static String toRegex(final String wildcardString) {
        // Per http://docs.oracle.com/javase/tutorial/essential/regex/literals.html
        // the Java regex metachars are: <([{\^-=$!|]})?*+.>
        return "^" + wildcardString.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)‌​\\?\\+\\.\\>]", "\\\\$0").replaceAll("\\*", ".*") + "$";
    }
}
