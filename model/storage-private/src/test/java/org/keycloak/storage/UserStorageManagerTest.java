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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UserStorageManagerTest {

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
}
