package org.keycloak.models.policy;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ResourcePolicyConditionSpi implements Spi {

    public static final String NAME = "rlm-policy-condition";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ResourcePolicyConditionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ResourcePolicyConditionProviderFactory.class;
    }
}
