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

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public interface MapStorage<K, V> {

    /**
     * Creates an object in the store identified by given {@code key}.
     * @param key Key of the object as seen in the logical level
     * @param value Entity
     * @return Reference to the entity created in the store
     * @throws NullPointerException if object or its {@code id} is {@code null}
     */
    V create(K key, V value);

    /**
     * Returns object with the given {@code key} from the storage or {@code null} if object does not exist.
     * @param key Must not be {@code null}.
     * @return See description
     */
    V read(K key);

    /**
     * Returns stream of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param criteria
     * @return Stream of objects. Never returns {@code null}.
     */
    Stream<V> read(ModelCriteriaBuilder criteria);

    /**
     * Updates the object with the given {@code id} in the storage if it already exists.
     * @param id
     * @throws NullPointerException if object or its {@code id} is {@code null}
     */
    V update(K key, V value);

    /**
     * Deletes object with the given {@code key} from the storage, if exists, no-op otherwise.
     * @param key
     */
    V delete(K key);

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
     * @return See description
     */
    ModelCriteriaBuilder createCriteriaBuilder();

    @Deprecated
    Set<Map.Entry<K,V>> entrySet();

}
