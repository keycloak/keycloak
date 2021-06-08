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

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.map.common.AbstractEntity;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface MapKeycloakTransaction<K, V extends AbstractEntity<K>, M> extends KeycloakTransaction {

    /**
     * Instructs this transaction to add a new value into the underlying store on commit.
     *
     * @param key an identifier that will be associated with the {@code value}
     * @param value the value
     */
    void create(K key, V value);

    /**
     * Provides possibility to lookup for values by a {@code key} in the underlying store with respect to changes done
     * in current transaction.
     *
     * @param key identifier of a value
     * @return a value associated with the given {@code key}
     */
    V read(K key);

    /**
     * Looks up a value in the current transaction with corresponding key, returns {@code defaultValueFunc} when
     * the transaction does not contain a value for the {@code key} identifier.
     *
     * @param key identifier of a value
     * @param defaultValueFunc fallback function if the transaction does not contain a value that corresponds to {@code key}
     * @return a value associated with the given {@code key}, or the result of {@code defaultValueFunc}
     *
     */
    V read(K key, Function<K, V> defaultValueFunc);

    /**
     * Returns a stream of values from underlying storage that are updated based on the current transaction changes;
     * i.e. the result contains updates and excludes of records that have been created, updated or deleted in this
     * transaction by methods {@link MapKeycloakTransaction#create}, {@link MapKeycloakTransaction#update},
     * {@link MapKeycloakTransaction#delete}, etc.
     *
     * @param mcb criteria to filter values
     * @return values that fulfill the given criteria, that are updated based on changes in the current transaction
     */
    Stream<V> read(ModelCriteriaBuilder<M> mcb);


    /**
     * Returns a number of values present in the underlying storage that fulfill the given criteria with respect to
     * changes done in the current transaction.
     *
     * @param mcb criteria to filter values
     * @return number of values present in the storage that fulfill the given criteria
     */
    long getCount(ModelCriteriaBuilder<M> mcb);

    /**
     * Instructs this transaction to force-update the {@code value} associated with the identifier {@code key} in the
     * underlying store on commit.
     *
     * @param key identifier of the {@code value}
     * @param value updated version of the value
     */
    void update(K key, V value);

    /**
     * Returns an updated version of the {@code orig} object as updated in this transaction.
     *
     * If the underlying store handles transactions on its own, this can return {@code orig} directly.
     *
     * @param orig possibly stale version of some object from the underlying store
     * @return the {@code orig} object as visible from this transaction, or {@code null} if the object has been removed.
     */
    default V getUpdated(V orig) {
        return orig;
    }

    /**
     * Instructs this transaction to update the {@code value} associated with the identifier {@code key} in the
     * underlying store on commit, if by the time of {@code commit} the {@code shouldPut} predicate returns {@code true}
     *
     * @param key identifier of the {@code value}
     * @param value new version of the value
     * @param shouldPut predicate to check in commit phase
     */
    void updateIfChanged(K key, V value, Predicate<V> shouldPut);

    /**
     * Instructs this transaction to delete a value associated with the identifier {@code key} from the underlying store
     * on commit.
     *
     * @param key identifier of a value
     */
    void delete(K key);

    /**
     * Instructs this transaction to remove values (identified by {@code mcb} filter) from the underlying store on commit.
     *
     * @param artificialKey key to record the transaction with, must be a key that does not exist in this transaction to
     *                      prevent collisions with other operations in this transaction
     * @param mcb criteria to delete values
     */
    long delete(K artificialKey, ModelCriteriaBuilder<M> mcb);

}
