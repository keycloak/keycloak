package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

/**
 * The class is a factory class of {@link ClientIdMetadataDocumentExecutor}.
 *
 * <p>It provides the following configurations:
 * <ul>
 *     <li>Client Metadata Validation</li>
 *     <ul>
 *         <li>Only Allow Confidential Client: only accept a confidential client</li>
 *         <li>All URIs Restrict same domain: a client metadata includes properties whose values are URIs and an authorization server might access them.
 *         To prevent Server-side request forgery (SSRF), only allows these properties whose values are under the same domain of the permitted domains.</li>
 *     </ul>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentExecutorFactory extends AbstractClientIdMetadataDocumentExecutorFactory {

    public static final String PROVIDER_ID = "client-id-metadata-document";

    public static final String ONLY_ALLOW_CONFIDENTIAL_CLIENT = "only-allow-confidential-client";
    public static final String ALL_URIS_RESTRICT_SAME_DOMAIN = "cimd-all_uris-restrict-same-domain";


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        addCommonConfigProperties(configProperties);

        // additional settings

        // Client Metadata Validation

        ProviderConfigProperty property = new ProviderConfigProperty(
                ONLY_ALLOW_CONFIDENTIAL_CLIENT,
                "Only Allow Confidential Client",
                "If ON, then the executor only accept a Client Metadata showing a confidential client.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALL_URIS_RESTRICT_SAME_DOMAIN,
                "All URIs Restrict same domain",
                "If ON, then the executor checks client_id and redirect_uris of an authorization request " +
                        "and properties of client metadata whose value is URI is under the same domain defined by permitted domains.",
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
