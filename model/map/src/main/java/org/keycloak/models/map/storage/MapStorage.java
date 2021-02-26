/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import java.util.stream.Stream;

/**
 * Implementation of this interface interacts with a persistence storage storing various entities, e.g. users, realms.
 * It contains basic object CRUD operations as well as bulk {@link #read(org.keycloak.models.map.storage.ModelCriteriaBuilder)}
 * and bulk {@link #delete(org.keycloak.models.map.storage.ModelCriteriaBuilder)} operations, 
 * and operation for determining the number of the objects satisfying given criteria
 * ({@link #getCount(org.keycloak.models.map.storage.ModelCriteriaBuilder)}).
 *
 * @author hmlnarik
 * @param <K> Type of the primary key. Various storages can
 * @param <V> Type of the stored values that contains all the data stripped of session state. In other words, in the entities
 *            there are only IDs and mostly primitive types / {@code String}, never references to {@code *Model} instances.
 *            See the {@code Abstract*Entity} classes in this module.
 * @param <M> Type of the {@code *Model} corresponding to the stored value, e.g. {@code UserModel}. This is used for
 *            filtering via model fields in {@link ModelCriteriaBuilder} which is necessary to abstract from physical
 *            layout and thus to support no-downtime upgrade.
 */
public interface MapStorage<K, V extends AbstractEntity<K>, M> {

    /**
     * Creates an object in the store identified by given {@code key}.
     * @param key Key of the object as seen in the logical level
     * @param value Entity
     * @return Reference to the entity created in the store
     * @throws NullPointerException if object or its {@code key} is {@code null}
     */
    V create(K key, V value);

    /**
     * Returns object with the given {@code key} from the storage or {@code null} if object does not exist.
     * <br>
     * TODO: Consider returning {@code Optional<V>} instead.
     * @param key Key of the object. Must not be {@code null}.
     * @return See description
     * @throws NullPointerException if the {@code key} is {@code null}
     */
    V read(K key);

    /**
     * Returns stream of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param criteria Criteria filtering out the object, originally obtained 
     *   from {@link #createCriteriaBuilder()} method of this object.
     *   If {@code null}, it returns an empty stream.
     * @return Stream of objects. Never returns {@code null}.
     * @throws IllegalStateException If {@code criteria} is not compatible, i.e. has not been originally created
     *   by the {@link #createCriteriaBuilder()} method of this object.
     */
    Stream<V> read(ModelCriteriaBuilder<M> criteria);

    /**
     * Returns the number of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param criteria
     * @return Number of objects. Never returns {@code null}.
     * @throws IllegalStateException If {@code criteria} is not compatible, i.e. has not been originally created
     *   by the {@link #createCriteriaBuilder()} method of this object.
     */
    long getCount(ModelCriteriaBuilder<M> criteria);

    /**
     * Updates the object with the given {@code id} in the storage if it already exists.
     * @param key Primary key of the object to update
     * @param value Updated value
     * @throws NullPointerException if object or its {@code id} is {@code null}
     */
    V update(K key, V value);

    /**
     * Deletes object with the given {@code key} from the storage, if exists, no-op otherwise.
     * @param key
     * @return Returns {@code true} if the object has been deleted or result cannot be determined, {@code false} otherwise.
     */
    boolean delete(K key);

    /**
     * Deletes objects that match the given criteria.
     * @param criteria
     * @return Number of removed objects (might return {@code -1} if not supported)
     * @throws IllegalStateException If {@code criteria} is not compatible, i.e. has not been originally created
     *   by the {@link #createCriteriaBuilder()} method of this object.
     */
    long delete(ModelCriteriaBuilder<M> criteria);

    
    /**
     * Returns criteria builder for the storage engine.
     * The criteria are specified in the given criteria builder based on model properties.
     * <br>
     * <b>Note:</b> While the criteria are formulated in terms of model properties,
     * the storage engine may in turn process them into the best form that suits the
     * underlying storage engine query language, e.g. to conditions on storage
     * attributes or REST query parameters.
     * If possible, do <i>not</i> delay filtering after the models are reconstructed from
     * storage entities, in most cases this would be highly inefficient.
     *
     * @return See description. Never returns {@code null}
     */
    ModelCriteriaBuilder<M> createCriteriaBuilder();

    
    /**
     * Creates a {@code MapKeycloakTransaction} object that tracks a new transaction related to this storage.
     * In case of JPA or similar, the transaction object might be supplied by the container (via JTA) or
     * shared same across storages accessing the same database within the same session; in other cases
     * (e.g. plain map) a separate transaction handler might be created per each storage.
     *
     * @return See description. Never returns {@code null}
     */
    public MapKeycloakTransaction<K, V, M> createTransaction(KeycloakSession session);

    /**
     * Returns a {@link StringKeyConvertor} that is used to convert primary keys
     * from {@link String} to internal representation and vice versa.
     * 
     * @return See above. Never returns {@code null}.
     */
    public StringKeyConvertor<K> getKeyConvertor();

}
