package org.keycloak.authentication.authenticators.client;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpiffeClientAuthenticator extends AbstractJWTClientAuthenticator implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "spiffe-jwt";

    public static final String TRUST_DOMAIN_KEY = "spiffeTrustDomain";

    static final List<ProviderConfigProperty> CONFIG = List.of(
            new ProviderConfigProperty(TRUST_DOMAIN_KEY, "Trust Domain", "SPIFFE Trust Domain", ProviderConfigProperty.STRING_TYPE, null)
    );

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "SPIFFE JWT-SVID";
    }

    @Override
    public String getHelpText() {
        return "Validates client using SPIFFE JWT SVIDs";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG;
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return CONFIG;
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        return Collections.emptyMap();
    }

    @Override
    protected JWTClientValidator getValidator(ClientAuthenticationFlowContext context) {
        return new SpiffeClientValidator(context, getId());
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SPIFFE_JWT);
    }
}
