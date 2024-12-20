package org.keycloak.test.framework.server;

import org.keycloak.test.framework.annotations.InjectKeycloakUrls;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;

public class KeycloakUrlsSupplier implements Supplier<KeycloakUrls, InjectKeycloakUrls> {

    @Override
    public Class<InjectKeycloakUrls> getAnnotationClass() {
        return InjectKeycloakUrls.class;
    }

    @Override
    public Class<KeycloakUrls> getValueType() {
        return KeycloakUrls.class;
    }

    @Override
    public KeycloakUrls getValue(InstanceContext<KeycloakUrls, InjectKeycloakUrls> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        return new KeycloakUrls(server.getBaseUrl(), server.getManagementBaseUrl());
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakUrls, InjectKeycloakUrls> a, RequestedInstance<KeycloakUrls, InjectKeycloakUrls> b) {
        return true;
    }
}
