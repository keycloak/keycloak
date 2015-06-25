package org.keycloak.authentication;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FormAuthenticatorSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "form-authenticator";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return FormAuthenticator.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return FormAuthenticatorFactory.class;
    }

}
