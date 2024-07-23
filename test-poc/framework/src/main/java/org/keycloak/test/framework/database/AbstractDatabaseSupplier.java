package org.keycloak.test.framework.database;

import org.keycloak.test.framework.annotations.KeycloakTestDatabase;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;

public abstract class AbstractDatabaseSupplier implements Supplier<TestDatabase, KeycloakTestDatabase> {

    protected static final String DEFAULT_DB_USERNAME = "keycloak";
    protected static final String DEFAULT_DB_PASSWORD = "Password1!";

    @Override
    public Class<KeycloakTestDatabase> getAnnotationClass() {
        return KeycloakTestDatabase.class;
    }

    @Override
    public Class<TestDatabase> getValueType() {
        return TestDatabase.class;
    }

    @Override
    public TestDatabase getValue(InstanceContext<TestDatabase, KeycloakTestDatabase> instanceContext) {
        TestDatabase testDatabase = getTestDatabase();
        testDatabase.start();
        return testDatabase;
    }

    @Override
    public boolean compatible(InstanceContext<TestDatabase, KeycloakTestDatabase> a, RequestedInstance<TestDatabase, KeycloakTestDatabase> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    abstract TestDatabase getTestDatabase();

    @Override
    public void close(InstanceContext<TestDatabase, KeycloakTestDatabase> instanceContext) {
        instanceContext.getValue().stop();
    }
}
