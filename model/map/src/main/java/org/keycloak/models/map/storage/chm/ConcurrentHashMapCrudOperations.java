package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;


import java.util.stream.Stream;

public interface ConcurrentHashMapCrudOperations<V extends AbstractEntity & UpdatableEntity, M> {
    /**
     * Creates an object in the store. ID of the {@code value} may be prescribed in id of the {@code value}.
     * If the id is {@code null} or its format is not matching the store internal format for ID, then
     * the {@code value}'s ID will be generated and returned in the id of the return value.
     * @param value Entity to create in the store
     * @throws NullPointerException if {@code value} is {@code null}
     * @see AbstractEntity#getId()
     * @return Entity representing the {@code value} in the store. It may or may not be the same instance as {@code value}
     */
    V create(V value);

    /**
     * Returns object with the given {@code key} from the storage or {@code null} if object does not exist.
     * <br>
     * TODO: Consider returning {@code Optional<V>} instead.
     * @param key Key of the object. Must not be {@code null}.
     * @return See description
     * @throws NullPointerException if the {@code key} is {@code null}
     */
    public V read(String key);

    /**
     * Updates the object with the key of the {@code value}'s ID in the storage if it already exists.
     *
     * @param value Updated value
     * @throws NullPointerException if the object or its {@code id} is {@code null}
     * @see AbstractEntity#getId()
     */
    V update(V value);

    /**
     * Deletes object with the given {@code key} from the storage, if exists, no-op otherwise.
     * @param key
     * @return Returns {@code true} if the object has been deleted or result cannot be determined, {@code false} otherwise.
     */
    boolean delete(String key);

    /**
     * Deletes objects that match the given criteria.
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Number of removed objects (might return {@code -1} if not supported)
     */
    long delete(QueryParameters<M> queryParameters);

    /**
     * Returns stream of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Stream of objects. Never returns {@code null}.
     */
    Stream<V> read(QueryParameters<M> queryParameters);

    /**
     * Returns the number of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Number of objects. Never returns {@code null}.
     */
    long getCount(QueryParameters<M> queryParameters);
}
