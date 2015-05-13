package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationMapperSpi implements Spi {

    @Override
    public String getName() {
        return "userFederationMapper";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return UserFederationMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserFederationMapper.class;
    }
}
