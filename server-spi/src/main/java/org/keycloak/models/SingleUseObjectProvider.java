/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.util.Map;

import org.keycloak.provider.Provider;

/**
 * Provides a cache to store data for single-use use case or the details about used action tokens.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SingleUseObjectProvider extends Provider {

    /**
     * Suffix appended to a key to indicate that a token is considered revoked. For revoked tokens, only the methods
     * {@link #put} and {@link #contains} must be used.
     */
    String REVOKED_KEY = ".revoked";

    /**
     * Stores the given data and guarantees that data should be available in the store for at least the time specified
     * by {@code lifespanSeconds} parameter.
     *
     * @param key             identifier for the single-use object. Must not be {@code null}.
     * @param lifespanSeconds minimum lifespan for which the key will be kept in the store. Must be positive.
     * @param notes           data to associate with the key. For revoked tokens, this must be an empty Map.
     * @throws NullPointerException     if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code lifespanSeconds} is not positive.
     */
    void put(String key, long lifespanSeconds, Map<String, String> notes);

    /**
     * Gets data associated with the given key.
     *
     * @param key identifier for the single-use object. Must not be {@code null}.
     * @return data associated with the given key, or {@code null} if there is no associated data.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    Map<String, String> get(String key);

    /**
     * Returns data only if the removal was successful. Implementations must guarantee that removal is single-use: if
     * two threads (even on different cluster nodes or different multi-site nodes) call {@code remove(key)}
     * concurrently, only one of them is allowed to succeed and return data. It must not happen that both succeed.
     *
     * @param key identifier for the single-use object. Must not be {@code null}.
     * @return context data associated with the key, or {@code null} if there is no context data available.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    Map<String, String> remove(String key);

    /**
     * Replaces data associated with the given key in the store if the store contains the key.
     *
     * @param key   identifier for the single-use object. Must not be {@code null}.
     * @param notes new data to be stored.
     * @return {@code true} if the store contains the key and data was replaced, otherwise {@code false}.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    boolean replace(String key, Map<String, String> notes);

    /**
     * Tries to put the key into the store. It will succeed only if the key is not already present.
     *
     * @param key               identifier for the single-use object. Must not be {@code null}.
     * @param lifespanInSeconds minimum lifespan for which the key will be kept in the store. Must be positive.
     * @return {@code true} if the key was successfully put into the store, meaning the same key wasn't in the store
     * before.
     * @throws NullPointerException     if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code lifespanInSeconds} is not positive.
     */
    boolean putIfAbsent(String key, long lifespanInSeconds);

    /**
     * Checks if there is a record in the store for the given key.
     *
     * @param key identifier for the single-use object. Must not be {@code null}.
     * @return {@code true} if the record is present in the store, {@code false} otherwise.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    boolean contains(String key);
}
