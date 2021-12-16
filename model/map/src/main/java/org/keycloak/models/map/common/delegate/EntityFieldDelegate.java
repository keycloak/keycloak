package org.keycloak.models.map.common.delegate;

import java.util.Collection;
import java.util.Map;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;

public interface EntityFieldDelegate<E> extends UpdatableEntity {
    // Non-collection values
    Object get(EntityField<E> field);
    default <T> void set(EntityField<E> field, T value) {}

    default <T> void collectionAdd(EntityField<E> field, T value) {
        @SuppressWarnings("unchecked")
        Collection<T> c = (Collection<T>) get(field);
        if (c != null) {
            c.add(value);
        }
    }
    default <T> Object collectionRemove(EntityField<E> field, T value) {
        Collection<?> c = (Collection<?>) get(field);
        return c == null ? null : c.remove(value);
    }

    /**
     * 
     * @param <K> Key type
     * @param <T> Value type
     * @param field Field identifier. Should be one of the generated {@code *Fields} enum constants.
     * @param key Key
     * @param valueClass class of the value
     * @return
     */
    default <K> Object mapGet(EntityField<E> field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        return m == null ? null : m.get(key);
    }
    default <K, T> void mapPut(EntityField<E> field, K key, T value) {
        @SuppressWarnings("unchecked")
        Map<K, T> m = (Map<K, T>) get(field);
        if (m != null) {
            m.put(key, value);
        }
    }
    default <K> Object mapRemove(EntityField<E> field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        if (m != null) {
            return m.remove(key);
        }
        return null;
    }

}
