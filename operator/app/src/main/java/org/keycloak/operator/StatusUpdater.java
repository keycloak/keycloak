package org.keycloak.operator;

public interface StatusUpdater<T> {

    void updateStatus(T status);
}
