package org.keycloak.models.map.common.delegate;

public interface HasEntityFieldDelegate<E> {
    EntityFieldDelegate<E> getEntityFieldDelegate();
}
