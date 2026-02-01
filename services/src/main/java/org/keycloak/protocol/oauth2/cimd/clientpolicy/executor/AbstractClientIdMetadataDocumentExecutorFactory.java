package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oauth2.cimd.provider.PersistentClientIdMetadataDocumentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

/**
 * The abstract class is the factory class of {@link AbstractClientIdMetadataDocumentExecutor}.
 *
 * <p>It provides the following configurations:
 * <ul>
 *     <li>Client ID Verification / Client Metadata Verification (URL related)</li>
 *     <ul>
 *         <li>Allow loopback address: allows IPv4/IPv6 loopback address (for development environment)</li>
 *         <li>Allow private address: allows private address (for development environment)</li>
 *         <li>Allow http scheme: allows http scheme of a URI (for development environment)<</li>
 *     </ul>
 *     <li>Client ID Validation</li>
 *     <ul>
 *         <li>Allow permitted domains: only allow a URI whose hostname is under the one of the permitted domain (wildcard * can be used)</li>
 *     </ul>
 *     <li>Client Metadata Validation</li>
 *     <ul>
 *         <li>Restrict same domain: only allow {client_id} and {redirect_uri} parameter of an authorization request whose hostname is under the one of the permitted domain (wildcard * can be used)</li>
 *         <li>Required properties: only allow a client metadata that includes all required properties</li>
 *     </ul>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractClientIdMetadataDocumentExecutorFactory implements ClientPolicyExecutorProviderFactory {

    // Client ID Verification
    public static final String ALLOW_LOOPBACK_ADDRESS = "cimd-allow-loopback-address";
    public static final String ALLOW_PRIVATE_ADDRESS = "cimd-allow-private-address";
    public static final String ALLOW_HTTP_SCHEME = "cimd-allow-http-scheme";

    // Client ID Validation
    public static final String ALLOW_PERMITTED_DOMAINS = "cimd-allow-permitted-domains";

    // Client Metadata Validation
    public static final String REQUIRED_PROPERTIES = "cimd-required-properties";
    public static final String RESTRICT_SAME_DOMAIN = "cimd-restrict-same-domain";

    // Factory Global Setting: CIMD Provider Name
    // Name in properties: spi-client-policy-executor-client-id-metadata-document-cimd-provider-name
    protected String cimdProviderName;

    @Override
    public void init(Config.Scope config) {
        cimdProviderName = config.get("cimdProviderName");
        if (cimdProviderName == null || cimdProviderName.isBlank()) {
            cimdProviderName = PersistentClientIdMetadataDocumentProviderFactory.PROVIDER_ID; // default
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getHelpText() {
        return "On receiving an authorization request, this executor process the request by following OAuth Client ID Metadata Document (Internet Draft).";
    }

    static protected void addCommonConfigProperties(List<ProviderConfigProperty> configProperties) {
        // Client ID Verification / Client Metadata Verification (URL related)
        ProviderConfigProperty property = new ProviderConfigProperty(
                ALLOW_LOOPBACK_ADDRESS,
                "Allow loopback address",
                "If ON, then the executor allows loopback address as a valid Client ID URL and property of Client Metadata whose value is URL: client_uri, logo_uri, tos_uri, policy_uri, jwks_uri. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALLOW_PRIVATE_ADDRESS,
                "Allow private address",
                "If ON, then the executor allows private address as a valid Client ID URL and property of Client Metadata whose value is URL: client_uri, logo_uri, tos_uri, policy_uri, jwks_uri. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALLOW_HTTP_SCHEME,
                "Allow http scheme",
                "If ON, then the executor allows http scheme as a valid Client ID URL and property of Client Metadata whose value is URL: client_uri, logo_uri, tos_uri, policy_uri, jwks_uri. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        // Client ID Validation
        property = new ProviderConfigProperty(
                ALLOW_PERMITTED_DOMAINS,
                "Allow permitted domains",
                "If some domains are filled, then the executor only accept a Client ID URL whose host part exactly matches one of the filled domains." +
                        "If not filled, then all domains are possible to use. The domains are checked by using regex. " +
                        "For example use pattern like this '(.*)\\.example\\.org' if you want to accept the Client ID URL whose domain is 'example.org'." +
                        "Don't forget to use escaping of special characters like dots as otherwise dot is interpreted as any character in regex!",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);

        // Client Metadata Validation
        property = new ProviderConfigProperty(
                RESTRICT_SAME_DOMAIN,
                "Restrict same domain",
                "If ON, then the executor checks Client ID URL and Redirect URI of an authorization request are under the same one of allow permitted domains." +
                        "Moreover, the executor checks if Redirect URIs in Client Metadata has at least one entry whose domain is the same as " +
                        "Client ID URL and Redirect URI of the authorization request. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                REQUIRED_PROPERTIES,
                "Required properties",
                "If client metadata does not include all the properties, the executor does not accept the client metadata.",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);
    }

}
