/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Local and federated user storage share the realm's database with everything else, so a failure
 * there is a general outage, not an "external provider is unreachable" situation. Graceful
 * degradation (introduced for cases like a down LDAP server) must not swallow it.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/51268">#51268</a>
 */
public class UserStorageManagerTest {

    private static final RuntimeException FAILURE = new RuntimeException("simulated storage failure");

    @Test
    public void queryWithGracefulDegradationPropagatesFailureForLocalStorage() {
        // the local/federated provider does not implement UserStorageProvider
        Object localProvider = new Object();

        UserStorageManager.PaginatedQuery failing = (provider, first, max) -> { throw FAILURE; };

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> UserStorageManager.queryWithGracefulDegradation(localProvider, failing, 0, 10));
        assertSame(FAILURE, thrown);
    }

    @Test
    public void queryWithGracefulDegradationSwallowsFailureForExternalProvider() {
        UserStorageProvider externalProvider = new UserStorageProvider() {
            @Override
            public void close() {
            }
        };

        UserStorageManager.PaginatedQuery failing = (provider, first, max) -> { throw FAILURE; };

        Stream<UserModel> result = UserStorageManager.queryWithGracefulDegradation(externalProvider, failing, 0, 10);
        assertThat(result.toList(), empty());
    }

    private static UserStorageProvider externalProvider() {
        return new UserStorageProvider() {
            @Override
            public void close() {
            }
        };
    }

    /**
     * A lazy stream that emits the users of the first page(s) and then fails while it is being
     * consumed, e.g. an LDAP server that stops responding between the pages of a paginated search.
     */
    private static UserStorageManager.PaginatedQuery lazyFailingQuery() {
        return (provider, first, max) -> Stream.concat(
                Stream.of(new UserModelDelegate(null), new UserModelDelegate(null)),
                Stream.generate(() -> {
                    throw FAILURE;
                }));
    }

    @Test
    public void queryWithGracefulDegradationSwallowsLazyFailureDuringBoundedQuery() {
        // bounded queries are materialized, so a failure raised lazily while the stream is consumed
        // degrades the provider to an empty result: the users of the pages that succeeded before the
        // failure must not surface as a partial result
        Stream<UserModel> result = UserStorageManager.queryWithGracefulDegradation(externalProvider(), lazyFailingQuery(), 0, 10);

        assertThat(result.toList(), empty());
    }

    @Test
    public void queryWithGracefulDegradationPropagatesLazyFailureDuringUnboundedQuery() {
        // unbounded queries stay lazy and stream directly to the consumer, so a failure raised
        // during that consumption propagates to the consumer
        Stream<UserModel> result = UserStorageManager.queryWithGracefulDegradation(externalProvider(), lazyFailingQuery(), 0, null);

        RuntimeException thrown = assertThrows(RuntimeException.class, result::toList);
        assertSame(FAILURE, thrown);
    }

    @Test
    public void queryWithGracefulDegradationCapsMaterializedResultToRequestedWindow() {
        // a lazy stream that ignores the pagination arguments and streams many more users than
        // requested, like the paginated LDAP search: it must not be consumed entirely, but capped
        // to the window the caller requested
        UserStorageManager.PaginatedQuery oversized = (provider, first, max) ->
                Stream.<UserModel>generate(() -> new UserModelDelegate(null)).limit(1000);

        assertThat(UserStorageManager.queryWithGracefulDegradation(externalProvider(), oversized, 0, 10).toList(), hasSize(10));
        assertThat(UserStorageManager.queryWithGracefulDegradation(externalProvider(), oversized, 7, 10).toList(), hasSize(17));
    }

    @Test
    public void queryWithGracefulDegradationDoesNotOverflowWhenPaginationWindowExceedsMaxInt() {
        // firstResult + maxResults overflows int; the cap must be computed with long arithmetic so
        // Stream.limit does not reject it and degrade a healthy provider to an empty result
        UserStorageManager.PaginatedQuery healthy = (provider, first, max) ->
                Stream.of(new UserModelDelegate(null), new UserModelDelegate(null));

        assertThat(UserStorageManager.queryWithGracefulDegradation(externalProvider(), healthy, 1, Integer.MAX_VALUE).toList(), hasSize(2));
    }

    @Test
    public void queryWithGracefulDegradationClosesProviderStreamAfterBoundedMaterialization() {
        // the materialized result is a new stream over the collected list, so the original provider
        // stream must be closed to release provider-owned resources; the unbounded path hands the
        // stream to the caller unchanged and stays untouched
        AtomicBoolean closed = new AtomicBoolean();
        UserStorageManager.PaginatedQuery closeable = (provider, first, max) ->
                Stream.<UserModel>generate(() -> new UserModelDelegate(null)).limit(5).onClose(() -> closed.set(true));

        assertThat(UserStorageManager.queryWithGracefulDegradation(externalProvider(), closeable, 0, 5).toList(), hasSize(5));
        assertThat(closed.get(), is(true));
    }

    @Test
    public void queryWithGracefulDegradationClosesProviderStreamOnLazyFailureDuringBoundedQuery() {
        // the provider stream must also be closed when the failure surfaces lazily during the
        // materialization, before the query is degraded to an empty result
        AtomicBoolean closed = new AtomicBoolean();
        UserStorageManager.PaginatedQuery lazyFailingWithCloseHandler = (provider, first, max) ->
                Stream.concat(Stream.of(new UserModelDelegate(null)),
                        Stream.<UserModel>generate(() -> { throw FAILURE; }))
                      .onClose(() -> closed.set(true));

        Stream<UserModel> result = UserStorageManager.queryWithGracefulDegradation(externalProvider(), lazyFailingWithCloseHandler, 0, 10);

        assertThat(result.toList(), empty());
        assertThat(closed.get(), is(true));
    }

    @Test
    public void countQueryWithGracefulDegradationPropagatesFailureForLocalStorage() {
        Object localProvider = new Object();
        UserStorageManager manager = new UserStorageManager(null);

        UserStorageManager.CountQuery failing = (provider, first, max) -> { throw FAILURE; };

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> manager.countQueryWithGracefulDegradation(localProvider, failing, 0, 10));
        assertSame(FAILURE, thrown);
    }

    @Test
    public void countQueryWithGracefulDegradationSwallowsFailureForExternalProvider() {
        UserStorageProvider externalProvider = new UserStorageProvider() {
            @Override
            public void close() {
            }
        };
        UserStorageManager manager = new UserStorageManager(null);

        UserStorageManager.CountQuery failing = (provider, first, max) -> { throw FAILURE; };

        int count = manager.countQueryWithGracefulDegradation(externalProvider, failing, 0, 10);
        assertThat(count, is(0));
    }
}
