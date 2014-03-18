package org.keycloak.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Set of services shared for whole Keycloak server
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakRegistry {

    private final ConcurrentMap<Class<?>, Object> services = new ConcurrentHashMap<Class<?>, Object>();

    public <T> void putService(Class<T> type, T object) {
        services.put(type, object);
    }

    public <T> T putServiceIfAbsent(Class<T> type, T object) {
        // Put only if absent and always return the version from registry
        services.putIfAbsent(type, object);
        return (T) services.get(type);
    }

    public <T> T getService(Class<T> type) {
        return (T) services.get(type);
    }
}
