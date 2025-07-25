package org.keycloak.testframework.conditions;

import org.keycloak.testframework.database.TestDatabase;

import java.lang.annotation.Annotation;

class DisabledForDatabasesCondition extends AbstractDisabledForSupplierCondition {

    @Override
    Class<?> valueType() {
        return TestDatabase.class;
    }

    Class<? extends Annotation> annotation() {
        return DisabledForDatabases.class;
    }

}
