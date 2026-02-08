package org.keycloak.common.util;

public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);
}
