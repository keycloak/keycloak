/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes.remote.remover;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;

/**
 * A {@link ConditionalRemover} that does not remove anything.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
public class EmptyConditionalRemover<K, V> implements ConditionalRemover<K, V> {

    private static final EmptyConditionalRemover<?, ?> INSTANCE = new EmptyConditionalRemover<>();

    @SuppressWarnings("unchecked")
    public static <K1, V1> ConditionalRemover<K1, V1> instance() {
        return (ConditionalRemover<K1, V1>) INSTANCE;
    }


    @Override
    public boolean willRemove(K key, V value) {
        return false;
    }

    @Override
    public void executeRemovals(RemoteCache<K, V> cache, AggregateCompletionStage<Void> stage) {
        //no-op
    }
}
