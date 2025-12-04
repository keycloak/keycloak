package org.keycloak.protocol.oauth2.cimd.clientpolicy.condition;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.condition.AbstractClientPolicyConditionProviderFactory;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;

/**
 * The class is the factory class of {@link ClientIdUriSchemeCondition}.
 * It provides one configuration: Scheme part of the URI.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdUriSchemeConditionFactory extends AbstractClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "client-id-uri";

    public static final String CLIENT_ID_URI_SCHEME = "client-id-uri-scheme";

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
        return "The condition checks whether client_id is URI and its scheme.).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
