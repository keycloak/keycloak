package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

/**
 * The abstract class is the factory class of {@link AbstractClientIdMetadataDocumentExecutor}.
 *
 * <p>It provides the following configurations:
 * <ul>
 *     <li>Client ID Verification / Client Metadata Verification (URL related)</li>
 *     <ul>
 *         <li>Allow http scheme: allows http scheme of a URI (for development environment)<</li>
 *     </ul>
 *     <li>Client ID Validation</li>
 *     <ul>
 *         <li>Trusted domains: only allow a URI whose hostname is under the one of the permitted domain (wildcard * can be used)</li>
 *     </ul>
 *     <li>Client Metadata Validation</li>
 *     <ul>
 *         <li>Restrict same domain: only allow {client_id} and {redirect_uri} parameter of an authorization request whose hostname is under the one of the permitted domain (wildcard * can be used)</li>
 *         <li>Required properties: only allow a client metadata that includes all required properties</li>
 *         <li>Allowed grant types: exclude grant types not included in the allow list from a client metadata</li>
 *     </ul>
 *     <li>Resource Indicators (RFC 8707) support</li>
 *     <ul>
 *         <li>Resource indicator allow list: only allow a {@code resource} parameter value that is included in the configured allow list</li>
 *     </ul>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractClientIdMetadataDocumentExecutorFactory
        implements ClientPolicyExecutorProviderFactory, EnvironmentDependentProviderFactory {

    // Client ID Verification
    public static final String ALLOW_HTTP_SCHEME = "cimd-allow-http-scheme";
    public static final String ALLOW_PRIVATE_USE_ADDRESSES = "cimd-allow-private-use-addresses";

    // Client ID Validation
    public static final String TRUSTED_DOMAINS = "cimd-allow-permitted-domains";

    // Client Metadata Validation
    public static final String REQUIRED_PROPERTIES = "cimd-required-properties";
    public static final String RESTRICT_SAME_DOMAIN = "cimd-restrict-same-domain";
    public static final String ALLOWED_GRANT_TYPES = "cimd-allowed-grant-types";

    // Resource Indicators (RFC 8707) support
    public static final String RESOURCE_INDICATOR_ALLOW_LIST = "cimd-resource-indicator-allow-list";

    // Factory Global Settings
    public static final String CONFIG_CIMD_PROVIDER_NAME = "cimd-provider-name";
    public static final String CONFIG_MIN_CACHE_TIME = "min-cache-time";
    public static final String CONFIG_MAX_CACHE_TIME = "max-cache-time";
    public static final String CONFIG_UPPER_LIMIT_METADATA_BYTES = "upper-limit-metadata-bytes";

    protected ClientIdMetadataDocumentExecutorFactoryProviderConfig providerConfig;

    @Override
    public void init(Config.Scope config) {
        providerConfig = new ClientIdMetadataDocumentExecutorFactoryProviderConfig(config);
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
                ALLOW_HTTP_SCHEME,
                "Allow http scheme",
                "If ON, then the executor allows http scheme as a valid Client ID URL and property of Client Metadata whose value is URL: client_uri, logo_uri, tos_uri, policy_uri, jwks_uri. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALLOW_PRIVATE_USE_ADDRESSES,
                "Allow private-use addresses",
                "If OFF, then the executor rejects a URL that the server fetches itself, namely the Client ID URL and the " +
                        "jwks_uri, sector_identifier_uri and request_uris properties of Client Metadata, whose host resolves to a " +
                        "loopback, private, link-local, unique local or multicast address. Such URLs are rejected even if their host " +
                        "matches one of the trusted domains, which prevents Server-Side Request Forgery (SSRF) against hosts only " +
                        "reachable from the server. It can be ON only for development environment. It must be OFF in production environment.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        // Client ID Validation
        property = new ProviderConfigProperty(
                TRUSTED_DOMAINS,
                "Trusted domains",
                "If some domains are filled, the executor only accepts the following URL-formatted parameters whose host part matches one of the filled domains: " +
                        "Authorization request parameters: client_id, redirect_uri, " +
                        "Client metadata properties: client_id, redirect_uris, jwks_uri, logo_uri, policy_uri, tos_uri, client_uri, sector_identifier_uri, request_uris. " +
                        "The domains are checked by using wildcard pattern matching (e.g. '*.example.org'). " +
                        "If the domains not filled, the executor denies all such the parameters and properties. " +
                        "For example, use pattern like '*.example.org' if you want to accept the parameter / property whose domain is 'example.org' or its subdomains.",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);

        // Client Metadata Validation
        property = new ProviderConfigProperty(
                RESTRICT_SAME_DOMAIN,
                "Restrict same domain",
                "If ON, then the executor checks Client ID URL and Redirect URI of an authorization request " +
                "and properties of client metadata whose value is URI are under the same one of allow permitted domains.",
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

        property = new ProviderConfigProperty(
                ALLOWED_GRANT_TYPES,
                "Allowed grant types",
                "Grant types that a client metadata can enable for the client. " +
                        "The executor excludes the grant types not included in this list from the grant_types property of the client metadata. " +
                        "If the list is vacant, only 'authorization_code' and 'refresh_token' are allowed. " +
                        "The executor also excludes the response types whose grant type is not allowed from the response_types property, and rejects the client metadata if no response type is left. " +
                        "For an existing client, the executor disables the grant types no longer allowed even if the client metadata is not re-fetched or is not modified. " +
                        "The executor also rejects a request obtaining a token by a grant type not allowed, for example a client_credentials grant request, the token response of the implicit or hybrid flow, or a UMA permission request. " +
                        "'urn:ietf:params:oauth:grant-type:uma-ticket' is allowed only if 'client_credentials' is allowed too. " +
                        "Be careful when adding other grant types, as a client metadata is fetched and registered without any authentication. " +
                        "For example, 'client_credentials' gives the client a service account.",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);

        // Resource Indicators (RFC 8707) support
        property = new ProviderConfigProperty(
                RESOURCE_INDICATOR_ALLOW_LIST,
                "Resource indicator allow list",
                "Only effective if Resource Indicators feature (RFC 8707) is enabled. " +
                        "If a resource parameter value of an authorization request is not included in this allow list, the executor rejects the request. " +
                        "If the request does not include a resource parameter, the executor does not enforce this allow list. " +
                        "If the allow list is vacant, no resource parameter value is accepted.",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_CIMD_PROVIDER_NAME)
                .type("string")
                .helpText("Provider to use for the CIMD")
                .defaultValue(ClientIdMetadataDocumentExecutorFactoryProviderConfig.DEFAULT_CONFIG_CIMD_PROVIDER_NAME)
                .add()

                .property()
                .name(CONFIG_MIN_CACHE_TIME)
                .type("int")
                .helpText("Min cache time of client metadata in seconds for the CIMD.")
                .defaultValue(ClientIdMetadataDocumentExecutorFactoryProviderConfig.DEFAULT_CONFIG_MIN_CACHE_TIME)
                .add()

                .property()
                .name(CONFIG_MAX_CACHE_TIME)
                .type("int")
                .helpText("Max cache time of client metadata in seconds for the CIMD.")
                .defaultValue(ClientIdMetadataDocumentExecutorFactoryProviderConfig.DEFAULT_CONFIG_MAX_CACHE_TIME)
                .add()

                .property()
                .name(CONFIG_UPPER_LIMIT_METADATA_BYTES)
                .type("int")
                .helpText("Client metadata upper limit in byte for the CIMD.")
                .defaultValue(ClientIdMetadataDocumentExecutorFactoryProviderConfig.DEFAULT_CONFIG_UPPER_LIMIT_METADATA_BYTES)
                .add()

                .build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CIMD);
    }
}
