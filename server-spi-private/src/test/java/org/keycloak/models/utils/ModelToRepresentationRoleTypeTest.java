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
package org.keycloak.models.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelToRepresentationRoleTypeTest {

    @Test
    public void roleRepresentationsMarkOnlyClientRolesAsClientRole() {
        RoleRepresentation client = ModelToRepresentation.toRepresentation(role(RoleModel.Type.CLIENT));
        RoleRepresentation realm = ModelToRepresentation.toRepresentation(role(RoleModel.Type.REALM));
        RoleRepresentation organization = ModelToRepresentation.toBriefRepresentation(role(RoleModel.Type.ORGANIZATION));

        assertTrue(client.getClientRole());
        assertFalse(realm.getClientRole());
        assertFalse(organization.getClientRole());
    }

    private static RoleModel role(RoleModel.Type type) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> type.name().toLowerCase() + "-role";
            case "getName" -> type.name().toLowerCase();
            case "getDescription" -> "description";
            case "isComposite" -> false;
            case "getType" -> type;
            case "getContainerId" -> type.name().toLowerCase() + "-container";
            case "getAttributes" -> Map.of();
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return 0;
    }
}
