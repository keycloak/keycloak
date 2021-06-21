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

import org.jboss.logging.Logger;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.util.ResolveRelative;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RedirectUtils {

    private static final Logger logger = Logger.getLogger(RedirectUtils.class);

    public static String verifyRealmRedirectUri(KeycloakSession session, String redirectUri) {
        Set<String> validRedirects = getValidateRedirectUris(session);
        return verifyRedirectUri(session, null, redirectUri, validRedirects, true);
    }

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
        Set<String> resolveValidRedirects = new HashSet<>();
        for (String validRedirect : validRedirects) {
            if (validRedirect.startsWith("/")) {
                validRedirect = relativeToAbsoluteURI(session, rootUrl, validRedirect);
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
                resolveValidRedirects.add(validRedirect);
            } else {
                resolveValidRedirects.add(validRedirect);
            }
        }
        return resolveValidRedirects;
    }

    private static Set<String> getValidateRedirectUris(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return session.clientStorageManager().getAllRedirectUrisOfEnabledClients(realm).entrySet().stream()
          .filter(me -> me.getKey().isEnabled() && OIDCLoginProtocol.LOGIN_PROTOCOL.equals(me.getKey().getProtocol()) && !me.getKey().isBearerOnly() && (me.getKey().isStandardFlowEnabled() || me.getKey().isImplicitFlowEnabled()))
          .map(me -> resolveValidRedirects(session, me.getKey().getRootUrl(), me.getValue()))
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
    }

    public static String verifyRedirectUri(KeycloakSession session, String rootUrl, String redirectUri, Set<String> validRedirects, boolean requireRedirectUri) {
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        RealmModel realm = session.getContext().getRealm();

        if (redirectUri != null) {
            try {
                URI uri = URI.create(redirectUri);
                redirectUri = uri.normalize().toString();
            } catch (IllegalArgumentException cause) {
                logger.debug("Invalid redirect uri", cause);
                return null;
            } catch (Exception cause) {
                logger.debug("Unexpected error when parsing redirect uri", cause);
                return null;
            }
        }

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
            redirectUri = lowerCaseHostname(redirectUri);

            String r = redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(session, rootUrl, validRedirects);

            boolean valid = matchesRedirects(resolveValidRedirects, r);

            if (!valid && (r.startsWith(Constants.INSTALLED_APP_URL) || r.startsWith(Constants.INSTALLED_APP_LOOPBACK)) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(resolveValidRedirects, r);
            }
            if (valid && redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(session, rootUrl, redirectUri);
            }
            redirectUri = valid ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }

    private static String lowerCaseHostname(String redirectUri) {
        int n = redirectUri.indexOf('/', 7);
        if (n == -1) {
            return redirectUri.toLowerCase();
        } else {
            return redirectUri.substring(0, n).toLowerCase() + redirectUri.substring(n);
        }
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

    private static boolean matchesRedirects(Set<String> validRedirects, String redirect) {
        for (String validRedirect : validRedirects) {
            if (validRedirect.contains("*") && !validRedirect.contains("?")) {
                // strip off the query component - we don't check them when wildcards are effective
                String r = redirect.contains("?") ? redirect.substring(0, redirect.indexOf("?")) : redirect;

                if (equalsByWildcards(r, validRedirect)) return true;
                int length = validRedirect.length();
                if (validRedirect.charAt(length - 1) == '/') length--;
                // strip off trailing '/'
                validRedirect = validRedirect.substring(0, length);
                if (validRedirect.equals(r)) return true;
            } else if (validRedirect.equals(redirect)) return true;
        }
        return false;
    }

    private static boolean equalsByWildcards(String str, String pattern) {
        int strLength = str.length();
        int wildcardsFilterLength = pattern.length();

        // lookup table for storing results of subproblems
        boolean[][] lookup = new boolean[strLength + 1][wildcardsFilterLength + 1];

        // initialise lookup table to false
        for (int i = 0; i < strLength + 1; i++)
            Arrays.fill(lookup[i], false);

        // empty pattern can match with empty string
        lookup[0][0] = true;

        // Only '*' can match with empty string
        for (int j = 1; j <= wildcardsFilterLength; j++)
            if (pattern.charAt(j - 1) == '*')
                lookup[0][j] = lookup[0][j - 1];

        // fill the table in bottom-up fashion
        for (int i = 1; i <= strLength; i++)
        {
            for (int j = 1; j <= wildcardsFilterLength; j++)
            {
                // Two cases if we see a '*'
                // a) We ignore '*'' character and move to next  character in the pattern,
                //     i.e., '*' indicates an empty sequence.
                // b) '*' character matches with its character in input
                if (pattern.charAt(j - 1) == '*')
                    lookup[i][j] = lookup[i][j - 1] || lookup[i - 1][j];

                // Current characters are considered as matching if characters actually match
                else if (str.charAt(i - 1) == pattern.charAt(j - 1))
                    lookup[i][j] = lookup[i - 1][j - 1];
                // If characters don't match
                else
                    lookup[i][j] = false;
            }
        }
        return lookup[strLength][wildcardsFilterLength];
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

    private static String getFirstValidRedirectUri(Collection<String> validRedirects) {
        final String redirectUri = validRedirects.stream().findFirst().orElse(null);
        return (redirectUri != null) ? validateRedirectUriWildcard(redirectUri) : null;
    }

    public static String getFirstValidRedirectUri(KeycloakSession session, String rootUrl, Set<String> validRedirects) {
        return getFirstValidRedirectUri(resolveValidRedirects(session, rootUrl, validRedirects));
    }
}
