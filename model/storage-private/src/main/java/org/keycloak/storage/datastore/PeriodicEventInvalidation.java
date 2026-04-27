package org.keycloak.storage.datastore;

import org.keycloak.provider.InvalidationHandler;

public enum PeriodicEventInvalidation implements InvalidationHandler.InvalidableObjectType {
    JPA_EVENT_STORE,
}
