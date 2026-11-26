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
package org.keycloak.services.clientregistration.policy.impl;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefaultRolesClientRegistrationPolicyTest {

    private final DefaultRolesClientRegistrationPolicy policy = new DefaultRolesClientRegistrationPolicy();

    @Test
    public void beforeRegisterRejectsNullElements() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setDefaultRoles(new String[]{null});
        try {
            policy.beforeRegister(context(rep));
            fail("Expected ClientRegistrationPolicyException");
        } catch (ClientRegistrationPolicyException expected) {
            assertEquals("Not permitted to set null defaultRoles", expected.getMessage());
        }
    }

    @Test
    public void beforeUpdateRejectsNullElementsEvenWhenCurrentDefaultsAreEmpty() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setDefaultRoles(new String[]{null});
        try {
            policy.beforeUpdate(context(rep), clientWithoutDefaultRoles());
            fail("Expected ClientRegistrationPolicyException");
        } catch (ClientRegistrationPolicyException expected) {
            assertEquals("Not permitted to set null defaultRoles", expected.getMessage());
        }
    }

    @Test
    public void beforeRegisterRejectsNonEmptyDefaultRoles() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setDefaultRoles(new String[]{"role-a"});
        try {
            policy.beforeRegister(context(rep));
            fail("Expected ClientRegistrationPolicyException");
        } catch (ClientRegistrationPolicyException expected) {
            assertEquals("Not permitted to set defaultRoles", expected.getMessage());
        }
    }

    private static ClientRegistrationContext context(ClientRepresentation rep) {
        return new ClientRegistrationContext() {
            @Override
            public ClientRepresentation getClient() {
                return rep;
            }

            @Override
            public KeycloakSession getSession() {
                return null;
            }

            @Override
            public ClientRegistrationProvider getProvider() {
                return null;
            }
        };
    }

    private static ClientModel clientWithoutDefaultRoles() {
        RoleModel defaultRole = (RoleModel) Proxy.newProxyInstance(RoleModel.class.getClassLoader(),
                new Class<?>[]{RoleModel.class},
                (proxy, method, args) -> {
                    if ("getCompositesStream".equals(method.getName())) {
                        return Stream.empty();
                    }
                    if (boolean.class.equals(method.getReturnType())) {
                        return false;
                    }
                    return null;
                });

        RealmModel realm = (RealmModel) Proxy.newProxyInstance(RealmModel.class.getClassLoader(),
                new Class<?>[]{RealmModel.class},
                (proxy, method, args) -> {
                    if ("getDefaultRole".equals(method.getName())) {
                        return defaultRole;
                    }
                    if (boolean.class.equals(method.getReturnType())) {
                        return false;
                    }
                    return null;
                });

        return (ClientModel) Proxy.newProxyInstance(ClientModel.class.getClassLoader(),
                new Class<?>[]{ClientModel.class},
                (proxy, method, args) -> {
                    if ("getId".equals(method.getName())) {
                        return "client-id";
                    }
                    if ("getRealm".equals(method.getName())) {
                        return realm;
                    }
                    if (boolean.class.equals(method.getReturnType())) {
                        return false;
                    }
                    return null;
                });
    }
}
