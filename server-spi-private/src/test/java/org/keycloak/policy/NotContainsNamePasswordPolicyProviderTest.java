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
package org.keycloak.policy;

import java.lang.reflect.Proxy;

import org.keycloak.models.UserModel;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NotContainsNamePasswordPolicyProviderTest {

    private final NotContainsNamePasswordPolicyProvider provider = new NotContainsNamePasswordPolicyProvider();

    @Test
    public void passwordEqualsFirstNameShouldReject() {
        PolicyError error = provider.validate(null, user("Alice", "Smith", null), "alice");

        assertNotNull(error);
    }

    @Test
    public void passwordContainsFirstNameWithDifferentCaseShouldReject() {
        PolicyError error = provider.validate(null, user("Alice", "Smith", null), "ALICE2024!");

        assertNotNull(error);
    }

    @Test
    public void passwordContainsLastNameShouldReject() {
        PolicyError error = provider.validate(null, user("John", "Smith", null), "mySmith99");

        assertNotNull(error);
    }

    @Test
    public void passwordContainsDisplayNameShouldReject() {
        PolicyError error = provider.validate(null, user(null, null, "Jane Doe"), "jane doe 2026");

        assertNotNull(error);
    }

    @Test
    public void passwordDoesNotContainAnyNameShouldAccept() {
        PolicyError error = provider.validate(null, user("Alice", "Smith", null), "Tr0ub4dor&3");

        assertNull(error);
    }

    @Test
    public void nullFirstNameShouldAcceptWhenPasswordDoesNotContainOtherNames() {
        PolicyError error = provider.validate(null, user(null, "Smith", null), "nullfirstname!");

        assertNull(error);
    }

    @Test
    public void shortNameBelowThresholdShouldAccept() {
        PolicyError error = provider.validate(null, user("Ed", null, null), "education123");

        assertNull(error);
    }

    @Test
    public void nameExactlyAtThresholdShouldBeChecked() {
        PolicyError error = provider.validate(null, user("Bob", null, null), "BOB_the_builder");

        assertNotNull(error);
    }

    @Test
    public void nullPasswordShouldAccept() {
        PolicyError error = provider.validate(null, user("Alice", "Smith", null), null);

        assertNull(error);
    }

    @Test
    public void usernameOnlyValidationShouldAccept() {
        PolicyError error = provider.validate("alice", "alice");

        assertNull(error);
    }

    private static UserModel user(String firstName, String lastName, String displayName) {
        return (UserModel) Proxy.newProxyInstance(
                NotContainsNamePasswordPolicyProviderTest.class.getClassLoader(),
                new Class<?>[] { UserModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getFirstName" -> firstName;
                    case "getLastName" -> lastName;
                    case "getFirstAttribute" -> "displayName".equals(args[0]) ? displayName : null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == void.class || !type.isPrimitive()) {
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
