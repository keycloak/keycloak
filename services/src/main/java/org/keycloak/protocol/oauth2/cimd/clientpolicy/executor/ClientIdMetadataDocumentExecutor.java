package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * The class is a concrete class of {@link AbstractClientIdMetadataDocumentExecutor}.
 * The class provide additional checks and processes, which are not determined by the CIMD and MCP specifications so these are keycloak-specific ones.
 *
 * <p>Client Metadata Validation:
 * The class provides the following policies:
 * <ul>
 *     <li>only accept a confidential client</li>
 *     <li>under the same domain as Server-side request forgery(SSRF) countermeasure: client_id, redirect_uri, client_uri, logo_uri, tos_uri,policy_uri, jwks_uri</li>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentExecutor extends AbstractClientIdMetadataDocumentExecutor<ClientIdMetadataDocumentExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientIdMetadataDocumentExecutor.class);

    protected Logger getLogger() {
        return logger;
    }

    public ClientIdMetadataDocumentExecutor(KeycloakSession session) {
        super(session);
    }

    @Override
    public String getProviderId() {
        return ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID;
    }

    @Override
    public Class<ClientIdMetadataDocumentExecutor.Configuration> getExecutorConfigurationClass() {
        return ClientIdMetadataDocumentExecutor.Configuration.class;
    }

    @Override
    public void setupConfiguration(ClientIdMetadataDocumentExecutor.Configuration config) {
        this.configuration = Optional.ofNullable(config).orElse(createDefaultConfiguration());
    }

    private ClientIdMetadataDocumentExecutor.Configuration createDefaultConfiguration() {
        return new ClientIdMetadataDocumentExecutor.Configuration();
    }

    public static class Configuration extends AbstractClientIdMetadataDocumentExecutor.Configuration {
        // additional settings
        // Client Metadata Validation
        @JsonProperty(ClientIdMetadataDocumentExecutorFactory.ONLY_ALLOW_CONFIDENTIAL_CLIENT)
        protected boolean onlyAllowConfidentialClient = false;
        @JsonProperty(ClientIdMetadataDocumentExecutorFactory.ALL_URIS_RESTRICT_SAME_DOMAIN)
        protected boolean allUrisRestrictSameDomain = false;

        public Configuration() {
            super();
        }

        public boolean isOnlyAllowConfidentialClient() {
            return onlyAllowConfidentialClient;
        }

        public void setOnlyAllowConfidentialClient(boolean onlyAllowConfidentialClient) {
            this.onlyAllowConfidentialClient = onlyAllowConfidentialClient;
        }

        public boolean isAllUrisRestrictSameDomain() {
            return allUrisRestrictSameDomain;
        }

        public void setAllUrisRestrictSameDomain(boolean allUrisRestrictSameDomain) {
            this.allUrisRestrictSameDomain = allUrisRestrictSameDomain;
        }
    }

    // Client Metadata Validation Errors
    public static final String ERR_METADATA_NO_CONFIDENTIAL_CLIENT = "Invalid Client Metadata: confidential client is only allowed.";
    public static final String ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS = "Invalid Client Metadata: ether jwks or jwks_uri property is required.";
    public static final String ERR_METADATA_NO_ALL_URIS_SAMEDOMAIN = "Invalid Client Metadata: some uri property is not under the same permitted domain";

    @Override
    protected void validateClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        super.validateClientMetadata(clientIdURI, redirectUriURI, clientOIDC);

        // only accept a confidential client
        if (configuration.isOnlyAllowConfidentialClient()) {
            if (clientOIDC.getTokenEndpointAuthMethod() == null || !ALLOWED_ALGORITHMS.contains(clientOIDC.getTokenEndpointAuthMethod())) {
                getLogger().warn("not confidential client");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT);
            }
            if (clientOIDC.getJwksUri() == null && clientOIDC.getJwks() == null) {
                getLogger().warn("confidential client but jwks or jwks_uri properties is not included");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
            if (clientOIDC.getJwksUri() != null && clientOIDC.getJwks() != null) {
                getLogger().warn("confidential client but both jwks and jwks_uri properties are included");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
        }

        // same domain : client_id, redirect_uri, client_uri, logo_uri, tos_uri,, policy_uri, jwks_uri
        List<String> trustedDomains = convertContentFilledList(getConfiguration().getAllowPermittedDomains());
        if (getConfiguration().isAllUrisRestrictSameDomain() && trustedDomains != null && !trustedDomains.isEmpty()) {
            List<String> l = Stream.of(clientOIDC.getClientId(), clientOIDC.getClientUri(), clientOIDC.getLogoUri(), clientOIDC.getTosUri(), clientOIDC.getPolicyUri(), clientOIDC.getJwksUri())
                    .filter(Objects::nonNull).toList();
            try {
                for (String s : l) {
                    URI u = new URI(s);
                    if (trustedDomains.stream().filter(i->!i.isBlank()).noneMatch(i -> checkTrustedDomain(u.getHost(), i) && checkTrustedDomain(redirectUriURI.getHost(), i))) {
                        getLogger().warnv("not under the same domain = {0}", u.getHost());
                        throw invalidClientId(ERR_METADATA_NO_ALL_URIS_SAMEDOMAIN);
                    }
                }
            } catch (URISyntaxException e) {
                getLogger().warnv("URI not resolved {0}}", e);
                throw invalidClientId(ERR_METADATA_NO_ALL_URIS_SAMEDOMAIN);
            }
        }

    }

    protected static final Set<String> ALLOWED_ALGORITHMS = new LinkedHashSet<>(Arrays.asList(
            OIDCLoginProtocol.PRIVATE_KEY_JWT,
            OIDCLoginProtocol.TLS_CLIENT_AUTH
    ));
}
