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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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

    @Test
    public void shouldRejectIncompleteDeprecatedPasswordCredential() throws Exception {
        CredentialRepresentation deprecatedPassword = JsonSerialization.readValue(
                "{ \"type\": \"password\" }", CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("jane");
        user.setCredentials(List.of(deprecatedPassword));

        try {
            invokeConvertDeprecatedCredentialsFormat(user);
            fail("Expected ModelValidationException");
        } catch (ModelValidationException expected) {
            assertEquals("Invalid deprecated password credential format: missing hashIterations, algorithm, hashedSaltedValue, salt", expected.getMessage());
        }
    }

    @Test
    public void shouldNameMissingFieldForIncompleteDeprecatedPasswordCredential() throws Exception {
        CredentialRepresentation deprecatedPassword = JsonSerialization.readValue(
                "{ \"type\": \"password\", \"hashIterations\": 27500, \"algorithm\": \"pbkdf2-sha256\","
                        + " \"hashedSaltedValue\": \"hash\" }",
                CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("jane");
        user.setCredentials(List.of(deprecatedPassword));

        try {
            invokeConvertDeprecatedCredentialsFormat(user);
            fail("Expected ModelValidationException");
        } catch (ModelValidationException expected) {
            assertEquals("Invalid deprecated password credential format: missing salt", expected.getMessage());
        }
    }

    @Test
    public void shouldConvertCompleteDeprecatedPasswordCredential() throws Exception {
        CredentialRepresentation deprecatedPassword = JsonSerialization.readValue(
                "{ \"type\": \"password\", \"hashIterations\": 27500, \"algorithm\": \"pbkdf2-sha256\","
                        + " \"hashedSaltedValue\": \"hash\", \"salt\": \"salt\" }",
                CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("john");
        user.setCredentials(List.of(deprecatedPassword));

        invokeConvertDeprecatedCredentialsFormat(user);

        assertNotNull(deprecatedPassword.getCredentialData());
        assertNotNull(deprecatedPassword.getSecretData());
        assertEquals(Integer.valueOf(10), deprecatedPassword.getPriority());
    }

    @Test
    public void shouldRejectIncompleteDeprecatedOtpCredential() throws Exception {
        CredentialRepresentation deprecatedOtp = JsonSerialization.readValue(
                "{ \"type\": \"totp\" }", CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("jane");
        user.setCredentials(List.of(deprecatedOtp));

        try {
            invokeConvertDeprecatedCredentialsFormat(user);
            fail("Expected ModelValidationException");
        } catch (ModelValidationException expected) {
            assertEquals("Invalid deprecated OTP credential format: missing digits, counter, period, algorithm, hashedSaltedValue", expected.getMessage());
        }
    }

    @Test
    public void shouldConvertCompleteDeprecatedOtpCredential() throws Exception {
        CredentialRepresentation deprecatedOtp = JsonSerialization.readValue(
                "{ \"type\": \"totp\", \"digits\": 6, \"counter\": 0, \"period\": 30, \"algorithm\": \"HmacSHA1\","
                        + " \"hashedSaltedValue\": \"secret\" }",
                CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("john");
        user.setCredentials(List.of(deprecatedOtp));

        invokeConvertDeprecatedCredentialsFormat(user);

        assertNotNull(deprecatedOtp.getCredentialData());
        assertNotNull(deprecatedOtp.getSecretData());
        assertEquals(Integer.valueOf(20), deprecatedOtp.getPriority());
        assertEquals("otp", deprecatedOtp.getType());
    }

    private static void invokeConvertDeprecatedCredentialsFormat(UserRepresentation user) throws Exception {
        Method method = RepresentationToModel.class.getDeclaredMethod(
                "convertDeprecatedCredentialsFormat", UserRepresentation.class);
        method.setAccessible(true);
        try {
            method.invoke(null, user);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
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
