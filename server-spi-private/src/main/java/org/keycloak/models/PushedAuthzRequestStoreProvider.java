/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.provider.Provider;

import java.util.Map;
import java.util.UUID;

/**
 * Provides single-use cache for Pushed Authorization Request. The data of this request may be used only once.
 */
public interface PushedAuthzRequestStoreProvider extends Provider {

    /**
     * Stores the given data and guarantees that data should be available in the store for at least the time specified by {@param lifespanSeconds} parameter.
     *
     * @param key             unique identifier
     * @param lifespanSeconds time to live
     * @param codeData        the data to store
     */
    void put(UUID key, int lifespanSeconds, Map<String, String> codeData);


    /**
     * This method returns data just if removal was successful. Implementation should guarantee that "remove" is single-use. So if
     * 2 threads (even on different cluster nodes or on different cross-dc nodes) calls "remove(123)" concurrently, then just one of them
     * is allowed to succeed and return data back. It can't happen that both will succeed.
     *
     * @param key unique identifier
     * @return context data related Pushed Authorization Request. It returns null if there is no context data available.
     */
    Map<String, String> remove(UUID key);
}
