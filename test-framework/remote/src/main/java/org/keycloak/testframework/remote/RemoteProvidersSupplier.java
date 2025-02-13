package org.keycloak.testframework.remote;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigInterceptor;

public class RemoteProvidersSupplier implements Supplier<RemoteProviders, InjectRemoteProviders>, KeycloakServerConfigInterceptor<TestDatabase, InjectTestDatabase> {

    @Override
    public RemoteProviders getValue(InstanceContext<RemoteProviders, InjectRemoteProviders> instanceContext) {
        return new RemoteProviders();
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<RemoteProviders, InjectRemoteProviders> a, RequestedInstance<RemoteProviders, InjectRemoteProviders> b) {
        return true;
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        return serverConfig.dependency("org.keycloak.testframework", "keycloak-test-framework-remote-providers");
    }
}
