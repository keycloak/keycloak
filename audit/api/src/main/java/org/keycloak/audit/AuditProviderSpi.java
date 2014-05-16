package org.keycloak.audit;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuditProviderSpi implements Spi {

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AuditProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AuditProviderFactory.class;
    }

}
