package org.keycloak.testframework.admin;

import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

import java.security.KeyStore;

public class AdminClientFactorySupplier implements Supplier<AdminClientFactory, InjectAdminClientFactory> {

    @Override
    public AdminClientFactory getValue(InstanceContext<AdminClientFactory, InjectAdminClientFactory> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);

        if (!server.isTlsEnabled()) {
            return new AdminClientFactory(server.getBaseUrl());
        }

        ManagedCertificates managedCert = instanceContext.getDependency(ManagedCertificates.class);
        KeyStore serverKeyStore = managedCert.getKeyStore();
        return new AdminClientFactory(server.getBaseUrl(), serverKeyStore);
    }

    @Override
    public boolean compatible(InstanceContext<AdminClientFactory, InjectAdminClientFactory> a, RequestedInstance<AdminClientFactory, InjectAdminClientFactory> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<AdminClientFactory, InjectAdminClientFactory> instanceContext) {
        instanceContext.getValue().close();
    }

}
