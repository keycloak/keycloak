/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.junit.Assert;
import org.junit.Test;


public class NotEmailPasswordPolicyProviderTest {

    private final PasswordPolicyContext context = PasswordPolicyContext.builder()
            .username("username")
            .email("email")
            .build();

    @Test
    public void testUsernameValidationIsNoOp() {
        Assert.assertNull(new NotEmailPasswordPolicyProvider(null).validate("username", "username"));
    }

    @Test
    public void test() {
        Assert.assertNull(new NotEmailPasswordPolicyProvider(null).validate(context, "password"));
    }

    @Test
    public void testPasswordEqualsUsername() {
        Assert.assertNull(new NotEmailPasswordPolicyProvider(null).validate(context, "username"));
    }

    @Test
    public void testNullEmail() {
        Assert.assertNull(new NotEmailPasswordPolicyProvider(null).validate(PasswordPolicyContext.builder().username("username").build(), "username"));
    }

    @Test
    public void testEquals() {
        Assert.assertNotNull(new NotEmailPasswordPolicyProvider(null).validate(context, "email"));
    }

    @Test
    public void testEqualsIgnoreCase() {
        Assert.assertNotNull(new NotEmailPasswordPolicyProvider(null).validate(context, "EMAIL"));
    }
}
