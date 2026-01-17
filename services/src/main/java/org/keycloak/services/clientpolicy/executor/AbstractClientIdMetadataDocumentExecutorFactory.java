package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
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
    public static final String CONSENT_REQUIRED = "cimd-consent-required";
    public static final String FULL_SCOPE_DISABLED = "cimd-full-scope-disabled";

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
    public String getHelpText() {
        return "On receiving an authorization request, this executor process the request by following OAuth Client ID Metadata Document (Internet Draft).";
    }

    static protected void addCommonConfigProperties(List<ProviderConfigProperty> configProperties) {
        // Client ID Verification
        ProviderConfigProperty property = new ProviderConfigProperty(
                ALLOW_LOOPBACK_ADDRESS,
                "Allow loopback address",
                "If ON, then the executor allows loopback address as a valid Client ID URL. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALLOW_PRIVATE_ADDRESS,
                "Allow private address",
                "If ON, then the executor allows private address as a valid Client ID URL. " +
                        "It can be ON only for development environment. It must be OFF in production environment. ",
                ProviderConfigProperty.BOOLEAN_TYPE,
                false);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                ALLOW_HTTP_SCHEME,
                "Allow http scheme",
                "If ON, then the executor allows http scheme as a valid Client ID URL. " +
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

        property = new ProviderConfigProperty(
                CONSENT_REQUIRED,
                "Consent required",
                "If ON, then an end-user is always required to grant consent to the client.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                true);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                FULL_SCOPE_DISABLED,
                "Full scope disabled",
                "If ON, then client is not allowed to use all the scopes.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                true);
        configProperties.add(property);
    }

}
