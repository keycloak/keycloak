package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentExecutorFactory extends AbstractClientIdMetadataDocumentExecutorFactory {

    public static final String PROVIDER_ID = "client-id-metadata-document";

    public static final String ONLY_ALLOW_CONFIDENTIAL_CLIENT = "only-allow-confidential-client";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        addCommonConfigProperties(configProperties);
        // additional settings
        ProviderConfigProperty property = new ProviderConfigProperty(
                ONLY_ALLOW_CONFIDENTIAL_CLIENT,
                "Only Allow Confidential Client",
                "If ON, then the executor only accept a Client Metadata showing a confidential client.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);
    }

    @Override
    public ClientPolicyExecutorProvider<ClientIdMetadataDocumentExecutor.Configuration> create(KeycloakSession session) {
        return new ClientIdMetadataDocumentExecutor(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
