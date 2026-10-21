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

import java.util.stream.Stream;

import org.keycloak.models.UserModel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
