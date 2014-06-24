package org.keycloak.login;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFormsSpi implements Spi {
    @Override
    public String getName() {
        return "login";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return LoginFormsProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return LoginFormsProviderFactory.class;
    }
}
