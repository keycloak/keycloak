package org.keycloak.testframework.injection;

public interface SupplierOrder {

    int BEFORE_KEYCLOAK_SERVER = 100;
    int KEYCLOAK_SERVER = 250;
    int BEFORE_REALM = 500;
    int REALM = 750;
    int DEFAULT = 1000;

}
