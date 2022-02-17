package org.keycloak.models.map.common.delegate;

import java.util.Collection;
import java.util.Map;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;

public interface EntityFieldDelegate<E> extends UpdatableEntity {
    // Non-collection values
    <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field);
    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value) {}

    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void collectionAdd(EF field, T value) {
        @SuppressWarnings("unchecked")
        Collection<T> c = (Collection<T>) get(field);
        if (c != null) {
            c.add(value);
        }
    }
    default <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object collectionRemove(EF field, T value) {
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
    default <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapGet(EF field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        return m == null ? null : m.get(key);
    }
    default <K, T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void mapPut(EF field, K key, T value) {
        @SuppressWarnings("unchecked")
        Map<K, T> m = (Map<K, T>) get(field);
        if (m != null) {
            m.put(key, value);
        }
    }
    default <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapRemove(EF field, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(field);
        if (m != null) {
            return m.remove(key);
        }
        return null;
    }

}
