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

package org.keycloak.authorization.fgap;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.KeycloakSession;

import org.junit.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.EXTEND_CHILDREN_CACHE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUP_PERMISSIONS_CACHE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.SCHEMA;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PermissionCachesTest {

    private final Map<String, Object> attributes = new HashMap<>();
    private final KeycloakSession session = proxy(KeycloakSession.class, (method, args) -> switch (method) {
        case "getAttribute" -> attributes.get((String) args[0]);
        case "setAttribute" -> attributes.put((String) args[0], args[1]);
        case "removeAttribute" -> attributes.remove((String) args[0]);
        default -> throw new UnsupportedOperationException(method);
    });

    @Test
    public void testGroupPermissionsAreCachedByGroup() {
        ResourceServer resourceServer = resourceServer("rs1");
        List<Policy> teamPermissions = List.of(proxy(Policy.class, (method, args) -> {
            throw new UnsupportedOperationException(method);
        }));
        Map<String, List<Policy>> cache = new HashMap<>(Map.of("team", teamPermissions));
        attributes.put(GROUP_PERMISSIONS_CACHE + ".rs1", cache);

        // cached permissions are returned without looking up the store, which the session does not provide
        assertSame(teamPermissions, SCHEMA.getGroupPermissions(session, resourceServer, "team"));
    }

    @Test
    public void testClearPermissionCachesOnlyClearsTheGivenResourceServer() {
        attributes.put(GROUP_PERMISSIONS_CACHE + ".rs1", new HashMap<>());
        attributes.put(EXTEND_CHILDREN_CACHE + ".rs1", Boolean.TRUE);
        attributes.put(GROUP_PERMISSIONS_CACHE + ".rs2", new HashMap<>());
        attributes.put(EXTEND_CHILDREN_CACHE + ".rs2", Boolean.FALSE);

        SCHEMA.clearPermissionCaches(session, resourceServer("rs1"));

        assertNull(attributes.get(GROUP_PERMISSIONS_CACHE + ".rs1"));
        assertNull(attributes.get(EXTEND_CHILDREN_CACHE + ".rs1"));
        assertEquals(Boolean.FALSE, attributes.get(EXTEND_CHILDREN_CACHE + ".rs2"));
        assertEquals(Map.of(), attributes.get(GROUP_PERMISSIONS_CACHE + ".rs2"));
    }

    private ResourceServer resourceServer(String id) {
        return proxy(ResourceServer.class, (method, args) -> {
            if ("getId".equals(method)) {
                return id;
            }
            throw new UnsupportedOperationException(method);
        });
    }

    @FunctionalInterface
    private interface Handler {
        Object invoke(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (instance, method, args) -> {
                    if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(instance, method, args);
                    }
                    return handler.invoke(method.getName(), args);
                });
    }
}
