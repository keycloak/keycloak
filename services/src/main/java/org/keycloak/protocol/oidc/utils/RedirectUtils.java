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

    private static final Set<String> FORBIDDEN_OIDC_PARAMS = Set.of(
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
            if (containsForbiddenOidcParameters(originalRedirect)){
                return null;
            }

            // check if the passed URI allows wildcards
            boolean allowWildcards = areWildcardsAllowed(originalRedirect);

            String r = redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(session, rootUrl, validRedirects);

            String valid = matchesRedirects(resolveValidRedirects, r, allowWildcards);

            if (valid == null && "http".equals(originalRedirect.getScheme()) && LOOPBACK_INTERFACES.contains(originalRedirect.getHost())) {
                String redirectWithDefaultPort = KeycloakUriBuilder.fromUri(originalRedirect).port(80).buildAsString();
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
                if (!valid.startsWith(scheme + ":") && !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
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

    private static boolean containsForbiddenOidcParameters(URI originalRedirect) {
        String query = originalRedirect.getRawQuery();
        if (query != null && !query.isEmpty()) {
            MultivaluedHashMap<String, String> params =UriUtils.decodeQueryString(query);
            for (String paramName : params.keySet()) {
                if (FORBIDDEN_OIDC_PARAMS.contains(paramName.toLowerCase(Locale.ROOT))) {
                    logger.warnf("Redirect URI rejected: contains forbidden OIDC parameter '%s' in query string: scheme=%s, host=%s, path=%s",
                            paramName,
                            originalRedirect.getScheme(),
                            originalRedirect.getHost(),
                            originalRedirect.getPath());
                    return true;
                }
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
    private final static Pattern HOST_LABEL_PATTERN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?");

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
            String matchedRedirect = matchesRedirect(validRedirect, redirect, allowWildcards);
            if (matchedRedirect != null) {
                return matchedRedirect;
            }
        }
        return null;
    }

    public static boolean matchesOrigin(Set<String> validOrigins, String origin) {
        if (origin == null || validOrigins == null || validOrigins.isEmpty()) {
            return false;
        }

        for (String validOrigin : validOrigins) {
            if ("*".equals(validOrigin) || validOrigin.equals(origin) || matchesWildcardHost(validOrigin, origin, false, true)) {
                return true;
            }
        }

        return false;
    }

    private static String getSingleValidRedirectUri(Collection<String> validRedirects) {
        if (validRedirects.size() != 1) return null;
        String validRedirect = validRedirects.iterator().next();
        if (hasWildcardHost(validRedirect)) {
            return null;
        }
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

    public static boolean hasWildcardHost(String uri) {
        URI parsedUri = toUri(uri);
        return hasWildcardHost(parsedUri);
    }

    public static boolean isValidWildcardHostPattern(String uri) {
        URI parsedUri = toUri(uri);
        return parsedUri != null && WildcardHostPattern.parse(parsedUri) != null;
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
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static String matchesRedirect(String validRedirect, String redirect, boolean allowWildcards) {
        if ("*".equals(validRedirect)) {
            // the valid redirect * is a full wildcard for http(s) even if the redirect URI does not allow wildcards
            return validRedirect;
        }

        if (matchesWildcardHost(validRedirect, redirect, allowWildcards, false)) {
            return validRedirect;
        }

        String validRedirectWildcard = allowWildcards && !isValidWildcardHostPattern(validRedirect) ? checkValidRedirectWildcard(validRedirect) : null;
        if (validRedirectWildcard != null) {
            if (matchesRedirectPathWildcard(validRedirectWildcard, redirect)) {
                return validRedirectWildcard;
            }
        } else if (validRedirect.equals(redirect)) {
            return validRedirect;
        }

        return null;
    }

    private static String checkValidRedirectWildcard(String validRedirect) {
        if (!validRedirect.endsWith("*") || validRedirect.contains("?") || validRedirect.contains("#")) {
            return null;
        }

        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(validRedirect, false);
        if (uriBuilder.getPath() != null) {
            return validRedirect;
        }

        if (uriBuilder.getAuthority() != null) {
            if (uriBuilder.getAuthority().equals("*") || uriBuilder.getAuthority().endsWith(":*")) {
                return validRedirect;
            }

            return validRedirect.substring(0, validRedirect.length() - 1) + "/*";
        }

        if (uriBuilder.getSsp() != null) {
            return validRedirect;
        }

        return null;
    }

    private static boolean matchesRedirectPathWildcard(String validRedirect, String redirect) {
        // strip off the query or fragment components - we don't check them when wildcards are effective
        int idx = redirect.indexOf('?');
        if (idx == -1) {
            idx = redirect.indexOf('#');
        }

        String r = idx == -1 ? redirect : redirect.substring(0, idx);
        int length = validRedirect.length() - 1;
        String redirectPrefix = validRedirect.substring(0, length);
        if (r.startsWith(redirectPrefix)) {
            return true;
        }

        if (length - 1 > 0 && redirectPrefix.charAt(length - 1) == '/') {
            redirectPrefix = redirectPrefix.substring(0, length - 1);
        }

        return redirectPrefix.equals(r);
    }

    private static boolean matchesWildcardHost(String validRedirect, String redirect, boolean allowContextPathWildcard, boolean requireOrigin) {
        URI validRedirectUri = toUri(validRedirect);
        URI redirectUri = toUri(redirect);
        if (!hasWildcardHost(validRedirectUri) || !isAbsoluteHttpUri(redirectUri) || redirectUri.getHost() == null) {
            return false;
        }

        WildcardHostPattern wildcardPattern = WildcardHostPattern.parse(validRedirectUri);
        if (wildcardPattern == null) {
            return false;
        }

        AuthorityParts redirectAuthority = AuthorityParts.parse(redirectUri.getRawAuthority());
        if (redirectAuthority == null || redirectAuthority.userInfo != null || redirectAuthority.host.indexOf('*') != -1) {
            return false;
        }

        if (!wildcardPattern.matches(redirectUri.getScheme(), redirectAuthority)) {
            return false;
        }

        if (requireOrigin) {
            return wildcardPattern.isOriginPattern()
                    && isEmpty(redirectUri.getRawPath())
                    && redirectUri.getRawQuery() == null
                    && redirectUri.getRawFragment() == null;
        }

        if (wildcardPattern.isPathWildcard()) {
            return allowContextPathWildcard
                    && wildcardPattern.rawQuery == null
                    && wildcardPattern.rawFragment == null
                    && matchesPathWildcard(wildcardPattern.rawPath, redirectUri.getRawPath());
        }

        return Objects.equals(wildcardPattern.rawPath, redirectUri.getRawPath())
                && Objects.equals(wildcardPattern.rawQuery, redirectUri.getRawQuery())
                && Objects.equals(wildcardPattern.rawFragment, redirectUri.getRawFragment());
    }

    private static boolean matchesPathWildcard(String validPath, String redirectPath) {
        String redirect = redirectPath == null ? "" : redirectPath;
        String redirectPrefix = validPath.substring(0, validPath.length() - 1);
        if (redirect.startsWith(redirectPrefix)) {
            return true;
        }

        if (!redirectPrefix.isEmpty() && redirectPrefix.charAt(redirectPrefix.length() - 1) == '/') {
            redirectPrefix = redirectPrefix.substring(0, redirectPrefix.length() - 1);
        }

        return redirectPrefix.equals(redirect);
    }

    private static boolean hasWildcardHost(URI uri) {
        return isAbsoluteHttpUri(uri) && uri.getRawAuthority() != null && uri.getRawAuthority().indexOf('*') != -1;
    }

    private static boolean isAbsoluteHttpUri(URI uri) {
        return uri != null && uri.isAbsolute() && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()));
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isValidHostLabel(String label) {
        return label != null && HOST_LABEL_PATTERN.matcher(label).matches();
    }

    private static final class WildcardHostPattern {
        private final String scheme;
        private final String rawPort;
        private final String firstLabelPrefix;
        private final String firstLabelSuffix;
        private final String[] remainingLabels;
        private final String rawPath;
        private final String rawQuery;
        private final String rawFragment;

        private WildcardHostPattern(String scheme, String rawPort, String firstLabelPrefix, String firstLabelSuffix,
                                    String[] remainingLabels, String rawPath, String rawQuery, String rawFragment) {
            this.scheme = scheme;
            this.rawPort = rawPort;
            this.firstLabelPrefix = firstLabelPrefix;
            this.firstLabelSuffix = firstLabelSuffix;
            this.remainingLabels = remainingLabels;
            this.rawPath = rawPath;
            this.rawQuery = rawQuery;
            this.rawFragment = rawFragment;
        }

        private static WildcardHostPattern parse(URI uri) {
            if (!isAbsoluteHttpUri(uri) || uri.getRawUserInfo() != null) {
                return null;
            }

            AuthorityParts authority = AuthorityParts.parse(uri.getRawAuthority());
            if (authority == null || authority.userInfo != null || authority.host.indexOf('[') != -1
                    || authority.port != null && authority.port.indexOf('*') != -1) {
                return null;
            }

            int wildcardIdx = authority.host.indexOf('*');
            if (wildcardIdx == -1 || wildcardIdx != authority.host.lastIndexOf('*')) {
                return null;
            }

            String[] labels = authority.host.split("\\.", -1);
            if (labels.length < 2) {
                return null;
            }

            String firstLabel = labels[0];
            if (firstLabel.indexOf('*') == -1 || firstLabel.indexOf('*') != firstLabel.lastIndexOf('*')) {
                return null;
            }

            if (!isValidHostLabel(firstLabel.replace('*', 'a'))) {
                return null;
            }

            for (int i = 1; i < labels.length; i++) {
                if (!isValidHostLabel(labels[i]) || labels[i].indexOf('*') != -1) {
                    return null;
                }
            }

            int firstLabelWildcardIdx = firstLabel.indexOf('*');
            return new WildcardHostPattern(uri.getScheme(), authority.port,
                    firstLabel.substring(0, firstLabelWildcardIdx),
                    firstLabel.substring(firstLabelWildcardIdx + 1),
                    Arrays.copyOfRange(labels, 1, labels.length),
                    uri.getRawPath(),
                    uri.getRawQuery(),
                    uri.getRawFragment());
        }

        private boolean matches(String actualScheme, AuthorityParts actualAuthority) {
            if (!scheme.equals(actualScheme) || !Objects.equals(rawPort, actualAuthority.port)) {
                return false;
            }

            String[] actualLabels = actualAuthority.host.split("\\.", -1);
            if (actualLabels.length != remainingLabels.length + 1) {
                return false;
            }

            for (String actualLabel : actualLabels) {
                if (actualLabel.isEmpty()) {
                    return false;
                }
            }

            if (!matchesFirstLabel(actualLabels[0])) {
                return false;
            }

            for (int i = 0; i < remainingLabels.length; i++) {
                if (!remainingLabels[i].equals(actualLabels[i + 1])) {
                    return false;
                }
            }

            return true;
        }

        private boolean isPathWildcard() {
            return rawPath != null && rawPath.endsWith("*") && rawPath.indexOf('*') == rawPath.length() - 1;
        }

        private boolean isOriginPattern() {
            return isEmpty(rawPath) && rawQuery == null && rawFragment == null;
        }

        private boolean matchesFirstLabel(String actualFirstLabel) {
            return !actualFirstLabel.isEmpty()
                    && actualFirstLabel.length() >= firstLabelPrefix.length() + firstLabelSuffix.length()
                    && actualFirstLabel.startsWith(firstLabelPrefix)
                    && actualFirstLabel.endsWith(firstLabelSuffix);
        }
    }

    private static final class AuthorityParts {
        private final String userInfo;
        private final String host;
        private final String port;

        private AuthorityParts(String userInfo, String host, String port) {
            this.userInfo = userInfo;
            this.host = host;
            this.port = port;
        }

        private static AuthorityParts parse(String rawAuthority) {
            if (rawAuthority == null || rawAuthority.isEmpty()) {
                return null;
            }

            String authority = rawAuthority;
            String userInfo = null;
            int atIdx = authority.lastIndexOf('@');
            if (atIdx != -1) {
                userInfo = authority.substring(0, atIdx);
                authority = authority.substring(atIdx + 1);
            }

            if (authority.isEmpty()) {
                return null;
            }

            String host;
            String port = null;
            if (authority.startsWith("[")) {
                int closingBracketIdx = authority.indexOf(']');
                if (closingBracketIdx == -1) {
                    return null;
                }

                host = authority.substring(0, closingBracketIdx + 1);
                String remainder = authority.substring(closingBracketIdx + 1);
                if (!remainder.isEmpty()) {
                    if (!remainder.startsWith(":")) {
                        return null;
                    }

                    port = remainder.substring(1);
                    if (port.isEmpty()) {
                        return null;
                    }
                }
            } else {
                int portSeparatorIdx = authority.lastIndexOf(':');
                if (portSeparatorIdx != -1) {
                    host = authority.substring(0, portSeparatorIdx);
                    port = authority.substring(portSeparatorIdx + 1);
                    if (host.isEmpty() || port.isEmpty()) {
                        return null;
                    }
                } else {
                    host = authority;
                }
            }

            return new AuthorityParts(userInfo, host, port);
        }
    }
}
