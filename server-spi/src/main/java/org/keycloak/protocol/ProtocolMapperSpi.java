package org.keycloak.protocol;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProtocolMapperSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "protocol-mapper";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ProtocolMapper.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ProtocolMapper.class;
    }

}
