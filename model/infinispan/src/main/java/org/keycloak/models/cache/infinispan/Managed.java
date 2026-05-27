package org.keycloak.models.cache.infinispan;

import java.util.HashMap;

import org.keycloak.connections.jpa.support.EntityManagers;

public class Managed<T> {

    HashMap<String, T> map = new HashMap<>();

    public T get(String key) {
        return map.get(key);
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public void put(String id, T value) {
        if (EntityManagers.isBatchMode()) {
            return;
        }
        map.put(id, value);
    }

}
