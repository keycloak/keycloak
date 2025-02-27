package org.keycloak.infinispan.module.factory;

import org.infinispan.factories.AbstractComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.keycloak.infinispan.module.certificates.CertificateReloadManager;
import org.keycloak.infinispan.module.configuration.global.KeycloakConfiguration;

@DefaultFactoryFor(classes = CertificateReloadManager.class)
public class CertificateReloadManagerFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String componentName) {
        var kcConfig = globalConfiguration.module(KeycloakConfiguration.class);
        if (kcConfig == null) {
            return null;
        }
        var sessionFactory = kcConfig.keycloakSessionFactory();
        var certificateHolder = kcConfig.jGroupsCertificateHolder();
        if (sessionFactory == null || certificateHolder == null) {
            throw new IllegalStateException("KeycloakConfiguration is not null when the certificate reload is required.");
        }
        return new CertificateReloadManager(sessionFactory, certificateHolder, kcConfig.jgroupsCertificateRotation());
    }
}
