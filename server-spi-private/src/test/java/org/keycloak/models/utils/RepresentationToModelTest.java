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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.keycloak.models.ModelException;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.junit.Assert;
import org.junit.Test;

public class RepresentationToModelTest {

    @Test
    public void shouldRejectIncompleteDeprecatedPasswordCredential() throws Exception {
        CredentialRepresentation deprecatedPassword = JsonSerialization.readValue(
                "{ \"type\": \"password\" }", CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("jane");
        user.setCredentials(List.of(deprecatedPassword));

        try {
            invokeConvertDeprecatedCredentialsFormat(user);
            Assert.fail("Expected ModelException");
        } catch (ModelException expected) {
            Assert.assertEquals("Invalid deprecated password credential format", expected.getMessage());
        }
    }

    @Test
    public void shouldConvertCompleteDeprecatedPasswordCredential() throws Exception {
        CredentialRepresentation deprecatedPassword = JsonSerialization.readValue(
                "{ \"type\": \"password\", \"hashIterations\": 27500, \"algorithm\": \"pbkdf2-sha256\", \"hashedSaltedValue\": \"hash\", \"salt\": \"salt\" }",
                CredentialRepresentation.class);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("john");
        user.setCredentials(List.of(deprecatedPassword));

        invokeConvertDeprecatedCredentialsFormat(user);

        Assert.assertNotNull(deprecatedPassword.getCredentialData());
        Assert.assertNotNull(deprecatedPassword.getSecretData());
        Assert.assertEquals(Integer.valueOf(10), deprecatedPassword.getPriority());
    }

    private static void invokeConvertDeprecatedCredentialsFormat(UserRepresentation user) throws Exception {
        Method method = RepresentationToModel.class.getDeclaredMethod("convertDeprecatedCredentialsFormat", UserRepresentation.class);
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
}
