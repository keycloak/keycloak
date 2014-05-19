package org.keycloak.account;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountSpi implements Spi {

    @Override
    public String getName() {
        return "account";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AccountProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AccountProviderFactory.class;
    }

}
