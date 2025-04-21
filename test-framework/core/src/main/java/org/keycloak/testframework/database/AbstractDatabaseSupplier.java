package org.keycloak.testframework.database;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigInterceptor;

public abstract class AbstractDatabaseSupplier implements Supplier<TestDatabase, InjectTestDatabase>, KeycloakServerConfigInterceptor<TestDatabase, InjectTestDatabase> {

    @Override
    public TestDatabase getValue(InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        TestDatabase testDatabase = getTestDatabase();
        testDatabase.start();
        return testDatabase;
    }

    @Override
    public boolean compatible(InstanceContext<TestDatabase, InjectTestDatabase> a, RequestedInstance<TestDatabase, InjectTestDatabase> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    abstract TestDatabase getTestDatabase();

    @Override
    public void close(InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        return serverConfig.options(instanceContext.getValue().serverConfig());
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}
