/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.services.util.ResolveRelative;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RedirectUtils {

    public static final Set<String> LOOPBACK_INTERFACES = new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "[::1]"));

    static final Set<String> FORBIDDEN_OIDC_PARAMS = Set.of(
                                                                     OAuth2Constants.CODE,
                                                                     OAuth2Constants.ID_TOKEN,
                                                                     OAuth2Constants.ACCESS_TOKEN,
                                                                     OAuth2Constants.TOKEN_TYPE,
                                                                     OAuth2Constants.EXPIRES_IN,
                                                                     OAuth2Constants.STATE,
                                                                     OAuth2Constants.ISSUER,
                                                                     OAuth2Constants.ERROR,
                                                                     OAuth2Constants.ERROR_DESCRIPTION,
                                                                     OAuth2Constants.SESSION_STATE,
                                                                     OAuth2Constants.RESPONSE,
                                                                     Constants.KC_ACTION,
                                                                     Constants.KC_ACTION_STATUS
                                                                   );

    private static final Logger logger = Logger.getLogger(RedirectUtils.class);

    public static String verifyRedirectUri(KeycloakSession session, String redirectUri, ClientModel client) {
        return verifyRedirectUri(session, redirectUri, client, true);
    }

    public static String verifyRedirectUri(KeycloakSession session, String redirectUri, ClientModel client, boolean requireRedirectUri) {
        if (client != null)
            return verifyRedirectUri(session, client.getRootUrl(), redirectUri, client.getRedirectUris(), requireRedirectUri);
        return null;
    }

    public static Set<String> resolveValidRedirects(KeycloakSession session, String rootUrl, Set<String> validRedirects) {
        // If the valid redirect URI is relative (no scheme, host, port) then use the request's scheme, host, and port
        // the set is ordered by length to get the longest match first
        Set<String> resolveValidRedirects = new TreeSet<>((String s1, String s2) -> s1.length() == s2.length()? s1.compareTo(s2) : s1.length() < s2.length()? 1 : -1);
        for (String validRedirect : validRedirects) {
            if (validRedirect.startsWith("/")) {
                validRedirect = relativeToAbsoluteURI(session, rootUrl, validRedirect);
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
            }
            resolveValidRedirects.add(validRedirect);
        }
        return resolveValidRedirects;
    }

    public static String verifyRedirectUri(KeycloakSession session, String rootUrl, String redirectUri, Set<String> validRedirects, boolean requireRedirectUri) {
        return verifyRedirectUri(session, rootUrl, redirectUri, validRedirects, requireRedirectUri, FORBIDDEN_OIDC_PARAMS);
    }

    public static String verifyRedirectUri(KeycloakSession session, String rootUrl, String redirectUri, Set<String> validRedirects, boolean requireRedirectUri, Set<String> forbiddenParams) {
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        RealmModel realm = session.getContext().getRealm();

        if (redirectUri == null) {
            if (!requireRedirectUri) {
                redirectUri = getSingleValidRedirectUri(validRedirects);
            }

            if (redirectUri == null) {
                logger.debug("No Redirect URI parameter specified");
                return null;
            }
        } else if (validRedirects.isEmpty()) {
            logger.debug("No Redirect URIs supplied");
            redirectUri = null;
        } else {
            URI originalRedirect = toUri(redirectUri);
            if (originalRedirect == null) {
                // invalid URI passed as redirectUri
                return null;
            }

            // Check for HTTP Parameter Pollution - forbidden OIDC response parameters in redirect URI
            if (containsForbiddenOidcParameters(originalRedirect, forbiddenParams)) {
                return null;
            }

            // check if the passed URI allows wildcards
            boolean allowWildcards = areWildcardsAllowed(originalRedirect);

            String r = redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(session, rootUrl, validRedirects);

            String valid = matchesRedirects(resolveValidRedirects, r, allowWildcards);

            String redirectHost = originalRedirect.getHost();
            if (valid == null && "http".equalsIgnoreCase(originalRedirect.getScheme())
                    && redirectHost != null
                    && LOOPBACK_INTERFACES.contains(redirectHost.toLowerCase(Locale.ROOT))) {
                // Normalize scheme so default port 80 is omitted by KeycloakUriBuilder.
                String redirectWithDefaultPort = KeycloakUriBuilder.fromUri(originalRedirect)
                        .scheme(originalRedirect.getScheme().toLowerCase(Locale.ROOT))
                        .port(80)
                        .buildAsString();
                valid = matchesRedirects(resolveValidRedirects, redirectWithDefaultPort, allowWildcards);
            }

            if (valid != null && !originalRedirect.isAbsolute()) {
                // return absolute if the original URI is relative
                if (!redirectUri.startsWith("/")) {
                    redirectUri = "/" + redirectUri;
                }
                redirectUri = relativeToAbsoluteURI(session, rootUrl, redirectUri);
            }

            String scheme = originalRedirect.getScheme();
            if (valid != null && scheme != null) {
                // check the scheme is valid, it should be http(s) or explicitly allowed by the validation
                if (!schemeEqualsPrefix(valid, scheme) && !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                    logger.debugf("Invalid URI because scheme is not allowed: %s", redirectUri);
                    valid = null;
                }
            }

            redirectUri = valid != null ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }

    private static boolean containsForbiddenOidcParameters(URI originalRedirect, Set<String> forbiddenParams) {
        if (forbiddenParams == null || forbiddenParams.isEmpty()) {
            return false;
        }
        //Check query string
        String query = originalRedirect.getRawQuery();
        if (hasForbiddenParams(query, forbiddenParams, "query", originalRedirect)) {
            return true;
        }

        // Check fragment (response_mode=fragment puts OIDC params here)
        String fragment = originalRedirect.getRawFragment();
        return hasForbiddenParams(fragment, forbiddenParams, "fragment", originalRedirect);
    }

    private static boolean hasForbiddenParams(String paramString, Set<String> forbiddenParams, String component, URI originalRedirect) {
        if (paramString == null || paramString.isEmpty()) {
            return false;
        }
        MultivaluedHashMap<String, String> params = UriUtils.decodeQueryString(paramString);
        for (String paramName : params.keySet()) {
            if (forbiddenParams.stream().map(param -> param.toLowerCase(Locale.ROOT))
                    .anyMatch(paramName.toLowerCase(Locale.ROOT)::equals)) {
                logger.warnf("Redirect URI rejected: contains forbidden OIDC parameter '%s' in %s: scheme=%s, host=%s, path=%s",
                        paramName, component, originalRedirect.getScheme(), originalRedirect.getHost(), originalRedirect.getPath());
                return true;
            }
        }
        return false;
    }

    private static URI toUri(String redirectUri) {
        URI uri = null;
        if (redirectUri != null) {
            try {
                uri = URI.create(redirectUri);
            } catch (IllegalArgumentException cause) {
                logger.debugf(cause, "Invalid redirect uri %s", redirectUri);
            } catch (Exception cause) {
                logger.debugf(cause, "Unexpected error when parsing redirect uri %s", redirectUri);
            }
        }
        return uri;
    }

    // any access to parent folder /../ is unsafe with or without encoding
    //   <sep>             = / | %2F | %5C | \
    //   <dots>            = "..", including %2E and %252E (double-encoded) variants
    //   <terminator>      = / | %2F | %5C | \ | ; | %3B | %09 | %0A | %0D | %00 | end-of-input
    private final static Pattern UNSAFE_PATH_PATTERN = Pattern.compile(
            "(/|%2[fF]|%5[cC]|\\\\)(%2[eE]|%252[eE]|\\.){2}(/|%2[fF]|%5[cC]|\\\\|;|%3[bB]|%09|%0[aAdD]|%00|$)");

    private static boolean areWildcardsAllowed(URI redirectUri) {
        // wildcars are only allowed if no user-info and no unparsed authority and no unsafe pattern in path
        return redirectUri.getRawUserInfo() == null
                && !(redirectUri.getRawAuthority() != null && redirectUri.getRawUserInfo() == null && redirectUri.getHost() == null && redirectUri.getPort() == -1)
                && (redirectUri.getRawPath() == null || !UNSAFE_PATH_PATTERN.matcher(redirectUri.getRawPath()).find());
    }

    private static String relativeToAbsoluteURI(KeycloakSession session, String rootUrl, String relative) {
        if (rootUrl != null) {
            rootUrl = ResolveRelative.resolveRootUrl(session, rootUrl);
        }

        if (rootUrl == null || rootUrl.isEmpty()) {
            rootUrl = UriUtils.getOrigin(session.getContext().getUri().getBaseUri());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(rootUrl);
        sb.append(relative);
        return sb.toString();
    }

    // return the String that matched the redirect or null if not matched
    private static String matchesRedirects(Set<String> validRedirects, String redirect, boolean allowWildcards) {
        logger.tracef("matchesRedirects: redirect URL to check: %s, allow wildcards: %b, Configured valid redirect URLs: %s", redirect, allowWildcards, validRedirects);
        for (String validRedirect : validRedirects) {
            if ("*".equals(validRedirect)) {
                // the valid redirect * is a full wildcard for http(s) even if the redirect URI does not allow wildcards
                return validRedirect;
            } else {
                String validRedirectWildcard = allowWildcards ? checkValidRedirectWildcard(validRedirect) : null;
                if (validRedirectWildcard != null) {
                    // strip off the query or fragment components - we don't check them when wildcards are effective
                    int idx = redirect.indexOf('?');
                    if (idx == -1) {
                        idx = redirect.indexOf('#');
                    }
                    String r = idx == -1 ? redirect : redirect.substring(0, idx);
                    // strip off *
                    int length = validRedirectWildcard.length() - 1;
                    validRedirectWildcard = validRedirectWildcard.substring(0, length);
                    if (redirectUriStartsWith(r, validRedirectWildcard)) {
                        return validRedirectWildcard;
                    }
                    // strip off trailing '/'
                    if (length - 1 > 0 && validRedirectWildcard.charAt(length - 1) == '/') {
                        length--;
                    }
                    validRedirectWildcard = validRedirectWildcard.substring(0, length);
                    if (redirectUriEquals(validRedirectWildcard, r)) {
                        return validRedirectWildcard;
                    }
                } else if (redirectUriEquals(validRedirect, redirect)) {
                    return validRedirect;
                }
            }
        }
        return null;
    }

    private static boolean schemeEqualsPrefix(String uri, String scheme) {
        String prefix = scheme + ":";
        return uri.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean redirectUriEquals(String configured, String redirect) {
        if (configured.equals(redirect)) {
            return true;
        }
        URI configuredUri = toUri(configured);
        URI redirectUri = toUri(redirect);
        if (configuredUri == null || redirectUri == null) {
            return false;
        }
        if (!UriUtils.schemeHostAndPortEqual(configuredUri, redirectUri)) {
            return false;
        }
        if (configuredUri.isOpaque() || redirectUri.isOpaque()) {
            if (configuredUri.isOpaque() != redirectUri.isOpaque()) {
                return false;
            }
            return Objects.equals(configuredUri.getRawSchemeSpecificPart(), redirectUri.getRawSchemeSpecificPart())
                    && Objects.equals(configuredUri.getRawFragment(), redirectUri.getRawFragment());
        }
        return UriUtils.rawUserInfoEqual(configuredUri, redirectUri)
                && Objects.equals(configuredUri.getRawPath(), redirectUri.getRawPath())
                && Objects.equals(configuredUri.getRawQuery(), redirectUri.getRawQuery())
                && Objects.equals(configuredUri.getRawFragment(), redirectUri.getRawFragment());
    }

    private static boolean redirectUriStartsWith(String redirect, String prefix) {
        if (redirect.startsWith(prefix)) {
            return true;
        }
        URI redirectUri = toUri(redirect);
        if (redirectUri == null) {
            return false;
        }
        URI prefixUri = toUri(prefix);
        if (prefixUri == null) {
            return redirectUriStartsWithUnparseablePrefix(redirect, redirectUri, prefix);
        }
        // Port wildcards strip to an authority ending in ':' (e.g. https://example.com:).
        // Do not treat path prefixes that merely end with ':' the same way.
        String prefixAuthority = prefixUri.getRawAuthority();
        if (prefixAuthority != null && prefixAuthority.endsWith(":")) {
            if (!UriUtils.schemeAndHostEqual(redirectUri, prefixUri)) {
                return false;
            }
            // Port wildcard requires an explicit port in the redirect (reject host-only URIs).
            if (!UriUtils.hasExplicitPort(redirectUri)) {
                return false;
            }
        } else if (!UriUtils.schemeHostAndPortEqual(redirectUri, prefixUri)) {
            return false;
        }
        if (!UriUtils.rawUserInfoEqual(redirectUri, prefixUri)) {
            return false;
        }
        if (redirectUri.isOpaque() || prefixUri.isOpaque()) {
            String redirectSsp = redirectUri.getRawSchemeSpecificPart() != null ? redirectUri.getRawSchemeSpecificPart() : "";
            String prefixSsp = prefixUri.getRawSchemeSpecificPart() != null ? prefixUri.getRawSchemeSpecificPart() : "";
            return redirectSsp.startsWith(prefixSsp);
        }
        String redirectPath = redirectUri.getRawPath() != null ? redirectUri.getRawPath() : "";
        String prefixPath = prefixUri.getRawPath() != null ? prefixUri.getRawPath() : "";
        return redirectPath.startsWith(prefixPath);
    }

    private static boolean redirectUriStartsWithUnparseablePrefix(String redirect, URI redirectUri, String prefix) {
        int colonIdx = prefix.indexOf(':');
        if (colonIdx < 0) {
            return false;
        }
        String prefixScheme = prefix.substring(0, colonIdx);
        String prefixSsp = prefix.substring(colonIdx + 1);
        String redirectScheme = redirectUri.getScheme();
        if (redirectScheme == null || !redirectScheme.equalsIgnoreCase(prefixScheme)) {
            return false;
        }
        if (redirectUri.isOpaque()) {
            String redirectSsp = redirectUri.getRawSchemeSpecificPart() != null ? redirectUri.getRawSchemeSpecificPart() : "";
            return redirectSsp.startsWith(prefixSsp);
        }
        if (prefixSsp.isEmpty()) {
            return true;
        }
        if (!redirect.regionMatches(true, 0, prefix, 0, colonIdx + 1)) {
            return false;
        }
        return redirect.substring(colonIdx + 1).startsWith(prefixSsp);
    }

    private static String checkValidRedirectWildcard(String validRedirect) {
        if (!validRedirect.endsWith("*") || validRedirect.contains("?") || validRedirect.contains("#")) {
            return null; // no wildcard as before
        }
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(validRedirect, false);
        if (uriBuilder.getPath() != null) {
            return validRedirect; // wildcard valid on path
        }
        if (uriBuilder.getAuthority() != null) {
            if (uriBuilder.getAuthority().equals("*") || uriBuilder.getAuthority().endsWith(":*")) {
                return validRedirect; // on authority just full wildcard or on port
            } else {
                // treat the wildcard after the authority
                validRedirect = validRedirect.substring(0, validRedirect.length() - 1);
                validRedirect = validRedirect + "/*";
                return validRedirect;
            }
        }
        if (uriBuilder.getSsp() != null) {
            return validRedirect; // wildcard valid on SSP
        }
        return null;
    }

    private static String getSingleValidRedirectUri(Collection<String> validRedirects) {
        if (validRedirects.size() != 1) return null;
        String validRedirect = validRedirects.iterator().next();
        return validateRedirectUriWildcard(validRedirect);
    }

    public static String validateRedirectUriWildcard(String redirectUri) {
        if (redirectUri == null)
            return null;

        int idx = redirectUri.indexOf("/*");
        if (idx > -1) {
            redirectUri = redirectUri.substring(0, idx);
        }
        return redirectUri;
    }

    public static Set<String> resolveUrlsWithRedirects(KeycloakSession session, List<String> origUrls,
                                                       String rootUrl, List<String> redirectUris, boolean returnAsOrigins) {

        Set<String> refactoredUrls = (origUrls != null) ? new HashSet<>(origUrls) : new HashSet<>();
        if (refactoredUrls.contains(Constants.INCLUDE_REDIRECTS)) {
            refactoredUrls.remove(Constants.INCLUDE_REDIRECTS);

            Set<String> redirectsToProcess = (redirectUris != null) ? new HashSet<>(redirectUris) : Collections.emptySet();
            for (String redirectUri : resolveValidRedirects(session, rootUrl, redirectsToProcess)) {
                if (isValidScheme(redirectUri)) {
                    if (returnAsOrigins) {
                        refactoredUrls.add(UriUtils.getOrigin(redirectUri));
                    } else {
                        refactoredUrls.add(redirectUri);
                    }
                }
            }
        }
        return refactoredUrls;
    }

    private static boolean isValidScheme(String url) {
        return url != null
                && (url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8));
    }
}
