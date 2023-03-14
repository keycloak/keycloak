package org.keycloak.services.clientpolicy.executor;

import org.keycloak.Config.Scope;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class LoAEnforcerExecutorFactory implements ClientPolicyExecutorProviderFactory  {

    public static final String PROVIDER_ID = "loa-enforcer";

    public static final String MIN_ACR = "min-acr";

    private static final ProviderConfigProperty MIN_ACR_PROPERTY = new ProviderConfigProperty(
            MIN_ACR, "Minimum ACR", "The minimum ACR that should be enforced.", ProviderConfigProperty.STRING_TYPE,
            Constants.MINIMUM_LOA);

    public static final String USE_CLIENT_ACRS = "use-client-acrs";

    private static final ProviderConfigProperty USE_CLIENT_ACRS_PROPERTY = new ProviderConfigProperty(
            USE_CLIENT_ACRS, "Use client's default ACRs", "Whether to enforce client's default ACRs or not.", ProviderConfigProperty.BOOLEAN_TYPE,
            false);

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new LoAEnforcerExecutor(session);
    }

    @Override
    public void init(Scope config) {
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
        return "It makes the client enforce a certain level of authentication.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(MIN_ACR_PROPERTY, USE_CLIENT_ACRS_PROPERTY);
    }

}
