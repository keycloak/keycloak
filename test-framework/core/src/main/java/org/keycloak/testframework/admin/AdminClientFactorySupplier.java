package org.keycloak.testframework.admin;

import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

public class AdminClientFactorySupplier implements Supplier<AdminClientFactory, InjectAdminClientFactory> {

    @Override
    public Class<InjectAdminClientFactory> getAnnotationClass() {
        return InjectAdminClientFactory.class;
    }

    @Override
    public Class<AdminClientFactory> getValueType() {
        return AdminClientFactory.class;
    }

    @Override
    public AdminClientFactory getValue(InstanceContext<AdminClientFactory, InjectAdminClientFactory> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        return new AdminClientFactory(server.getBaseUrl());
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
