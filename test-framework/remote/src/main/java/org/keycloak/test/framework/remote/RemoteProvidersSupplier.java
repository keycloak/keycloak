package org.keycloak.test.framework.remote;

import org.keycloak.test.framework.annotations.InjectTestDatabase;
import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierOrder;
import org.keycloak.test.framework.server.KeycloakServerConfigBuilder;
import org.keycloak.test.framework.server.KeycloakServerConfigInterceptor;

public class RemoteProvidersSupplier implements Supplier<RemoteProviders, InjectRemoteProviders>, KeycloakServerConfigInterceptor<TestDatabase, InjectTestDatabase> {
    @Override
    public Class<InjectRemoteProviders> getAnnotationClass() {
        return InjectRemoteProviders.class;
    }

    @Override
    public Class<RemoteProviders> getValueType() {
        return RemoteProviders.class;
    }

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
        return serverConfig.dependency("org.keycloak.test", "keycloak-test-framework-remote-providers");
    }
}
