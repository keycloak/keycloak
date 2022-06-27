package org.keycloak.operator.controllers;

public interface StatusUpdater<T> {

    void updateStatus(T status);
}
