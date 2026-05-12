/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.ResolveRelative;

import org.jboss.logging.Logger;

/**
 * Utility class for validating client host values against a client's registered URLs.
 * Used to prevent SSRF attacks by ensuring that dynamic host values (like client_session_host)
 * only reference hosts that are already configured and trusted for the client.
 * Resolves [CVE-2026-4874] Server-Side Request Forgery via OIDC token endpoint
 */
public class ClientHostUtils {

    private static final Logger logger = Logger.getLogger(ClientHostUtils.class);

    /**
     * Validates that a hostname matches one of the client's registered URLs
     * (redirect URIs, root URL, management URL, base URL).
     * This validation prevents SSRF attacks by ensuring that only hosts that
     * administrators have explicitly configured for the client can be used.
     *
     * @param hostname the hostname to validate
     * @param client the client model containing registered URLs
     * @param session the Keycloak session for URL resolution
     * @return true if the hostname matches a registered client URL, false otherwise
     */
    public static boolean isHostAllowedForClient(String hostname, ClientModel client, KeycloakSession session) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return false;
        }

        if (client == null) {
            return false;
        }

        // Extract just the hostname (strip port if present)
        String bareHostname = extractHostname(hostname);

        // Extract allowed hosts from client's configured URLs
        List<String> allowedHosts = new ArrayList<>();
        addHostFromUrl(client.getRootUrl(), client, session, allowedHosts);
        addHostFromUrl(client.getManagementUrl(), client, session, allowedHosts);
        addHostFromUrl(client.getBaseUrl(), client, session, allowedHosts);

        // Extract from redirect URIs - returns true if wildcard match found
        if (extractHostsFromRedirectUris(client, session, hostname, allowedHosts)) {
            return true;
        }

        // Check if the hostname matches any allowed host (case-insensitive)
        for (String allowedHost : allowedHosts) {
            if (allowedHost != null && allowedHost.equalsIgnoreCase(bareHostname)) {
                logger.debugf("Host '%s' matches allowed host '%s' for client '%s'",
                        hostname, allowedHost, client.getClientId());
                return true;
            }
        }
        logger.debugf("Host '%s' does not match any registered URL for client '%s'. Allowed hosts: %s",
                hostname, client.getClientId(), allowedHosts);
        return false;
    }

    private static String extractHostname(String hostPort) {
        if (hostPort == null) {
            return null;
        }

        try {
            // Prepend a scheme since input is hostname:port, not full URI
            return new URI("https://" + hostPort).getHost();
        } catch (URISyntaxException e) {
            logger.debugf("Could not parse hostname: %s", hostPort);
            return null;
        }
    }

    private static void addHostFromUri(String uri, List<String> hosts) {
        if (uri == null || uri.isEmpty()) {
            return;
        }

        try {
            String host = new URI(uri).getHost();
            if (host != null) {
                if (!hosts.contains(host)) {
                    hosts.add(host);
                }
            }
        } catch (URISyntaxException e) {
            logger.debugf("Could not extract host from URI: %s", uri);
        }
    }

    private static void addHostFromUrl(String url, ClientModel client, KeycloakSession session, List<String> hosts) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            String resolved = ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), url);
            String host = new URL(resolved).getHost();
            if (host != null) {
                if (!hosts.contains(host)) {
                    hosts.add(host);
                }
            }
        } catch (MalformedURLException e) {
            logger.debugf("Could not extract host from URL: %s", url);
        }
    }

    private static boolean extractHostsFromRedirectUris(ClientModel client, KeycloakSession session,
                                                        String hostname, List<String> allowedHosts) {
        Set<String> redirectUris = client.getRedirectUris();
        if (redirectUris == null || redirectUris.isEmpty()) {
            return false;
        }

        Set<String> resolvedRedirects = RedirectUtils.resolveValidRedirects(
                session, client.getRootUrl(), redirectUris
        );

        for (String redirectUri : resolvedRedirects) {
            if ("*".equals(redirectUri)) {
                logger.debugf("Client '%s' has wildcard redirect URI - allowing host '%s'",
                        client.getClientId(), hostname);
                return true;
            }
            addHostFromUri(redirectUri, allowedHosts);
        }
        return false;
    }

}
