package org.keycloak.authentication;

import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthenticatorSpi implements Spi {

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public String getName() {
        return "authenticator";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return Authenticator.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AuthenticatorFactory.class;
    }

}
