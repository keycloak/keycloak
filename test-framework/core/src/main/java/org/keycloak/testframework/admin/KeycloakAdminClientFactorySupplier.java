package org.keycloak.testframework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

public class KeycloakAdminClientFactorySupplier implements Supplier<KeycloakAdminClientFactory, InjectAdminClientFactory> {

    @Override
    public Class<InjectAdminClientFactory> getAnnotationClass() {
        return InjectAdminClientFactory.class;
    }

    @Override
    public Class<KeycloakAdminClientFactory> getValueType() {
        return KeycloakAdminClientFactory.class;
    }

    @Override
    public KeycloakAdminClientFactory getValue(InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);

        return new KeycloakAdminClientFactory(instanceContext, server.getBaseUrl(), OAuth2Constants.CLIENT_CREDENTIALS);
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> a, RequestedInstance<KeycloakAdminClientFactory, InjectAdminClientFactory> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext) {
        instanceContext.getValue().close();
    }

}
