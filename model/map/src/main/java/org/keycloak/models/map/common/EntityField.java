package org.keycloak.models.map.common;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a field in an entity with appropriate accessors.
 * 
 * @author hmlnarik
 * @param <E>
 */
public interface EntityField<E> {

    /**
     * Returns name of this field with no spaces where each word starts with a capital letter.
     * @return
     */
    String getName();
    /**
     * Returns name of this field in lowercase with words separated by a dash ({@code -}).
     * @return
     */
    String getNameDashed();
    /**
     * Returns the value of this field.
     *
     * @param e Entity
     * @return Value of the field
     */
    Object get(E e);

    /**
     * Sets the value of this field. Does nothing by default. If you want to have a field set, override this method.
     * @param <T>
     * @param e Entity
     * @param value Value of the field
     */
    default <T> void set(E e, T value) {};

    /**
     * Adds an element to the collection stored in this field.
     * @param e Entity
     * @param value Value to be added to the collection
     * @throws ClassCastException If this field is not a collection.
     */
    default <T> void collectionAdd(E e, T value) {
        @SuppressWarnings("unchecked")
        Collection<T> c = (Collection<T>) get(e);
        if (c != null) {
            c.add(value);
        }
    }
    /**
     * Removes an element from the collection stored in this field.
     * @param e Entity
     * @param value Value to be added to the collection
     * @return Defined by the underlying field. Preferrably it should return deleted object, but it can return
     *    {@code true / false} indication of removal, or just {@code null}.
     * @throws ClassCastException If this field is not a collection.
     */
    default <T> Object collectionRemove(E e, T value) {
        Collection<?> c = (Collection<?>) get(e);
        return c == null ? null : c.remove(value);
    }

    /**
     * Retrieves a value from the map stored in this field.
     * @param e Entity
     * @param key Requested key
     * @return Object mapped to this key
     * @throws ClassCastException If this field is not a map.
     */
    default <K> Object mapGet(E e, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(e);
        return m == null ? null : m.get(key);
    }
    /**
     * Adds a mapping to the map stored in this field.
     * @param e Entity
     * @param key Key to map
     * @param value Mapped value
     * @throws ClassCastException If this field is not a map.
     */
    default <K, T> void mapPut(E e, K key, T value) {
        @SuppressWarnings("unchecked")
        Map<K, T> m = (Map<K, T>) get(e);
        if (m != null) {
            m.put(key, value);
        }
    }
    /**
     * Removes a mapping from the map stored in this field.
     * @param e Entity
     * @param key Key to remove
     * @return Object mapped to this key
     * @throws ClassCastException If this field is not a map.
     */
    default <K> Object mapRemove(E e, K key) {
        @SuppressWarnings("unchecked")
        Map<K, ?> m = (Map<K, ?>) get(e);
        if (m != null) {
            return m.remove(key);
        }
        return null;
    }

    /**
     * @return Returns the most specific type of this field.
     */
    default Class<?> getFieldClass() { return Object.class; }

    /**
     * @return If this field is a collection, returns type of its elements; otherwise returns {@code Void} class.
     */
    default Class<?> getCollectionElementClass() { return Void.class; }

    /**
     * @return If this field is a map, returns type of its keys; otherwise returns {@code Void} class.
     */
    default Class<?> getMapKeyClass() { return Void.class; }

    /**
     * @return If this field is a map, returns type of its values; otherwise returns {@code Void} class.
     */
    default Class<?> getMapValueClass() { return Void.class; }
}
