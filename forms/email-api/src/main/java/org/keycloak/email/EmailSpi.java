package org.keycloak.email;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailSpi implements Spi {
    @Override
    public String getName() {
        return "email";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return EmailProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return EmailProviderFactory.class;
    }
}
