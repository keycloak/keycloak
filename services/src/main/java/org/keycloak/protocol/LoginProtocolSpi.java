package org.keycloak.protocol;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginProtocolSpi implements Spi {

    @Override
    public String getName() {
        return "login-protocol";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return LoginProtocol.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return LoginProtocolFactory.class;
    }

}
