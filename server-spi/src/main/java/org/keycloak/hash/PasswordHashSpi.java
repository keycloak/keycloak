package org.keycloak.hash;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public class PasswordHashSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "password-hash";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return PasswordHashProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return PasswordHashProviderFactory.class;
    }
}
