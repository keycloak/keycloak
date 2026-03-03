package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureResourceIndicatorExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-resource-indicator-check";

    public static final String PERMITTED_RESOURCES = "allow-permitted-resources";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureResourceIndicatorExecutor(session);
    }

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
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "On the authorization endpoint, this executor validates the resource parameter in the authorization request, "
                + "and on the token endpoint it checks that the resource parameter in the token request matches the one sent in the authorization request. "
                + "If validation fails, it denies the request.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    static {
        addCommonConfigProperties(configProperties);
    }

    static protected void addCommonConfigProperties(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty(
                PERMITTED_RESOURCES,
                "Permitted resources",
                "If filled, then the executor only accepts resource parameters whose values exactly match one of the permitted resources.",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null);
        configProperties.add(property);
    }
}
