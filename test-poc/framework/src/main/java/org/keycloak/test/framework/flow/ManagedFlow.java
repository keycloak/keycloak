package org.keycloak.test.framework.flow;

public interface ManagedFlow {

    void build();

    void execute();

    void rollback();

    void close();
}
