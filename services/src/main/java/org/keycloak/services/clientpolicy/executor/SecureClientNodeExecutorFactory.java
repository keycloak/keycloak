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
                                + "these regex patterns. Matching is case-sensitive and is performed "
                                + "against the full hostname value (excluding any port suffix). "
                                + "For IPv6 addresses include the "
                                + "surrounding brackets if your adapter sends them (e.g. \\[::1\\]). "
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
