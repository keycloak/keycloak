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
import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.ResolveRelative;

import org.jboss.logging.Logger;

/**
 * Utility class for validating client host values against a client's registered URLs.
 * Used to prevent SSRF attacks by ensuring that dynamic host values (like client_session_host)
 * only reference hosts that are already configured and trusted for the client.
 * <p>
 * The trust model is: an administrator configures a client policy with the
 * {@code secure-client-node-hostname} executor, which gates the {@code register-node} write
 * endpoint against an admin-curated regex allowlist. Hostnames that pass that gate are stored
 * in {@code registeredNodes}. This utility therefore treats {@code registeredNodes} as a
 * pre-validated set and uses it (together with the management URL) as the source of allowed
 * hosts for dynamic values such as {@code client_session_host}.
 * <p>
 * Resolves [CVE-2026-4874] Server-Side Request Forgery via OIDC token endpoint.
 */
public class ClientHostUtils {

    private static final Logger logger = Logger.getLogger(ClientHostUtils.class);

    /**
     * Validates that a hostname matches one of the client's registered nodes
     * or the host component of the administrator-configured management URL.
     * <p>
     * Registered nodes are trusted here; if the {@code secure-client-node-hostname}
     * executor is active, hostnames will have been validated at registration time.
     * Without that executor, any hostname may be present.
     *
     * @param hostname the hostname (or {@code host:port}) to validate
     * @param client   the client model containing registered nodes and management URL
     * @param session  the Keycloak session used for relative URL resolution
     * @return {@code true} if the hostname matches a registered node or the management URL host,
     *         {@code false} otherwise
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

        // Start with hosts from registered cluster nodes (pre-validated by the executor allowlist
        // at registration time) and add the host from the management URL as a fallback.
        List<String> allowedHosts = extractHostsFromRegisteredNodes(client);
        addHostFromUrl(client.getManagementUrl(), client, session, allowedHosts);

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

    /**
     * Returns the hostnames of all registered cluster nodes for the client.
     * These are considered pre-validated because they were accepted through the
     * {@code secure-client-node-hostname} executor allowlist at registration time.
     */
    private static List<String> extractHostsFromRegisteredNodes(ClientModel client) {
        List<String> allowedHosts = new ArrayList<>();
        Optional.ofNullable(client.getRegisteredNodes())
                .ifPresent(nodes -> nodes.keySet().forEach(allowedHosts::add));
        return allowedHosts;
    }
}
