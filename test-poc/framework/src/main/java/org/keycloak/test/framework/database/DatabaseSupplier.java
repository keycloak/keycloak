package org.keycloak.test.framework.database;

import org.keycloak.test.framework.KeycloakTestDatabase;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;

public abstract class DatabaseSupplier implements Supplier<TestDatabase, KeycloakTestDatabase> {

    protected static TestDatabase testDatabase;

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
        InstanceWrapper<TestDatabase, KeycloakTestDatabase> wrapper = new InstanceWrapper<>(this, annotation);
        testDatabase = registry.getDependency(TestDatabase.class, wrapper);
        testDatabase.start();
        return wrapper;
    }

    @Override
    public boolean compatible(InstanceWrapper<TestDatabase, KeycloakTestDatabase> a, InstanceWrapper<TestDatabase, KeycloakTestDatabase> b) {
        return true;
    }

    public TestDatabase getTestDatabase() {
        return testDatabase;
    }
}
