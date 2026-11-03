package org.keycloak.testframework.injection.mocks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MockInstances {

    public static final Set<Object> INSTANCES = ConcurrentHashMap.newKeySet();
    public static final Set<Object> CLOSED_INSTANCES = ConcurrentHashMap.newKeySet();

    public static void reset() {
        INSTANCES.clear();
        CLOSED_INSTANCES.clear();
    }

}
