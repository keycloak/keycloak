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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.keycloak.common.util.KeycloakUriBuilder;
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
    private final static Pattern UNSAFE_PATH_PATTERN = Pattern.compile(
            "(/|%2[fF]|%5[cC]|\\\\)(%2[eE]|\\.){2}(/|%2[fF]|%5[cC]|\\\\)|(/|%2[fF]|%5[cC]|\\\\)(%2[eE]|\\.){2}$");

    private static boolean areWildcardsAllowed(URI redirectUri) {
        // wildcars are only allowed if no user-info and no unsafe pattern in path
        return redirectUri.getRawUserInfo() == null
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
            } else if (validRedirect.endsWith("*") && !validRedirect.contains("?") && allowWildcards) {
                // strip off the query or fragment components - we don't check them when wildcards are effective
                int idx = redirect.indexOf('?');
                if (idx == -1) {
                    idx = redirect.indexOf('#');
                }
                String r = idx == -1 ? redirect : redirect.substring(0, idx);
                // strip off *
                int length = validRedirect.length() - 1;
                validRedirect = validRedirect.substring(0, length);
                if (r.startsWith(validRedirect)) return validRedirect;
                // strip off trailing '/'
                if (length - 1 > 0 && validRedirect.charAt(length - 1) == '/') length--;
                validRedirect = validRedirect.substring(0, length);
                if (validRedirect.equals(r)) return validRedirect;
            } else if (validRedirect.equals(redirect)) return validRedirect;
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
}
