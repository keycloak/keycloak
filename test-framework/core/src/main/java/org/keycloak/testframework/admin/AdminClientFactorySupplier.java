package org.keycloak.testframework.admin;

import java.util.List;
import javax.net.ssl.SSLContext;

import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

public class AdminClientFactorySupplier implements Supplier<AdminClientFactory, InjectAdminClientFactory> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<AdminClientFactory, InjectAdminClientFactory> instanceContext) {
        return DependenciesBuilder.create(KeycloakServer.class).add(ManagedCertificates.class).build();
    }

    @Override
    public AdminClientFactory getValue(InstanceContext<AdminClientFactory, InjectAdminClientFactory> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        ManagedCertificates managedCert = instanceContext.getDependency(ManagedCertificates.class);

        if (!managedCert.isTlsEnabled()) {
            return new AdminClientFactory(server.getBaseUrl());
        } else {
            SSLContext sslContext = managedCert.getClientSSLContext();
            return new AdminClientFactory(server.getBaseUrl(), sslContext);
        }
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
