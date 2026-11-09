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
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.timer.TimerProvider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

/**
 * Periodic user storage sync tasks must cancel themselves when the object they synchronize is gone,
 * otherwise they keep failing once per period until the server is restarted.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/54744">#54744</a>
 */
public class UserStorageSyncTaskTest {

    private static final String REALM_ID = "realm-id";
    private static final String PROVIDER_ID = "storage-id";

    @Test
    public void runCancelsTaskWhenRealmIsRemoved() {
        // No realm found, simulating a deleted realm
        AtomicReference<String> cancelledTask = new AtomicReference<>();
        KeycloakSession session = createSession(null, cancelledTask);

        UserStorageSyncTask task = createTask();

        ModelIllegalStateException thrown = assertThrows(ModelIllegalStateException.class, () -> task.run(session));

        assertThat(thrown.getMessage(), is("Realm with id " + REALM_ID + " not found"));
        assertThat(cancelledTask.get(), is(task.getTaskName()));
    }

    @Test
    public void runCancelsTaskWhenComponentIsRemoved() {
        RealmModel realm = stub(RealmModel.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getComponent":
                    return null;
                case "getName":
                    return REALM_ID;
                default:
                    return defaultReturn(method);
            }
        });

        AtomicReference<String> cancelledTask = new AtomicReference<>();
        KeycloakSession session = createSession(realm, cancelledTask);

        UserStorageSyncTask task = createTask();

        ModelIllegalStateException thrown = assertThrows(ModelIllegalStateException.class, () -> task.run(session));

        assertThat(thrown.getMessage(), is("User storage provider with id " + PROVIDER_ID + " not found in realm " + REALM_ID));
        assertThat(cancelledTask.get(), is(task.getTaskName()));
    }

    private UserStorageSyncTask createTask() {
        UserStorageProviderModel provider = new UserStorageProviderModel();
        provider.setId(PROVIDER_ID);
        provider.setParentId(REALM_ID);
        provider.setName("test-storage");
        provider.setChangedSyncPeriod(30);
        return new UserStorageSyncTask(provider, UserStorageProviderModel.SyncMode.CHANGED);
    }

    private KeycloakSession createSession(RealmModel realm, AtomicReference<String> cancelledTask) {
        KeycloakContext context = stub(KeycloakContext.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "setRealm":
                    return null;
                case "getRealm":
                    return realm;
                default:
                    return defaultReturn(method);
            }
        });

        RealmProvider realms = stub(RealmProvider.class, (proxy, method, args) -> {
            if ("getRealm".equals(method.getName())) {
                return realm;
            }
            return defaultReturn(method);
        });

        ClusterProvider cluster = stub(ClusterProvider.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "isPrimaryClusterSupported":
                case "isPrimaryCluster":
                    return true;
                default:
                    return defaultReturn(method);
            }
        });

        TimerProvider timer = stub(TimerProvider.class, (proxy, method, args) -> {
            if ("cancelTask".equals(method.getName())) {
                cancelledTask.set((String) args[0]);
                return null;
            }
            return defaultReturn(method);
        });

        return stub(KeycloakSession.class, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getProvider":
                    if (TimerProvider.class.equals(args[0])) {
                        return timer;
                    }
                    if (ClusterProvider.class.equals(args[0])) {
                        return cluster;
                    }
                    return null;
                case "realms":
                    return realms;
                case "getContext":
                    return context;
                default:
                    return defaultReturn(method);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(UserStorageSyncTaskTest.class.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultReturn(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return 0;
    }
}
