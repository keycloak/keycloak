package org.keycloak.services.clientpolicy.executor;

import java.net.URI;
import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentExecutor extends AbstractClientIdMetadataDocumentExecutor<ClientIdMetadataDocumentExecutor.Configuration>  {

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
    protected URI verifyClientId(final String clientId) throws ClientPolicyException {
        URI clientIdUri = super.verifyClientId(clientId);
        // additional checks
        return clientIdUri;
    }

    @Override
    protected URI verifyAuthorizationRequest(PreAuthorizationRequestContext preAuthorizationRequestContext) throws ClientPolicyException {
        URI redirectUri = super.verifyAuthorizationRequest(preAuthorizationRequestContext);
        // additional checks
        return redirectUri;
    }

    @Override
    protected void validateClientId(final URI clientUri) throws ClientPolicyException {
        super.validateClientId(clientUri);
        // additional checks
    }

    @Override
    protected OIDCClientRepresentationWithCacheControl fetchClientMetadata(final URI clientIdUri) throws ClientPolicyException {
        OIDCClientRepresentationWithCacheControl oidcClientWithCacheControl = super.fetchClientMetadata(clientIdUri);
        // overriding or additional checks
        return oidcClientWithCacheControl;
    }

    @Override
    protected URI verifyClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        URI clientIdURIfromMetadata = super.verifyClientMetadata(clientIdURI, redirectUriURI, clientOIDC);
        // additional checks
        return clientIdURIfromMetadata;
    }

    @Override
    protected void validateClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        super.validateClientMetadata(clientIdURI, redirectUriURI, clientOIDC);

        // only accept a confidential client
        if (configuration.isOnlyAllowConfidentialClient()) {
            if (clientOIDC.getTokenEndpointAuthMethod() == null || !ALLOWED_ALGORITHMS.contains(clientOIDC.getTokenEndpointAuthMethod())) {
                getLogger().debug("not confidential client");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT);
            }
            if (clientOIDC.getJwksUri() == null && clientOIDC.getJwks() == null) {
                getLogger().debug("confidential client but jwks or jwks_uri properties is not included");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
            if (clientOIDC.getJwksUri() != null && clientOIDC.getJwks() != null) {
                getLogger().debug("confidential client but both jwks and jwks_uri properties are included");
                throw invalidClientId(ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
            }
        }

    }

    @Override
    protected ClientModel persistClientMetadata(RealmModel realm, OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        ClientModel client = super.persistClientMetadata(realm, clientOIDCWithCacheControl);
        // overriding or additional checks
        return client;
    }

    @Override
    protected ClientModel updateClientMetadata(RealmModel realm, OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        ClientModel client = super.updateClientMetadata(realm, clientOIDCWithCacheControl);
        // overriding or additional checks
        return client;
    }
}
