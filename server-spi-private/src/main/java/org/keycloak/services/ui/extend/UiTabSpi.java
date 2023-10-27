package org.keycloak.services.ui.extend;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class UiTabSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "ui-tab";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return UiTabProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UiTabProviderFactory.class;
    }
}
