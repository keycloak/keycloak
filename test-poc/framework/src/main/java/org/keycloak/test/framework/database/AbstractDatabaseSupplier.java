package org.keycloak.test.framework.database;

import org.keycloak.test.framework.KeycloakTestDatabase;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
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
    public InstanceWrapper<TestDatabase, KeycloakTestDatabase> getValue(Registry registry, KeycloakTestDatabase annotation) {
        TestDatabase testDatabase = getTestDatabase();
        testDatabase.start();
        return new InstanceWrapper<>(this, annotation, testDatabase, LifeCycle.GLOBAL);
    }

    @Override
    public boolean compatible(InstanceWrapper<TestDatabase, KeycloakTestDatabase> a, RequestedInstance<TestDatabase, KeycloakTestDatabase> b) {
        return true;
    }

    abstract TestDatabase getTestDatabase();

}
