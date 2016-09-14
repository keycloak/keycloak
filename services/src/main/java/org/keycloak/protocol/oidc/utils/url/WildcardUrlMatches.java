package org.keycloak.protocol.oidc.utils.url;

import org.keycloak.protocol.oidc.utils.Regexify;

import java.net.URL;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Attempts to determine whether the given wildcard url string matches an input string assumed to be a normal url.  Note that
 * this *only* currently evaluates protocol, host, port, and path.  Query parameters and fragments are not evaluated.
 *
 * E.X.
 * new WildcardUrlStringMatch(http://www.redhat.com/*).test("http://www.redhat.com/foo.html?lang=en) = true
 */
public class WildcardUrlMatches implements Predicate<URL> {

    public static final String MATCH_ANYTHING_REGEX = "^.*$";
    private final URL baseCaseWithWildcards;

    public WildcardUrlMatches(final URL baseCaseWithWildcards) {
        this.baseCaseWithWildcards = baseCaseWithWildcards;
    }

    @Override
    public boolean test(final URL regularUrlString) {

        // first check on the protocol, host, and port
        boolean matches = regularUrlString.getProtocol().matches(Regexify.asString.apply(baseCaseWithWildcards.getProtocol())) &&
                regularUrlString.getHost().matches(Regexify.asString.apply(baseCaseWithWildcards.getHost())) &&
                String.valueOf(regularUrlString.getPort()).matches(Regexify.asString.apply(String.valueOf(baseCaseWithWildcards.getPort())));

        // Special Snowflake Case = when the path ends in 'foo/*', then 'foo' must also be accepted as valid
        final String wildcardPathString = Optional.ofNullable(baseCaseWithWildcards.getPath()).orElse("");
        final boolean specialSnowflakeCase = wildcardPathString.endsWith("/*");
        if (matches) {
            final String regularUrlPathString = Optional.ofNullable(regularUrlString.getPath()).orElse("");
            matches = regularUrlPathString.matches(Regexify.asString.apply(wildcardPathString)) ||
                    (specialSnowflakeCase && regularUrlPathString.matches(Regexify.asString.apply(wildcardPathString.substring(0, wildcardPathString.length() - 2))));
        }

        // Now perform a naive check on the query params (if applicable)
        if (matches && !specialSnowflakeCase) {
            matches = Optional.ofNullable(regularUrlString.getQuery()).orElse("").equals(Optional.ofNullable(baseCaseWithWildcards.getQuery()).orElse(""));
        }

        return matches;
    }
}
