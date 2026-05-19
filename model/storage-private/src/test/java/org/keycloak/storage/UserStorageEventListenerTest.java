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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmProvider;

import org.junit.Test;

public class UserStorageEventListenerTest {

    @Test
    public void removedEventIsIgnoredWhenRealmDoesNotExist() {
        UserStorageEventListener listener = new UserStorageEventListener(sessionFactory(null));

        listener.eventReceived(event(true));
    }

    @Test(expected = RuntimeException.class)
    public void activeEventFailsWhenRealmDoesNotExist() {
        UserStorageEventListener listener = new UserStorageEventListener(sessionFactory(null));

        listener.eventReceived(event(false));
    }

    private static UserStorageProviderClusterEvent event(boolean removed) {
        UserStorageProviderModel provider = new UserStorageProviderModel();
        provider.setId("provider-id");
        provider.setName("provider");
        provider.setProviderId("provider");

        return UserStorageProviderClusterEvent.createEvent(removed, "missing-realm", provider);
    }

    private static KeycloakSessionFactory sessionFactory(RealmProvider realmProvider) {
        KeycloakSession session = session(realmProvider);

        return (KeycloakSessionFactory) Proxy.newProxyInstance(
                UserStorageEventListenerTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "create" -> session;
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static KeycloakSession session(RealmProvider realmProvider) {
        KeycloakContext context = context();
        KeycloakTransactionManager transactionManager = transactionManager();
        Map<String, Object> attributes = new HashMap<>();
        RealmProvider realms = realmProvider == null ? missingRealmProvider() : realmProvider;

        return (KeycloakSession) Proxy.newProxyInstance(
                UserStorageEventListenerTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContext" -> context;
                    case "getTransactionManager" -> transactionManager;
                    case "getAttribute" -> attributes.get(args[0]);
                    case "setAttribute" -> attributes.put((String) args[0], args[1]);
                    case "realms" -> realms;
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static KeycloakContext context() {
        return (KeycloakContext) Proxy.newProxyInstance(
                UserStorageEventListenerTest.class.getClassLoader(),
                new Class<?>[] { KeycloakContext.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static KeycloakTransactionManager transactionManager() {
        return (KeycloakTransactionManager) Proxy.newProxyInstance(
                UserStorageEventListenerTest.class.getClassLoader(),
                new Class<?>[] { KeycloakTransactionManager.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static RealmProvider missingRealmProvider() {
        return (RealmProvider) Proxy.newProxyInstance(
                UserStorageEventListenerTest.class.getClassLoader(),
                new Class<?>[] { RealmProvider.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == void.class) {
            return null;
        }
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
