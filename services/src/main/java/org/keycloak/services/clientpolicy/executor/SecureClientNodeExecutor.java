/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientNodeRegistrationContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

public class SecureClientNodeExecutor implements ClientPolicyExecutorProvider<SecureClientNodeExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureClientNodeExecutor.class);

    private final KeycloakSession session;
    private List<Pattern> allowedPatterns;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty("hostname-allowed-patterns")
        private List<String> hostnameAllowedPatterns;

        public List<String> getHostnameAllowedPatterns() {
            return hostnameAllowedPatterns;
        }

        public void setHostnameAllowedPatterns(List<String> hostnameAllowedPatterns) {
            this.hostnameAllowedPatterns = hostnameAllowedPatterns;
        }
    }

    public SecureClientNodeExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.allowedPatterns = new ArrayList<>();
        List<String> patternsAsStrings = config.getHostnameAllowedPatterns();
        if (patternsAsStrings != null) {
            for (String p : patternsAsStrings) {
                if (p == null) {
                    logger.warn("Ignoring null regex pattern entry in configuration.");
                    continue;
                }
                if (p.length() > SecureClientNodeExecutorFactory.HOSTNAME_REGEX_MAX_LENGTH) {
                    logger.warnv("Ignoring regex pattern exceeding maximum length of {0} characters: {1}",
                            SecureClientNodeExecutorFactory.HOSTNAME_REGEX_MAX_LENGTH, p);
                    continue;
                }
                try {
                    this.allowedPatterns.add(Pattern.compile(p));
                } catch (PatternSyntaxException e) {
                    logger.warnv("Ignoring invalid regex pattern in configuration: {0}", p);
                }
            }
        }
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == ClientPolicyEvent.REGISTER_NODE
                && context instanceof ClientNodeRegistrationContext nodeCtx) {
            validateNodeHost(nodeCtx.getNodeHost());
        }
    }

    @Override
    public String getProviderId() {
        return SecureClientNodeExecutorFactory.PROVIDER_ID;
    }

    private void validateNodeHost(String nodeHost) throws ClientPolicyException {
        if (nodeHost == null || nodeHost.isBlank()) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname is empty.");
        }

        if (!nodeHost.equals(nodeHost.strip())) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname must not contain leading or trailing whitespace.");
        }

        if (allowedPatterns == null || allowedPatterns.isEmpty()) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "No valid hostname patterns configured in the executor. "
                            + "Node registration is blocked by policy.");
        }

        // Structurally validate and normalise using the RFC 3986 URI parser.
        // Returns the canonical host string for allowlist matching:
        //   "app.example.com"  → "app.example.com"
        //   "192.0.2.1"        → "192.0.2.1"
        //   "[2001:db8::1]"    → "2001:db8::1"  (brackets stripped; patterns match without brackets)
        // Bare IPv6 without brackets and port-suffixed values are rejected inside parseAndValidateHost.
        String canonical = parseAndValidateHost(nodeHost);

        if (canonical.length() > SecureClientNodeExecutorFactory.MAX_NODE_HOST_LENGTH) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname exceeds maximum allowed length.");
        }

        // Match the canonical form against the allowlist.
        // For bracketed IPv6 input "[2001:db8::1]", canonical is "2001:db8::1",
        // so patterns should be written without brackets (e.g. ^2001:db8::1$).
        boolean matchFound = allowedPatterns.stream()
                .anyMatch(p -> p.matcher(canonical).matches());

        if (!matchFound) {
            logger.warnv(
                    "Blocked node registration for hostname ''{0}'' - "
                            + "does not match any allowed pattern.",
                    nodeHost);
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname is not permitted by policy.");
        }
    }

    /**
     * Parses {@code nodeHost} using the JDK RFC 3986 URI parser and returns the
     * canonical host string for allowlist matching.
     *
     * <p>Accepted forms:
     * <ul>
     *   <li>DNS hostname — {@code app.example.com} → {@code app.example.com}</li>
     *   <li>IPv4 address — {@code 192.0.2.1} → {@code 192.0.2.1}</li>
     *   <li>Bracketed IPv6 — {@code [2001:db8::1]} → {@code 2001:db8::1} (brackets stripped for matching)</li>
     * </ul>
     *
     * <p>Bare IPv6 without brackets (e.g. {@code 2001:db8::1}) is rejected because
     * the JDK URI parser requires brackets to identify an IPv6 literal in an authority
     * component; without them {@code getHost()} returns {@code null}.
     *
     * <p>Port suffixes are rejected: {@code app.example.com:8443} and
     * {@code [2001:db8::1]:8443} both cause a {@link ClientPolicyException}.
     *
     * <p>Any value containing URI structural characters ({@code /}, {@code ?},
     * {@code #}, {@code @}, etc.) produces a parsed host that does not round-trip
     * to the original input and is rejected.
     */
    private static String parseAndValidateHost(String nodeHost) throws ClientPolicyException {
        try {
            // Mirror the approach in ClientHostUtils.extractHostname(): prepend "https://"
            // so the JDK RFC 3986 parser handles all host forms including bare IPv6.
            // URI(scheme, userInfo, host, port, path, query, fragment) is NOT used here
            // because its host parameter rejects bare IPv6 colons. The string constructor
            // is used instead so the parser itself decides what is a valid authority.
            java.net.URI uri = new java.net.URI("https://" + nodeHost);

            // Reject port suffixes.
            if (uri.getPort() != -1) {
                throw new ClientPolicyException(
                        OAuthErrorException.INVALID_REQUEST,
                        "Client cluster node hostname must not include a port number. "
                                + "Supply a bare hostname or IP address only "
                                + "(e.g. 'app.example.com', '192.0.2.1', or '2001:db8::1').");
            }

            String parsedHost = uri.getHost();
            if (parsedHost == null || parsedHost.isBlank()) {
                throw new ClientPolicyException(
                        OAuthErrorException.INVALID_REQUEST,
                        "Client cluster node hostname is not a valid DNS name or IP address.");
            }

            // Verify the input round-trips through the parser as a plain host with no
            // authority sub-components (userinfo, path, query, fragment).
            //
            // For a DNS name or IPv4, uri.getHost() returns the value as-is, so nodeHost equals parsedHost.
            // For bare IPv6 "2001:db8::1", uri.getHost() returns "2001:db8::1" and nodeHost equals it.
            // For bracketed IPv6 "[2001:db8::1]", uri.getHost() returns "[::1]" WITH the brackets
            // still present, so nodeHost equals parsedHost directly.
            // Anything with structural noise (path, fragment, userinfo) produces a parsedHost
            // that is a strict substring of nodeHost — the round-trip check catches all of these.
            if (!nodeHost.equals(parsedHost)) {
                throw new ClientPolicyException(
                        OAuthErrorException.INVALID_REQUEST,
                        "Client cluster node hostname contains URI structural characters "
                                + "and is not permitted.");
            }

            // For pattern matching, strip the brackets from IPv6 so patterns are written
            // as plain addresses (e.g. ^2001:db8::1$ rather than ^\[2001:db8::1\]$).
            if (parsedHost.startsWith("[") && parsedHost.endsWith("]")) {
                return parsedHost.substring(1, parsedHost.length() - 1);
            }

            return parsedHost;  // canonical form for DNS, IPv4, and bare IPv6
        } catch (java.net.URISyntaxException e) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname is not a valid DNS name or IP address.");
        }
    }
}
