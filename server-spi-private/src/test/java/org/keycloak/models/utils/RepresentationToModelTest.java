/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RepresentationToModelTest {

    @Test
    public void importRolesReusesExistingClientRole() {
        Map<String, RoleModel> clientRoles = new HashMap<>();
        AtomicInteger addedRoles = new AtomicInteger();

        RoleModel existingRole = role("CALL_ACCESS");
        clientRoles.put("CALL_ACCESS", existingRole);

        ClientModel client = client(clientRoles, addedRoles);
        RealmModel realm = realm("account", client);

        RolesRepresentation roles = new RolesRepresentation();
        roles.setClient(Collections.singletonMap("account",
                Collections.singletonList(new RoleRepresentation("CALL_ACCESS", "imported", false))));

        RepresentationToModel.importRoles(roles, realm);

        assertEquals(0, addedRoles.get());
        assertEquals("imported", existingRole.getDescription());
    }

    private static RealmModel realm(String clientId, ClientModel client) {
        return (RealmModel) Proxy.newProxyInstance(RepresentationToModelTest.class.getClassLoader(), new Class[]{RealmModel.class},
                (proxy, method, args) -> {
                    if ("getClientByClientId".equals(method.getName()) && clientId.equals(args[0])) {
                        return client;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static ClientModel client(Map<String, RoleModel> roles, AtomicInteger addedRoles) {
        return (ClientModel) Proxy.newProxyInstance(RepresentationToModelTest.class.getClassLoader(), new Class[]{ClientModel.class},
                (proxy, method, args) -> {
                    if ("getRole".equals(method.getName())) {
                        return roles.get(args[0]);
                    }
                    if ("addRole".equals(method.getName())) {
                        addedRoles.incrementAndGet();
                        RoleModel role = role(args.length == 1 ? (String) args[0] : (String) args[1]);
                        roles.put(role.getName(), role);
                        return role;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static RoleModel role(String name) {
        return (RoleModel) Proxy.newProxyInstance(RepresentationToModelTest.class.getClassLoader(), new Class[]{RoleModel.class},
                new Object() {
                    private String description;

                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("getName".equals(method.getName())) {
                            return name;
                        }
                        if ("getDescription".equals(method.getName())) {
                            return description;
                        }
                        if ("setDescription".equals(method.getName())) {
                            description = (String) args[0];
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
                }::invoke);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (void.class.equals(returnType)) {
            return null;
        }
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return 0;
    }
}
