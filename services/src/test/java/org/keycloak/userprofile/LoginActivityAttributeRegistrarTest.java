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
package org.keycloak.userprofile;

import org.junit.Assert;
import org.junit.Test;

public class LoginActivityAttributeRegistrarTest {

    @Test
    public void simpleClientIdKeepsExactAuditorCompatibleName() {
        // Plain alphanumeric client IDs are the common case and must stay byte-for-byte identical to
        // what the community keycloak-auditor plugin already writes, so existing deployments need no migration.
        Assert.assertEquals("aud_usr_last-login_my-client",
                LoginActivityAttributeRegistrar.perClientAttributeName("my-client"));
        Assert.assertEquals("aud_usr_last-login_my.client_01",
                LoginActivityAttributeRegistrar.perClientAttributeName("my.client_01"));
    }

    @Test
    public void urlLikeClientIdIsSanitizedInsteadOfProducingAnInvalidAttributeName() {
        // SAML entity IDs/URLs contain ':' and '/', which UPConfigUtils.ATTRIBUTE_NAME_PATTERN rejects.
        // Registering such a name used to make the whole realm's user-profile config invalid, breaking
        // logins for every client in the realm - see https://github.com/keycloak/keycloak/pull/55586.
        String name = LoginActivityAttributeRegistrar.perClientAttributeName("http://localhost:8280/sales-post/");

        Assert.assertTrue(name.matches("aud_usr_last-login_[a-zA-Z0-9._-]+"));
    }

    @Test
    public void sanitizedNameIsStableAcrossCalls() {
        String clientId = "http://localhost:8280/sales-post/";

        Assert.assertEquals(LoginActivityAttributeRegistrar.perClientAttributeName(clientId),
                LoginActivityAttributeRegistrar.perClientAttributeName(clientId));
    }

    @Test
    public void differentUnsafeClientIdsDoNotCollideAfterSanitization() {
        // "a/b" and "a:b" both sanitize their single unsafe character down to the identical "a_b"
        // prefix, so without a uniqueness suffix they'd collide and share one "last login" attribute.
        String first = LoginActivityAttributeRegistrar.perClientAttributeName("a/b");
        String second = LoginActivityAttributeRegistrar.perClientAttributeName("a:b");

        Assert.assertNotEquals(first, second);
    }
}
