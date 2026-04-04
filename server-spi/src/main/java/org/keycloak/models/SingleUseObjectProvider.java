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
     * Suffix to a key to indicate that token is considered revoked.
     * For revoked tokens, only the methods {@link #put} and {@link #contains} must be used.
     */
    String REVOKED_KEY = ".revoked";

    /**
     * Stores the given data and guarantees that data should be available in the store for at least the time specified by {@param lifespanSeconds} parameter
     * @param key String
     * @param lifespanSeconds Minimum lifespan for which successfully added key will be kept in the cache.
     * @param notes For revoked tokens, this must be an empty Map.
     */
    void put(String key, long lifespanSeconds, Map<String, String> notes);

    /**
     * Gets data associated with the given key.
     * @param key String
     * @return Map<String, String> Data associated with the given key or {@code null} if there is no associated data.
     */
    Map<String, String> get(String key);

    /**
     * This method returns data just if removal was successful. Implementation should guarantee that "remove" is single-use. So if
     * 2 threads (even on different cluster nodes or on different multi-site nodes) calls "remove(123)" concurrently, then just one of them
     * is allowed to succeed and return data back. It can't happen that both will succeed.
     *
     * @param key String
     * @return context data associated to the key. It returns {@code null} if there are no context data available.
     */
    Map<String, String> remove(String key);

    /**
     * Replaces data associated with the given key in the store if the store contains the key.
     * @param key String
     * @param notes Map<String, String> New data to be stored
     * @return {@code true} if the store contains the key and data was replaced, otherwise {@code false}.
     */
    boolean replace(String key, Map<String, String> notes);

    /**
     * Will try to put the key into the cache. It will succeed just if key is not already there.
     *
     * @param key
     * @param lifespanInSeconds Minimum lifespan for which successfully added key will be kept in the cache.
     * @return true if the key was successfully put into the cache. This means that same key wasn't in the cache before
     */
    boolean putIfAbsent(String key, long lifespanInSeconds);

    /**
     * Checks if there is a record in the store for the given key.
     * @param key String
     * @return {@code true} if the record is present in the store, {@code false} otherwise.
     */
    boolean contains(String key);
}
