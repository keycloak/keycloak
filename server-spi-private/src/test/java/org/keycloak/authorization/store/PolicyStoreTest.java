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

package org.keycloak.authorization.store;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.authorization.model.Policy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PolicyStoreTest {

    @Test
    public void testExistsByResourceTypeAndConfigMatchesExactValue() {
        PolicyStore store = store(Map.of("Groups", List.of(policy(Map.of()), policy(Map.of("extendChildren", "true")))));

        assertTrue(store.existsByResourceTypeAndConfig(null, "Groups", "extendChildren", "true"));
        assertFalse(store.existsByResourceTypeAndConfig(null, "Groups", "extendChildren", "TRUE"));
        assertFalse(store.existsByResourceTypeAndConfig(null, "Groups", "other", "true"));
        assertFalse(store.existsByResourceTypeAndConfig(null, "Users", "extendChildren", "true"));
    }

    @SuppressWarnings("unchecked")
    private static PolicyStore store(Map<String, List<Policy>> policiesByType) {
        return proxy(PolicyStore.class, (instance, method, args) -> {
            if ("findByResourceType".equals(method.getName()) && args.length == 3) {
                policiesByType.getOrDefault((String) args[1], List.of()).forEach((Consumer<Policy>) args[2]);
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Policy policy(Map<String, String> config) {
        return proxy(Policy.class, (instance, method, args) -> {
            if ("getConfig".equals(method.getName())) {
                return config;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (instance, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(instance, method, args);
            }
            return handler.invoke(instance, method, args);
        });
    }
}
