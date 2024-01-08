package org.keycloak.services.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.OAuthErrorException;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class SecureRedirectUrisExecutor implements ClientPolicyExecutorProvider<SecureRedirectUrisExecutor.Configuration> {
    private Configuration configuration;

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        ClientRepresentation proposedClient;
        switch (context.getEvent()) {
            case REGISTER:
                proposedClient = ((AdminClientRegisterContext) context).getProposedClientRepresentation();
                break;
            case UPDATE:
                proposedClient = ((AdminClientUpdateContext) context).getProposedClientRepresentation();
                break;
            default:
                return;
        }

        for (String s : proposedClient.getRedirectUris()) {
            validate(s);
        }
    }

    void validate(String redirectUri) throws ClientPolicyException {
        UriValidation validation;
        try {
            validation = new UriValidation(redirectUri);
        } catch (URISyntaxException e) {
            throw invalidRedirectUri("Invalid Redirect Uri: invalid uri syntax");
        }

        if (configuration.allowOpen) {
            if (!"dev".equals(System.getProperty("kc.profile"))) {
                throw invalidRedirectUri("Invalid Redirect Uri: allow open redirect uris only in dev mode");
            }
            return;
        }

        if (!configuration.allowPrivateUseSchema && validation.isPrivateUseSchema()) {
            throw invalidRedirectUri("Invalid Redirect Uri: not allowed private use schema");
        }

        if (!configuration.allowLoopbackInterface && validation.isLoopbackInterface()) {
            throw invalidRedirectUri("Invalid Redirect Uri: not allowed loopback interface");
        }

        if (!configuration.allowHttp && validation.isHttp()) {
            throw invalidRedirectUri("Invalid Redirect Uri: not allowed HTTP");
        }

        if (!configuration.allowWildcardContextPath && validation.isWildcardContextPath()) {
            throw invalidRedirectUri("Invalid Redirect Uri: not allowed wildcard context path");
        }

        // if we allow loopback interface but the interface is not in permitted domains
        // there should allow loopback interface
        if (!configuration.getPermittedDomains().isEmpty() &&
            !validation.matchDomains(configuration.getPermittedDomains()) &&
            !validation.isLoopbackInterface()) {
            throw invalidRedirectUri("Invalid Redirect Uri: not allowed domain");
        }
    }

    static class UriValidation {
        private final URI uri;

        public UriValidation(String uriString) throws URISyntaxException {
            this.uri = new URI(uriString);
        }

        boolean isPrivateUseSchema() {
            return uri.isAbsolute() && !isHttp() && !isHttps();
        }

        boolean isLoopbackInterface() {
            try {
                InetAddress addr = InetAddress.getByName(uri.getHost());
                return addr.isLoopbackAddress();
            } catch (UnknownHostException e) {
                return false;
            }
        }

        boolean isHttp() {
            return "http".equals(uri.getScheme());
        }

        boolean isHttps() {
            return "https".equals(uri.getScheme());
        }

        boolean matchDomain(String domainPattern) {
            return uri.getHost().matches(domainPattern);
        }

        boolean matchDomains(List<String> permittedDomains) {
            return permittedDomains.stream().anyMatch(this::matchDomain);
        }

        boolean isWildcardContextPath() {
            return uri.getPath().startsWith("/*") || uri.getPath().startsWith("*");
        }
    }

    private static ClientPolicyException invalidRedirectUri(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setupConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public String getProviderId() {
        return SecureRedirectUrisExecutorFactory.PROVIDER_ID;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty(SecureRedirectUrisExecutorFactory.ALLOW_PRIVATE_USE_SCHEMA)
        private boolean allowPrivateUseSchema;
        @JsonProperty(SecureRedirectUrisExecutorFactory.ALLOW_LOOPBACK_INTERFACE)
        private boolean allowLoopbackInterface = true;
        @JsonProperty(SecureRedirectUrisExecutorFactory.ALLOW_HTTP)
        private boolean allowHttp;
        @JsonProperty(SecureRedirectUrisExecutorFactory.ALLOW_WILDCARD_CONTEXT_PATH)
        private boolean allowWildcardContextPath = true;
        @JsonProperty(SecureRedirectUrisExecutorFactory.ALLOW_OPEN)
        private boolean allowOpen;
        @JsonProperty(SecureRedirectUrisExecutorFactory.PERMITTED_DOMAINS)
        private List<String> permittedDomains = Collections.emptyList();

        public boolean isAllowPrivateUseSchema() {
            return allowPrivateUseSchema;
        }

        public void setAllowPrivateUseSchema(boolean allowPrivateUseSchema) {
            this.allowPrivateUseSchema = allowPrivateUseSchema;
        }

        public boolean isAllowLoopbackInterface() {
            return allowLoopbackInterface;
        }

        public void setAllowLoopbackInterface(boolean allowLoopbackInterface) {
            this.allowLoopbackInterface = allowLoopbackInterface;
        }

        public boolean isAllowHttp() {
            return allowHttp;
        }

        public void setAllowHttp(boolean allowHttp) {
            this.allowHttp = allowHttp;
        }

        public boolean isAllowWildcardContextPath() {
            return allowWildcardContextPath;
        }

        public void setAllowWildcardContextPath(boolean allowWildcardContextPath) {
            this.allowWildcardContextPath = allowWildcardContextPath;
        }

        public boolean isAllowOpen() {
            return allowOpen;
        }

        public void setAllowOpen(boolean allowOpen) {
            this.allowOpen = allowOpen;
        }

        public List<String> getPermittedDomains() {
            return permittedDomains;
        }

        public void setPermittedDomains(List<String> permittedDomains) {
            this.permittedDomains = permittedDomains;
        }
    }
}
