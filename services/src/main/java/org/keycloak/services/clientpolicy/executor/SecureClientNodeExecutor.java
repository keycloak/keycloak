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
import org.keycloak.utils.ReservedCharValidator;

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

        if (containsPort(nodeHost)) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname must not include a port number. "
                            + "Supply a bare hostname only (e.g. 'app.example.com' or '[2001:db8::1]').");
        }

        if (allowedPatterns.isEmpty()) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "No valid hostname patterns configured in the executor. "
                            + "Node registration is blocked by policy.");
        }

        String normalised = nodeHost.strip();
        try {
            ReservedCharValidator.validate(normalised);
        }  catch (ReservedCharValidator.ReservedCharException e) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname uses reserved characters.");
        }

        if (normalised.length() > SecureClientNodeExecutorFactory.MAX_NODE_HOST_LENGTH) {
            throw new ClientPolicyException(
                    OAuthErrorException.INVALID_REQUEST,
                    "Client cluster node hostname exceeds maximum allowed length.");
        }

        boolean matchFound = allowedPatterns.stream()
                .anyMatch(p -> p.matcher(normalised).matches());

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

    private static boolean containsPort(String nodeHost) {
        if (nodeHost.startsWith("[")) {
            // Bracketed IPv6: "[::1]" is fine, "[::1]:8443" has a port
            int closingBracket = nodeHost.lastIndexOf(']');
            return closingBracket != -1 && closingBracket < nodeHost.length() - 1;
        }
        // DNS name or IPv4: any colon indicates a port (bare IPv6 is not valid here)
        return nodeHost.contains(":");
    }
}
