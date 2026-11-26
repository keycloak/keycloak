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

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class SecureClientNodeExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-client-node-hostname";

    // Max length for an admin-configured regex pattern
    public static final int HOSTNAME_REGEX_MAX_LENGTH = 300;

    // Max length of a node hostname (RFC 1035)
    public static final int MAX_NODE_HOST_LENGTH = 253;


    private static final ProviderConfigProperty HOSTNAME_ALLOWED_PATTERNS_PROPERTY =
            new ProviderConfigProperty(
                    "hostname-allowed-patterns",
                    "Allowed Hostname Patterns",
                    "A node hostname is accepted only if it matches at least one of "
                                + "these regex patterns. Hostnames are normalised to lowercase before matching. "
                                + "Patterns should be written in lowercase. "
                                + "DNS hostnames and IPv4 addresses are matched after lowercasing. "
                                + "IPv6 addresses may be submitted with or without brackets (e.g. 2001:db8::1 or [2001:db8::1]); "
                                + "patterns must always be written without brackets (e.g. ^2001:db8::1$). "
                                + "Port suffixes are always rejected. "
                                + "Avoid nested quantifiers such as (x+)+ which can cause slow matching. "
                                + "If no valid patterns are configured, all registrations are blocked. "
                                + "Regex pattern cannot be longer than 300 characters, otherwise skipped.",
                    ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                    null);

    @Override
    public String getHelpText() {
        return "Validates client cluster node hostnames against allowed regex "
                + "patterns during node registration. Prevents SSRF via the legacy adapter node "
                + "registration endpoint.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(HOSTNAME_ALLOWED_PATTERNS_PROPERTY);
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureClientNodeExecutor(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
