package org.keycloak.testframework.injection.mocks;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;

public class MockInstances {

    public static final Set<Object> INSTANCES = new ConcurrentHashSet<>();
    public static final Set<Object> CLOSED_INSTANCES = new ConcurrentHashSet<>();

    public static void reset() {
        INSTANCES.clear();
        CLOSED_INSTANCES.clear();
    }

}
