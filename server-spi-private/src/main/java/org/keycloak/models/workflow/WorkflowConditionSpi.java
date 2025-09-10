package org.keycloak.models.workflow;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class WorkflowConditionSpi implements Spi {

    public static final String NAME = "workflow-condition";

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
        return WorkflowConditionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return WorkflowConditionProviderFactory.class;
    }
}
