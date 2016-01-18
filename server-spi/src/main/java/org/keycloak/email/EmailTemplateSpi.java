package org.keycloak.email;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailTemplateSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "emailTemplate";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return EmailTemplateProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return org.keycloak.email.EmailTemplateProviderFactory.class;
    }
}
