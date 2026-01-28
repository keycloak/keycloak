package org.keycloak.infinispan.module.factory;

import org.keycloak.infinispan.module.configuration.global.KeycloakConfiguration;
import org.keycloak.jgroups.certificates.CertificateReloadManager;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;

import org.infinispan.factories.AbstractComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;

@DefaultFactoryFor(classes = CertificateReloadManager.class)
public class CertificateReloadManagerFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String componentName) {
        var kcConfig = globalConfiguration.module(KeycloakConfiguration.class);
        if (kcConfig == null) {
            return null;
        }
        var sessionFactory = kcConfig.keycloakSessionFactory();
        if (supportsReloadAndRotation(sessionFactory)) {
            return new CertificateReloadManager(sessionFactory);
        }
        return null;
    }

    private boolean supportsReloadAndRotation(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            var provider = session.getProvider(JGroupsCertificateProvider.class);
            return provider != null && provider.isEnabled() && provider.supportRotateAndReload();
        }
    }
}
