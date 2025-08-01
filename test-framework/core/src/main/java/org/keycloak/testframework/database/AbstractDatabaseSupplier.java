package org.keycloak.testframework.database;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfigInterceptor;

public abstract class AbstractDatabaseSupplier implements Supplier<TestDatabase, InjectTestDatabase>, KeycloakServerConfigInterceptor<TestDatabase, InjectTestDatabase> {

    @Override
    public TestDatabase getValue(InstanceContext<TestDatabase, InjectTestDatabase> instanceContext) {
        DatabaseConfigBuilder builder = DatabaseConfigBuilder
              .create()
              .preventReuse(instanceContext.getLifeCycle() != LifeCycle.GLOBAL);

        DatabaseConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        builder = config.configure(builder);

        TestDatabase testDatabase = getTestDatabase();
        testDatabase.start(builder.build());
        return testDatabase;
    }

    @Override
    public boolean compatible(InstanceContext<TestDatabase, InjectTestDatabase> a, RequestedInstance<TestDatabase, InjectTestDatabase> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
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
        String kcServerType = Config.getSelectedSupplier(KeycloakServer.class);
        TestDatabase database = instanceContext.getValue();

        // If both KeycloakServer and TestDatabase run in container, we need to configure Keycloak with internal
        // url that is accessible within docker network
        if ("cluster".equals(kcServerType) &&
                database instanceof AbstractContainerTestDatabase containerDatabase) {
            return serverConfig.options(containerDatabase.serverConfig(true));
        }

        return serverConfig.options(database.serverConfig());
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_KEYCLOAK_SERVER;
    }
}
