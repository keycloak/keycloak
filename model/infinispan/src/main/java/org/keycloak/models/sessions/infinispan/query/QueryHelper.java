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

package org.keycloak.models.sessions.infinispan.query;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.infinispan.client.hotrod.impl.query.RemoteQuery;
import org.infinispan.commons.api.query.Query;
import org.infinispan.query.dsl.QueryResult;

public final class QueryHelper {

    /**
     * Converts a single projection results into a long value.
     */
    public static final Function<Object[], Long> SINGLE_PROJECTION_TO_LONG = projection -> {
        assert projection.length == 1;
        return (long) projection[0];
    };

    /**
     * Converts a single projection value into a {@link String}.
     */
    public static final Function<Object[], String> SINGLE_PROJECTION_TO_STRING = projection -> {
        assert projection.length == 1;
        return String.valueOf(projection[0]);
    };

    /**
     * Converts a projection with two values into a {@link Map.Entry} of {@link String} and {@link Long}, where the key
     * is the first projection, and the second is the second project.
     */
    public static final Function<Object[], Map.Entry<String, Long>> PROJECTION_TO_STRING_LONG_ENTRY = projection -> {
        assert projection.length == 2;
        return Map.entry((String) projection[0], (long) projection[1]);
    };

    private QueryHelper() {
    }

    /**
     * Fetches a single value from the query.
     * <p>
     * This method changes the {@link Query} state to return just a single value.
     *
     * @param query   The {@link Query} instance.
     * @param mapping The {@link Function} that maps the query results (projection) into the result.
     * @param <T>     The {@link Query} response type.
     * @param <R>     The {@link Optional} type.
     * @return An {@link Optional} with the {@link Query} results mapped.
     */
    public static <T, R> Optional<R> fetchSingle(Query<T> query, Function<T, R> mapping) {
        query.hitCountAccuracy(1).maxResults(1);
        try (var iterator = query.iterator()) {
            return iterator.hasNext() ? Optional.ofNullable(mapping.apply(iterator.next())) : Optional.empty();
        }
    }

    /**
     * Streams using batching over all results from the {@link Query}.
     * <p>
     * If a large result set is expected, this method is recommended to avoid loading downloading a lot of data in a
     * single request.
     * <p>
     * The results are fetched on demand.
     * <p>
     * Warning: This method changes ignores the start offset and the max results. It will return everything.
     *
     * @param query     The {@link Query} instance.
     * @param batchSize The number of results to fetch for each remote request.
     * @param mapping   The {@link Function} that maps the query results (projection) into the result.
     * @param <T>       The {@link Query} response type.
     * @param <R>       The {@link Stream} type.
     * @return A {@link Stream} with the results.
     */
    public static <T, R> Stream<R> streamAll(Query<T> query, int batchSize, Function<T, R> mapping) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new BatchingIterator<>(query, batchSize, mapping), 0), false);
    }

    /**
     * Performs the {@link Query} and returns the results.
     * <p>
     * This method is preferred to {@link Query#list()} since it does not have to compute an accurate hit count (affects
     * Indexed query performance).
     * <p>
     * If a large dataset is expected, use {@link #streamAll(Query, int, Function)}.
     *
     * @param query   The {@link Query} instance.
     * @param mapping The {@link Function} that maps the query results (projection) into the result.
     * @param <T>     The {@link Query} response type.
     * @param <R>     The {@link Collection} type.
     * @return A {@link Collection} with the results.
     */
    public static <T, R> Collection<R> toCollection(Query<T> query, Function<T, R> mapping) {
        try (var iterator = query.iterator()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                    .map(mapping)
                    .collect(Collectors.toList());
        }
    }

    // TODO to be removed. A publisher was added to the Infinispan API since version 15.1.
    private static class BatchingIterator<T, R> implements Iterator<R> {

        private final RemoteQuery<T> query;
        private final int batchSize;
        private final Function<T, R> mapping;
        private int currentOffset;
        private Iterator<T> currentResults;
        private CompletableFuture<QueryResult<T>> nextResults;
        private R next;
        private boolean completed;

        private BatchingIterator(Query<T> query, int batchSize, Function<T, R> mapping) {
            assert query instanceof RemoteQuery<T>;
            this.query = (RemoteQuery<T>) query.startOffset(0).hitCountAccuracy(batchSize).maxResults(batchSize);
            this.batchSize = batchSize;
            this.mapping = mapping;
            currentResults = Collections.emptyIterator();
            executeQueryAsync();
            fetchNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public R next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            var result = next;
            fetchNext();
            return result;
        }

        private void executeQueryAsync() {
            nextResults = query.executeAsync().toCompletableFuture();
        }

        private void fetchNext() {
            while (true) {
                while (currentResults.hasNext()) {
                    next = mapping.apply(currentResults.next());
                    if (next != null) {
                        return;
                    }
                }
                if (completed) {
                    next = null;
                    return;
                }
                useNextResultsAndRequestMore();
            }
        }

        private void useNextResultsAndRequestMore() {
            var rsp = nextResults.join();
            var resultList = rsp.list();
            if (resultList.isEmpty()) {
                completed = true;
                return;
            }
            currentResults = resultList.iterator();
            if (resultList.size() < batchSize) {
                completed = true;
                return;
            }
            currentOffset += resultList.size();
            if (rsp.count().isExact() && currentOffset >= rsp.count().value()) {
                completed = true;
                return;
            }
            query.startOffset(currentOffset);
            executeQueryAsync();
        }
    }

}
