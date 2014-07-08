package org.keycloak.models.realms;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmSpi implements Spi {

    @Override
    public String getName() {
        return "modelRealms";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RealmProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RealmProviderFactory.class;
    }

}
