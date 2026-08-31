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
import java.util.Locale;
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
            validateNodeHosts(nodeCtx.getNodeHosts());
        }
    }

    @Override
    public String getProviderId() {
        return SecureClientNodeExecutorFactory.PROVIDER_ID;
    }

    private void validateNodeHosts(List<String> nodeHosts) throws ClientPolicyException {
        if (nodeHosts == null || nodeHosts.isEmpty()) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname list is empty.");
        }

        if (allowedPatterns == null || allowedPatterns.isEmpty()) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "No valid hostname patterns configured in the executor. "
                            + "Node registration is blocked by policy.");
        }

        for (String nodeHost : nodeHosts) {
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

            String canonical = parseAndValidateHost(nodeHost);

            if (canonical.length() > SecureClientNodeExecutorFactory.MAX_NODE_HOST_LENGTH) {
                throw new ClientPolicyException(
                        OAuthErrorException.INVALID_REQUEST,
                        "Client cluster node hostname exceeds maximum allowed length.");
            }

            // Match the canonical form against the allowlist.
            boolean matchFound = allowedPatterns.stream()
                    .anyMatch(p -> p.matcher(canonical.toLowerCase(Locale.ROOT)).matches());

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
    }

    /**
     * Parses {@code nodeHost} using the JDK RFC 3986 URI parser and returns the
     * canonical host string for allowlist matching.
     *
     * <p>Accepted forms:
     * <ul>
     *   <li>DNS hostname — {@code app.example.com} → {@code app.example.com}</li>
     *   <li>IPv4 address — {@code 192.0.2.1} → {@code 192.0.2.1}</li>
     *   <li>IPv6 address — {@code [2001:db8::1]} → {@code 2001:db8::1} (brackets stripped for matching)</li>
     *   <li>IPv6 bare address — {@code 2001:db8::1} → {@code 2001:db8::1}</li>
     * </ul>
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
            // Bare IPv6 (e.g. "2001:db8::1") needs brackets for the JDK URI parser.
            String uriHost = nodeHost;
            if (looksLikeBareIpv6(nodeHost)) {
                uriHost = "[" + nodeHost + "]";
            }

            java.net.URI uri = new java.net.URI("https://" + uriHost);

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

            String strippedParsed = stripBrackets(parsedHost);
            String strippedUri    = stripBrackets(uriHost);

            if (!strippedUri.equalsIgnoreCase(strippedParsed)) {
                throw new ClientPolicyException(
                        OAuthErrorException.INVALID_REQUEST,
                        "Client cluster node hostname contains URI structural characters "
                                + "and is not permitted.");
            }

            return strippedParsed;
        } catch (java.net.URISyntaxException e) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname is not a valid DNS name or IP address.");
        }
    }

    private static boolean looksLikeBareIpv6(String host) {
        if (host.startsWith("[")) {
            return false;
        }
        int firstColon = host.indexOf(':');
        return firstColon >= 0 && host.indexOf(':', firstColon + 1) >= 0;
    }

    private static String stripBrackets(String s) {
        return (s.startsWith("[") && s.endsWith("]"))
                ? s.substring(1, s.length() - 1)
                : s;
    }
}
