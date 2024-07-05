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

package org.keycloak.models.sessions.infinispan.changes.remote.remover.iteration;

import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Predicate;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;

/**
 * An iteration based implementation of {@link ConditionalRemover}.
 * <p>
 * This class is not performance efficient since it has to download the full {@link RemoteCache} content to perform the
 * removal tests.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
abstract class IterationBasedConditionalRemover<K, V> implements ConditionalRemover<K, V>, Predicate<Map.Entry<K, MetadataValue<V>>> {

    @Override
    public final void executeRemovals(RemoteCache<K, V> cache, AggregateCompletionStage<Void> stage) {
        if (isEmpty()) {
            return;
        }
        var rmStage = Flowable.fromPublisher(cache.publishEntriesWithMetadata(null, 2048))
                .filter(this)
                .map(Map.Entry::getKey)
                .flatMapCompletable(key -> Completable.fromCompletionStage(cache.removeAsync(key)))
                .toCompletionStage(null);
        stage.dependsOn(rmStage);
    }

    @Override
    public final boolean test(Map.Entry<K, MetadataValue<V>> entry) throws Throwable {
        return willRemove(entry.getKey(), entry.getValue().getValue());
    }

    /**
     * @return {@code true} if this implementation won't remove anything. It avoids iterating over the
     * {@link RemoteCache} contents.
     */
    abstract boolean isEmpty();

}
