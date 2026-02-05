package org.keycloak.protocol.oauth2.cimd.clientpolicy.condition;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.condition.AbstractClientPolicyConditionProviderFactory;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;

/**
 * The class is the factory class of {@link ClientIdUriSchemeCondition}.
 * It provides two configuration: Scheme part and host part of the URI.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdUriSchemeConditionFactory extends AbstractClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "client-id-uri";

    public static final String CLIENT_ID_URI_SCHEME = "client-id-uri-scheme";
    public static final String TRUSTED_DOMAINS = "client-id-uri-allow-permitted-domains";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        addCommonConfigProperties(configProperties);

        ProviderConfigProperty property;
        property = new ProviderConfigProperty(
                CLIENT_ID_URI_SCHEME,
                "URI scheme",
                "Scheme part of the URI",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null);
        configProperties.add(property);

        property = new ProviderConfigProperty(
                TRUSTED_DOMAINS,
                "Trusted domains",
                "If some domains are filled, The condition evaluates to true " +
                        "if the host part of client_id parameter in an authorization request matches one of the filled domains. " +
                        "Otherwise, the condition evaluates to false " +
                        "The domains are checked by using regex. " +
                        "If the domains not filled, the condition evaluate to false regardless of the client_id parameter value. " +
                        "For example, use pattern like this '(.*)\\.example\\.org' if you want to accept the parameter / property whose domain is 'example.org'." +
                        "Don't forget to use escaping of special characters like dots as otherwise dot is interpreted as any character in regex!",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);
    }

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new ClientIdUriSchemeCondition(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "The condition checks that the scheme part of client_id parameter matches one of the filled ones " +
               "and the host part of the client_id matches one of the filled domains.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
