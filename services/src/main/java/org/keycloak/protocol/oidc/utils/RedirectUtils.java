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

import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RedirectUtils {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static String verifyRealmRedirectUri(final UriInfo uriInfo, final String redirectUri, final RealmModel realm) {
        final Set<String> validRedirects = getValidateRedirectUris(uriInfo, realm);
        return verifyRedirectUri(uriInfo, null, redirectUri, realm, validRedirects);
    }

    public static String verifyRedirectUri(final UriInfo uriInfo, final String redirectUri, final RealmModel realm, final ClientModel client) {
        final Set<String> validRedirects = client.getRedirectUris();
        return verifyRedirectUri(uriInfo, client.getRootUrl(), redirectUri, realm, validRedirects);
    }

    public static Set<String> resolveValidRedirects(final UriInfo uriInfo, final String rootUrl, final Set<String> validRedirects) {
        // If the valid redirect URI is relative (no scheme, host, port) then use the request's scheme, host, and port
        final Set<String> resolveValidRedirects = new HashSet<>();
        for (String validRedirect : validRedirects) {
            if (validRedirect.startsWith("/")) {
                validRedirect = relativeToAbsoluteURI(uriInfo, rootUrl, validRedirect);
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
                resolveValidRedirects.add(validRedirect);
            } else { // already full url
                resolveValidRedirects.add(validRedirect);
            }
        }
        return resolveValidRedirects;
    }

    private static Set<String> getValidateRedirectUris(final UriInfo uriInfo, final RealmModel realm) {
        final Set<String> redirects = new HashSet<>();
        for (final ClientModel client : realm.getClients()) {
            redirects.addAll(resolveValidRedirects(uriInfo, client.getRootUrl(), client.getRedirectUris()));
        }
        return redirects;
    }

    private static String verifyRedirectUri(final UriInfo uriInfo, final String rootUrl, String redirectUri, final RealmModel realm, final Set<String> validRedirects) {
        if (redirectUri == null) {
            logger.debug("No Redirect URI parameter specified");
            return null;
        } else if (validRedirects.isEmpty()) {
            logger.debug("No Redirect URIs supplied");
            redirectUri = null;
        } else {
            redirectUri = lowerCaseHostname(redirectUri);
            if (redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(uriInfo, rootUrl, redirectUri);
            }

            String r = redirectUri;
            final Set<String> resolveValidRedirects = resolveValidRedirects(uriInfo, rootUrl, validRedirects);

            boolean valid = matchesRedirects(resolveValidRedirects, r);

            if (!valid && r.startsWith(Constants.INSTALLED_APP_URL) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                final StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(resolveValidRedirects, r);
            }
            redirectUri = valid ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }

    private static String lowerCaseHostname(final String redirectUri) {
        final int n = redirectUri.indexOf('/', 7);
        if (n == -1) {
            return redirectUri.toLowerCase();
        } else {
            return redirectUri.substring(0, n).toLowerCase() + redirectUri.substring(n);
        }
    }

    private static String relativeToAbsoluteURI(final UriInfo uriInfo, String rootUrl, String relative) {
        if (rootUrl == null || rootUrl.isEmpty()) {
            final URI baseUri = uriInfo.getBaseUri();
            String uri = baseUri.getScheme() + "://" + baseUri.getHost();
            if (baseUri.getPort() != -1) {
                uri += ":" + baseUri.getPort();
            }
            rootUrl = uri;
        }
        relative = rootUrl + relative;
        return relative;
    }

    private static boolean matchesRedirects(final Set<String> validRedirects, final String requestedRedirect) {
        for (final String validRedirect : validRedirects) {
            if (validRedirect.equals(requestedRedirect)) {
                return true;
            } else if (validRedirect.contains("*") && new WildcardUrlStringMatches(validRedirect).test(requestedRedirect)) {
                return true;
            }
        }
        return false;
    }

}
