package org.keycloak.test.framework.flow;

public interface ManagedFlow {

    void execute();

    void rollback();
}
