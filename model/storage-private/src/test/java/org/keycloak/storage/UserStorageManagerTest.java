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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UserStorageManagerTest {

    private static final RuntimeException FAILURE = new RuntimeException("simulated storage failure");

    private final List<String> delegatedUsernames = new ArrayList<>();

    private UserStorageManager manager;

    @Before
    public void setUp() {
        UserProvider localStorage = stub(UserProvider.class, (method, args) -> {
            if (method.getName().equals("addUser") && args.length == 5) {
                delegatedUsernames.add((String) args[2]);
                return null;
            }
            return defaultValue(method);
        });

        KeycloakSession[] sessionRef = new KeycloakSession[1];
        KeycloakSession session = stub(KeycloakSession.class, (method, args) -> {
            if (method.getName().equals("getProvider")) {
                Class<?> requested = (Class<?>) args[0];
                if (requested == DatastoreProvider.class) {
                    return new DefaultDatastoreProvider(null, sessionRef[0]);
                }
                if (requested == UserProvider.class) {
                    return localStorage;
                }
            }
            return defaultValue(method);
        });
        sessionRef[0] = session;

        manager = new UserStorageManager(session);
    }

    @Test
    public void rejectsMissingUsername() {
        assertThrows(ModelException.class, () -> manager.addUser(null, null));
        assertThrows(ModelException.class, () -> manager.addUser(null, "id", null, true, true));
        assertTrue(delegatedUsernames.isEmpty());
    }

    @Test
    public void rejectsEmptyUsername() {
        assertThrows(ModelException.class, () -> manager.addUser(null, ""));
        assertThrows(ModelException.class, () -> manager.addUser(null, "id", "", true, true));
        assertTrue(delegatedUsernames.isEmpty());
    }

    @Test
    public void rejectsWhitespaceUsername() {
        assertThrows(ModelException.class, () -> manager.addUser(null, "   "));
        assertThrows(ModelException.class, () -> manager.addUser(null, "id", "   ", true, true));
        assertTrue(delegatedUsernames.isEmpty());
    }

    @Test
    public void lowercasesUsernameBeforeDelegating() {
        manager.addUser(null, "id", "JDoe", true, true);

        assertEquals(List.of("jdoe"), delegatedUsernames);
    }

    private static Object defaultValue(Method method) {
        Class<?> type = method.getReturnType();
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == void.class) return null;
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> iface, BiFunction<Method, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
                (proxy, method, args) -> handler.apply(method, args));
    }

    @Test
    public void realmRemovalNotifiesAfterCommitWithoutRepeatingComponentCleanup() {
        assertRealmRemovalNotifications(true);
    }

    @Test
    public void realmRemovalDoesNotNotifyAfterRollback() {
        assertRealmRemovalNotifications(false);
    }

    private void assertRealmRemovalNotifications(boolean commit) {
        List<KeycloakTransaction> afterCompletion = new ArrayList<>();
        List<StoreSyncEvent> events = new ArrayList<>();
        AtomicInteger localRealmRemovals = new AtomicInteger();
        AtomicInteger federatedRealmRemovals = new AtomicInteger();
        AtomicInteger externalRealmRemovals = new AtomicInteger();
        ComponentModel first = new UserStorageProviderModel();
        first.setId("first");
        ComponentModel second = new UserStorageProviderModel();
        second.setId("second");

        RealmModel realm = stub(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm";
            case "getComponentsStream" -> {
                assertThat(args[0], is("realm"));
                assertThat(args[1], is(UserStorageProvider.class.getName()));
                yield Stream.of(first, second);
            }
            default -> throw new AssertionError("Unexpected realm call: " + method);
        });
        UserProvider localStorage = stub(UserProvider.class, (proxy, method, args) -> {
            assertThat(method.getName(), is("preRemove"));
            assertThat(args.length, is(1));
            assertSame(realm, args[0]);
            localRealmRemovals.incrementAndGet();
            return null;
        });
        UserFederatedStorageProvider federatedStorage = stub(UserFederatedStorageProvider.class, (proxy, method, args) -> {
            assertThat(method.getName(), is("preRemove"));
            assertThat(args.length, is(1));
            assertSame(realm, args[0]);
            federatedRealmRemovals.incrementAndGet();
            return null;
        });
        KeycloakTransactionManager transactionManager = stub(KeycloakTransactionManager.class, (proxy, method, args) -> {
            assertThat(method.getName(), is("enlistAfterCompletion"));
            KeycloakTransaction transaction = (KeycloakTransaction) args[0];
            transaction.begin();
            afterCompletion.add(transaction);
            return null;
        });
        KeycloakSessionFactory sessionFactory = stub(KeycloakSessionFactory.class, (proxy, method, args) -> {
            assertThat(method.getName(), is("publish"));
            events.add((StoreSyncEvent) args[0]);
            return null;
        });
        KeycloakSession session = stub(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTransactionManager" -> transactionManager;
            case "getKeycloakSessionFactory" -> sessionFactory;
            case "getProvider" -> {
                assertSame(UserFederatedStorageProvider.class, args[0]);
                yield federatedStorage;
            }
            default -> throw new AssertionError("Unexpected session call: " + method);
        });
        UserStorageProvider externalStorage = new UserStorageProvider() {
            @Override
            public void preRemove(RealmModel removedRealm) {
                assertSame(realm, removedRealm);
                externalRealmRemovals.incrementAndGet();
            }

            @Override
            public void close() {
            }
        };
        UserStorageManager manager = new UserStorageManager(session) {
            @Override
            protected UserProvider localStorage() {
                return localStorage;
            }

            @Override
            protected <T> void consumeEnabledStorageProvidersWithTimeout(RealmModel removedRealm, Class<T> capabilityInterface,
                    Consumer<T> consumer) {
                consumer.accept(capabilityInterface.cast(externalStorage));
            }
        };

        manager.preRemove(realm);

        assertThat(localRealmRemovals.get(), is(1));
        assertThat(federatedRealmRemovals.get(), is(1));
        assertThat(externalRealmRemovals.get(), is(1));
        assertThat(events, empty());
        assertThat(afterCompletion.size(), is(2));

        afterCompletion.forEach(transaction -> {
            if (commit) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        });

        if (commit) {
            assertThat(events.size(), is(2));
            assertSame(first, events.get(0).getModel());
            assertSame(second, events.get(1).getModel());
            events.forEach(event -> {
                assertSame(realm, event.getRealm());
                assertThat(event.getRemoved(), is(true));
            });
        } else {
            assertThat(events, empty());
        }
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(UserStorageManagerTest.class.getClassLoader(), new Class<?>[] { type }, handler));
    }

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
