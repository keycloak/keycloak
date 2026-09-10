package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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

    public ClientIdMetadataDocumentExecutor(KeycloakSession session, ClientIdMetadataDocumentExecutorFactoryProviderConfig providerConfig) {
        super(session, providerConfig);
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

        public Configuration() {
            super();
        }

        public boolean isOnlyAllowConfidentialClient() {
            return onlyAllowConfidentialClient;
        }

        public void setOnlyAllowConfidentialClient(boolean onlyAllowConfidentialClient) {
            this.onlyAllowConfidentialClient = onlyAllowConfidentialClient;
        }
    }

    // Client Metadata Validation Errors
    public static final String ERR_METADATA_NO_CONFIDENTIAL_CLIENT = "Invalid Client Metadata: confidential client is only allowed.";
    public static final String ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS = "Invalid Client Metadata: ether jwks or jwks_uri property is required.";

    @Override
    protected void validateClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        super.validateClientMetadata(clientIdURI, redirectUriURI, clientOIDC);

        // only accept a confidential client
        if (configuration.isOnlyAllowConfidentialClient()) {
            if (clientOIDC.getTokenEndpointAuthMethod() == null || !ALLOWED_ALGORITHMS.contains(clientOIDC.getTokenEndpointAuthMethod())) {
                getLogger().warn("not confidential client");
                throw invalidClientIdMetadata(ERR_METADATA_NO_CONFIDENTIAL_CLIENT);
            }
            if (clientOIDC.getJwksUri() == null && clientOIDC.getJwks() == null) {
                getLogger().warn("confidential client but jwks or jwks_uri properties is not included");
                throw invalidClientIdMetadata(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
            if (clientOIDC.getJwksUri() != null && clientOIDC.getJwks() != null) {
                getLogger().warn("confidential client but both jwks and jwks_uri properties are included");
                throw invalidClientIdMetadata(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
        }

    }

    protected static final Set<String> ALLOWED_ALGORITHMS = new LinkedHashSet<>(Arrays.asList(
            OIDCLoginProtocol.PRIVATE_KEY_JWT,
            OIDCLoginProtocol.TLS_CLIENT_AUTH
    ));
}
