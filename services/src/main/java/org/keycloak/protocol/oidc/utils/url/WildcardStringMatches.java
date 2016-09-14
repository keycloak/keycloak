package org.keycloak.protocol.oidc.utils.url;

import java.net.URL;
import java.util.function.Predicate;

/**
 * Wrapper class to handle conversion of strings to URLs when attempting to find out what matches.  If any problems are encountered
 * constructing a URL from the given strings (either in the base case or predicate test case), then result will always be "false".
 *
 * @see org.keycloak.procotol.oidc.utils.url.WildcardUrlMatches for full description on what will and will not match.
 * @see java.net.URL for more info on URL construction
 */
public class WildcardStringMatches implements Predicate<String> {

    private Predicate<URL> urlStringMatches;

    /**
     * @param urlToMatch base case against which tests should be applied.  Is allowed to have wildcards in either part of the host name, or the path.
     */
    public WildcardStringMatches(final String urlToMatch) {
        try {
            urlStringMatches = new WildcardUrlMatches(new URL(urlToMatch));
        } catch (Exception e) {
            urlStringMatches = url -> false;
        }
    }

    @Override
    public boolean test(final String matchString) {
        try {
            return urlStringMatches.test(new URL(matchString));
        } catch (Exception e) {
            return false;
        }
    }
}
