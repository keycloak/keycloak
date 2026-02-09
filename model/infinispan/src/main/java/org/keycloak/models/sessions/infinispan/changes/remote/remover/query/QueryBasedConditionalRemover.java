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

package org.keycloak.models.sessions.infinispan.changes.remote.remover.query;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.query.RemoteQuery;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.jboss.logging.Logger;

/**
 * An implementation of {@link ConditionalRemover} that uses the delete statement to remove entries from a
 * {@link RemoteCache}.
 * <p>
 * This class is generic and requires the concrete implementation to provide the entity, the condition clause and the
 * parameters.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
abstract class QueryBasedConditionalRemover<K, V> implements ConditionalRemover<K, V> {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private static final String QUERY_FMT = "DELETE FROM %s WHERE %s";

    @Override
    public void executeRemovals(RemoteCache<K, V> cache, AggregateCompletionStage<Void> stage) {
        if (isEmpty()) {
            return;
        }
        stage.dependsOn(executeDeleteStatement(cache));
    }

    private CompletionStage<?> executeDeleteStatement(RemoteCache<K, V> cache) {
        var isTrace = logger.isTraceEnabled();
        var deleteStatement = QUERY_FMT.formatted(getEntity(), getQueryConditions());
        if (isTrace) {
            logger.tracef("About to execute delete statement in cache '%s': %s", cache.getName(), deleteStatement);
        }
        RemoteQuery<?> query = (RemoteQuery<?>) cache.query(deleteStatement)
                .setParameters(getQueryParameters());
        var stage = query.executeStatementAsync();
        if (isTrace) {
            return stage.thenAccept(removed -> logger.debugf("Delete Statement removed %d entries from cache '%s'", removed, cache.getName()));
        }
        return stage;
    }

    /**
     * @return The Infinispan ProtoStream entity.
     */
    abstract String getEntity();

    /**
     * @return The remove condition clause to test.
     */
    abstract String getQueryConditions();

    /**
     * @return The {@link Map} with the parameter name and its value. If the condition does not have any parameter, it
     * should return an empty map.
     */
    abstract Map<String, Object> getQueryParameters();

    /**
     * @return {@code true} if the concrete implement won't remove anything. This is an optimization to avoid creating
     * and sending the delete statement.
     */
    abstract boolean isEmpty();
}
